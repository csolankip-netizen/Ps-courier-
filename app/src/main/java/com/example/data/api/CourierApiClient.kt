package com.example.data.api

import com.example.data.model.Address
import com.example.data.model.CarrierPartner
import com.example.data.model.DeliveryZone
import com.example.data.model.ParcelDetails
import com.example.data.model.PaymentMode
import com.example.data.model.PincodeDirectory
import com.example.data.model.PsRateCalculation
import com.example.data.model.ShipmentStatus
import com.example.data.model.TrackingCheckpoint
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class ApiLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val carrier: String,
    val endpoint: String,
    val method: String,
    val requestPayload: String,
    val responsePayload: String,
    val statusCode: Int = 200,
    val durationMs: Long,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

class CourierApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val indiaPostService: IndiaPostApiService = Retrofit.Builder()
        .baseUrl("https://api.indiapost.gov.in/production/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(IndiaPostApiService::class.java)

    val shadowfaxService: ShadowfaxApiService = Retrofit.Builder()
        .baseUrl("https://api.shadowfax.in/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(ShadowfaxApiService::class.java)

    val apiLogs = mutableListOf<ApiLogEntry>()

    private fun logApi(
        carrier: String,
        endpoint: String,
        method: String,
        request: String,
        response: String,
        duration: Long
    ) {
        val entry = ApiLogEntry(
            carrier = carrier,
            endpoint = endpoint,
            method = method,
            requestPayload = request,
            responsePayload = response,
            durationMs = duration
        )
        apiLogs.add(0, entry)
        if (apiLogs.size > 50) apiLogs.removeLast()
    }

    /**
     * Calculates the exact PS Courier rate based on the custom specification:
     * - Zone A: ₹80 (Inclusive of 18% GST)
     * - Zone B: ₹90 (Inclusive of 18% GST)
     * - Zone C/D: ₹100 (Inclusive of 18% GST)
     * - Zone E: ₹120 (Inclusive of 18% GST)
     * - COD charge: ₹50 (Inclusive of 18% GST)
     * - Return / RTO rate: Equal to original delivery rate (Inclusive of 18% GST)
     * - Pricing model: Tax INCLUSIVE of 18% GST
     * - Routing: First checks Shadowfax serviceability. If unavailable, falls back to Indian Post.
     */
    suspend fun calculatePsCourierRate(
        originPin: String,
        destPin: String,
        parcel: ParcelDetails,
        paymentMode: PaymentMode,
        forceShadowfaxUnavailable: Boolean = false
    ): PsRateCalculation {
        val startTime = System.currentTimeMillis()
        delay(200) // network verification latency

        val zone = PincodeDirectory.resolveZone(originPin, destPin)
        val (assignedCarrier, isSfxServiceable) = PincodeDirectory.checkServiceabilityAndCarrier(
            originPin, destPin, forceShadowfaxUnavailable
        )

        // Tax-inclusive pricing:
        val baseDeliveryCharge = zone.flatRate // Flat rate: Zone A ₹80, Zone B ₹90, Zone C/D ₹100, Zone E ₹120 (Inclusive)
        val codCharge = if (paymentMode == PaymentMode.COD) 50.0 else 0.0 // Flat ₹50 (Inclusive)
        val rtoCharge = zone.rtoCharge // Equal to original delivery rate

        val totalPayable = round(baseDeliveryCharge + codCharge)

        // Back-calculate the 18% GST component embedded in the total price:
        val taxableBaseFreight = round(baseDeliveryCharge / 1.18)
        val taxableCodBase = if (codCharge > 0) round(codCharge / 1.18) else 0.0
        val taxableAmount = taxableBaseFreight + taxableCodBase
        val gst = round(totalPayable - taxableAmount)

        val routingNote = if (isSfxServiceable) {
            "Shadowfax Express Serviceable: Assigned as Primary Partner for fast doorstep fulfillment."
        } else {
            "Shadowfax Unserviceable for this route: Automatically routed via Indian Post (Speed Post) for guaranteed national delivery."
        }

        // Record realistic API logs
        val duration = System.currentTimeMillis() - startTime
        logApi(
            carrier = "Shadowfax Serviceability API",
            endpoint = "/api/v3/clients/serviceability",
            method = "POST",
            request = """{"pickupPin":"$originPin","dropPin":"$destPin","weightKg":${parcel.weightKg}}""",
            response = """{"serviceable":$isSfxServiceable,"slaHours":${if (zone == DeliveryZone.ZONE_A) 24 else 48}}""",
            duration = duration
        )

        if (!isSfxServiceable) {
            logApi(
                carrier = "Indian Post EAD Gateway",
                endpoint = "/v2/article/routing-check",
                method = "POST",
                request = """{"originPin":"$originPin","destPin":"$destPin","fallback":true}""",
                response = """{"status":"SERVICEABLE","hub":"NSH-ROUTED","service":"SPEED_POST"}""",
                duration = duration + 25
            )
        }

        return PsRateCalculation(
            zone = zone,
            baseDeliveryCharge = baseDeliveryCharge,
            taxableBaseFreight = taxableBaseFreight,
            codCharge = codCharge,
            taxableCodBase = taxableCodBase,
            rtoCharge = rtoCharge,
            gst = gst,
            totalPayable = totalPayable,
            carrierAssigned = assignedCarrier,
            isShadowfaxServiceable = isSfxServiceable,
            routingNote = routingNote
        )
    }

    private fun round(value: Double): Double = (value * 100).roundToInt() / 100.0

    suspend fun bookShipment(
        carrierAssigned: CarrierPartner,
        sender: Address,
        receiver: Address,
        parcel: ParcelDetails,
        paymentMode: PaymentMode,
        rateInfo: PsRateCalculation
    ): Pair<String, String> { // Pair(awbNumber, routingCode)
        val startTime = System.currentTimeMillis()
        delay(300)

        val (awb, routingCode) = when (carrierAssigned) {
            CarrierPartner.SHADOWFAX -> {
                val digits = (10000000..99999999).random()
                val awbNum = "SFX$digits"
                val route = "SFX-${sender.pincode.take(3)}-${receiver.pincode.take(3)}-${rateInfo.zone.code.replace(" ", "")}"
                logApi(
                    carrier = "Shadowfax Logistics",
                    endpoint = "/api/v3/orders/create",
                    method = "POST",
                    request = """{"clientOrderId":"PS-${System.currentTimeMillis()}","pickup":"${sender.pincode}","drop":"${receiver.pincode}","mode":"${if (paymentMode == PaymentMode.COD) "cod" else "prepaid"}"}""",
                    response = """{"success":true,"waybill":"$awbNum","routingCode":"$route"}""",
                    duration = System.currentTimeMillis() - startTime
                )
                Pair(awbNum, route)
            }
            CarrierPartner.INDIAN_POST -> {
                val digits = (100000000..999999999).random()
                val awbNum = "EM${digits}IN"
                val route = "INP-${receiver.pincode.take(3)}-NSH-${rateInfo.zone.code.replace(" ", "")}"
                logApi(
                    carrier = "Indian Post EAD",
                    endpoint = "/v2/article/manifest",
                    method = "POST",
                    request = """{"articleType":"SPEED_POST","consigneePin":"${receiver.pincode}","declaredVal":${parcel.declaredValue}}""",
                    response = """{"status":"SUCCESS","articleNumber":"$awbNum","route":"$route"}""",
                    duration = System.currentTimeMillis() - startTime
                )
                Pair(awbNum, route)
            }
        }

        return Pair(awb, routingCode)
    }

    fun getInitialTrackingEvents(
        carrier: CarrierPartner,
        senderCity: String,
        receiverCity: String,
        awb: String
    ): List<TrackingCheckpoint> {
        val now = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        return when (carrier) {
            CarrierPartner.SHADOWFAX -> listOf(
                TrackingCheckpoint(
                    timestamp = now,
                    location = "$senderCity Shadowfax Facility",
                    status = "Order Manifest Created (Primary Shadowfax)",
                    description = "AWB $awb assigned. Doorstep rider allocated for courier collection."
                )
            )
            CarrierPartner.INDIAN_POST -> listOf(
                TrackingCheckpoint(
                    timestamp = now,
                    location = "$senderCity Sub Post Office",
                    status = "E-Manifested via Indian Post (Fallback Route)",
                    description = "Electronic Advanced Data generated for article $awb. Routed via Indian Post Speed Post."
                )
            )
        }
    }
}
