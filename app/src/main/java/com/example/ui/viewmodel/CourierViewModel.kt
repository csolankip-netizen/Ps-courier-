package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiLogEntry
import com.example.data.local.AppDatabase
import com.example.data.local.SavedAddressEntity
import com.example.data.model.Address
import com.example.data.model.CarrierPartner
import com.example.data.model.DeliveryZone
import com.example.data.model.ParcelCategory
import com.example.data.model.ParcelDetails
import com.example.data.model.PaymentMode
import com.example.data.model.PincodeDirectory
import com.example.data.model.PsRateCalculation
import com.example.data.model.SettlementHelper
import com.example.data.model.ShipmentBooking
import com.example.data.model.ShipmentStatus
import com.example.data.model.WalletTransaction
import com.example.data.repository.CourierRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourierViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val repository = CourierRepository(database.courierDao())

    val allShipments: StateFlow<List<ShipmentBooking>> = repository.allShipments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedAddresses: StateFlow<List<SavedAddressEntity>> = repository.savedAddresses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletTransactions: StateFlow<List<WalletTransaction>> = repository.walletTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletBalance: StateFlow<Double> = repository.walletTransactions
        .map { txns -> txns.firstOrNull()?.balanceAfter ?: 2500.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2500.0)

    // --- Booking State ---
    private val _bookingStep = MutableStateFlow(1) // 1: Addresses, 2: Parcel, 3: Rates & Routing, 4: Payment, 5: Confirmed
    val bookingStep: StateFlow<Int> = _bookingStep.asStateFlow()

    private val _senderAddress = MutableStateFlow(
        Address(
            name = "PS Courier Central Hub",
            phone = "9820192834",
            email = "hub@pscourier.in",
            street = "Plot 42, Okhla Industrial Area Phase 3",
            city = "New Delhi",
            state = "Delhi",
            pincode = "110020",
            tag = "Warehouse"
        )
    )
    val senderAddress: StateFlow<Address> = _senderAddress.asStateFlow()

    private val _receiverAddress = MutableStateFlow(
        Address(
            name = "Priya Nair",
            phone = "9876543210",
            email = "priya.nair@gmail.com",
            street = "Flat 402, Green Glen Layout, Bellandur",
            city = "Bengaluru",
            state = "Karnataka",
            pincode = "560038",
            tag = "Customer"
        )
    )
    val receiverAddress: StateFlow<Address> = _receiverAddress.asStateFlow()

    private val _parcelDetails = MutableStateFlow(ParcelDetails())
    val parcelDetails: StateFlow<ParcelDetails> = _parcelDetails.asStateFlow()

    private val _selectedPaymentMode = MutableStateFlow(PaymentMode.WALLET)
    val selectedPaymentMode: StateFlow<PaymentMode> = _selectedPaymentMode.asStateFlow()

    private val _pickupSlot = MutableStateFlow("Tomorrow 10:00 AM - 1:00 PM")
    val pickupSlot: StateFlow<String> = _pickupSlot.asStateFlow()

    // Serviceability & Routing state
    private val _forceShadowfaxUnavailable = MutableStateFlow(false)
    val forceShadowfaxUnavailable: StateFlow<Boolean> = _forceShadowfaxUnavailable.asStateFlow()

    private val _calculatedRate = MutableStateFlow<PsRateCalculation?>(null)
    val calculatedRate: StateFlow<PsRateCalculation?> = _calculatedRate.asStateFlow()

    private val _isLoadingRate = MutableStateFlow(false)
    val isLoadingRate: StateFlow<Boolean> = _isLoadingRate.asStateFlow()

    private val _isBookingInProgress = MutableStateFlow(false)
    val isBookingInProgress: StateFlow<Boolean> = _isBookingInProgress.asStateFlow()

    private val _confirmedBooking = MutableStateFlow<ShipmentBooking?>(null)
    val confirmedBooking: StateFlow<ShipmentBooking?> = _confirmedBooking.asStateFlow()

    // --- Standalone Rate Calculator State ---
    private val _calcOriginPin = MutableStateFlow("110020")
    val calcOriginPin: StateFlow<String> = _calcOriginPin.asStateFlow()

    private val _calcDestPin = MutableStateFlow("560038")
    val calcDestPin: StateFlow<String> = _calcDestPin.asStateFlow()

    private val _calcWeightKg = MutableStateFlow(0.5)
    val calcWeightKg: StateFlow<Double> = _calcWeightKg.asStateFlow()

    private val _calcPaymentMode = MutableStateFlow(PaymentMode.WALLET)
    val calcPaymentMode: StateFlow<PaymentMode> = _calcPaymentMode.asStateFlow()

    private val _calcResult = MutableStateFlow<PsRateCalculation?>(null)
    val calcResult: StateFlow<PsRateCalculation?> = _calcResult.asStateFlow()

    private val _isCalculatingStandalone = MutableStateFlow(false)
    val isCalculatingStandalone: StateFlow<Boolean> = _isCalculatingStandalone.asStateFlow()

    // --- Tracking State ---
    private val _trackingSearchAwb = MutableStateFlow("")
    val trackingSearchAwb: StateFlow<String> = _trackingSearchAwb.asStateFlow()

    private val _activeTrackedShipment = MutableStateFlow<ShipmentBooking?>(null)
    val activeTrackedShipment: StateFlow<ShipmentBooking?> = _activeTrackedShipment.asStateFlow()

    private val _isSearchingTracking = MutableStateFlow(false)
    val isSearchingTracking: StateFlow<Boolean> = _isSearchingTracking.asStateFlow()

    private val _trackingSearchError = MutableStateFlow<String?>(null)
    val trackingSearchError: StateFlow<String?> = _trackingSearchError.asStateFlow()

    // --- API Logs State ---
    private val _apiLogs = MutableStateFlow<List<ApiLogEntry>>(emptyList())
    val apiLogs: StateFlow<List<ApiLogEntry>> = _apiLogs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            refreshApiLogs()
            calculateRateForCurrentBooking()
            calculateStandaloneRate()
        }
    }

    fun refreshApiLogs() {
        _apiLogs.value = repository.apiClient.apiLogs.toList()
    }

    // Step navigation
    fun setBookingStep(step: Int) {
        _bookingStep.value = step
    }

    fun updateSenderAddress(updated: Address) {
        var addr = updated
        if (updated.pincode.length == 6) {
            PincodeDirectory.getCityState(updated.pincode)?.let { (city, state) ->
                if (addr.city.isBlank() || addr.city != city) {
                    addr = addr.copy(city = city, state = state)
                }
            }
        }
        _senderAddress.value = addr
    }

    fun updateReceiverAddress(updated: Address) {
        var addr = updated
        if (updated.pincode.length == 6) {
            PincodeDirectory.getCityState(updated.pincode)?.let { (city, state) ->
                if (addr.city.isBlank() || addr.city != city) {
                    addr = addr.copy(city = city, state = state)
                }
            }
        }
        _receiverAddress.value = addr
    }

    fun selectAddressFromBook(address: SavedAddressEntity, isSender: Boolean) {
        val converted = Address(
            name = address.name,
            phone = address.phone,
            email = address.email,
            street = address.street,
            landmark = address.landmark,
            city = address.city,
            state = address.state,
            pincode = address.pincode,
            tag = address.tag,
            isDefault = address.isDefault
        )
        if (isSender) {
            _senderAddress.value = converted
        } else {
            _receiverAddress.value = converted
        }
    }

    fun updateParcelDetails(details: ParcelDetails) {
        _parcelDetails.value = details
    }

    fun setPaymentMode(mode: PaymentMode) {
        _selectedPaymentMode.value = mode
        calculateRateForCurrentBooking()
    }

    fun setPickupSlot(slot: String) {
        _pickupSlot.value = slot
    }

    fun toggleForceShadowfaxUnavailable(unavailable: Boolean) {
        _forceShadowfaxUnavailable.value = unavailable
        calculateRateForCurrentBooking()
    }

    fun calculateRateForCurrentBooking() {
        viewModelScope.launch {
            _isLoadingRate.value = true
            try {
                val origin = _senderAddress.value.pincode.ifBlank { "110020" }
                val dest = _receiverAddress.value.pincode.ifBlank { "560038" }
                val result = repository.calculateRate(
                    originPin = origin,
                    destPin = dest,
                    parcel = _parcelDetails.value,
                    paymentMode = _selectedPaymentMode.value,
                    forceShadowfaxUnavailable = _forceShadowfaxUnavailable.value
                )
                _calculatedRate.value = result
                refreshApiLogs()
            } catch (_: Exception) {
            } finally {
                _isLoadingRate.value = false
            }
        }
    }

    fun confirmBooking(razorpayPaymentId: String? = null, razorpayOrderId: String? = null) {
        val rate = _calculatedRate.value ?: return
        viewModelScope.launch {
            _isBookingInProgress.value = true
            try {
                val booking = repository.bookParcel(
                    sender = _senderAddress.value,
                    receiver = _receiverAddress.value,
                    parcel = _parcelDetails.value,
                    paymentMode = _selectedPaymentMode.value,
                    rateInfo = rate,
                    pickupSlot = _pickupSlot.value,
                    razorpayPaymentId = razorpayPaymentId,
                    razorpayOrderId = razorpayOrderId
                )
                _confirmedBooking.value = booking
                _activeTrackedShipment.value = booking
                _bookingStep.value = 5 // Confirmation step
                refreshApiLogs()
            } catch (_: Exception) {
            } finally {
                _isBookingInProgress.value = false
            }
        }
    }

    fun resetBooking() {
        _bookingStep.value = 1
        _confirmedBooking.value = null
        _forceShadowfaxUnavailable.value = false
        calculateRateForCurrentBooking()
    }

    // --- Standalone Rate Calculator Actions ---
    fun updateCalcOriginPin(pin: String) {
        _calcOriginPin.value = pin
        calculateStandaloneRate()
    }

    fun updateCalcDestPin(pin: String) {
        _calcDestPin.value = pin
        calculateStandaloneRate()
    }

    fun updateCalcWeight(weight: Double) {
        _calcWeightKg.value = weight
        calculateStandaloneRate()
    }

    fun updateCalcPaymentMode(mode: PaymentMode) {
        _calcPaymentMode.value = mode
        calculateStandaloneRate()
    }

    fun calculateStandaloneRate(forceShadowfaxUnavailable: Boolean = false) {
        viewModelScope.launch {
            _isCalculatingStandalone.value = true
            try {
                val parcel = ParcelDetails(weightKg = _calcWeightKg.value)
                val result = repository.calculateRate(
                    originPin = _calcOriginPin.value,
                    destPin = _calcDestPin.value,
                    parcel = parcel,
                    paymentMode = _calcPaymentMode.value,
                    forceShadowfaxUnavailable = forceShadowfaxUnavailable
                )
                _calcResult.value = result
                refreshApiLogs()
            } catch (_: Exception) {
            } finally {
                _isCalculatingStandalone.value = false
            }
        }
    }

    // --- Tracking Actions ---
    fun updateTrackingSearchAwb(awb: String) {
        _trackingSearchAwb.value = awb
        _trackingSearchError.value = null
    }

    fun trackShipmentByAwb(awb: String) {
        val query = awb.trim()
        if (query.isBlank()) return
        _trackingSearchAwb.value = query
        viewModelScope.launch {
            _isSearchingTracking.value = true
            _trackingSearchError.value = null
            try {
                val existing = repository.findShipmentByAwb(query)
                if (existing != null) {
                    _activeTrackedShipment.value = existing
                } else {
                    val inList = allShipments.value.find { it.awbNumber.equals(query, ignoreCase = true) }
                    if (inList != null) {
                        _activeTrackedShipment.value = inList
                    } else {
                        _trackingSearchError.value = "No shipment found for AWB: $query."
                    }
                }
            } catch (_: Exception) {
                _trackingSearchError.value = "Error querying tracking network."
            } finally {
                _isSearchingTracking.value = false
            }
        }
    }

    fun setActiveTrackingShipment(shipment: ShipmentBooking) {
        _activeTrackedShipment.value = shipment
        _trackingSearchAwb.value = shipment.awbNumber
    }

    fun simulateNextMilestone(shipment: ShipmentBooking) {
        viewModelScope.launch {
            val (nextStatus, location, desc) = when (shipment.status) {
                ShipmentStatus.BOOKED -> Triple(
                    ShipmentStatus.PICKUP_SCHEDULED,
                    "${shipment.sender.city} Sorting Station",
                    "Courier pickup rider scheduled for package handover."
                )
                ShipmentStatus.PICKUP_SCHEDULED -> Triple(
                    ShipmentStatus.PICKED_UP,
                    "${shipment.sender.city} Hub (${shipment.carrierAssigned.displayName})",
                    "Parcel picked up from sender and inbounded."
                )
                ShipmentStatus.PICKED_UP -> Triple(
                    ShipmentStatus.IN_TRANSIT,
                    "National Transshipment Hub (${shipment.rateInfo.zone.code})",
                    "Dispatched via linehaul transit towards ${shipment.receiver.city}."
                )
                ShipmentStatus.IN_TRANSIT -> Triple(
                    ShipmentStatus.OUT_FOR_DELIVERY,
                    "${shipment.receiver.city} Hub",
                    "Out for delivery to ${shipment.receiver.name}. ${if (shipment.paymentMode == PaymentMode.COD) "Collect COD: ₹${shipment.parcel.declaredValue.toInt()}" else "Prepaid package"}"
                )
                ShipmentStatus.OUT_FOR_DELIVERY -> Triple(
                    ShipmentStatus.DELIVERED,
                    "${shipment.receiver.city} - ${shipment.receiver.pincode}",
                    "Delivered successfully. Signature verified."
                )
                ShipmentStatus.DELIVERED -> return@launch
                else -> return@launch
            }

            repository.addTrackingMilestone(shipment, nextStatus, location, desc)
            repository.findShipmentByAwb(shipment.awbNumber)?.let {
                _activeTrackedShipment.value = it
            }
            refreshApiLogs()
        }
    }

    // --- Address Book Actions ---
    fun saveAddress(address: SavedAddressEntity) {
        viewModelScope.launch {
            repository.saveAddress(address)
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            repository.deleteAddress(id)
        }
    }

    fun setDefaultAddress(id: Long) {
        viewModelScope.launch {
            repository.setDefaultAddress(id)
        }
    }

    // --- Wallet Actions ---
    fun topUpWallet(amount: Double, razorpayPaymentId: String? = null) {
        viewModelScope.launch {
            repository.topUpWallet(amount, razorpayPaymentId)
        }
    }
}
