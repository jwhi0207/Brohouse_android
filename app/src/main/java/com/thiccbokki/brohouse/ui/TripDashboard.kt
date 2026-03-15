package com.thiccbokki.brohouse.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thiccbokki.brohouse.TripViewModel
import com.thiccbokki.brohouse.data.HouseDetails
import com.thiccbokki.brohouse.data.SupplyItem
import com.thiccbokki.brohouse.data.TripMember
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDashboard(
    viewModel: TripViewModel,
    isAdmin: Boolean,
    onNavigateToHouseDetails: () -> Unit,
    onNavigateToSupplies: () -> Unit,
    onNavigateToInvite: () -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    val members by viewModel.members.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val supplyItems by viewModel.supplyItems.collectAsState()
    val memberCosts by viewModel.memberCosts.collectAsState()
    val currentUid = viewModel.currentUid
    val context = LocalContext.current

    var editNightsMember by remember { mutableStateOf<TripMember?>(null) }
    var addPaymentMember by remember { mutableStateOf<TripMember?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val houseDetails = trip?.let {
        HouseDetails(
            houseURL = it.houseURL,
            totalNights = it.totalNights,
            totalCost = it.totalCost,
            thumbnailURL = it.thumbnailURL
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Debt Tracker") },
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
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                                onClick = { showOverflowMenu = false; onSignOut() }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                HouseDetailsCard(
                    details = houseDetails,
                    guestCount = members.size,
                    onClick = {
                        val url = houseDetails?.houseURL?.trim()
                        if (!url.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                SuppliesCard(
                    supplyItems = supplyItems,
                    onClick = onNavigateToSupplies,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (members.isEmpty()) {
                item {
                    Text(
                        text = if (isAdmin) "Nobody wants to go T_T\nTap the person icon to invite friends"
                               else "Nobody wants to go T_T",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(members, key = { it.uid }) { member ->
                    val computedOwed = memberCosts[member.uid] ?: 0.0
                    MemberRowView(
                        member = member,
                        computedOwed = computedOwed,
                        isAdmin = isAdmin,
                        isCurrentUser = member.uid == currentUid,
                        onEditNights = { editNightsMember = member },
                        onAddPayment = { addPaymentMember = member }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
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
}

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
    val currency = NumberFormat.getCurrencyInstance(Locale.US)
    val canEditNights = isCurrentUser || isAdmin
    val canAddPayment = isAdmin

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(seed = member.avatarSeed)
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(member.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Nights chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.NightlightRound,
                        contentDescription = null,
                        tint = Color(0xFF3F51B5),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    val n = member.nightsStayed
                    Text(
                        if (n == 0) "Nights TBD" else "$n ${if (n == 1) "night" else "nights"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                // Balance chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPaidUp && computedOwed > 0.0) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "Paid up",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    } else {
                        Text(
                            if (member.nightsStayed == 0) "Set nights to calculate"
                            else "$%,.2f Due".format(remaining),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (member.nightsStayed == 0) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else Color(0xFFE57373)
                        )
                    }
                }
                // Paid chip — only shown when something has been paid
                if (member.amountPaid > 0.0 && !isPaidUp) {
                    Text(
                        "$%,.2f Paid".format(member.amountPaid),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        if (canEditNights || canAddPayment) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
fun HouseDetailsCard(
    details: HouseDetails?,
    guestCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalNights = details?.totalNights ?: 0
    val totalCost = details?.totalCost ?: 0.0
    val thumbnailURL = details?.thumbnailURL

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Text(
                "House Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailURL != null) {
                    AsyncImage(
                        model = thumbnailURL,
                        contentDescription = "House photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                "House Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCell(
                    label = "Total Nights",
                    value = "$totalNights",
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                StatCell(
                    label = "Total Cost",
                    value = NumberFormat.getCurrencyInstance(Locale.US).format(totalCost),
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                StatCell(
                    label = "No. of Guests",
                    value = "$guestCount",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
