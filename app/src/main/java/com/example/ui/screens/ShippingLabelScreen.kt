package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentMode
import com.example.data.model.ShipmentBooking
import com.example.ui.components.BarcodeGraphic
import com.example.ui.components.QrCodeGraphic
import com.example.ui.viewmodel.CourierViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShippingLabelScreen(
    viewModel: CourierViewModel,
    shipment: ShipmentBooking?,
    onNavigateBack: () -> Unit
) {
    val allShipments by viewModel.allShipments.collectAsState()
    val currentShipment = shipment ?: allShipments.firstOrNull()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PS Courier Shipping Label (4x6)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentShipment != null) {
                        IconButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        """
                                        PS Courier Logistics Shipping Label
                                        AWB: ${currentShipment.awbNumber}
                                        Assigned Carrier: ${currentShipment.carrierAssigned.displayName}
                                        Zone: ${currentShipment.rateInfo.zone.code} (${currentShipment.rateInfo.zone.title})
                                        To: ${currentShipment.receiver.name}, ${currentShipment.receiver.city} - ${currentShipment.receiver.pincode}
                                        From: ${currentShipment.sender.name}, ${currentShipment.sender.city}
                                        Routing Code: ${currentShipment.routingCode}
                                        Payment: ${if (currentShipment.paymentMode == PaymentMode.COD) "COD: ₹${currentShipment.parcel.declaredValue.toInt()} (Collect Cash)" else "PREPAID"}
                                        """.trimIndent()
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share PS Courier Label"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (currentShipment == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No consignment selected for label generation.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("shipping_label_screen"),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("thermal_label_view")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // 1. Header: PS Courier + Assigned Carrier + Zone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PS COURIER",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Partner: ${currentShipment.carrierAssigned.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = currentShipment.rateInfo.zone.code.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = currentShipment.routingCode,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        DividerBlack()
                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Barcode & QR Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BarcodeGraphic(
                                code = currentShipment.awbNumber,
                                modifier = Modifier.weight(1f),
                                height = 50.dp,
                                barColor = Color.Black
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            QrCodeGraphic(
                                data = "PSCOURIER|${currentShipment.awbNumber}|${currentShipment.receiver.pincode}|${currentShipment.carrierAssigned.name}",
                                size = 68.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        DividerBlack()
                        Spacer(modifier = Modifier.height(6.dp))

                        // 3. SHIP TO (Consignee)
                        Text(
                            text = "SHIP TO (DELIVERY ADDRESS):",
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentShipment.receiver.name.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = currentShipment.receiver.street,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "${currentShipment.receiver.city}, ${currentShipment.receiver.state}",
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PIN: ${currentShipment.receiver.pincode}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "PH: ${currentShipment.receiver.phone}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        DividerBlack()
                        Spacer(modifier = Modifier.height(6.dp))

                        // 4. SHIPPER (Return to Origin)
                        Text(
                            text = "RETURN IF UNDELIVERED TO (SHIPPER):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "${currentShipment.sender.name} | ${currentShipment.sender.street}, ${currentShipment.sender.city} - ${currentShipment.sender.pincode}",
                            fontSize = 10.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Sender Contact: ${currentShipment.sender.phone}",
                            fontSize = 10.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        DividerBlack()
                        Spacer(modifier = Modifier.height(6.dp))

                        // 5. Package Specs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Weight: ${currentShipment.parcel.weightKg} kg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Vol: ${"%.2f".format(currentShipment.parcel.volumetricWeightKg)} kg", fontSize = 11.sp, color = Color.Black)
                            Text("Category: ${currentShipment.parcel.category.label}", fontSize = 11.sp, color = Color.Black)
                            Text("Pcs: 1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 6. Bold COD / PREPAID Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, Color.Black, RoundedCornerShape(2.dp))
                                .background(if (currentShipment.paymentMode == PaymentMode.COD) Color(0xFFFEF08A) else Color(0xFFE2E8F0))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentShipment.paymentMode == PaymentMode.COD) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "CASH ON DELIVERY (COD)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "COLLECT CASH: ₹${currentShipment.parcel.declaredValue.toInt()}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            } else {
                                Text(
                                    text = "PREPAID - DO NOT COLLECT CASH",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 7. Footer Policy Notice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "RTO Charge = Forward Rate (100%)",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "PS Manifest Ref: ${currentShipment.id}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Print / Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Sending thermal 4x6 label to printer...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_label_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Label", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AWB", currentShipment.awbNumber))
                            Toast.makeText(context, "AWB ${currentShipment.awbNumber} copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy AWB", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DividerBlack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.Black)
    )
}
