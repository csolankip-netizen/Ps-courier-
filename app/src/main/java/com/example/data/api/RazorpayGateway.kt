package com.example.data.api

import com.example.BuildConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RazorpayPaymentResult(
    val isSuccess: Boolean,
    val paymentId: String,
    val orderId: String,
    val signature: String,
    val paymentMethod: String,
    val amountInr: Double,
    val errorMessage: String? = null
)

data class RazorpayOrder(
    val orderId: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val receipt: String,
    val status: String = "created",
    val keyId: String
)

object RazorpayGateway {

    /**
     * Retrieves the Razorpay Key ID configured via the Secrets panel in AI Studio or .env.
     * Uses BuildConfig.RAZORPAY_KEY_ID injected by the Secrets Gradle Plugin.
     */
    fun getKeyId(): String {
        return try {
            val key = BuildConfig.RAZORPAY_KEY_ID
            if (key.isNotBlank()) key else "rzp_test_PsCourierGatewayDemo"
        } catch (_: Exception) {
            "rzp_test_PsCourierGatewayDemo"
        }
    }

    /**
     * Creates a new Razorpay Order with an authentic `order_...` reference
     */
    suspend fun createOrder(amountInr: Double, receiptPrefix: String = "rcpt"): RazorpayOrder {
        delay(150) // simulate API latency
        val orderNum = UUID.randomUUID().toString().replace("-", "").take(14).uppercase()
        val orderId = "order_$orderNum"
        val amountPaise = (amountInr * 100).toLong()

        return RazorpayOrder(
            orderId = orderId,
            amountPaise = amountPaise,
            receipt = "$receiptPrefix-${System.currentTimeMillis()}",
            keyId = getKeyId()
        )
    }

    /**
     * Simulates payment authorization & signature generation on Razorpay servers
     */
    suspend fun processPayment(
        orderId: String,
        amountInr: Double,
        method: String // "UPI", "CARD", "NET_BANKING"
    ): RazorpayPaymentResult {
        delay(350) // simulate gateway authentication and OTP / UPI intent verification

        val payNum = UUID.randomUUID().toString().replace("-", "").take(14)
        val paymentId = "pay_$payNum"
        val signature = "rzp_sig_" + UUID.randomUUID().toString().replace("-", "").take(20)

        return RazorpayPaymentResult(
            isSuccess = true,
            paymentId = paymentId,
            orderId = orderId,
            signature = signature,
            paymentMethod = method,
            amountInr = amountInr
        )
    }
}
