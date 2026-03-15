package com.thiccbokki.brohouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.thiccbokki.brohouse.TripViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailsScreen(viewModel: TripViewModel, isAdmin: Boolean, onNavigateBack: () -> Unit) {
    val trip by viewModel.trip.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var urlText by remember { mutableStateOf("") }
    var nightsText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    val validNights = nightsText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()?.takeIf { it >= 0 }
    val validCost = costText.trim().toDoubleOrNull()?.takeIf { it >= 0.01 }
    val canSave = isAdmin && validNights != null && validCost != null && !isSaving

    LaunchedEffect(trip) {
        val t = trip
        if (!initialized && t != null) {
            urlText = t.houseURL
            nightsText = if (t.totalNights > 0) "${t.totalNights}" else ""
            costText = if (t.totalCost > 0) String.format("%.2f", t.totalCost) else ""
            initialized = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveComplete.collectLatest { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("House Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 16.dp)
                            )
                        } else {
                            TextButton(
                                onClick = {
                                    if (canSave) {
                                        viewModel.saveHouseDetails(
                                            url = urlText.trim(),
                                            nights = validNights!!,
                                            cost = validCost!!
                                        )
                                    }
                                },
                                enabled = canSave
                            ) { Text("Save") }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isAdmin) {
                Text(
                    "View only — only admins can edit house details",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Text("House URL", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = urlText,
                onValueChange = { if (isAdmin) urlText = it },
                label = { Text("https://") },
                singleLine = true,
                readOnly = !isAdmin,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = if (isAdmin) ({
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { urlText = it }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                }) else null,
                modifier = Modifier.fillMaxWidth()
            )

            val context = LocalContext.current
            val url = urlText.trim()
            if (url.isNotBlank()) {
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open in Browser")
                }
            }

            Text("Total Nights", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = nightsText,
                onValueChange = { if (isAdmin) nightsText = it.filter(Char::isDigit) },
                label = { Text("0") },
                singleLine = true,
                readOnly = !isAdmin,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = if (isAdmin) ({ Text("Must be 0 or greater.") }) else null,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Total Cost", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = costText,
                onValueChange = { if (isAdmin) costText = it },
                label = { Text("0.01") },
                prefix = { Text("$") },
                singleLine = true,
                readOnly = !isAdmin,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = if (isAdmin) ({ Text("Must be at least $0.01.") }) else null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
