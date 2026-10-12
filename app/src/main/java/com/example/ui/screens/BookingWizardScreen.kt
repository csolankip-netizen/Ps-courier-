package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SavedAddressEntity
import com.example.data.model.Address
import com.example.data.model.CarrierPartner
import com.example.data.model.DeliveryZone
import com.example.data.model.ParcelCategory
import com.example.data.model.ParcelDetails
import com.example.data.model.PaymentMode
import com.example.data.model.PsRateCalculation
import com.example.data.model.ShipmentBooking
import com.example.ui.components.CarrierBadge
import com.example.ui.components.RazorpayCheckoutDialog
import com.example.ui.components.ZoneBadge
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.IndiaPostRed
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.ShadowfaxTeal
import com.example.ui.viewmodel.CourierViewModel

@Composable
fun BookingWizardScreen(
    viewModel: CourierViewModel,
    onNavigateBack: () -> Unit,
    onViewLabel: (ShipmentBooking) -> Unit,
    onTrackShipment: (String) -> Unit,
    onOpenAddressBook: () -> Unit
) {
    val step by viewModel.bookingStep.collectAsState()
    val sender by viewModel.senderAddress.collectAsState()
    val receiver by viewModel.receiverAddress.collectAsState()
    val parcel by viewModel.parcelDetails.collectAsState()
    val calculatedRate by viewModel.calculatedRate.collectAsState()
    val paymentMode by viewModel.selectedPaymentMode.collectAsState()
    val pickupSlot by viewModel.pickupSlot.collectAsState()
    val isLoadingRate by viewModel.isLoadingRate.collectAsState()
    val isBookingInProgress by viewModel.isBookingInProgress.collectAsState()
    val confirmedBooking by viewModel.confirmedBooking.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val forceShadowfaxUnavailable by viewModel.forceShadowfaxUnavailable.collectAsState()

    var showRazorpayDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("booking_wizard_screen")
    ) {
        // --- Top Bar with Step Indicators ---
        BookingWizardTopBar(
            currentStep = step,
            onBack = {
                if (step > 1 && step < 5) {
                    viewModel.setBookingStep(step - 1)
                } else {
                    onNavigateBack()
                }
            }
        )

        StepProgressBar(currentStep = step)

        // --- Step Content ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (step) {
                1 -> Step1Addresses(
                    sender = sender,
                    receiver = receiver,
                    savedAddresses = savedAddresses,
                    onUpdateSender = { viewModel.updateSenderAddress(it) },
                    onUpdateReceiver = { viewModel.updateReceiverAddress(it) },
                    onSelectFromBook = { addr, isSender -> viewModel.selectAddressFromBook(addr, isSender) },
                    onOpenAddressBook = onOpenAddressBook,
                    onNext = {
                        if (sender.pincode.length < 6 || receiver.pincode.length < 6) {
                            Toast.makeText(context, "Please enter valid 6-digit Pincodes", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.setBookingStep(2)
                        }
                    }
                )
                2 -> Step2ParcelDetails(
                    parcel = parcel,
                    onUpdateParcel = { viewModel.updateParcelDetails(it) },
                    onNext = {
                        viewModel.calculateRateForCurrentBooking()
                        viewModel.setBookingStep(3)
                    }
                )
                3 -> Step3RoutingAndRates(
                    rate = calculatedRate,
                    isLoading = isLoadingRate,
                    forceShadowfaxUnavailable = forceShadowfaxUnavailable,
                    onToggleShadowfaxAvailability = { viewModel.toggleForceShadowfaxUnavailable(it) },
                    onRefresh = { viewModel.calculateRateForCurrentBooking() },
                    onNext = {
                        viewModel.setBookingStep(4)
                    }
                )
                4 -> Step4PaymentAndPickup(
                    rate = calculatedRate,
                    paymentMode = paymentMode,
                    walletBalance = walletBalance,
                    pickupSlot = pickupSlot,
                    isBooking = isBookingInProgress,
                    onSelectPayment = { viewModel.setPaymentMode(it) },
                    onSelectPickupSlot = { viewModel.setPickupSlot(it) },
                    onConfirmBooking = {
                        if (paymentMode == PaymentMode.RAZORPAY) {
                            showRazorpayDialog = true
                        } else if (paymentMode == PaymentMode.WALLET && (calculatedRate?.totalPayable ?: 0.0) > walletBalance) {
                            Toast.makeText(context, "Insufficient wallet balance. Please recharge via Razorpay or choose COD.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.confirmBooking()
                        }
                    }
                )
                5 -> Step5Confirmation(
                    booking = confirmedBooking,
                    onViewLabel = { confirmedBooking?.let { onViewLabel(it) } },
                    onTrack = { confirmedBooking?.let { onTrackShipment(it.awbNumber) } },
                    onBookAnother = { viewModel.resetBooking() }
                )
            }
        }
    }

    if (showRazorpayDialog && calculatedRate != null) {
        RazorpayCheckoutDialog(
            amountInr = calculatedRate!!.totalPayable,
            description = "PS Courier Parcel Freight (${calculatedRate!!.zone.code})",
            onDismiss = { showRazorpayDialog = false },
            onPaymentSuccess = { result ->
                showRazorpayDialog = false
                viewModel.confirmBooking(
                    razorpayPaymentId = result.paymentId,
                    razorpayOrderId = result.orderId
                )
            }
        )
    }
}

