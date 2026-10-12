package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryZone
import com.example.data.model.PaymentMode
import com.example.data.model.PincodeDirectory
import com.example.data.model.SettlementHelper
import com.example.ui.components.CarrierBadge
import com.example.ui.components.ZoneBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.IndiaPostRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ShadowfaxTeal
import com.example.ui.viewmodel.CourierViewModel

@Composable
fun RateComparisonScreen(
    viewModel: CourierViewModel,
    onBookWithCalculatedRoute: () -> Unit
) {
    val originPin by viewModel.calcOriginPin.collectAsState()
    val destPin by viewModel.calcDestPin.collectAsState()
    val weightKg by viewModel.calcWeightKg.collectAsState()
    val paymentMode by viewModel.calcPaymentMode.collectAsState()
    val calcResult by viewModel.calcResult.collectAsState()
    val isCalculating by viewModel.isCalculatingStandalone.collectAsState()

    var forceShadowfaxUnavailable by remember { mutableStateOf(false) }

    val originCity = PincodeDirectory.lookup(originPin)?.let { "${it.city}, ${it.state}" } ?: "Origin Area"
    val destCity = PincodeDirectory.lookup(destPin)?.let { "${it.city}, ${it.state}" } ?: "Destination Area"
    val (nextPayout, cycleText) = remember {
        val s = SettlementHelper.getNextSettlementSchedule()
        Pair(s.first, s.second)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rate_comparison_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "PS Courier Custom Rate Calculator",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Zone A ₹80 • Zone B ₹90 • Zone C/D ₹100 • Zone E ₹120 • Flat COD ₹50",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Route Input Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Origin & Destination PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = originPin,
                                onValueChange = { if (it.length <= 6) viewModel.updateCalcOriginPin(it) },
                                label = { Text("Origin PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = originCity,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val temp = originPin
                                viewModel.updateCalcOriginPin(destPin)
                                viewModel.updateCalcDestPin(temp)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", tint = MaterialTheme.colorScheme.primary)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = destPin,
                                onValueChange = { if (it.length <= 6) viewModel.updateCalcDestPin(it) },
                                label = { Text("Destination PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = destCity,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Weight Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weight:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = "${"%.1f".format(weightKg)} kg",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = weightKg.toFloat(),
                        onValueChange = { viewModel.updateCalcWeight(it.toDouble()) },
                        valueRange = 0.1f..15f,
                        steps = 29
                    )

                    // Payment Mode Toggle (Prepaid vs COD)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("Prepaid (Wallet/UPI)", PaymentMode.WALLET),
                            Pair("Cash on Delivery (₹50 COD)", PaymentMode.COD)
                        ).forEach { (label, mode) ->
                            val isSel = paymentMode == mode
                            Surface(
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateCalcPaymentMode(mode) }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulation Switch: Force Shadowfax Unavailable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Shadowfax Unavailable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Falls back to Indian Post Speed Post", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = forceShadowfaxUnavailable,
                            onCheckedChange = {
                                forceShadowfaxUnavailable = it
                                viewModel.calculateStandaloneRate(it)
                            }
                        )
                    }
                }
            }
        }

        // Calculation Result Card
        if (calcResult != null) {
            val result = calcResult!!

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rate_result_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CarrierBadge(carrier = result.carrierAssigned)
                            ZoneBadge(zone = result.zone)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Carrier Serviceability Routing Note
                        Surface(
                            color = if (result.isShadowfaxServiceable) ShadowfaxTeal.copy(alpha = 0.10f) else IndiaPostRed.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (result.isShadowfaxServiceable) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (result.isShadowfaxServiceable) ShadowfaxTeal else IndiaPostRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = result.routingNote,
                                    fontSize = 11.sp,
                                    color = if (result.isShadowfaxServiceable) ShadowfaxTeal else IndiaPostRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fare Breakdown Line Items
                        CostRow("${result.zone.code} Delivery Flat Rate (${result.zone.title})", "₹${result.baseDeliveryCharge.toInt()} (Incl. 18% GST)")
                        if (result.codCharge > 0) {
                            CostRow("COD Fee (Doorstep Cash Collection)", "₹${result.codCharge.toInt()} (Incl. 18% GST)")
                        }
                        CostRow("Return / RTO Parcel Charge (If Returned)", "₹${result.rtoCharge.toInt()} (Same as forward rate)")
                        CostRow("Net Taxable Value (Before Tax)", "₹${"%.2f".format(result.taxableBaseFreight + result.taxableCodBase)}")
                        CostRow("Embedded 18% GST Component", "₹${"%.2f".format(result.gst)}")

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Estimated Cost", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tax inclusive of 18% GST", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "₹${"%.2f".format(result.totalPayable)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onBookWithCalculatedRoute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Book Parcel at This Rate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Settlement Cycle & RTO Policy Info Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("PS Courier Key Policies", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• RTO Policy: Return or RTO parcels cost the exact same as original delivery rate.", fontSize = 11.sp)
                    Text("• Settlement Cycle: COD payouts occur 3 times a week (Mon, Wed, Fri). Next: $nextPayout.", fontSize = 11.sp)
                    Text("• Pricing: All flat rates are tax inclusive of 18% GST (Zone A ₹80, Zone B ₹90, Zone C/D ₹100, Zone E ₹120, COD ₹50).", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CostRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
