package com.bennybokki.frientrip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bennybokki.frientrip.TripViewModel
import com.bennybokki.frientrip.data.HouseDetails
import com.bennybokki.frientrip.data.Ride
import com.bennybokki.frientrip.data.RideRequest
import com.bennybokki.frientrip.data.SupplyItem
import com.bennybokki.frientrip.data.TripMember
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDashboard(
    viewModel: TripViewModel,
    isAdmin: Boolean,
    onNavigateToHouseDetails: () -> Unit,
    onNavigateToSupplies: () -> Unit,
    onNavigateToCarpool: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val members by viewModel.members.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val supplyItems by viewModel.supplyItems.collectAsState()
    val rides by viewModel.rides.collectAsState()
    val rideRequests by viewModel.rideRequests.collectAsState()
    val memberCosts by viewModel.memberCosts.collectAsState()
    val currentUid = viewModel.currentUid

    var editNightsMember by remember { mutableStateOf<TripMember?>(null) }
    var addPaymentMember by remember { mutableStateOf<TripMember?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val houseDetails = trip?.let {
        HouseDetails(
            houseURL = it.houseURL,
            totalNights = it.totalNights,
            totalCost = it.totalCost,
            thumbnailURL = it.thumbnailURL,
            address = it.address,
            checkInMillis = it.checkInMillis,
            checkOutMillis = it.checkOutMillis
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                trip?.name ?: "Trip",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${members.size} ${if (members.size == 1) "Guest" else "Guests"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (isAdmin) {
                            IconButton(
                                onClick = { showRenameDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Rename trip",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onNavigateToInvite) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Invite People")
                        }
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sign Out") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
                                onClick = { showOverflowMenu = false; onSignOut() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── House hero card ───────────────────────────────────────────────
            item {
                HouseHeroCard(
                    details = houseDetails,
                    guestCount = members.size,
                    onClick = onNavigateToHouseDetails,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Feature cards (2-column) ──────────────────────────────────────
            item {
                val unclaimed = supplyItems.count { !it.isClaimed }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        icon = Icons.Default.ShoppingCart,
                        badge = if (unclaimed > 0) "$unclaimed New" else null,
                        title = "Supplies",
                        subtitle = if (supplyItems.isEmpty()) "No items yet"
                                   else "${supplyItems.size} items total",
                        onClick = onNavigateToSupplies,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        icon = Icons.Default.DirectionsCar,
                        badge = null,
                        title = "Carpool",
                        subtitle = "Tap to view rides",
                        onClick = onNavigateToCarpool,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Group Members header ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Group Members",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onNavigateToInvite) {
                        Text("View All", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // ── Member rows ───────────────────────────────────────────────────
            if (members.isEmpty()) {
                item {
                    Text(
                        text = if (isAdmin) "Nobody here yet\nTap the person icon to invite friends"
                               else "Nobody here yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        members.forEachIndexed { index, member ->
                            val computedOwed = memberCosts[member.uid] ?: 0.0
                            MemberRowView(
                                member = member,
                                computedOwed = computedOwed,
                                isAdmin = isAdmin,
                                isCurrentUser = member.uid == currentUid,
                                onEditNights = { editNightsMember = member },
                                onAddPayment = { addPaymentMember = member }
                            )
                            if (index < members.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editNightsMember?.let { member ->
        EditNightsSheet(
            currentNights = member.nightsStayed,
            maxNights = trip?.totalNights ?: 0,
            onDismiss = { editNightsMember = null },
            onSave = { nights ->
                viewModel.updateNights(member, nights)
                editNightsMember = null
            }
        )
    }

    addPaymentMember?.let { member ->
        val computedOwed = memberCosts[member.uid] ?: 0.0
        AddPaymentSheet(
            computedOwed = computedOwed,
            amountPaid = member.amountPaid,
            onDismiss = { addPaymentMember = null },
            onSave = { amount ->
                viewModel.addPayment(member, amount)
                addPaymentMember = null
            }
        )
    }

    if (showRenameDialog) {
        RenameTripDialog(
            currentName = trip?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                viewModel.renameTrip(newName)
                showRenameDialog = false
            }
        )
    }
}

@Composable
private fun RenameTripDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    val trimmed = text.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Trip") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (trimmed.isNotEmpty()) onConfirm(trimmed) }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed) },
                enabled = trimmed.isNotEmpty() && trimmed != currentName
            ) { Text("Rename") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── House hero card ───────────────────────────────────────────────────────────

@Composable
fun HouseHeroCard(
    details: HouseDetails?,
    guestCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalNights = details?.totalNights ?: 0
    val totalCost = details?.totalCost ?: 0.0
    val thumbnailURL = details?.thumbnailURL
    val address = details?.address.orEmpty()
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Background image or placeholder
            if (!thumbnailURL.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailURL,
                    contentDescription = "House photo",
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
                        )
                )
            }

            // Gradient scrim over bottom half
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
            if (totalCost > 0) {
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
                            currency.format(totalCost),
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

            // Bottom content — name, address, chips
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                if (details?.houseURL?.isNotBlank() == true) {
                    Text(
                        "Lodging",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Add Lodging Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val checkIn = details?.checkInMillis ?: 0L
                    val checkOut = details?.checkOutMillis ?: 0L
                    if (checkIn > 0 && checkOut > 0) {
                        val fmt = SimpleDateFormat("MMM d", Locale.US)
                        HeroChip(
                            icon = Icons.Default.CalendarMonth,
                            label = "${fmt.format(Date(checkIn))} – ${fmt.format(Date(checkOut))}"
                        )
                    }
                    HeroChip(
                        icon = Icons.Default.Group,
                        label = "$guestCount ${if (guestCount == 1) "Guest" else "Guests"}"
                    )
                }
            }
        } // end Box

        // ── Address row ──────────────────────────────────────────────────
        if (address.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { clipboard.setText(AnnotatedString(address)) }
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(address)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = "Open in Maps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        } // end Column
    }
}

@Composable
internal fun HeroChip(icon: ImageVector, label: String) {
    Surface(
        shape = CircleShape,
        color = Color(0x99000000)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

// ── Feature card ──────────────────────────────────────────────────────────────

@Composable
private fun FeatureCard(
    icon: ImageVector,
    badge: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (badge != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Member row ────────────────────────────────────────────────────────────────

@Composable
fun MemberRowView(
    member: TripMember,
    computedOwed: Double,
    isAdmin: Boolean,
    isCurrentUser: Boolean,
    onEditNights: () -> Unit,
    onAddPayment: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val remaining = computedOwed - member.amountPaid
    val isPaidUp = remaining <= 0.0
    val canEditNights = isCurrentUser || isAdmin
    val canAddPayment = isAdmin
    val currency = NumberFormat.getCurrencyInstance(Locale.US)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(seed = member.avatarSeed)
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(member.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.NightlightRound,
                    contentDescription = null,
                    tint = Color(0xFF5C6BC0),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(3.dp))
                val n = member.nightsStayed
                Text(
                    if (n == 0) "Nights TBD" else "$n ${if (n == 1) "night" else "nights"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Status badge
        when {
            computedOwed == 0.0 || member.nightsStayed == 0 -> Unit
            isPaidUp -> StatusBadge("PAID UP", Color(0xFF1B5E20), Color(0xFFE8F5E9))
            else -> StatusBadge(currency.format(remaining) + " DUE", Color(0xFFB71C1C), Color(0xFFFFEBEE))
        }

        if (canEditNights || canAddPayment) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (canEditNights) {
                        DropdownMenuItem(
                            text = { Text("Edit Nights") },
                            leadingIcon = { Icon(Icons.Filled.NightlightRound, null) },
                            onClick = { showMenu = false; onEditNights() }
                        )
                    }
                    if (canAddPayment) {
                        DropdownMenuItem(
                            text = { Text("Add Payment") },
                            leadingIcon = { Icon(Icons.Filled.AttachMoney, null) },
                            onClick = { showMenu = false; onAddPayment() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ── Supplies summary card (kept for compatibility) ────────────────────────────

@Composable
fun SuppliesCard(
    supplyItems: List<SupplyItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unclaimed = supplyItems.count { !it.isClaimed }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Supplies", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (supplyItems.isEmpty()) "No items yet — tap to add supplies"
                else "${supplyItems.size} ${if (supplyItems.size == 1) "item" else "items"}, $unclaimed unclaimed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ── House details card (kept for compatibility) ───────────────────────────────

@Composable
fun HouseDetailsCard(
    details: HouseDetails?,
    guestCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HouseHeroCard(details = details, guestCount = guestCount, onClick = onClick, modifier = modifier)
}

@Composable
fun CarpoolCard(
    rides: List<Ride>,
    rideRequests: List<RideRequest>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicleCount = rides.size
    val openSeats = rides.sumOf { it.availableSeats }
    val needRideCount = rideRequests.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Carpool", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            if (rides.isEmpty() && rideRequests.isEmpty()) {
                Text(
                    "No rides yet — tap to organize carpools",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                val parts = mutableListOf<String>()
                if (vehicleCount > 0) {
                    parts.add("$vehicleCount ${if (vehicleCount == 1) "vehicle" else "vehicles"}, $openSeats open ${if (openSeats == 1) "seat" else "seats"}")
                }
                if (needRideCount > 0) {
                    parts.add("$needRideCount need a ride")
                }
                Text(
                    parts.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
