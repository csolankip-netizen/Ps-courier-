package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DeliveryZone(
    val code: String,
    val title: String,
    val description: String,
    val flatRate: Double
) {
    ZONE_A("Zone A", "Intra-City / Local", "Pickup and delivery within the same city", 80.0),
    ZONE_B("Zone B", "Intra-State / Regional", "Within the same state / regional cluster", 90.0),
    ZONE_C_D("Zone C/D", "Metro-to-Metro / Rest of India", "Between major metros and interstate routes", 100.0),
    ZONE_E("Zone E", "Special / North East / J&K", "Remote areas, hills, islands, and North East states", 120.0);

    val rtoCharge: Double
        get() = flatRate // Return or RTO parcels cost the same as the original delivery rate
}

enum class CarrierPartner(
    val id: String,
    val displayName: String,
    val typeName: String,
    val priorityRank: Int
) {
    SHADOWFAX("SHADOWFAX", "Shadowfax Logistics", "Primary Express Partner", 1),
    INDIAN_POST("INDIAN_POST", "Indian Post (Speed Post)", "Guaranteed National Fallback", 2)
}

data class Address(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val street: String = "",
    val landmark: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val tag: String = "Warehouse", // "Warehouse", "Office", "Home"
    val isDefault: Boolean = false
) {
    val fullAddress: String
        get() = listOfNotNull(
            street.takeIf { it.isNotBlank() },
            landmark.takeIf { it.isNotBlank() },
            "$city, $state - $pincode".takeIf { city.isNotBlank() || pincode.isNotBlank() }
        ).joinToString(", ")
}

enum class ParcelCategory(val label: String) {
    GENERAL("General Merchandise"),
    DOCUMENTS("Documents & Papers"),
    ELECTRONICS("Consumer Electronics"),
    CLOTHING("Apparel & Garments"),
    MEDICINE("Healthcare & Meds"),
    FRAGILE("Fragile & Glassware")
}

data class ParcelDetails(
    val category: ParcelCategory = ParcelCategory.GENERAL,
    val weightKg: Double = 0.5,
    val lengthCm: Double = 20.0,
    val widthCm: Double = 15.0,
    val heightCm: Double = 10.0,
    val declaredValue: Double = 500.0,
    val description: String = "Commercial Goods",
    val isFragile: Boolean = false
) {
    val volumetricWeightKg: Double
        get() = (lengthCm * widthCm * heightCm) / 5000.0

    val chargeableWeightKg: Double
        get() = maxOf(weightKg, volumetricWeightKg)
}

enum class PaymentMode(val title: String, val subtitle: String) {
    WALLET("PS Courier Wallet", "Instant deduction from prepaid balance"),
    RAZORPAY("Razorpay Gateway", "UPI (GPay/PhonePe/Paytm), Cards & Net Banking"),
    COD("Cash on Delivery (COD)", "₹50 COD fee applies • Collected at doorstep")
}

data class PsRateCalculation(
    val zone: DeliveryZone,
    val baseDeliveryCharge: Double, // Inclusive rate: Zone A ₹80, Zone B ₹90, Zone C/D ₹100, Zone E ₹120
    val taxableBaseFreight: Double, // Freight value before 18% GST
    val codCharge: Double,          // Inclusive COD fee: ₹50.0 (or ₹0.0)
    val taxableCodBase: Double,     // COD fee value before 18% GST
    val rtoCharge: Double,          // Equal to baseDeliveryCharge (inclusive)
    val gst: Double,                // Embedded 18% GST amount
    val totalPayable: Double,       // Exact flat total payable (Inclusive of 18% GST)
    val carrierAssigned: CarrierPartner,
    val isShadowfaxServiceable: Boolean,
    val routingNote: String
)

enum class ShipmentStatus(val label: String, val stepIndex: Int) {
    BOOKED("Order Booked", 0),
    PICKUP_SCHEDULED("Pickup Scheduled", 1),
    PICKED_UP("Picked Up by Courier", 2),
    IN_TRANSIT("In Transit (Hub Sorting)", 3),
    OUT_FOR_DELIVERY("Out for Delivery", 4),
    DELIVERED("Delivered", 5),
    RTO_INITIATED("RTO Initiated (Return to Origin)", 6),
    RETURNED_TO_ORIGIN("Returned to Origin (RTO)", 7),
    CANCELLED("Cancelled", -1)
}

data class TrackingCheckpoint(
    val timestamp: String,
    val location: String,
    val status: String,
    val description: String,
    val isDone: Boolean = true
)

data class ShipmentBooking(
    val id: String,
    val awbNumber: String,
    val carrierAssigned: CarrierPartner,
    val sender: Address,
    val receiver: Address,
    val parcel: ParcelDetails,
    val rateInfo: PsRateCalculation,
    val paymentMode: PaymentMode,
    val isPaid: Boolean,
    val status: ShipmentStatus,
    val bookingDate: String,
    val expectedDeliveryDate: String,
    val pickupSlot: String = "Tomorrow 10:00 AM - 1:00 PM",
    val routingCode: String,
    val trackingEvents: List<TrackingCheckpoint>,
    val razorpayPaymentId: String? = null,
    val razorpayOrderId: String? = null
)

enum class TransactionType(val label: String, val isCredit: Boolean) {
    WALLET_TOPUP("Wallet Top-Up", true),
    COD_REMITTANCE("COD Payout Settled", true),
    SHIPMENT_BOOKING("Parcel Freight Paid", false),
    RTO_CHARGE("RTO Return Shipping Fee", false)
}

data class WalletTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val description: String,
    val referenceAwb: String? = null,
    val timestamp: String,
    val balanceAfter: Double
)

