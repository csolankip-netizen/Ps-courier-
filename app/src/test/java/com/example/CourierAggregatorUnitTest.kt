package com.example

import com.example.data.api.CourierApiClient
import com.example.data.model.Address
import com.example.data.model.CarrierPartner
import com.example.data.model.DeliveryZone
import com.example.data.model.ParcelDetails
import com.example.data.model.PaymentMode
import com.example.data.model.PincodeDirectory
import com.example.data.model.SettlementHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourierAggregatorUnitTest {

    @Test
    fun testZoneResolutionAndFlatRates() {
        // Zone A: Intra-City (New Delhi 110001 -> New Delhi 110020)
        val zoneA = PincodeDirectory.resolveZone("110001", "110020")
        assertEquals(DeliveryZone.ZONE_A, zoneA)
        assertEquals(80.0, zoneA.flatRate, 0.01)
        assertEquals(80.0, zoneA.rtoCharge, 0.01)

        // Zone B: Intra-State (Mumbai 400001 -> Pune 411001 both in Maharashtra)
        val zoneB = PincodeDirectory.resolveZone("400001", "411001")
        assertEquals(DeliveryZone.ZONE_B, zoneB)
        assertEquals(90.0, zoneB.flatRate, 0.01)
        assertEquals(90.0, zoneB.rtoCharge, 0.01)

        // Zone C/D: Metro to Metro / ROI (Delhi 110001 -> Bengaluru 560001)
        val zoneCD = PincodeDirectory.resolveZone("110001", "560001")
        assertEquals(DeliveryZone.ZONE_C_D, zoneCD)
        assertEquals(100.0, zoneCD.flatRate, 0.01)
        assertEquals(100.0, zoneCD.rtoCharge, 0.01)

        // Zone E: Special / North East / J&K (Delhi 110001 -> Guwahati 781001)
        val zoneE = PincodeDirectory.resolveZone("110001", "781001")
        assertEquals(DeliveryZone.ZONE_E, zoneE)
        assertEquals(120.0, zoneE.flatRate, 0.01)
        assertEquals(120.0, zoneE.rtoCharge, 0.01)
    }

    @Test
    fun testShadowfaxFirstAndIndianPostFallbackRouting() {
        // Metro route: Delhi to Bengaluru -> Shadowfax is serviceable (Primary partner)
        val (carrier1, sfxOk1) = PincodeDirectory.checkServiceabilityAndCarrier("110001", "560001")
        assertEquals(CarrierPartner.SHADOWFAX, carrier1)
        assertTrue(sfxOk1)

        // Remote route: Delhi to Guwahati (781001) -> Shadowfax unavailable, falls back to Indian Post
        val (carrier2, sfxOk2) = PincodeDirectory.checkServiceabilityAndCarrier("110001", "781001")
        assertEquals(CarrierPartner.INDIAN_POST, carrier2)
        assertFalse(sfxOk2)

        // Forced unavailable simulation -> falls back to Indian Post
        val (carrier3, sfxOk3) = PincodeDirectory.checkServiceabilityAndCarrier("110001", "560001", forceShadowfaxUnavailable = true)
        assertEquals(CarrierPartner.INDIAN_POST, carrier3)
        assertFalse(sfxOk3)
    }

    @Test
    fun testRateCalculatorCalculationsWithTaxInclusiveGst() = runBlocking {
        val client = CourierApiClient()
        val parcel = ParcelDetails(weightKg = 1.0)

        // Prepaid Zone A calculation (Tax INCLUSIVE of 18% GST):
        // Total payable is exactly ₹80.0. Taxable freight = ₹67.80, embedded 18% GST = ₹12.20
        val rateA = client.calculatePsCourierRate("110001", "110020", parcel, PaymentMode.WALLET)
        assertEquals(80.0, rateA.baseDeliveryCharge, 0.01)
        assertEquals(0.0, rateA.codCharge, 0.01)
        assertEquals(80.0, rateA.rtoCharge, 0.01)
        assertEquals(67.80, rateA.taxableBaseFreight, 0.01)
        assertEquals(12.20, rateA.gst, 0.01)
        assertEquals(80.0, rateA.totalPayable, 0.01)

        // COD Zone B calculation (Tax INCLUSIVE of 18% GST):
        // Zone B ₹90 base + ₹50 COD = ₹140 total payable.
        // Taxable base freight = ₹76.27, taxable COD = ₹42.37, embedded 18% GST = ₹21.36
        val rateB = client.calculatePsCourierRate("400001", "411001", parcel, PaymentMode.COD)
        assertEquals(90.0, rateB.baseDeliveryCharge, 0.01)
        assertEquals(50.0, rateB.codCharge, 0.01)
        assertEquals(90.0, rateB.rtoCharge, 0.01)
        assertEquals(76.27, rateB.taxableBaseFreight, 0.01)
        assertEquals(42.37, rateB.taxableCodBase, 0.01)
        assertEquals(21.36, rateB.gst, 0.01)
        assertEquals(140.0, rateB.totalPayable, 0.01)
    }

    @Test
    fun testRazorpayGatewayOrderAndPaymentSimulation() = runBlocking {
        val order = com.example.data.api.RazorpayGateway.createOrder(amountInr = 100.0)
        assertNotNull(order.orderId)
        assertTrue(order.orderId.startsWith("order_"))
        assertEquals(10000L, order.amountPaise)
        assertEquals("INR", order.currency)

        val result = com.example.data.api.RazorpayGateway.processPayment(
            orderId = order.orderId,
            amountInr = 100.0,
            method = "UPI"
        )
        assertTrue(result.isSuccess)
        assertTrue(result.paymentId.startsWith("pay_"))
        assertEquals(order.orderId, result.orderId)
        assertEquals("UPI", result.paymentMethod)
        assertEquals(100.0, result.amountInr, 0.01)
    }

    @Test
    fun testSettlementScheduleThreeTimesAWeek() {
        val (nextDay, cycleText, _) = SettlementHelper.getNextSettlementSchedule()
        assertNotNull(nextDay)
        assertTrue(nextDay in listOf("Monday", "Wednesday", "Friday"))
        assertTrue(cycleText.contains("3 Times / Week"))
    }
}
