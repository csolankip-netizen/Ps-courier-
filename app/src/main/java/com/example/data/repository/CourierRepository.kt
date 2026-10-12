package com.example.data.repository

import com.example.data.api.CourierApiClient
import com.example.data.local.CourierDao
import com.example.data.local.SavedAddressEntity
import com.example.data.local.ShipmentEntity
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.Address
import com.example.data.model.CarrierPartner
import com.example.data.model.DeliveryZone
import com.example.data.model.ParcelCategory
import com.example.data.model.ParcelDetails
import com.example.data.model.PaymentMode
import com.example.data.model.PsRateCalculation
import com.example.data.model.ShipmentBooking
import com.example.data.model.ShipmentStatus
import com.example.data.model.TrackingCheckpoint
import com.example.data.model.TransactionType
import com.example.data.model.WalletTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CourierRepository(
    private val courierDao: CourierDao,
    val apiClient: CourierApiClient = CourierApiClient()
) {

    val allShipments: Flow<List<ShipmentBooking>> = courierDao.getAllShipments().map { list ->
        list.map { it.toDomain() }
    }

    val savedAddresses: Flow<List<SavedAddressEntity>> = courierDao.getSavedAddresses()

    val walletTransactions: Flow<List<WalletTransaction>> = courierDao.getAllWalletTransactions().map { list ->
        list.map { it.toDomain() }
    }

    fun getShipmentById(id: String): Flow<ShipmentBooking?> = courierDao.getShipmentById(id).map { it?.toDomain() }

    fun getShipmentByAwbFlow(awb: String): Flow<ShipmentBooking?> = courierDao.getShipmentByAwbFlow(awb).map { it?.toDomain() }

    suspend fun findShipmentByAwb(awb: String): ShipmentBooking? {
        return courierDao.findShipmentByAwb(awb)?.toDomain()
    }

    suspend fun calculateRate(
        originPin: String,
        destPin: String,
        parcel: ParcelDetails,
        paymentMode: PaymentMode,
        forceShadowfaxUnavailable: Boolean = false
    ): PsRateCalculation {
        return apiClient.calculatePsCourierRate(
            originPin, destPin, parcel, paymentMode, forceShadowfaxUnavailable
        )
    }

    suspend fun bookParcel(
        sender: Address,
        receiver: Address,
        parcel: ParcelDetails,
        paymentMode: PaymentMode,
        rateInfo: PsRateCalculation,
        pickupSlot: String,
        razorpayPaymentId: String? = null,
        razorpayOrderId: String? = null
    ): ShipmentBooking {
        val (awb, routingCode) = apiClient.bookShipment(
            rateInfo.carrierAssigned, sender, receiver, parcel, paymentMode, rateInfo
        )

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val bookingDate = dateFormat.format(Date())

        val cal = java.util.Calendar.getInstance()
        val daysToAdd = if (rateInfo.zone == DeliveryZone.ZONE_A) 1 else 3
        cal.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
        val expectedDate = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(cal.time)

        val initialCheckpoints = apiClient.getInitialTrackingEvents(
            rateInfo.carrierAssigned,
            sender.city.ifBlank { "Origin" },
            receiver.city.ifBlank { "Destination" },
            awb
        )

        val id = "PS-" + UUID.randomUUID().toString().take(8).uppercase()

        val booking = ShipmentBooking(
            id = id,
            awbNumber = awb,
            carrierAssigned = rateInfo.carrierAssigned,
            sender = sender,
            receiver = receiver,
            parcel = parcel,
            rateInfo = rateInfo,
            paymentMode = paymentMode,
            isPaid = paymentMode != PaymentMode.COD,
            status = ShipmentStatus.PICKUP_SCHEDULED,
            bookingDate = bookingDate,
            expectedDeliveryDate = expectedDate,
            pickupSlot = pickupSlot,
            routingCode = routingCode,
            trackingEvents = initialCheckpoints,
            razorpayPaymentId = razorpayPaymentId,
            razorpayOrderId = razorpayOrderId
        )

        courierDao.insertShipment(booking.toEntity())

        // If paid via in-app wallet, deduct balance and record transaction
        if (paymentMode == PaymentMode.WALLET) {
            val currentBal = courierDao.getLatestWalletBalance() ?: 2500.0
            val newBal = maxOf(0.0, currentBal - rateInfo.totalPayable)
            val tx = WalletTransactionEntity(
                id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
                type = TransactionType.SHIPMENT_BOOKING.name,
                amount = rateInfo.totalPayable,
                description = "Freight booking for $awb (${rateInfo.zone.code} - ${rateInfo.carrierAssigned.displayName})",
                referenceAwb = awb,
                timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
                balanceAfter = newBal
            )
            courierDao.insertTransaction(tx)
        }

        return booking
    }

    suspend fun topUpWallet(amount: Double, razorpayPaymentId: String? = null) {
        val currentBal = courierDao.getLatestWalletBalance() ?: 2500.0
        val newBal = currentBal + amount
        val rzpNote = if (razorpayPaymentId != null) " (Razorpay: $razorpayPaymentId)" else ""
        val tx = WalletTransactionEntity(
            id = "TXN-" + UUID.randomUUID().toString().take(8).uppercase(),
            type = TransactionType.WALLET_TOPUP.name,
            amount = amount,
            description = "Prepaid wallet recharge via Razorpay Gateway$rzpNote",
            referenceAwb = razorpayPaymentId,
            timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
            balanceAfter = newBal
        )
        courierDao.insertTransaction(tx)
    }

    suspend fun addTrackingMilestone(shipment: ShipmentBooking, newStatus: ShipmentStatus, location: String, desc: String) {
        val updatedEvents = shipment.trackingEvents.toMutableList()
        val timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        updatedEvents.add(
            0,
            TrackingCheckpoint(
                timestamp = timestamp,
                location = location,
                status = newStatus.label,
                description = desc,
                isDone = true
            )
        )
        val eventsJson = serializeCheckpoints(updatedEvents)
        courierDao.updateShipmentStatus(shipment.id, newStatus.name, eventsJson)
    }

    // Address Book methods
    suspend fun saveAddress(address: SavedAddressEntity) {
        if (address.isDefault) {
            courierDao.clearDefaultAddresses()
        }
        courierDao.insertAddress(address)
    }

    suspend fun deleteAddress(id: Long) {
        courierDao.deleteAddress(id)
    }

    suspend fun setDefaultAddress(id: Long) {
        courierDao.clearDefaultAddresses()
        courierDao.setDefaultAddress(id)
    }

    suspend fun seedInitialDataIfEmpty() {
        // Seed Initial Wallet Transactions if none exist
        val existingTxns = courierDao.getAllWalletTransactions().first()
        if (existingTxns.isEmpty()) {
            val initialBalance = 2500.0
            courierDao.insertTransaction(
                WalletTransactionEntity(
                    id = "TXN-INIT-001",
                    type = TransactionType.WALLET_TOPUP.name,
                    amount = 2500.0,
                    description = "Opening Wallet Balance (Welcome Credit)",
                    referenceAwb = null,
                    timestamp = "10 Oct, 09:00 AM",
                    balanceAfter = initialBalance
                )
            )
            courierDao.insertTransaction(
                WalletTransactionEntity(
                    id = "TXN-COD-9821",
                    type = TransactionType.COD_REMITTANCE.name,
                    amount = 1450.0,
                    description = "COD Settlement Payout (Friday Cycle)",
                    referenceAwb = "SFX82947105",
                    timestamp = "Yesterday, 05:30 PM",
                    balanceAfter = 3950.0
                )
            )
            courierDao.insertTransaction(
                WalletTransactionEntity(
                    id = "TXN-FRT-1092",
                    type = TransactionType.SHIPMENT_BOOKING.name,
                    amount = 118.0,
                    description = "Freight booking for EM482910471IN (Zone C/D - Indian Post)",
                    referenceAwb = "EM482910471IN",
                    timestamp = "Yesterday, 11:30 AM",
                    balanceAfter = 3832.0
                )
            )
        }

        // Seed Address Book
        val existingAddresses = courierDao.getSavedAddresses().first()
        if (existingAddresses.isEmpty()) {
            courierDao.insertAddress(
                SavedAddressEntity(
                    name = "PS Courier Central Hub",
                    phone = "9820192834",
                    email = "hub@pscourier.in",
                    street = "Plot 42, Okhla Industrial Area Phase 3",
                    landmark = "Near Metro Station",
                    city = "New Delhi",
                    state = "Delhi",
                    pincode = "110020",
                    tag = "Warehouse",
                    isDefault = true
                )
            )
            courierDao.insertAddress(
                SavedAddressEntity(
                    name = "Priya Nair",
                    phone = "9876543210",
                    email = "priya.nair@gmail.com",
                    street = "Flat 402, Green Glen Layout, Bellandur",
                    landmark = "Opposite Cloudnine",
                    city = "Bengaluru",
                    state = "Karnataka",
                    pincode = "560038",
                    tag = "Customer",
                    isDefault = false
                )
            )
            courierDao.insertAddress(
                SavedAddressEntity(
                    name = "Vikram Logistics Point",
                    phone = "9988776655",
                    email = "dispatch.hyd@pscourier.in",
                    street = "Road 36, Jubilee Hills",
                    landmark = "Checkpost",
                    city = "Hyderabad",
                    state = "Telangana",
                    pincode = "500033",
                    tag = "Office",
                    isDefault = false
                )
            )
        }

        // Seed Sample Shipments showing Shadowfax & Indian Post Fallback
        val existingShipments = courierDao.getAllShipments().first()
        if (existingShipments.isEmpty()) {
            // 1. Primary partner: Shadowfax (Zone A / Intra-City)
            val sfxShipment = ShipmentBooking(
                id = "PS-SFX-9182",
                awbNumber = "SFX82947105",
                carrierAssigned = CarrierPartner.SHADOWFAX,
                sender = Address(
                    name = "PS Courier Bandra Depot",
                    phone = "9820192834",
                    street = "Bandra Kurla Complex",
                    city = "Mumbai",
                    state = "Maharashtra",
                    pincode = "400051"
                ),
                receiver = Address(
                    name = "Sameer Joshi",
                    phone = "9819283746",
                    street = "Link Road, Andheri West",
                    city = "Mumbai",
                    state = "Maharashtra",
                    pincode = "400053"
                ),
                parcel = ParcelDetails(
                    category = ParcelCategory.CLOTHING,
                    weightKg = 0.8,
                    lengthCm = 25.0,
                    widthCm = 20.0,
                    heightCm = 5.0,
                    declaredValue = 1450.0,
                    description = "Garment Package",
                    isFragile = false
                ),
                rateInfo = PsRateCalculation(
                    zone = DeliveryZone.ZONE_A,
                    baseDeliveryCharge = 80.0,
                    taxableBaseFreight = 67.80,
                    codCharge = 50.0,
                    taxableCodBase = 42.37,
                    rtoCharge = 80.0,
                    gst = 19.83,
                    totalPayable = 130.0,
                    carrierAssigned = CarrierPartner.SHADOWFAX,
                    isShadowfaxServiceable = true,
                    routingNote = "Shadowfax Express Serviceable: Assigned as Primary Partner for fast intra-city delivery."
                ),
                paymentMode = PaymentMode.COD,
                isPaid = false,
                status = ShipmentStatus.OUT_FOR_DELIVERY,
                bookingDate = "Yesterday, 04:00 PM",
                expectedDeliveryDate = "Today, by 4:00 PM",
                pickupSlot = "Completed",
                routingCode = "SFX-BOM-AND-01",
                trackingEvents = listOf(
                    TrackingCheckpoint(
                        timestamp = "Today, 08:30 AM",
                        location = "Andheri Delivery Station, Mumbai",
                        status = "Out for Delivery (Shadowfax)",
                        description = "Rider assigned for doorstep delivery. Collect Cash COD: ₹1,450."
                    ),
                    TrackingCheckpoint(
                        timestamp = "Yesterday, 08:00 PM",
                        location = "Bandra Hub, Mumbai",
                        status = "Picked Up & Inbounded",
                        description = "Package received from sender depot."
                    )
                )
            )
            courierDao.insertShipment(sfxShipment.toEntity())

            // 2. Fallback partner: Indian Post (Zone E / Special remote destination where Shadowfax was unserviceable)
            val inpShipment = ShipmentBooking(
                id = "PS-INP-7412",
                awbNumber = "EM482910471IN",
                carrierAssigned = CarrierPartner.INDIAN_POST,
                sender = Address(
                    name = "PS Central Hub Delhi",
                    phone = "9820192834",
                    street = "Okhla Industrial Area",
                    city = "New Delhi",
                    state = "Delhi",
                    pincode = "110020"
                ),
                receiver = Address(
                    name = "Biren Das",
                    phone = "9435012345",
                    street = "GS Road, Christian Basti",
                    city = "Guwahati",
                    state = "Assam",
                    pincode = "781001"
                ),
                parcel = ParcelDetails(
                    category = ParcelCategory.DOCUMENTS,
                    weightKg = 0.4,
                    lengthCm = 30.0,
                    widthCm = 22.0,
                    heightCm = 2.0,
                    declaredValue = 500.0,
                    description = "Confidential Contracts",
                    isFragile = false
                ),
                rateInfo = PsRateCalculation(
                    zone = DeliveryZone.ZONE_E,
                    baseDeliveryCharge = 120.0,
                    taxableBaseFreight = 101.69,
                    codCharge = 0.0,
                    taxableCodBase = 0.0,
                    rtoCharge = 120.0,
                    gst = 18.31,
                    totalPayable = 120.0,
                    carrierAssigned = CarrierPartner.INDIAN_POST,
                    isShadowfaxServiceable = false,
                    routingNote = "Shadowfax Unserviceable: Automatically routed via Indian Post (Speed Post) for guaranteed remote delivery."
                ),
                paymentMode = PaymentMode.WALLET,
                isPaid = true,
                status = ShipmentStatus.IN_TRANSIT,
                bookingDate = "Yesterday, 11:30 AM",
                expectedDeliveryDate = "Tomorrow, by 5:00 PM",
                pickupSlot = "Completed",
                routingCode = "INP-781-NSH-01",
                trackingEvents = listOf(
                    TrackingCheckpoint(
                        timestamp = "Today, 06:15 AM",
                        location = "Guwahati NSH Sorting Hub, Assam",
                        status = "Item Received at National Sorting Hub (Indian Post)",
                        description = "Air mail consignment opened and sorted to Christian Basti Delivery Office."
                    ),
                    TrackingCheckpoint(
                        timestamp = "Yesterday, 09:40 PM",
                        location = "Delhi Air Transit Center",
                        status = "Dispatched via Air Mail",
                        description = "Despatched on flight 6E-205 to Guwahati NSH."
                    )
                )
            )
            courierDao.insertShipment(inpShipment.toEntity())
        }
    }

    private fun ShipmentBooking.toEntity(): ShipmentEntity {
        return ShipmentEntity(
            id = id,
            awbNumber = awbNumber,
            carrierAssigned = carrierAssigned.name,
            senderName = sender.name,
            senderPhone = sender.phone,
            senderStreet = sender.street,
            senderCity = sender.city,
            senderState = sender.state,
            senderPincode = sender.pincode,
            receiverName = receiver.name,
            receiverPhone = receiver.phone,
            receiverStreet = receiver.street,
            receiverCity = receiver.city,
            receiverState = receiver.state,
            receiverPincode = receiver.pincode,
            parcelCategory = parcel.category.name,
            weightKg = parcel.weightKg,
            lengthCm = parcel.lengthCm,
            widthCm = parcel.widthCm,
            heightCm = parcel.heightCm,
            declaredValue = parcel.declaredValue,
            parcelDescription = parcel.description,
            isFragile = parcel.isFragile,
            zoneCode = rateInfo.zone.name,
            baseDeliveryCharge = rateInfo.baseDeliveryCharge,
            codCharge = rateInfo.codCharge,
            rtoCharge = rateInfo.rtoCharge,
            gst = rateInfo.gst,
            totalPayable = rateInfo.totalPayable,
            isShadowfaxServiceable = rateInfo.isShadowfaxServiceable,
            routingNote = rateInfo.routingNote,
            paymentMode = paymentMode.name,
            isPaid = isPaid,
            status = status.name,
            bookingDate = bookingDate,
            expectedDeliveryDate = expectedDeliveryDate,
            pickupSlot = pickupSlot,
            routingCode = routingCode,
            trackingEventsJson = serializeCheckpoints(trackingEvents),
            razorpayPaymentId = razorpayPaymentId,
            razorpayOrderId = razorpayOrderId
        )
    }

    private fun ShipmentEntity.toDomain(): ShipmentBooking {
        val carrier = try {
            CarrierPartner.valueOf(carrierAssigned)
        } catch (_: Exception) {
            CarrierPartner.SHADOWFAX
        }

        val zone = try {
            DeliveryZone.valueOf(zoneCode)
        } catch (_: Exception) {
            DeliveryZone.ZONE_A
        }

        val category = try {
            ParcelCategory.valueOf(parcelCategory)
        } catch (_: Exception) {
            ParcelCategory.GENERAL
        }

        val mode = try {
            PaymentMode.valueOf(paymentMode)
        } catch (_: Exception) {
            PaymentMode.WALLET
        }

        val stat = try {
            ShipmentStatus.valueOf(status)
        } catch (_: Exception) {
            ShipmentStatus.BOOKED
        }

        return ShipmentBooking(
            id = id,
            awbNumber = awbNumber,
            carrierAssigned = carrier,
            sender = Address(
                name = senderName,
                phone = senderPhone,
                street = senderStreet,
                city = senderCity,
                state = senderState,
                pincode = senderPincode
            ),
            receiver = Address(
                name = receiverName,
                phone = receiverPhone,
                street = receiverStreet,
                city = receiverCity,
                state = receiverState,
                pincode = receiverPincode
            ),
            parcel = ParcelDetails(
                category = category,
                weightKg = weightKg,
                lengthCm = lengthCm,
                widthCm = widthCm,
                heightCm = heightCm,
                declaredValue = declaredValue,
                description = parcelDescription,
                isFragile = isFragile
            ),
            rateInfo = PsRateCalculation(
                zone = zone,
                baseDeliveryCharge = baseDeliveryCharge,
                taxableBaseFreight = (baseDeliveryCharge / 1.18 * 100).toInt() / 100.0,
                codCharge = codCharge,
                taxableCodBase = if (codCharge > 0) (codCharge / 1.18 * 100).toInt() / 100.0 else 0.0,
                rtoCharge = rtoCharge,
                gst = gst,
                totalPayable = totalPayable,
                carrierAssigned = carrier,
                isShadowfaxServiceable = isShadowfaxServiceable,
                routingNote = routingNote
            ),
            paymentMode = mode,
            isPaid = isPaid,
            status = stat,
            bookingDate = bookingDate,
            expectedDeliveryDate = expectedDeliveryDate,
            pickupSlot = pickupSlot,
            routingCode = routingCode,
            trackingEvents = deserializeCheckpoints(trackingEventsJson),
            razorpayPaymentId = razorpayPaymentId,
            razorpayOrderId = razorpayOrderId
        )
    }

    private fun WalletTransactionEntity.toDomain(): WalletTransaction {
        val t = try {
            TransactionType.valueOf(type)
        } catch (_: Exception) {
            TransactionType.WALLET_TOPUP
        }
        return WalletTransaction(
            id = id,
            type = t,
            amount = amount,
            description = description,
            referenceAwb = referenceAwb,
            timestamp = timestamp,
            balanceAfter = balanceAfter
        )
    }

    private fun serializeCheckpoints(events: List<TrackingCheckpoint>): String {
        val jsonArray = JSONArray()
        events.forEach { event ->
            val obj = JSONObject().apply {
                put("timestamp", event.timestamp)
                put("location", event.location)
                put("status", event.status)
                put("description", event.description)
                put("isDone", event.isDone)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun deserializeCheckpoints(json: String): List<TrackingCheckpoint> {
        if (json.isBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            val list = mutableListOf<TrackingCheckpoint>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TrackingCheckpoint(
                        timestamp = obj.optString("timestamp", ""),
                        location = obj.optString("location", ""),
                        status = obj.optString("status", ""),
                        description = obj.optString("description", ""),
                        isDone = obj.optBoolean("isDone", true)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