object SettlementHelper {
    // Settlements occur 3 times a week: Monday, Wednesday, Friday
    fun getNextSettlementSchedule(): Triple<String, String, String> {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val nextDay = when (dayOfWeek) {
            Calendar.SATURDAY, Calendar.SUNDAY -> "Monday"
            Calendar.MONDAY, Calendar.TUESDAY -> "Wednesday"
            Calendar.WEDNESDAY, Calendar.THURSDAY -> "Friday"
            Calendar.FRIDAY -> "Monday"
            else -> "Wednesday"
        }

        val cycleText = "3 Times / Week (Mon, Wed, Fri)"
        val payoutNote = "All collected COD orders up to 48 hrs prior are disbursed to registered bank accounts."
        return Triple(nextDay, cycleText, payoutNote)
    }
}

object PincodeDirectory {
    data class PincodeInfo(
        val city: String,
        val state: String,
        val regionZone: String, // "North", "South", "East", "West", "North East", "Special"
        val isShadowfaxServiceable: Boolean = true
    )

    private val directory = mapOf(
        "110001" to PincodeInfo("New Delhi", "Delhi", "North", isShadowfaxServiceable = true),
        "110020" to PincodeInfo("New Delhi", "Delhi", "North", isShadowfaxServiceable = true),
        "400001" to PincodeInfo("Mumbai", "Maharashtra", "West", isShadowfaxServiceable = true),
        "400050" to PincodeInfo("Bandra Mumbai", "Maharashtra", "West", isShadowfaxServiceable = true),
        "560001" to PincodeInfo("Bengaluru", "Karnataka", "South", isShadowfaxServiceable = true),
        "560038" to PincodeInfo("Indiranagar Bengaluru", "Karnataka", "South", isShadowfaxServiceable = true),
        "600001" to PincodeInfo("Chennai", "Tamil Nadu", "South", isShadowfaxServiceable = true),
        "700001" to PincodeInfo("Kolkata", "West Bengal", "East", isShadowfaxServiceable = true),
        "500001" to PincodeInfo("Hyderabad", "Telangana", "South", isShadowfaxServiceable = true),
        "380001" to PincodeInfo("Ahmedabad", "Gujarat", "West", isShadowfaxServiceable = true),
        "411001" to PincodeInfo("Pune", "Maharashtra", "West", isShadowfaxServiceable = true),
        "302001" to PincodeInfo("Jaipur", "Rajasthan", "North", isShadowfaxServiceable = true),
        "226001" to PincodeInfo("Lucknow", "Uttar Pradesh", "North", isShadowfaxServiceable = true),
        "682001" to PincodeInfo("Kochi", "Kerala", "South", isShadowfaxServiceable = true),
        // Remote / Special Zones - Shadowfax marked false to trigger Indian Post fallback!
        "781001" to PincodeInfo("Guwahati", "Assam", "North East", isShadowfaxServiceable = false),
        "795001" to PincodeInfo("Imphal", "Manipur", "North East", isShadowfaxServiceable = false),
        "190001" to PincodeInfo("Srinagar", "Jammu & Kashmir", "Special", isShadowfaxServiceable = false),
        "194101" to PincodeInfo("Leh Ladakh", "Ladakh", "Special", isShadowfaxServiceable = false),
        "744101" to PincodeInfo("Port Blair", "Andaman & Nicobar", "Special", isShadowfaxServiceable = false)
    )

    fun lookup(pincode: String): PincodeInfo? = directory[pincode.trim()]

    fun getCityState(pincode: String): Pair<String, String>? {
        val info = lookup(pincode) ?: return null
        return Pair(info.city, info.state)
    }

    /**
     * Determines the PS Courier Delivery Zone based on Origin and Destination PINs:
     * - Zone A: Intra-City / Local (Same first 3 digits of PIN or same city) -> ₹80
     * - Zone B: Intra-State / Regional (Same state) -> ₹90
     * - Zone C/D: Metro to Metro / Rest of India -> ₹100
     * - Zone E: Special / North East / J&K / Remote -> ₹120
     */
    fun resolveZone(originPin: String, destPin: String): DeliveryZone {
        val originInfo = lookup(originPin)
        val destInfo = lookup(destPin)

        val isOriginSpecial = originInfo?.regionZone in listOf("North East", "Special")
        val isDestSpecial = destInfo?.regionZone in listOf("North East", "Special")

        if (isOriginSpecial || isDestSpecial) {
            return DeliveryZone.ZONE_E
        }

        val sameCity = (originPin.take(3) == destPin.take(3)) || (originInfo?.city == destInfo?.city && originInfo?.city != null)
        if (sameCity) {
            return DeliveryZone.ZONE_A
        }

        val sameState = (originInfo?.state == destInfo?.state && originInfo?.state != null)
        if (sameState) {
            return DeliveryZone.ZONE_B
        }

        return DeliveryZone.ZONE_C_D
    }

    /**
     * Priority Routing Rule:
     * First checks Shadowfax for serviceability.
     * If Shadowfax is unavailable/unserviceable, falls back to Indian Post!
     */
    fun checkServiceabilityAndCarrier(
        originPin: String,
        destPin: String,
        forceShadowfaxUnavailable: Boolean = false
    ): Pair<CarrierPartner, Boolean> {
        if (forceShadowfaxUnavailable) {
            return Pair(CarrierPartner.INDIAN_POST, false)
        }

        val originInfo = lookup(originPin)
        val destInfo = lookup(destPin)

        val originSfx = originInfo?.isShadowfaxServiceable ?: true
        val destSfx = destInfo?.isShadowfaxServiceable ?: true

        return if (originSfx && destSfx) {
            Pair(CarrierPartner.SHADOWFAX, true)
        } else {
            Pair(CarrierPartner.INDIAN_POST, false)
        }
    }
}
