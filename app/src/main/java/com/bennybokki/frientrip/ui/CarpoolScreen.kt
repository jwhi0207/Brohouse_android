package com.bennybokki.frientrip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bennybokki.frientrip.TripViewModel
import com.bennybokki.frientrip.data.Ride
import com.bennybokki.frientrip.data.RideRequest
import com.bennybokki.frientrip.data.TripMember
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpoolScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val rides by viewModel.rides.collectAsState()
    val rideRequests by viewModel.rideRequests.collectAsState()
    val members by viewModel.members.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val currentUid = viewModel.currentUid

    var showAddSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Ride?>(null) }
    var editingRide by remember { mutableStateOf<Ride?>(null) }

    val hasMyRequest = rideRequests.any { it.uid == currentUid }
    val alreadyOfferedRide = rides.any { it.driverUid == currentUid }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carpool", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (!alreadyOfferedRide) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Offer a Ride")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Vehicles heading ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vehicles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (rides.isNotEmpty()) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                "${rides.size} Active ${if (rides.size == 1) "Ride" else "Rides"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (rides.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🚗", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No rides yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "Tap + to offer a ride",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(rides, key = { it.id }) { ride ->
                    RideCard(
                        ride = ride,
                        members = members,
                        currentUid = currentUid,
                        canEdit = viewModel.canEditRide(ride),
                        onClaim = { viewModel.claimSeat(ride.id) },
                        onUnclaim = { viewModel.unclaimSeat(ride.id) },
                        onEdit = { editingRide = ride },
                        onDelete = { deleteTarget = ride },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // ── Need a Ride heading ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Need a Ride", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            if (hasMyRequest) viewModel.cancelRideRequest()
                            else viewModel.requestRide()
                        }
                    ) {
                        Icon(
                            if (hasMyRequest) Icons.Default.Cancel else Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (hasMyRequest) "Cancel Request" else "I Need a Ride",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (rideRequests.isEmpty()) {
                item {
                    Text(
                        "Nobody needs a ride yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(rideRequests, key = { it.uid }) { request ->
                    RideRequestRow(
                        request = request,
                        members = members,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddVehicleSheet(
            defaultDepartureMillis = trip?.checkInMillis ?: 0L,
            defaultReturnMillis = trip?.checkOutMillis ?: 0L,
            onDismiss = { showAddSheet = false },
            onConfirm = { emoji, label, location, seats, depTime, retTime, notes ->
                viewModel.addRide(emoji, label, location, seats, depTime, retTime, notes)
                showAddSheet = false
            }
        )
    }

    editingRide?.let { ride ->
        AddVehicleSheet(
            initialRide = ride,
            defaultDepartureMillis = trip?.checkInMillis ?: 0L,
            defaultReturnMillis = trip?.checkOutMillis ?: 0L,
            onDismiss = { editingRide = null },
            onConfirm = { emoji, label, location, seats, depTime, retTime, notes ->
                viewModel.updateRide(ride.id, emoji, label, location, seats, depTime, retTime, notes)
                editingRide = null
            }
        )
    }

    deleteTarget?.let { ride ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Remove Ride") },
            text = { Text("Remove ${ride.driverName}'s ${ride.vehicleLabel}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRide(ride.id); deleteTarget = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Ride card ─────────────────────────────────────────────────────────────────

@Composable
private fun RideCard(
    ride: Ride,
    members: List<TripMember>,
    currentUid: String,
    canEdit: Boolean,
    onClaim: () -> Unit,
    onUnclaim: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPassenger = currentUid in ride.passengerUids
    val isDriver = currentUid == ride.driverUid
    val fmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header ───────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ride.vehicleEmoji, fontSize = 28.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${ride.driverName} – ${ride.vehicleLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (ride.notes.isNotBlank()) {
                        Text(
                            ride.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (canEdit) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit Ride") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = { showMenu = false; onEdit() }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Ride") },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Departure + Return ────────────────────────────────────────────
            if (ride.departureLocation.isNotBlank() || ride.departureTime > 0) {
                RideInfoRow(
                    icon = Icons.Default.TripOrigin,
                    label = "DEPARTURE",
                    value = buildString {
                        if (ride.departureLocation.isNotBlank()) append(ride.departureLocation)
                        if (ride.departureTime > 0) {
                            if (isNotEmpty()) append(" • ")
                            append(fmt.format(Date(ride.departureTime)))
                        }
                    }
                )
            }
            if (ride.returnTime > 0) {
                Spacer(Modifier.height(6.dp))
                RideInfoRow(
                    icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                    label = "RETURN",
                    value = fmt.format(Date(ride.returnTime))
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Seat progress bar ─────────────────────────────────────────────
            val fraction = if (ride.totalSeats > 0) ride.passengerUids.size.toFloat() / ride.totalSeats else 0f
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Seat Availability", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (ride.isFull) {
                    Text("FULL", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                } else {
                    Text(
                        "${ride.passengerUids.size} / ${ride.totalSeats} Occupied",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (ride.isFull) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (!ride.isFull) {
                Text(
                    "${ride.availableSeats} ${if (ride.availableSeats == 1) "seat" else "seats"} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // ── Passengers ───────────────────────────────────────────────────
            if (ride.passengerUids.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "PASSENGERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(6.dp))
                StackedAvatars(
                    passengerUids = ride.passengerUids,
                    members = members
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Action button ─────────────────────────────────────────────────
            when {
                isDriver -> Unit // driver doesn't claim their own ride
                isPassenger -> OutlinedButton(
                    onClick = onUnclaim,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Leave Ride", fontWeight = FontWeight.Bold) }
                ride.isFull -> Button(
                    onClick = {},
                    enabled = false,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Full", fontWeight = FontWeight.Bold) }
                else -> Button(
                    onClick = onClaim,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Claim Seat", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun RideInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StackedAvatars(passengerUids: List<String>, members: List<TripMember>) {
    val avatarSizePx = 28
    val overlapPx = 8
    val passengers = passengerUids.mapNotNull { uid -> members.find { it.uid == uid } }
    val display = passengers.take(5)

    Layout(content = {
        display.forEach { member ->
            Box(
                modifier = Modifier
                    .size(avatarSizePx.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AvatarView(seed = member.avatarSeed, name = member.displayName, size = avatarSizePx.dp)
            }
        }
    }) { measurables, constraints ->
        val size = avatarSizePx.dp.roundToPx()
        val placeables = measurables.map { it.measure(Constraints.fixed(size, size)) }
        val totalWidth = if (placeables.isEmpty()) 0
                         else size + (placeables.size - 1) * (size - overlapPx.dp.roundToPx())
        layout(totalWidth.coerceAtLeast(0), size) {
            val overlap = overlapPx.dp.roundToPx()
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(index * (size - overlap), 0)
            }
        }
    }
}

// ── Ride request row ──────────────────────────────────────────────────────────

@Composable
private fun RideRequestRow(
    request: RideRequest,
    members: List<TripMember>,
    modifier: Modifier = Modifier
) {
    val member = members.find { it.uid == request.uid }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (member != null) {
                AvatarView(seed = member.avatarSeed, name = member.displayName, size = 40.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (request.notes.isNotBlank()) {
                    Text(
                        request.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

// ── Add Vehicle Sheet ─────────────────────────────────────────────────────────

private val vehicleEmojis = listOf("🚗", "🚐", "🛻", "🚌", "🏎️", "🚙")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleSheet(
    initialRide: Ride? = null,
    defaultDepartureMillis: Long,
    defaultReturnMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (emoji: String, label: String, location: String, seats: Int, depTime: Long, retTime: Long, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) { sheetState.expand() }

    var selectedEmoji by remember { mutableStateOf(initialRide?.vehicleEmoji ?: "🚗") }
    var vehicleLabel by remember { mutableStateOf(initialRide?.vehicleLabel ?: "") }
    var departureLocation by remember { mutableStateOf(initialRide?.departureLocation ?: "") }
    var totalSeats by remember { mutableIntStateOf(initialRide?.totalSeats ?: 4) }
    var departureMillis by remember { mutableLongStateOf(initialRide?.departureTime ?: defaultDepartureMillis) }
    var returnMillis by remember { mutableLongStateOf(initialRide?.returnTime ?: defaultReturnMillis) }
    var notes by remember { mutableStateOf(initialRide?.notes ?: "") }

    var showDepDatePicker by remember { mutableStateOf(false) }
    var showDepTimePicker by remember { mutableStateOf(false) }
    var showRetDatePicker by remember { mutableStateOf(false) }
    var showRetTimePicker by remember { mutableStateOf(false) }

    val canConfirm = vehicleLabel.trim().isNotEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (initialRide != null) "Edit Ride" else "Offer a Ride", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            // Emoji picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Vehicle", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vehicleEmojis.forEach { emoji ->
                        val selected = emoji == selectedEmoji
                        Surface(
                            onClick = { selectedEmoji = emoji },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected) androidx.compose.foundation.BorderStroke(
                                1.5.dp, MaterialTheme.colorScheme.primary
                            ) else null,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            // Vehicle label
            OutlinedTextField(
                value = vehicleLabel,
                onValueChange = { vehicleLabel = it },
                label = { Text("Vehicle description") },
                placeholder = { Text("e.g. Black Pickup Truck") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Departure location
            OutlinedTextField(
                value = departureLocation,
                onValueChange = { departureLocation = it },
                label = { Text("Departure location") },
                placeholder = { Text("e.g. Downtown Terminal") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Seats stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Total Seats", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = { if (totalSeats > 1) totalSeats-- },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        "$totalSeats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 32.dp),
                    )
                    IconButton(
                        onClick = { if (totalSeats < 12) totalSeats++ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            // Departure date + time
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Departure", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (departureMillis > 0) carpoolFormatDate(departureMillis) else "",
                        onValueChange = {},
                        placeholder = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDepDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = if (departureMillis > 0) carpoolFormatTime(departureMillis) else "",
                        onValueChange = {},
                        placeholder = { Text("Time") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDepTimePicker = true }) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Return date + time
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Return", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (returnMillis > 0) carpoolFormatDate(returnMillis) else "",
                        onValueChange = {},
                        placeholder = { Text("Date") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showRetDatePicker = true }) {
                                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = if (returnMillis > 0) carpoolFormatTime(returnMillis) else "",
                        onValueChange = {},
                        placeholder = { Text("Time") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showRetTimePicker = true }) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Roof rack for boards/skis") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        onConfirm(
                            selectedEmoji,
                            vehicleLabel.trim(),
                            departureLocation.trim(),
                            totalSeats,
                            departureMillis,
                            returnMillis,
                            notes.trim()
                        )
                    },
                    enabled = canConfirm,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text(if (initialRide != null) "Save" else "Add Ride") }
            }
        }
    }

    if (showDepDatePicker) {
        CarpoolDatePickerDialog(
            initialMillis = departureMillis,
            onDismiss = { showDepDatePicker = false },
            onConfirm = { millis ->
                departureMillis = carpoolMergeDateTime(millis, departureMillis)
                showDepDatePicker = false
            }
        )
    }
    if (showDepTimePicker) {
        CarpoolTimePickerDialog(
            initialMillis = departureMillis,
            onDismiss = { showDepTimePicker = false },
            onConfirm = { h, m ->
                departureMillis = carpoolMergeTime(departureMillis, h, m)
                showDepTimePicker = false
            }
        )
    }
    if (showRetDatePicker) {
        CarpoolDatePickerDialog(
            initialMillis = returnMillis,
            onDismiss = { showRetDatePicker = false },
            onConfirm = { millis ->
                returnMillis = carpoolMergeDateTime(millis, returnMillis)
                showRetDatePicker = false
            }
        )
    }
    if (showRetTimePicker) {
        CarpoolTimePickerDialog(
            initialMillis = returnMillis,
            onDismiss = { showRetTimePicker = false },
            onConfirm = { h, m ->
                returnMillis = carpoolMergeTime(returnMillis, h, m)
                showRetTimePicker = false
            }
        )
    }
}

// ── Date / Time pickers ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarpoolDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = if (initialMillis > 0) initialMillis else null
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onConfirm(it) } ?: onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = state) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarpoolTimePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val cal = remember(initialMillis) {
        Calendar.getInstance().apply { if (initialMillis > 0) timeInMillis = initialMillis }
    }
    val state = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun carpoolFormatDate(millis: Long): String =
    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(millis))

private fun carpoolFormatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(millis))

private fun carpoolMergeDateTime(dateMillis: Long, existingMillis: Long): Long {
    val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val timeCal = Calendar.getInstance().apply {
        timeInMillis = if (existingMillis > 0) existingMillis else dateMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
        set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun carpoolMergeTime(existingMillis: Long, hour: Int, minute: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = if (existingMillis > 0) existingMillis else System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
