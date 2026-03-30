package com.bennybokki.frientrip.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bennybokki.frientrip.TripViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION") // LocalClipboardManager — migrate to LocalClipboard + suspend when min API allows
@Composable
fun HouseDetailsScreen(viewModel: TripViewModel, isAdmin: Boolean, onNavigateBack: () -> Unit, onOpenDrawer: () -> Unit) {
    val trip by viewModel.trip.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var urlText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("") }
    var nightsText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var checkInMillis by remember { mutableLongStateOf(0L) }
    var checkOutMillis by remember { mutableLongStateOf(0L) }
    var initialized by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val validNights = nightsText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()?.takeIf { it >= 0 }
    val validCost = costText.trim().toDoubleOrNull()?.takeIf { it >= 0.01 }
    val canSave = isAdmin && validNights != null && validCost != null && !isSaving

    LaunchedEffect(trip) {
        val t = trip
        if (!initialized && t != null) {
            urlText = t.houseURL
            addressText = t.address
            nightsText = if (t.totalNights > 0) "${t.totalNights}" else ""
            costText = if (t.totalCost > 0) String.format("%.2f", t.totalCost) else ""
            checkInMillis = t.checkInMillis
            checkOutMillis = t.checkOutMillis
            initialized = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveComplete.collectLatest { onNavigateBack() }
    }

    // Date/time picker state
    var showCheckInDatePicker by remember { mutableStateOf(false) }
    var showCheckInTimePicker by remember { mutableStateOf(false) }
    var showCheckOutDatePicker by remember { mutableStateOf(false) }
    var showCheckOutTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "House Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                },
                actions = {
                    if (isAdmin) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 16.dp))
                        } else {
                            Button(
                                onClick = {
                                    if (canSave) viewModel.saveHouseDetails(
                                        url = urlText.trim(),
                                        address = addressText.trim(),
                                        nights = validNights!!,
                                        cost = validCost!!,
                                        checkInMillis = checkInMillis,
                                        checkOutMillis = checkOutMillis
                                    )
                                },
                                enabled = canSave,
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                            ) { Text("Save", style = MaterialTheme.typography.labelLarge) }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Hero image ────────────────────────────────────────────────────
            val thumbnailURL = trip?.thumbnailURL
            val houseURL = trip?.houseURL.orEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .then(
                        if (houseURL.isNotBlank()) Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(houseURL)))
                        } else Modifier
                    )
            ) {
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
                // Gradient scrim + label
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color(0xAA000000)
                            )
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    val label = trip?.name?.let { "$it Main View" } ?: "House Photo"
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                // "Open listing" badge — top-right, only when a URL is set
                if (houseURL.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xBB000000),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "View Listing",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ── Admin banner ──────────────────────────────────────────────────
            if (isAdmin) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Admin Only",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "You have editing permissions for this travel house.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ── House URL (admin only) ────────────────────────────────────────
            if (isAdmin) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "House URL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        placeholder = { Text("https://rentals.com/villa-123") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { urlText = it }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val url = urlText.trim()
                    if (url.isNotBlank()) {
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Open in Browser", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // ── Address ───────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Address",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { addressText = it },
                    placeholder = { Text("123 Beach Rd, Malibu, CA 90265") },
                    singleLine = true,
                    enabled = isAdmin,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Total Nights + Total Cost ─────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total Nights", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = nightsText,
                        onValueChange = { nightsText = it.filter(Char::isDigit) },
                        placeholder = { Text("5") },
                        singleLine = true,
                        enabled = isAdmin,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Icon(Icons.Default.NightlightRound, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total Cost", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        placeholder = { Text("1,200") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        enabled = isAdmin,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Arrival ───────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "ARRIVAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Check-in Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = if (checkInMillis > 0) formatDate(checkInMillis) else "",
                            onValueChange = {},
                            placeholder = { Text("mm/dd/yyyy") },
                            readOnly = true,
                            enabled = isAdmin,
                            trailingIcon = {
                                IconButton(onClick = { if (isAdmin) showCheckInDatePicker = true }, enabled = isAdmin) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", modifier = Modifier.size(18.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Check-in Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = if (checkInMillis > 0) formatTime(checkInMillis) else "",
                            onValueChange = {},
                            placeholder = { Text("03:00 PM") },
                            readOnly = true,
                            enabled = isAdmin,
                            trailingIcon = {
                                IconButton(onClick = { if (isAdmin) showCheckInTimePicker = true }, enabled = isAdmin) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Pick time", modifier = Modifier.size(18.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── Departure ─────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "DEPARTURE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Check-out Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = if (checkOutMillis > 0) formatDate(checkOutMillis) else "",
                            onValueChange = {},
                            placeholder = { Text("mm/dd/yyyy") },
                            readOnly = true,
                            enabled = isAdmin,
                            trailingIcon = {
                                IconButton(onClick = { if (isAdmin) showCheckOutDatePicker = true }, enabled = isAdmin) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date", modifier = Modifier.size(18.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Check-out Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        OutlinedTextField(
                            value = if (checkOutMillis > 0) formatTime(checkOutMillis) else "",
                            onValueChange = {},
                            placeholder = { Text("11:00 AM") },
                            readOnly = true,
                            enabled = isAdmin,
                            trailingIcon = {
                                IconButton(onClick = { if (isAdmin) showCheckOutTimePicker = true }, enabled = isAdmin) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Pick time", modifier = Modifier.size(18.dp))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // ── Date / Time Pickers ───────────────────────────────────────────────────

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
    if (showCheckInTimePicker) {
        AppTimePickerDialog(
            initialMillis = checkInMillis,
            onDismiss = { showCheckInTimePicker = false },
            onConfirm = { hour, minute ->
                checkInMillis = mergeTime(checkInMillis, hour, minute)
                showCheckInTimePicker = false
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
    if (showCheckOutTimePicker) {
        AppTimePickerDialog(
            initialMillis = checkOutMillis,
            onDismiss = { showCheckOutTimePicker = false },
            onConfirm = { hour, minute ->
                checkOutMillis = mergeTime(checkOutMillis, hour, minute)
                showCheckOutTimePicker = false
            }
        )
    }
}

