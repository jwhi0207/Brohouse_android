package com.bennybokki.frientrip.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION") // LocalClipboardManager — migrate to LocalClipboard + suspend when min API allows
@Composable
fun HouseDetailsScreen(viewModel: TripViewModel, isAdmin: Boolean, onNavigateBack: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
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

            // ── House URL ─────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "House URL",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { if (isAdmin) urlText = it },
                    placeholder = { Text("https://rentals.com/villa-123") },
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

            // ── Address ───────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Address",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = addressText,
                    onValueChange = { if (isAdmin) addressText = it },
                    placeholder = { Text("123 Beach Rd, Malibu, CA 90265") },
                    singleLine = true,
                    readOnly = !isAdmin,
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
                        onValueChange = { if (isAdmin) nightsText = it.filter(Char::isDigit) },
                        placeholder = { Text("5") },
                        singleLine = true,
                        readOnly = !isAdmin,
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
                        onValueChange = { if (isAdmin) costText = it },
                        placeholder = { Text("1,200") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        readOnly = !isAdmin,
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
        HouseDatePickerDialog(
            initialMillis = checkInMillis,
            onDismiss = { showCheckInDatePicker = false },
            onConfirm = { millis ->
                checkInMillis = mergeDateTime(millis, checkInMillis)
                showCheckInDatePicker = false
            }
        )
    }
    if (showCheckInTimePicker) {
        HouseTimePickerDialog(
            initialMillis = checkInMillis,
            onDismiss = { showCheckInTimePicker = false },
            onConfirm = { hour, minute ->
                checkInMillis = mergeTime(checkInMillis, hour, minute)
                showCheckInTimePicker = false
            }
        )
    }
    if (showCheckOutDatePicker) {
        HouseDatePickerDialog(
            initialMillis = checkOutMillis,
            onDismiss = { showCheckOutDatePicker = false },
            onConfirm = { millis ->
                checkOutMillis = mergeDateTime(millis, checkOutMillis)
                showCheckOutDatePicker = false
            }
        )
    }
    if (showCheckOutTimePicker) {
        HouseTimePickerDialog(
            initialMillis = checkOutMillis,
            onDismiss = { showCheckOutTimePicker = false },
            onConfirm = { hour, minute ->
                checkOutMillis = mergeTime(checkOutMillis, hour, minute)
                showCheckOutTimePicker = false
            }
        )
    }
}

// ── Date picker dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseDatePickerDialog(
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
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(it) } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

// ── Time picker dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseTimePickerDialog(
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
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDate(millis: Long): String =
    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(millis))

private fun formatTime(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.US).format(Date(millis))

/** Keep existing time, replace date portion. */
private fun mergeDateTime(dateMillis: Long, existingMillis: Long): Long {
    // DatePicker always returns UTC midnight — read date fields in UTC so they aren't shifted
    // by the device's local timezone offset (e.g. UTC-5 would turn Mar 18 00:00 UTC → Mar 17).
    val dateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMillis }
    val timeCal = Calendar.getInstance().apply {
        timeInMillis = if (existingMillis > 0) existingMillis else dateMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR,         dateCal.get(Calendar.YEAR))
        set(Calendar.MONTH,        dateCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY,  timeCal.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE,       timeCal.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** Keep existing date, replace time portion. */
private fun mergeTime(existingMillis: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = if (existingMillis > 0) existingMillis else System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
