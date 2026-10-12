package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShipmentBooking
import com.example.data.model.ShipmentStatus
import com.example.ui.components.CarrierBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.ZoneBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.IndiaPostRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.PrimaryNavyLight
import com.example.ui.theme.ShadowfaxTeal
import com.example.ui.viewmodel.CourierViewModel

@Composable
fun DashboardScreen(
    viewModel: CourierViewModel,
    onNavigateToBooking: () -> Unit,
    onNavigateToRateCalculator: () -> Unit,
    onNavigateToTracking: (String?) -> Unit,
    onNavigateToLabel: (ShipmentBooking) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToAddressBook: () -> Unit,
    onNavigateToHelpCenter: () -> Unit
) {
    val shipments by viewModel.allShipments.collectAsState()
    val walletBal by viewModel.walletBalance.collectAsState()
    val context = LocalContext.current
    var filterSelection by remember { mutableStateOf("All") }

    val filteredShipments = remember(shipments, filterSelection) {
        when (filterSelection) {
            "Active" -> shipments.filter { it.status != ShipmentStatus.DELIVERED && it.status != ShipmentStatus.CANCELLED }
            "Delivered" -> shipments.filter { it.status == ShipmentStatus.DELIVERED }
            else -> shipments
        }
    }

    val activeHeroShipment = shipments.firstOrNull {
        it.status == ShipmentStatus.IN_TRANSIT || it.status == ShipmentStatus.OUT_FOR_DELIVERY || it.status == ShipmentStatus.PICKUP_SCHEDULED
    } ?: shipments.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // --- 1. Top Header ---
        item {
            HeaderSection(onNavigateToHelpCenter)
        }

        // --- 2. In-App Wallet Summary Strip ---
        item {
            WalletBanner(walletBal = walletBal, onOpenWallet = onNavigateToWallet)
        }

        // --- 3. Active Shipment Tracker Card ---
        if (activeHeroShipment != null) {
            item {
                ActiveShipmentCard(
                    shipment = activeHeroShipment,
                    onTrack = { onNavigateToTracking(activeHeroShipment.awbNumber) },
                    onCopyAwb = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AWB", activeHeroShipment.awbNumber))
                        Toast.makeText(context, "AWB copied: ${activeHeroShipment.awbNumber}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // --- 4. 4 Quick Actions Grid ---
        item {
            QuickActionsGrid(
                onBookParcel = onNavigateToBooking,
                onRateCompare = onNavigateToRateCalculator,
                onOpenAddressBook = onNavigateToAddressBook,
                onOpenWallet = onNavigateToWallet
            )
        }

        // --- 5. Flat Rate Policy Strip ---
        item {
            RatePolicyNotice(onNavigateToRateCalculator)
        }

        // --- 6. Recent Shipments Header & Filters ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Consignments",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${shipments.size} Bookings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Active", "Delivered").forEach { tab ->
                        FilterChip(
                            label = tab,
                            isSelected = filterSelection == tab,
                            onClick = { filterSelection = tab }
                        )
                    }
                }
            }
        }

        // --- 7. Shipments List ---
        if (filteredShipments.isEmpty()) {
            item {
                EmptyShipmentsPlaceholder(onNavigateToBooking)
            }
        } else {
            items(filteredShipments, key = { it.id }) { item ->
                ShipmentListItem(
                    shipment = item,
                    onTrack = { onNavigateToTracking(item.awbNumber) },
                    onPrintLabel = { onNavigateToLabel(item) },
                    onCopyAwb = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AWB", item.awbNumber))
                        Toast.makeText(context, "AWB copied: ${item.awbNumber}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(onOpenHelp: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PrimaryNavy, PrimaryNavyLight.copy(alpha = 0.9f))
                )
            )
            .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AccentAmber,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Courier Aggregator",
                            color = Color(0xFFBAE6FD),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "PS Courier",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.clickable { onOpenHelp() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Help Desk",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Partner priority routing status pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CarrierPill(
                    name = "Shadowfax",
                    sub = "Primary Checked 1st",
                    color = ShadowfaxTeal
                )
                CarrierPill(
                    name = "Indian Post",
                    sub = "National Fallback",
                    color = IndiaPostRed
                )
            }
        }
    }
}

