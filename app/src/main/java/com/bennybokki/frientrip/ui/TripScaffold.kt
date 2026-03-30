package com.bennybokki.frientrip.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bennybokki.frientrip.TripViewModel
import kotlinx.coroutines.launch

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
    onNavigateToProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val innerNav = rememberNavController()
    val backStackEntry by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val trip by viewModel.trip.collectAsState()
    val members by viewModel.members.collectAsState()
    val isTripAdmin = isAdmin || trip?.ownerId == viewModel.currentUid
    val isOwner = trip?.ownerId == viewModel.currentUid
    val currentMember = members.find { it.uid == viewModel.currentUid }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun openDrawer() = scope.launch { drawerState.open() }
    fun closeDrawer() = scope.launch { drawerState.close() }

    fun navigateToTab(route: String) {
        innerNav.navigate(route) {
            popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val effectiveRoute = when (currentRoute) {
        "expenses" -> "dashboard"
        else -> currentRoute
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Trip?") },
            text = { Text("This will permanently delete \"${trip?.name}\" and all its data for everyone. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTrip { onNavigateBack() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(20.dp))

                // Profile avatar — tappable, navigates to profile screen
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(CircleShape)
                        .clickable {
                            closeDrawer()
                            onNavigateToProfile()
                        }
                ) {
                    if (currentMember != null) {
                        AvatarView(
                            seed = currentMember.avatarSeed,
                            colorIndex = currentMember.avatarColor,
                            name = currentMember.displayName,
                            size = 56.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Profile",
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                if (currentMember != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        currentMember.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Current trip name
                trip?.name?.let { name ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Change Trips
                NavigationDrawerItem(
                    label = { Text("Change Trips") },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                    selected = false,
                    onClick = {
                        closeDrawer()
                        onNavigateBack()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Trip Announcements (stub)
                NavigationDrawerItem(
                    label = { Text("Trip Announcements") },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    selected = false,
                    onClick = { closeDrawer() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Admin/owner-only items
                if (isTripAdmin) {
                    NavigationDrawerItem(
                        label = { Text("Manage Group") },
                        icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null) },
                        selected = false,
                        onClick = {
                            closeDrawer()
                            innerNav.navigate("manage_group")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    NavigationDrawerItem(
                        label = { Text("Trip History") },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        selected = false,
                        onClick = {
                            closeDrawer()
                            innerNav.navigate("trip_history")
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                HorizontalDivider()

                // Delete Trip (owner only)
                if (isOwner) {
                    NavigationDrawerItem(
                        label = { Text("Delete Trip", color = MaterialTheme.colorScheme.error) },
                        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = {
                            closeDrawer()
                            showDeleteDialog = true
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Sign Out
                NavigationDrawerItem(
                    label = { Text("Sign Out") },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    selected = false,
                    onClick = {
                        closeDrawer()
                        onSignOut()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tripTabs.forEach { tab ->
                        val selected = effectiveRoute == tab.route
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
                        isAdmin = isTripAdmin,
                        onNavigateToHouseDetails = { navigateToTab("house") },
                        onNavigateToSupplies = { navigateToTab("supplies") },
                        onNavigateToCarpool = { navigateToTab("carpool") },
                        onNavigateToInvite = { navigateToTab("group") },
                        onNavigateToExpenses = { innerNav.navigate("expenses") },
                        onOpenDrawer = { openDrawer() }
                    )
                }
                composable("house") {
                    HouseDetailsScreen(
                        viewModel = viewModel,
                        isAdmin = isTripAdmin,
                        onNavigateBack = { innerNav.popBackStack() },
                        onOpenDrawer = { openDrawer() }
                    )
                }
                composable("supplies") {
                    SuppliesScreen(
                        viewModel = viewModel,
                        isAdmin = isTripAdmin,
                        onNavigateBack = { innerNav.popBackStack() },
                        onOpenDrawer = { openDrawer() }
                    )
                }
                composable("carpool") {
                    CarpoolScreen(
                        viewModel = viewModel,
                        onNavigateBack = { innerNav.popBackStack() },
                        onOpenDrawer = { openDrawer() }
                    )
                }
                composable("group") {
                    InviteScreen(
                        viewModel = viewModel,
                        isAdmin = isTripAdmin,
                        onNavigateBack = { innerNav.popBackStack() },
                        onOpenDrawer = { openDrawer() }
                    )
                }
                composable("expenses") {
                    ExpensesScreen(
                        viewModel = viewModel,
                        isAdmin = isTripAdmin,
                        onNavigateBack = { innerNav.popBackStack() }
                    )
                }
                composable("manage_group") {
                    ManageGroupScreen(
                        viewModel = viewModel,
                        onNavigateBack = { innerNav.popBackStack() }
                    )
                }
                composable("trip_history") {
                    TripHistoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { innerNav.popBackStack() }
                    )
                }
            }
        }
    }
}
