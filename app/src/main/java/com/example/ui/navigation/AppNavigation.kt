package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ShipmentBooking
import com.example.ui.screens.AddressBookScreen
import com.example.ui.screens.BookingWizardScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HelpCenterScreen
import com.example.ui.screens.RateComparisonScreen
import com.example.ui.screens.ShippingLabelScreen
import com.example.ui.screens.TrackingScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.viewmodel.CourierViewModel

sealed class Screen(val title: String, val icon: ImageVector) {
    object Dashboard : Screen("Home", Icons.Default.Home)
    object Booking : Screen("Book", Icons.Default.Add)
    object RateCalculator : Screen("Rates", Icons.Default.Calculate)
    object Wallet : Screen("Wallet", Icons.Default.AccountBalanceWallet)
    object Tracking : Screen("Track", Icons.Default.LocationOn)
    object AddressBook : Screen("Addresses", Icons.Default.MenuBook)
    object HelpCenter : Screen("Help", Icons.Default.HelpOutline)
    object ShippingLabel : Screen("Labels", Icons.Default.Print)
}

@Composable
fun MainApp(
    viewModel: CourierViewModel = viewModel()
) {
    val navStack = remember { mutableStateListOf<Screen>(Screen.Dashboard) }
    val currentScreen = navStack.lastOrNull() ?: Screen.Dashboard

    var labelShipmentTarget by remember { mutableStateOf<ShipmentBooking?>(null) }
    var trackingTargetAwb by remember { mutableStateOf<String?>(null) }

    fun navigateTo(screen: Screen) {
        if (screen == Screen.Dashboard) {
            navStack.clear()
            navStack.add(Screen.Dashboard)
        } else {
            if (navStack.lastOrNull() != screen) {
                navStack.add(screen)
            }
        }
    }

    fun popBack() {
        if (navStack.size > 1) {
            navStack.removeLast()
        }
    }

    BackHandler(enabled = navStack.size > 1) {
        popBack()
    }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Booking,
        Screen.RateCalculator,
        Screen.Wallet,
        Screen.Tracking
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (screen == Screen.Booking) {
                                viewModel.resetBooking()
                            }
                            navigateTo(screen)
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_${screen.title.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is Screen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToBooking = {
                            viewModel.resetBooking()
                            navigateTo(Screen.Booking)
                        },
                        onNavigateToRateCalculator = { navigateTo(Screen.RateCalculator) },
                        onNavigateToTracking = { awb ->
                            trackingTargetAwb = awb
                            navigateTo(Screen.Tracking)
                        },
                        onNavigateToLabel = { shipment ->
                            labelShipmentTarget = shipment
                            navigateTo(Screen.ShippingLabel)
                        },
                        onNavigateToWallet = { navigateTo(Screen.Wallet) },
                        onNavigateToAddressBook = { navigateTo(Screen.AddressBook) },
                        onNavigateToHelpCenter = { navigateTo(Screen.HelpCenter) }
                    )
                }

                is Screen.Booking -> {
                    BookingWizardScreen(
                        viewModel = viewModel,
                        onNavigateBack = { popBack() },
                        onViewLabel = { shipment ->
                            labelShipmentTarget = shipment
                            navigateTo(Screen.ShippingLabel)
                        },
                        onTrackShipment = { awb ->
                            trackingTargetAwb = awb
                            navigateTo(Screen.Tracking)
                        },
                        onOpenAddressBook = { navigateTo(Screen.AddressBook) }
                    )
                }

                is Screen.RateCalculator -> {
                    RateComparisonScreen(
                        viewModel = viewModel,
                        onBookWithCalculatedRoute = {
                            viewModel.resetBooking()
                            viewModel.setBookingStep(1)
                            navigateTo(Screen.Booking)
                        }
                    )
                }

                is Screen.Wallet -> {
                    WalletScreen(
                        viewModel = viewModel,
                        onNavigateBack = { popBack() }
                    )
                }

                is Screen.AddressBook -> {
                    AddressBookScreen(
                        viewModel = viewModel,
                        onNavigateBack = { popBack() },
                        onSelectForBooking = { addr ->
                            viewModel.selectAddressFromBook(addr, true)
                            popBack()
                        }
                    )
                }

                is Screen.Tracking -> {
                    TrackingScreen(
                        viewModel = viewModel,
                        initialAwb = trackingTargetAwb,
                        onViewLabel = { shipment ->
                            labelShipmentTarget = shipment
                            navigateTo(Screen.ShippingLabel)
                        }
                    )
                }

                is Screen.HelpCenter -> {
                    HelpCenterScreen(
                        onNavigateBack = { popBack() }
                    )
                }

                is Screen.ShippingLabel -> {
                    ShippingLabelScreen(
                        viewModel = viewModel,
                        shipment = labelShipmentTarget,
                        onNavigateBack = { popBack() }
                    )
                }
            }
        }
    }
}