@Composable
private fun BookingWizardTopBar(currentStep: Int, onBack: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = when (currentStep) {
                    1 -> "Step 1: Addresses"
                    2 -> "Step 2: Parcel Dimensions"
                    3 -> "Step 3: Serviceability & Rate"
                    4 -> "Step 4: Payment & Pickup"
                    5 -> "Booking Confirmed"
                    else -> "Book Parcel"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StepProgressBar(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val steps = listOf("Addresses", "Parcel", "Routing", "Payment", "Manifest")
        steps.forEachIndexed { index, title ->
            val stepNum = index + 1
            val isDone = stepNum < currentStep
            val isCurrent = stepNum == currentStep

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            when {
                                isDone -> AccentEmerald
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    } else {
                        Text(
                            text = "$stepNum",
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- STEP 1: ADDRESSES ---
@Composable
private fun Step1Addresses(
    sender: Address,
    receiver: Address,
    savedAddresses: List<SavedAddressEntity>,
    onUpdateSender: (Address) -> Unit,
    onUpdateReceiver: (Address) -> Unit,
    onSelectFromBook: (SavedAddressEntity, Boolean) -> Unit,
    onOpenAddressBook: () -> Unit,
    onNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Address Book Quick Selector
        if (savedAddresses.isNotEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick from Address Book", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text(
                                text = "Manage",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onOpenAddressBook() }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedAddresses) { addr ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable { onSelectFromBook(addr, true) }
                                ) {
                                    Text(
                                        text = "From: ${addr.name} (${addr.city})",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sender Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pickup Details (FROM / ORIGIN)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sender.name,
                        onValueChange = { onUpdateSender(sender.copy(name = it)) },
                        label = { Text("Sender / Facility Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sender.phone,
                            onValueChange = { onUpdateSender(sender.copy(phone = it)) },
                            label = { Text("Phone Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sender.pincode,
                            onValueChange = { if (it.length <= 6) onUpdateSender(sender.copy(pincode = it)) },
                            label = { Text("PIN (6 digits)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sender.city,
                            onValueChange = { onUpdateSender(sender.copy(city = it)) },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sender.state,
                            onValueChange = { onUpdateSender(sender.copy(state = it)) },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sender.street,
                        onValueChange = { onUpdateSender(sender.copy(street = it)) },
                        label = { Text("Street Address / Warehouse Plot") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Receiver Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(AccentAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delivery Details (TO / DESTINATION)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = receiver.name,
                        onValueChange = { onUpdateReceiver(receiver.copy(name = it)) },
                        label = { Text("Customer / Receiver Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = receiver.phone,
                            onValueChange = { onUpdateReceiver(receiver.copy(phone = it)) },
                            label = { Text("Phone Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = receiver.pincode,
                            onValueChange = { if (it.length <= 6) onUpdateReceiver(receiver.copy(pincode = it)) },
                            label = { Text("PIN (6 digits)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = receiver.city,
                            onValueChange = { onUpdateReceiver(receiver.copy(city = it)) },
                            label = { Text("City") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = receiver.state,
                            onValueChange = { onUpdateReceiver(receiver.copy(state = it)) },
                            label = { Text("State") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = receiver.street,
                        onValueChange = { onUpdateReceiver(receiver.copy(street = it)) },
                        label = { Text("Door No, Apartment, Street") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step1_continue_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue to Parcel Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// --- STEP 2: PARCEL DETAILS ---
@Composable
private fun Step2ParcelDetails(
    parcel: ParcelDetails,
    onUpdateParcel: (ParcelDetails) -> Unit,
    onNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Select Parcel Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ParcelCategory.entries.toTypedArray()) { cat ->
                    val isSel = parcel.category == cat
                    Surface(
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { onUpdateParcel(parcel.copy(category = cat)) }
                    ) {
                        Text(
                            text = cat.label,
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dead Weight", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${"%.1f".format(parcel.weightKg)} kg", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Slider(
                        value = parcel.weightKg.toFloat(),
                        onValueChange = { onUpdateParcel(parcel.copy(weightKg = it.toDouble())) },
                        valueRange = 0.1f..15f,
                        steps = 29
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5, 1.0, 2.0, 5.0).forEach { qw ->
                            OutlinedButton(
                                onClick = { onUpdateParcel(parcel.copy(weightKg = qw)) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("${qw}kg", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dimensions (cm)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = parcel.lengthCm.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { l -> onUpdateParcel(parcel.copy(lengthCm = l)) } },
                            label = { Text("L") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = parcel.widthCm.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { w -> onUpdateParcel(parcel.copy(widthCm = w)) } },
                            label = { Text("W") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = parcel.heightCm.toString(),
                            onValueChange = { it.toDoubleOrNull()?.let { h -> onUpdateParcel(parcel.copy(heightCm = h)) } },
                            label = { Text("H") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = parcel.declaredValue.toInt().toString(),
                        onValueChange = { it.toDoubleOrNull()?.let { dv -> onUpdateParcel(parcel.copy(declaredValue = dv)) } },
                        label = { Text("Declared Value (₹) for Insurance / COD") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step2_continue_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Check Serviceability & Rates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// --- STEP 3: ROUTING & TARIFF MATRIX ---
@Composable
private fun Step3RoutingAndRates(
    rate: PsRateCalculation?,
    isLoading: Boolean,
    forceShadowfaxUnavailable: Boolean,
    onToggleShadowfaxAvailability: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onNext: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(14.dp))
                Text("Testing Shadowfax Serviceability API...", fontWeight = FontWeight.Bold)
                Text("Checking Indian Post fallback route...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    if (rate == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onRefresh) { Text("Calculate Rate") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Partner Serviceability Result Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Carrier Priority Routing Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CarrierBadge(carrier = rate.carrierAssigned)
                        ZoneBadge(zone = rate.zone)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = if (rate.isShadowfaxServiceable) ShadowfaxTeal.copy(alpha = 0.12f) else IndiaPostRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (rate.isShadowfaxServiceable) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (rate.isShadowfaxServiceable) ShadowfaxTeal else IndiaPostRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rate.routingNote,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (rate.isShadowfaxServiceable) ShadowfaxTeal else IndiaPostRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulation switch: Test Shadowfax unavailable fallback to Indian Post
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
                            Text("Forces routing to Indian Post fallback", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = forceShadowfaxUnavailable,
                            onCheckedChange = { onToggleShadowfaxAvailability(it) }
                        )
                    }
                }
            }
        }

        // Exact PS Courier Rate Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PS Courier Pricing Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    TariffRow("${rate.zone.code} Flat Delivery Rate", "₹${rate.baseDeliveryCharge.toInt()} (Incl. 18% GST)")
                    if (rate.codCharge > 0) {
                        TariffRow("Cash on Delivery (COD) Fee", "₹${rate.codCharge.toInt()} (Incl. 18% GST)")
                    }
                    TariffRow("RTO Return Shipping Rate (If Returned)", "₹${rate.rtoCharge.toInt()} (Same as original)")
                    TariffRow("Net Taxable Value (Before Tax)", "₹${"%.2f".format(rate.taxableBaseFreight + rate.taxableCodBase)}")
                    TariffRow("Embedded 18% GST Component", "₹${"%.2f".format(rate.gst)}")

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Payable Freight", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("Tax inclusive of 18% GST", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            text = "₹${"%.2f".format(rate.totalPayable)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("step3_continue_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Proceed to Payment & Pickup Slot", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TariffRow(title: String, value: String) {
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

// --- STEP 4: PAYMENT AND PICKUP ---
@Composable
private fun Step4PaymentAndPickup(
    rate: PsRateCalculation?,
    paymentMode: PaymentMode,
    walletBalance: Double,
    pickupSlot: String,
    isBooking: Boolean,
    onSelectPayment: (PaymentMode) -> Unit,
    onSelectPickupSlot: (String) -> Unit,
    onConfirmBooking: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Pickup slot selection
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Doorstep Courier Pickup Window", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    listOf(
                        "Tomorrow 10:00 AM - 1:00 PM",
                        "Tomorrow 2:00 PM - 5:00 PM",
                        "Today Express (Within 2 Hours)"
                    ).forEach { slot ->
                        val isSel = pickupSlot == slot
                        Surface(
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPickupSlot(slot) }
                                .padding(vertical = 3.dp)
                        ) {
                            Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSel, onClick = { onSelectPickupSlot(slot) })
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(slot, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        // Payment method selection
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    PaymentMode.entries.forEach { mode ->
                        val isSel = paymentMode == mode
                        Surface(
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPayment(mode) }
                                .padding(vertical = 3.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = isSel, onClick = { onSelectPayment(mode) })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(mode.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (mode == PaymentMode.WALLET) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(Bal: ₹${"%.0f".format(walletBalance)})", fontSize = 11.sp, color = AccentEmerald, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(mode.subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Settlement notice
        item {
            Surface(
                color = AccentEmerald.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COD settlements occur 3 times a week (Mon, Wed, Fri) to your wallet or bank account.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentEmerald
                    )
                }
            }
        }

        item {
            Button(
                onClick = onConfirmBooking,
                enabled = !isBooking,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_booking_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isBooking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registering Consignment...")
                } else {
                    Text(
                        text = "Confirm Booking (₹${"%.2f".format(rate?.totalPayable ?: 0.0)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// --- STEP 5: CONFIRMATION & MANIFEST ---
@Composable
private fun Step5Confirmation(
    booking: ShipmentBooking?,
    onViewLabel: () -> Unit,
    onTrack: () -> Unit,
    onBookAnother: () -> Unit
) {
    val context = LocalContext.current

    if (booking == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(AccentEmerald.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Consignment Booked Successfully!", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Handover assigned to ${booking.carrierAssigned.displayName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CarrierBadge(carrier = booking.carrierAssigned)
                        ZoneBadge(zone = booking.rateInfo.zone)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("AWB / WAYBILL NUMBER", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = booking.awbNumber,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AWB", booking.awbNumber))
                                Toast.makeText(context, "AWB copied: ${booking.awbNumber}", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pickup Slot", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(booking.pickupSlot, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Routing Code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(booking.routingCode, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (booking.razorpayPaymentId != null) "Paid via Razorpay Gateway"
                                    else if (booking.paymentMode == PaymentMode.WALLET) "Paid via PS Courier Wallet"
                                    else "Cash on Delivery (COD)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (booking.isPaid) AccentEmerald else AccentAmber
                                )
                                Text(
                                    text = "₹${"%.2f".format(booking.rateInfo.totalPayable)} (Incl. 18% GST)",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (booking.razorpayPaymentId != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Payment ID: ${booking.razorpayPaymentId} • Order: ${booking.razorpayOrderId ?: "N/A"}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onViewLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("view_shipping_label_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View 4x6 Shipping Label", fontWeight = FontWeight.Bold)
            }
        }

        item {
            OutlinedButton(
                onClick = onTrack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Track Live Parcel")
            }
        }

        item {
            OutlinedButton(
                onClick = onBookAnother,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Book Another Parcel")
            }
        }
    }
}
