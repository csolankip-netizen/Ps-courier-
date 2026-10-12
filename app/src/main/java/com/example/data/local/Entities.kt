package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shipments")
data class ShipmentEntity(
    @PrimaryKey val id: String,
    val awbNumber: String,
    val carrierAssigned: String, // "SHADOWFAX" or "INDIAN_POST"
    val senderName: String,
    val senderPhone: String,
    val senderStreet: String,
    val senderCity: String,
    val senderState: String,
    val senderPincode: String,
    val receiverName: String,
    val receiverPhone: String,
    val receiverStreet: String,
    val receiverCity: String,
    val receiverState: String,
    val receiverPincode: String,
    val parcelCategory: String,
    val weightKg: Double,
    val lengthCm: Double,
    val widthCm: Double,
    val heightCm: Double,
    val declaredValue: Double,
    val parcelDescription: String,
    val isFragile: Boolean,
    val zoneCode: String, // "ZONE_A", "ZONE_B", "ZONE_C_D", "ZONE_E"
    val baseDeliveryCharge: Double, // 80, 90, 100, 120
    val codCharge: Double, // 50 or 0
    val rtoCharge: Double, // equal to baseDeliveryCharge
    val gst: Double, // 18% of (base + cod)
    val totalPayable: Double,
    val isShadowfaxServiceable: Boolean,
    val routingNote: String,
    val paymentMode: String,
    val isPaid: Boolean,
    val status: String,
    val bookingDate: String,
    val expectedDeliveryDate: String,
    val pickupSlot: String,
    val routingCode: String,
    val trackingEventsJson: String,
    val razorpayPaymentId: String? = null,
    val razorpayOrderId: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_addresses")
data class SavedAddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val street: String,
    val landmark: String = "",
    val city: String,
    val state: String,
    val pincode: String,
    val tag: String = "Warehouse", // "Warehouse", "Office", "Home"
    val isDefault: Boolean = false
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // "WALLET_TOPUP", "COD_REMITTANCE", "SHIPMENT_BOOKING", "RTO_CHARGE"
    val amount: Double,
    val description: String,
    val referenceAwb: String? = null,
    val timestamp: String,
    val balanceAfter: Double,
    val createdTimestamp: Long = System.currentTimeMillis()
)
