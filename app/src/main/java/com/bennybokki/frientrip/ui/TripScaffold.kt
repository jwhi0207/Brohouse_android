package com.bennybokki.frientrip.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bennybokki.frientrip.TripViewModel

private data class TripTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val tripTabs = listOf(
    TripTab("dashboard", "Trip",     Icons.Outlined.Map,         Icons.Filled.Map),
    TripTab("house",     "Lodging",  Icons.Outlined.Hotel,       Icons.Filled.Hotel),
    TripTab("supplies",  "Supplies", Icons.Outlined.Checklist,   Icons.Filled.Checklist),
    TripTab("carpool",   "Carpool",  Icons.Outlined.DirectionsCar, Icons.Filled.DirectionsCar),
    TripTab("group",     "Group",    Icons.Outlined.Group,       Icons.Filled.Group),
)

@Composable
fun TripScaffold(
    viewModel: TripViewModel,
    isAdmin: Boolean,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun navigateToTab(route: String) {
        innerNav.navigate(route) {
            popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tripTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(tab.route) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNav,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                TripDashboard(
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    onNavigateToHouseDetails = { navigateToTab("house") },
                    onNavigateToSupplies = { navigateToTab("supplies") },
                    onNavigateToCarpool = { navigateToTab("carpool") },
                    onNavigateToInvite = { navigateToTab("group") },
                    onNavigateBack = onNavigateBack,
                    onSignOut = onSignOut
                )
            }
            composable("house") {
                HouseDetailsScreen(
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }
            composable("supplies") {
                SuppliesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }
            composable("carpool") {
                CarpoolScreen(
                    viewModel = viewModel,
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }
            composable("group") {
                InviteScreen(
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }
        }
    }
}

