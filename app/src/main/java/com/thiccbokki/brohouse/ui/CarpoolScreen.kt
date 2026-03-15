package com.thiccbokki.brohouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thiccbokki.brohouse.TripViewModel
import com.thiccbokki.brohouse.data.Ride
import com.thiccbokki.brohouse.data.RideRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarpoolScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val rides by viewModel.rides.collectAsState()
    val rideRequests by viewModel.rideRequests.collectAsState()
    val currentUid = viewModel.currentUid

    var showAddSheet by remember { mutableStateOf(false) }
    var editingRide by remember { mutableStateOf<Ride?>(null) }
    val myRequest = rideRequests.find { it.uid == currentUid }
    val alreadyOfferedRide = rides.any { it.driverUid == currentUid }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carpool") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!alreadyOfferedRide) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Offer a Ride")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            // Vehicles section
            item {
                SectionHeader(
                    title = "Vehicles",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (rides.isEmpty()) {
                item {
                    Text(
                        "No rides yet — be the first to offer one!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                }
            } else {
                items(rides, key = { it.id }) { ride ->
                    RideCard(
                        ride = ride,
                        currentUid = currentUid,
                        canEdit = viewModel.canEditRide(ride),
                        onClaim = { viewModel.claimSeat(ride.id) },
                        onUnclaim = { viewModel.unclaimSeat(ride.id) },
                        onEdit = { editingRide = ride },
                        onDelete = { viewModel.deleteRide(ride.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Need a Ride section
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    title = "Need a Ride",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    if (myRequest == null) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = { PlainTooltip { Text("You can't request a ride if you're driving") } },
                            state = rememberTooltipState(),
                            enableUserInput = alreadyOfferedRide
                        ) {
                            Button(
                                onClick = { viewModel.requestRide() },
                                enabled = !alreadyOfferedRide
                            ) {
                                Text("I Need a Ride")
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.cancelRideRequest() }) {
                            Text("Cancel Request")
                        }
                    }
                }
            }

            if (rideRequests.isEmpty()) {
                item {
                    Text(
                        "No ride requests yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(rideRequests, key = { it.uid }) { request ->
                    RideRequestRow(
                        request = request,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(88.dp)) } // FAB clearance
        }
    }

    if (showAddSheet) {
        AddVehicleSheet(
            onDismiss = { showAddSheet = false },
            onConfirm = { emoji, label, location, seats, depTime, retTime, rideNotes ->
                viewModel.addRide(emoji, label, location, seats, depTime, retTime, rideNotes)
                showAddSheet = false
            }
        )
    }

    editingRide?.let { ride ->
        AddVehicleSheet(
            initialRide = ride,
            onDismiss = { editingRide = null },
            onConfirm = { emoji, label, location, seats, depTime, retTime, rideNotes ->
                viewModel.updateRide(ride.id, emoji, label, location, seats, depTime, retTime, rideNotes)
                editingRide = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun RideCard(
    ride: Ride,
    currentUid: String,
    canEdit: Boolean,
    onClaim: () -> Unit,
    onUnclaim: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDriver = currentUid == ride.driverUid
    val isPassenger = currentUid in ride.passengerUids

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: emoji + label + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ride.vehicleEmoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ride.vehicleLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        "by ${ride.driverName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit ride",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete ride",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Details
            DetailRow(label = "From", value = ride.departureLocation)
            DetailRow(label = "Departing", value = formatRideTime(ride.departureTime))
            DetailRow(label = "Returning", value = formatRideTime(ride.returnTime))

            val seatText = if (ride.isFull) "Full (${ride.totalSeats}/${ride.totalSeats})"
                           else "${ride.availableSeats} of ${ride.totalSeats} seats open"
            DetailRow(label = "Seats", value = seatText)

            if (ride.passengerNames.isNotEmpty()) {
                DetailRow(label = "Passengers", value = ride.passengerNames.joinToString(", "))
            }

            if (ride.notes.isNotBlank()) {
                DetailRow(label = "Notes", value = ride.notes)
            }

            Spacer(Modifier.height(12.dp))

            // Claim / unclaim button
            when {
                isDriver -> Unit // driver can't claim their own car
                isPassenger -> {
                    OutlinedButton(
                        onClick = onUnclaim,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Leave Ride")
                    }
                }
                ride.isFull -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Full")
                    }
                }
                else -> {
                    Button(
                        onClick = onClaim,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Claim Seat")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.widthIn(min = 72.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun RideRequestRow(request: RideRequest, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(request.displayName, style = MaterialTheme.typography.bodyMedium)
            if (request.notes.isNotBlank()) {
                Text(
                    request.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 0.dp))
}

private fun formatRideTime(millis: Long): String {
    if (millis == 0L) return "TBD"
    return SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US).format(Date(millis))
}
