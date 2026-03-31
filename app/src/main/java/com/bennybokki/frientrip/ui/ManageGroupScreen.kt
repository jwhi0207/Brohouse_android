package com.bennybokki.frientrip.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bennybokki.frientrip.TripViewModel
import com.bennybokki.frientrip.data.TripMember
import com.bennybokki.frientrip.data.UserRepository
import com.bennybokki.frientrip.ui.theme.NeonGreen
import com.bennybokki.frientrip.ui.theme.VividCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGroupScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val ownerId = trip?.ownerId ?: ""
    val currentUid = viewModel.currentUid
    val currentMember = remember(members, currentUid) { members.find { it.uid == currentUid } }
    val isTripAdmin = currentUid == ownerId ||
            (currentMember != null && UserRepository.isAdminEmail(currentMember.email))

    // Sort: active members first (guests included), then deactivated; each group by name
    val sortedMembers = remember(members) {
        members.sortedWith(compareBy({ it.isDeactivated }, { it.displayName }))
    }

    var pendingDeactivate by remember { mutableStateOf<TripMember?>(null) }
    var pendingReactivate by remember { mutableStateOf<TripMember?>(null) }
    var pendingRemoveGuest by remember { mutableStateOf<TripMember?>(null) }
    var showAddGuestDialog by remember { mutableStateOf(false) }
    var guestNameInput by remember { mutableStateOf("") }
    var guestNameError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Group Members",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (isTripAdmin) {
                FloatingActionButton(onClick = { showAddGuestDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add guest member")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (sortedMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No members yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VividCard(
                        accentIndex = 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        sortedMembers.forEachIndexed { index, member ->
                            val isProtected = member.uid == ownerId ||
                                    UserRepository.isAdminEmail(member.email)
                            ManageGroupMemberRow(
                                member = member,
                                isProtected = isProtected,
                                onDeactivate = { pendingDeactivate = member },
                                onReactivate = { pendingReactivate = member },
                                onRemoveGuest = { pendingRemoveGuest = member }
                            )
                            if (index < sortedMembers.lastIndex) {
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

    // ── Add guest dialog ──────────────────────────────────────────────────────
    if (showAddGuestDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddGuestDialog = false
                guestNameInput = ""
                guestNameError = false
            },
            title = { Text("Add Guest Member") },
            text = {
                Column {
                    Text("Add someone who doesn't have the app. They'll be included in cost calculations.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = guestNameInput,
                        onValueChange = { guestNameInput = it; guestNameError = false },
                        label = { Text("Name") },
                        isError = guestNameError,
                        supportingText = if (guestNameError) {
                            { Text("Name cannot be empty") }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (guestNameInput.isBlank()) {
                            guestNameError = true
                        } else {
                            viewModel.addGuest(guestNameInput.trim())
                            showAddGuestDialog = false
                            guestNameInput = ""
                            guestNameError = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddGuestDialog = false
                    guestNameInput = ""
                    guestNameError = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Remove guest confirmation dialog ──────────────────────────────────────
    pendingRemoveGuest?.let { guest ->
        AlertDialog(
            onDismissRequest = { pendingRemoveGuest = null },
            title = { Text("Remove guest?") },
            text = {
                Text("Remove ${guest.displayName} from the trip? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeGuest(guest.uid)
                        pendingRemoveGuest = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveGuest = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Deactivate confirmation dialog ────────────────────────────────────────
    pendingDeactivate?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDeactivate = null },
            title = { Text("Are you sure?") },
            text = {
                Text(
                    "Are you sure you want to remove ${member.displayName} from the group? " +
                            "The user will no longer be able to access the trip."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deactivateMember(member.uid)
                        pendingDeactivate = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeactivate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Reactivate confirmation dialog ────────────────────────────────────────
    pendingReactivate?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingReactivate = null },
            title = { Text("Reactivate group member?") },
            text = { Text("Allow ${member.displayName} to access trip?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reactivateMember(member.uid)
                        pendingReactivate = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen
                    )
                ) {
                    Text("Reactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReactivate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ManageGroupMemberRow(
    member: TripMember,
    isProtected: Boolean,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onRemoveGuest: () -> Unit
) {
    val contentAlpha = if (member.isDeactivated) 0.4f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(modifier = Modifier.alpha(contentAlpha)) {
            AvatarView(
                seed = member.avatarSeed,
                colorIndex = member.avatarColor,
                name = member.displayName,
                size = 44.dp
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name + subtitle + status badges
        Column(modifier = Modifier.weight(1f).alpha(contentAlpha)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    member.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (member.isGuest) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Guest",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (member.isDeactivated) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Deactivated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            val subtitle = if (member.isGuest) "guest member" else member.email
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Action button — hidden for protected members (owner / global admin)
        if (!isProtected) {
            Spacer(Modifier.width(8.dp))
            when {
                member.isGuest -> {
                    Button(
                        onClick = onRemoveGuest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Remove", style = MaterialTheme.typography.labelMedium)
                    }
                }
                member.isDeactivated -> {
                    Button(
                        onClick = onReactivate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Reactivate", style = MaterialTheme.typography.labelMedium)
                    }
                }
                else -> {
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Deactivate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
