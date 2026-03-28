package com.bennybokki.frientrip.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bennybokki.frientrip.TripCreationParams
import com.bennybokki.frientrip.TripListViewModel
import com.bennybokki.frientrip.auth.AuthViewModel
import com.bennybokki.frientrip.data.Trip
import com.bennybokki.frientrip.ui.theme.ElectricCyan
import com.bennybokki.frientrip.ui.theme.VividCard
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
            onCreate = { params ->
                viewModel.createTrip(params)
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

    VividCard(
        onClick = onClick,
        accentIndex = 0,
        shape = RoundedCornerShape(20.dp),
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
                        if (trip.emoji.isNotBlank()) trip.emoji
                        else trip.name.firstOrNull()?.uppercaseChar()?.toString() ?: "T",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (trip.emoji.isNotBlank()) Color.Unspecified
                                else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
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
                            color = ElectricCyan
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
    VividCard(
        accentIndex = 5,
        shape = RoundedCornerShape(20.dp),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateTripSheet(onDismiss: () -> Unit, onCreate: (TripCreationParams) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var showMoreDetails by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf("") }
    var houseURL by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var checkInMillis by remember { mutableLongStateOf(0L) }
    var checkOutMillis by remember { mutableLongStateOf(0L) }
    var description by remember { mutableStateOf("") }
    var inviteEmails by remember { mutableStateOf(listOf<String>()) }
    var currentEmailInput by remember { mutableStateOf("") }

    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckOutDatePicker by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val trimmed = name.trim()

    val travelEmojis = listOf(
        "\uD83C\uDFD6\uFE0F", // beach
        "\u26F0\uFE0F",       // mountain
        "\uD83C\uDFC2",       // snowboard
        "\uD83C\uDFD5\uFE0F", // camping
        "\u2708\uFE0F",       // plane
        "\uD83C\uDFE0",       // house
        "\uD83C\uDF34",       // palm tree
        "\uD83C\uDF89",       // party
        "\u2600\uFE0F",       // sun
        "\u2744\uFE0F",       // snowflake
        "\uD83C\uDFDB\uFE0F", // classical building
        "\uD83D\uDE97"        // car
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Create New Trip", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            // ── Trip Name (required) ──
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Trip Name") },
                placeholder = { Text("e.g. Weekend in Paris") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // ── "Add more details" toggle ──
            TextButton(
                onClick = { showMoreDetails = !showMoreDetails },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (showMoreDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (showMoreDetails) "Less details" else "Add more details")
            }

            // ── Expandable optional fields ──
            AnimatedVisibility(visible = showMoreDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // ── Emoji ──
                    Text(
                        "TRIP VIBE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(travelEmojis.size) { index ->
                            val emoji = travelEmojis[index]
                            val isSelected = emoji == selectedEmoji
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        selectedEmoji = if (isSelected) "" else emoji
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(emoji, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }

                    // ── Dates ──
                    Text(
                        "DATES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = if (checkInMillis > 0) formatDate(checkInMillis) else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Check-in") },
                            placeholder = { Text("Select date") },
                            trailingIcon = {
                                IconButton(onClick = { showCheckInDatePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", modifier = Modifier.size(18.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = if (checkOutMillis > 0) formatDate(checkOutMillis) else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Check-out") },
                            placeholder = { Text("Select date") },
                            trailingIcon = {
                                IconButton(onClick = { showCheckOutDatePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", modifier = Modifier.size(18.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ── Location ──
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Location") },
                        placeholder = { Text("123 Beach Rd, Malibu, CA") },
                        singleLine = true,
                        trailingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── House URL ──
                    OutlinedTextField(
                        value = houseURL,
                        onValueChange = { houseURL = it },
                        label = { Text("House URL") },
                        placeholder = { Text("https://rentals.com/villa-123") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { houseURL = it }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Cost ──
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Total Cost") },
                        placeholder = { Text("0.00") },
                        singleLine = true,
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Description ──
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        placeholder = { Text("What's this trip about?") },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── Invite Friends ──
                    Text(
                        "INVITE FRIENDS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentEmailInput,
                            onValueChange = { currentEmailInput = it },
                            placeholder = { Text("friend@email.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilledIconButton(
                            onClick = {
                                val email = currentEmailInput.trim().lowercase()
                                if (email.contains("@") && email !in inviteEmails) {
                                    inviteEmails = inviteEmails + email
                                    currentEmailInput = ""
                                }
                            },
                            enabled = currentEmailInput.trim().contains("@"),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add email")
                        }
                    }
                    if (inviteEmails.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            inviteEmails.forEach { email ->
                                InputChip(
                                    selected = false,
                                    onClick = { inviteEmails = inviteEmails - email },
                                    label = { Text(email, style = MaterialTheme.typography.bodySmall) },
                                    trailingIcon = {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Action buttons ──
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
                    onClick = {
                        onCreate(
                            TripCreationParams(
                                name = trimmed,
                                houseURL = houseURL.trim(),
                                address = address.trim(),
                                totalCost = costText.trim().toDoubleOrNull() ?: 0.0,
                                checkInMillis = checkInMillis,
                                checkOutMillis = checkOutMillis,
                                description = description.trim(),
                                emoji = selectedEmoji,
                                inviteEmails = inviteEmails
                            )
                        )
                    },
                    enabled = trimmed.isNotEmpty(),
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Create") }
            }
        }
    }

    // ── Date Pickers ──
    if (showCheckInDatePicker) {
        AppDatePickerDialog(
            initialMillis = checkInMillis,
            onDismiss = { showCheckInDatePicker = false },
            onConfirm = { millis ->
                checkInMillis = mergeDateTime(millis, checkInMillis)
                showCheckInDatePicker = false
            }
        )
    }
    if (showCheckOutDatePicker) {
        AppDatePickerDialog(
            initialMillis = checkOutMillis,
            onDismiss = { showCheckOutDatePicker = false },
            onConfirm = { millis ->
                checkOutMillis = mergeDateTime(millis, checkOutMillis)
                showCheckOutDatePicker = false
            }
        )
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
