package com.thiccbokki.brohouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thiccbokki.brohouse.TripViewModel
import com.thiccbokki.brohouse.data.TripMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(
    viewModel: TripViewModel,
    isAdmin: Boolean,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    val trip by viewModel.trip.collectAsState()
    val pendingEmails = trip?.pendingInviteEmails ?: emptyList()

    var emailInput by remember { mutableStateOf("") }
    val trimmedEmail = emailInput.trim()
    val isValidEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Members") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Invite input — admin only
            if (isAdmin) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Invite by Email", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email address") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.inviteByEmail(trimmedEmail)
                                emailInput = ""
                            },
                            enabled = isValidEmail
                        ) { Text("Invite") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Pending invites — admin only
            if (isAdmin && pendingEmails.isNotEmpty()) {
                item {
                    Text(
                        "Pending Invites",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(pendingEmails) { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            email,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.cancelInvite(email) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel invite",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    HorizontalDivider()
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            // Current members — visible to all
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Current Members",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(members) { member ->
                MemberRow(member = member)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MemberRow(member: TripMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(seed = member.avatarSeed)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                member.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
