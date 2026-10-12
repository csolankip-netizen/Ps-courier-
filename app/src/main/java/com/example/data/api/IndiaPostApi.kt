package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Authentic schema matching India Post EAD (Electronic Advanced Data) and Speed Post Services
data class IndiaPostRateRequest(
    val senderPincode: String,
    val receiverPincode: String,
    val weightGrams: Int,
    val lengthCm: Int,
    val widthCm: Int,
    val heightCm: Int,
    val serviceType: String = "SPEED_POST", // "SPEED_POST" or "BUSINESS_PARCEL"
    val declaredValueInr: Double = 0.0,
    val isCod: Boolean = false
)

data class IndiaPostRateResponse(
    val status: String,
    val serviceCode: String,
    val serviceName: String,
    val baseTariff: Double,
    val fuelSurcharge: Double,
    val codCharge: Double,
    val gstAmount: Double,
    val totalPayable: Double,
    val transitDays: String,
    val destinationNsh: String // National Sorting Hub code e.g. "NSH-BLR-01"
)

data class IndiaPostBookingRequest(
    val clientBookingId: String,
    val senderName: String,
    val senderPhone: String,
    val senderAddress: String,
    val senderPincode: String,
    val consigneeName: String,
    val consigneePhone: String,
    val consigneeAddress: String,
    val consigneePincode: String,
    val serviceType: String,
    val weightGrams: Int,
    val declaredValue: Double,
    val codAmount: Double,
    val itemDescription: String
)

data class IndiaPostBookingResponse(
    val status: String,
    val articleNumber: String, // e.g. "EM738291045IN"
    val bookingReferenceNumber: String,
    val originOffice: String,
    val destinationOffice: String,
    val sortingHub: String,
    val routingBarcode: String,
    val bookingTimestamp: String
)

data class IndiaPostEvent(
    val eventDate: String,
    val eventTime: String,
    val officeName: String,
    val eventCode: String,
    val eventDescription: String
)

data class IndiaPostTrackResponse(
    val articleNumber: String,
    val bookedAt: String,
    val destinationPincode: String,
    val currentStatus: String,
    val deliveryLocation: String,
    val expectedDeliveryDate: String,
    val events: List<IndiaPostEvent>
)

interface IndiaPostApiService {
    @POST("v2/tariff/calculate")
    suspend fun calculateRate(
        @Header("X-IndiaPost-Auth-Key") apiKey: String,
        @Body request: IndiaPostRateRequest
    ): Response<IndiaPostRateResponse>

    @POST("v2/article/manifest")
    suspend fun bookArticle(
        @Header("X-IndiaPost-Auth-Key") apiKey: String,
        @Body request: IndiaPostBookingRequest
    ): Response<IndiaPostBookingResponse>

    @GET("v2/article/track/{articleNumber}")
    suspend fun trackArticle(
        @Header("X-IndiaPost-Auth-Key") apiKey: String,
        @Path("articleNumber") articleNumber: String
    ): Response<IndiaPostTrackResponse>
}
