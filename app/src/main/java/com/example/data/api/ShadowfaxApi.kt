package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

// Authentic schema matching Shadowfax Logistics REST APIs
data class ShadowfaxServiceabilityRequest(
    val pickupPincode: String,
    val deliveryPincode: String,
    val actualWeightGrams: Int,
    val volumetricWeightGrams: Int,
    val paymentMode: String, // "prepaid" or "cod"
    val orderAmount: Double,
    val deliveryType: String = "express" // "express" or "surface"
)

data class ShadowfaxServiceabilityResponse(
    val isServiceable: Boolean,
    val serviceType: String,
    val estimatedDeliveryHours: Int,
    val baseRate: Double,
    val codCharges: Double,
    val fuelSurcharge: Double,
    val gst: Double,
    val totalRate: Double,
    val nextPickupSlot: String,
    val routingHub: String
)

data class ShadowfaxOrderRequest(
    val clientOrderId: String,
    val pickupName: String,
    val pickupPhone: String,
    val pickupAddress: String,
    val pickupPincode: String,
    val dropName: String,
    val dropPhone: String,
    val dropAddress: String,
    val dropPincode: String,
    val paymentMode: String,
    val codAmount: Double,
    val declaredValue: Double,
    val weightGrams: Int,
    val packageDescription: String
)

data class ShadowfaxOrderResponse(
    val success: Boolean,
    val waybillNumber: String, // e.g. "SFX83920147"
    val routingCode: String,   // e.g. "SFX-DEL-HUB-04"
    val pickupToken: String,
    val labelUrl: String,
    val expectedPickupTime: String,
    val expectedDeliveryDate: String,
    val message: String
)

data class ShadowfaxMilestone(
    val status: String,
    val location: String,
    val timestamp: String,
    val message: String
)

data class ShadowfaxTrackResponse(
    val waybillNumber: String,
    val currentStatus: String,
    val originPincode: String,
    val destinationPincode: String,
    val pickupDate: String,
    val expectedDelivery: String,
    val riderName: String?,
    val riderPhone: String?,
    val milestones: List<ShadowfaxMilestone>
)

interface ShadowfaxApiService {
    @POST("api/v3/clients/serviceability")
    suspend fun checkServiceability(
        @Header("Authorization") token: String,
        @Body request: ShadowfaxServiceabilityRequest
    ): Response<ShadowfaxServiceabilityResponse>

    @POST("api/v3/orders/create")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body request: ShadowfaxOrderRequest
    ): Response<ShadowfaxOrderResponse>

    @GET("api/v3/orders/track/{waybill}")
    suspend fun trackOrder(
        @Header("Authorization") token: String,
        @Path("waybill") waybill: String
    ): Response<ShadowfaxTrackResponse>
}
