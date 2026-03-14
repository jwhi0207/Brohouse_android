package com.example.brohouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.brohouse.MainViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailsScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val houseDetails by viewModel.houseDetails.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var urlText by remember { mutableStateOf("") }
    var nightsText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    val validNights = nightsText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()?.takeIf { it >= 0 }
    val validCost = costText.trim().toDoubleOrNull()?.takeIf { it >= 0.01 }
    val canSave = validNights != null && validCost != null && !isSaving

    // Pre-fill from existing record
    LaunchedEffect(houseDetails) {
        if (!initialized) {
            houseDetails?.let { d ->
                urlText = d.houseURL
                nightsText = if (d.totalNights > 0) "${d.totalNights}" else ""
                costText = if (d.totalCost > 0) String.format("%.2f", d.totalCost) else ""
            }
            initialized = true
        }
    }

    // Navigate back when save completes
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
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp)
                        )
                    } else {
                        TextButton(
                            onClick = {
                                if (canSave) {
                                    viewModel.saveHouseDetails(
                                        url = urlText.trim(),
                                        nights = validNights!!,
                                        cost = validCost!!,
                                        currentDetails = houseDetails
                                    )
                                }
                            },
                            enabled = canSave
                        ) { Text("Save") }
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
            // House URL
            Text("House URL", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("https://") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { urlText = it }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Total Nights
            Text("Total Nights", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = nightsText,
                onValueChange = { nightsText = it.filter(Char::isDigit) },
                label = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Must be 0 or greater.") },
                modifier = Modifier.fillMaxWidth()
            )

            // Total Cost
            Text("Total Cost", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text("0.01") },
                prefix = { Text("$") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Must be at least $0.01.") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
