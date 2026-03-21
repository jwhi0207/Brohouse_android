package com.bennybokki.frientrip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bennybokki.frientrip.TripListViewModel
import com.bennybokki.frientrip.auth.AuthViewModel
import com.bennybokki.frientrip.data.Trip
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TripFilter { Upcoming, Past, Invites }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    viewModel: TripListViewModel,
    authViewModel: AuthViewModel,
    onNavigateToTrip: (String) -> Unit
) {
    val trips by viewModel.trips.collectAsState()
    val pendingInviteTrips by viewModel.pendingInviteTrips.collectAsState()
    var selectedTab by remember { mutableStateOf(TripFilter.Upcoming) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showJoinCodeDialog by remember { mutableStateOf(false) }
    val joinCodeError by viewModel.joinCodeError.collectAsState()
    val joinCodeLoading by viewModel.joinCodeLoading.collectAsState()
    val joinCodeSuccess by viewModel.joinCodeSuccess.collectAsState()

    LaunchedEffect(joinCodeSuccess) {
        if (joinCodeSuccess) {
            showJoinCodeDialog = false
            viewModel.clearJoinCodeSuccess()
        }
    }

    // TODO: split Upcoming/Past by startDate when that field is added to Trip
    val displayedTrips = when (selectedTab) {
        TripFilter.Upcoming, TripFilter.Past -> trips
        TripFilter.Invites -> pendingInviteTrips
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Trips",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { showJoinCodeDialog = true }) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = "Join with Code",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = { showSignOutDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Trip")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Tab row ───────────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TripFilter.entries) { tab ->
                    val selected = tab == selectedTab
                    val badgeCount = if (tab == TripFilter.Invites) pendingInviteTrips.size else 0
                    Surface(
                        onClick = { selectedTab = tab },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                tab.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (badgeCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Trip list ─────────────────────────────────────────────────────
            if (displayedTrips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            when (selectedTab) {
                                TripFilter.Upcoming -> "No trips yet"
                                TripFilter.Past -> "No past trips"
                                TripFilter.Invites -> "No pending invites"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (selectedTab) {
                                TripFilter.Upcoming -> "Tap + to create your first trip"
                                TripFilter.Past -> "Completed trips will appear here"
                                TripFilter.Invites -> "Invites are accepted automatically when you sign in"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 4.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedTrips, key = { it.id }) { trip ->
                        if (selectedTab == TripFilter.Invites) {
                            InviteCard(
                                trip = trip,
                                onAccept = { viewModel.acceptInvite(trip.id) }
                            )
                        } else {
                            TripCard(trip = trip, onClick = { onNavigateToTrip(trip.id) })
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateTripSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name ->
                viewModel.createTrip(name)
                showCreateSheet = false
            }
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.signOut()
                    showSignOutDialog = false
                }) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showJoinCodeDialog) {
        JoinWithCodeDialog(
            error = joinCodeError,
            isLoading = joinCodeLoading,
            onJoin = { code -> viewModel.joinByCode(code) },
            onDismiss = {
                viewModel.clearJoinCodeError()
                showJoinCodeDialog = false
            }
        )
    }

}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit) {
    val currency = NumberFormat.getCurrencyInstance(Locale.US)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Background image or placeholder
            if (!trip.thumbnailURL.isNullOrBlank()) {
                AsyncImage(
                    model = trip.thumbnailURL,
                    contentDescription = trip.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        trip.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.4f to Color.Transparent,
                            1f to Color(0xCC000000)
                        )
                    )
            )

            // Cost — top right
            if (trip.totalCost > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xBB000000),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            currency.format(trip.totalCost),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF40C4FF)
                        )
                        Text(
                            "Total Stay",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Bottom — trip name + chips
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trip.checkInMillis > 0 && trip.checkOutMillis > 0) {
                        val fmt = SimpleDateFormat("MMM d", Locale.US)
                        HeroChip(
                            icon = Icons.Default.CalendarMonth,
                            label = "${fmt.format(Date(trip.checkInMillis))} – ${fmt.format(Date(trip.checkOutMillis))}"
                        )
                    }
                    HeroChip(
                        icon = Icons.Default.Group,
                        label = "${trip.memberIds.size} ${if (trip.memberIds.size == 1) "Guest" else "Guests"}"
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteCard(trip: Trip, onAccept: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    trip.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    trip.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${trip.memberIds.size} ${if (trip.memberIds.size == 1) "member" else "members"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Button(
                onClick = onAccept,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Join", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripSheet(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    val trimmed = name.trim()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Create New Trip", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip Name") },
                placeholder = { Text("e.g. Weekend in Paris") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = { onCreate(trimmed) },
                    enabled = trimmed.isNotEmpty(),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Create") }
            }
        }
    }
}

@Composable
private fun JoinWithCodeDialog(
    error: String?,
    isLoading: Boolean,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val stripped = textFieldValue.text.filter { it.isLetterOrDigit() }
    val canJoin = stripped.length == 8 && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join with Invite Code") },
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        val newRaw = newValue.text.uppercase().filter { it.isLetterOrDigit() }.take(8)
                        val newFormatted = if (newRaw.length > 4) "${newRaw.substring(0, 4)}-${newRaw.substring(4)}" else newRaw
                        textFieldValue = TextFieldValue(
                            text = newFormatted,
                            selection = TextRange(newFormatted.length)
                        )
                    },
                    placeholder = { Text("XXXX-XXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(textFieldValue.text) },
                enabled = canJoin
            ) { Text("Join") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