@Composable
private fun CarrierPill(name: String, sub: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = Color(0xFFE2E8F0), fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun WalletBanner(walletBal: Double, onOpenWallet: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpenWallet() }
            .testTag("dashboard_wallet_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PrimaryNavy.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PrimaryNavy, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("In-App Wallet Balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${"%.2f".format(walletBal)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ActiveShipmentCard(
    shipment: ShipmentBooking,
    onTrack: () -> Unit,
    onCopyAwb: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("active_shipment_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarrierBadge(carrier = shipment.carrierAssigned)
                StatusBadge(status = shipment.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FROM",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = shipment.sender.city.ifBlank { "Origin" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = shipment.sender.pincode,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "TO",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = shipment.receiver.city.ifBlank { "Destination" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = shipment.receiver.pincode,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val progress = when (shipment.status) {
                ShipmentStatus.BOOKED -> 0.15f
                ShipmentStatus.PICKUP_SCHEDULED -> 0.35f
                ShipmentStatus.PICKED_UP -> 0.55f
                ShipmentStatus.IN_TRANSIT -> 0.75f
                ShipmentStatus.OUT_FOR_DELIVERY -> 0.90f
                ShipmentStatus.DELIVERED -> 1.0f
                else -> 0.5f
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AWB: ${shipment.awbNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onCopyAwb,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy AWB",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = onTrack,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Live Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QuickActionsGrid(
    onBookParcel: () -> Unit,
    onRateCompare: () -> Unit,
    onOpenAddressBook: () -> Unit,
    onOpenWallet: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Book Parcel",
                subtitle = "Zone Flat Rates",
                icon = Icons.Default.Add,
                color = PrimaryNavy,
                modifier = Modifier.weight(1f),
                onClick = onBookParcel,
                tag = "quick_book_parcel"
            )
            ActionTile(
                title = "Rate Matrix",
                subtitle = "Zone A to E Calculator",
                icon = Icons.Default.Calculate,
                color = AccentAmber,
                modifier = Modifier.weight(1f),
                onClick = onRateCompare,
                tag = "quick_compare_rates"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Address Book",
                subtitle = "Warehouses & Drops",
                icon = Icons.Default.MenuBook,
                color = ShadowfaxTeal,
                modifier = Modifier.weight(1f),
                onClick = onOpenAddressBook,
                tag = "quick_address_book"
            )
            ActionTile(
                title = "In-App Wallet",
                subtitle = "3x / Week Settlements",
                icon = Icons.Default.AccountBalanceWallet,
                color = PrimaryNavyLight,
                modifier = Modifier.weight(1f),
                onClick = onOpenWallet,
                tag = "quick_wallet"
            )
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = modifier
            .testTag(tag)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RatePolicyNotice(onOpenRates: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpenRates() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("PS Courier Pricing Policy", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Zone A ₹80 • Zone B ₹90 • Zone C/D ₹100 • Zone E ₹120\nCOD Flat ₹50 • RTO = 100% Rate • Tax Inclusive of 18% GST", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ShipmentListItem(
    shipment: ShipmentBooking,
    onTrack: () -> Unit,
    onPrintLabel: () -> Unit,
    onCopyAwb: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("shipment_item_${shipment.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CarrierBadge(carrier = shipment.carrierAssigned)
                    Spacer(modifier = Modifier.width(6.dp))
                    ZoneBadge(zone = shipment.rateInfo.zone)
                }
                StatusBadge(status = shipment.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${shipment.sender.city} → ${shipment.receiver.city}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "AWB: ${shipment.awbNumber}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${"%.2f".format(shipment.rateInfo.totalPayable)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (shipment.paymentMode.name == "COD") "COD (₹50 Fee)" else shipment.paymentMode.title,
                        fontSize = 10.sp,
                        color = if (shipment.isPaid) AccentEmerald else AccentAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onPrintLabel,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Label", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onTrack,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Track", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyShipmentsPlaceholder(onBook: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("No consignments in this filter", fontWeight = FontWeight.Bold)
            Text("Book a new parcel with Shadowfax or Indian Post", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = onBook) {
                Text("Book a Parcel")
            }
        }
    }
}
