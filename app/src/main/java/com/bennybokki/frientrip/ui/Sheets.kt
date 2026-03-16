package com.bennybokki.frientrip.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bennybokki.frientrip.data.Ride
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNightsSheet(
    currentNights: Int,
    maxNights: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var nightsText by remember { mutableStateOf(if (currentNights > 0) "$currentNights" else "") }
    val focusRequester = remember { FocusRequester() }
    val parsed = nightsText.toIntOrNull()
    val isValid = parsed != null && parsed >= 0 && (maxNights <= 0 || parsed <= maxNights)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Edit Nights", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = nightsText,
                onValueChange = { new ->
                    val digits = new.filter(Char::isDigit)
                    nightsText = digits
                },
                label = { Text("Nights") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    if (maxNights > 0) {
                        Text("Enter 0–$maxNights (trip is $maxNights ${if (maxNights == 1) "night" else "nights"} total)")
                    } else {
                        Text("Currently: $currentNights ${if (currentNights == 1) "night" else "nights"}")
                    }
                },
                isError = parsed != null && maxNights > 0 && parsed > maxNights,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (isValid) onSave(parsed!!) },
                    enabled = isValid
                ) { Text("Save") }
            }
        }
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentSheet(
    computedOwed: Double,
    amountPaid: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var amountText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val parsed = amountText.trim().toDoubleOrNull()?.takeIf { it > 0 }
    val remaining = computedOwed - amountPaid
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Add Payment", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            // Cost breakdown summary
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    PaymentSummaryRow("Total owed", currency.format(computedOwed))
                    PaymentSummaryRow("Already paid", currency.format(amountPaid))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    PaymentSummaryRow(
                        label = "Remaining",
                        value = if (remaining <= 0.0) "Paid up ✓" else currency.format(remaining),
                        valueColor = if (remaining <= 0.0) Color(0xFF4CAF50)
                                     else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Payment amount") },
                prefix = { Text("$") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (parsed != null) onSave(parsed) },
                    enabled = parsed != null
                ) { Text("Record") }
            }
        }
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

private val VEHICLE_EMOJIS = listOf("🚗", "🚐", "🛻", "🚌", "🏎️")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleSheet(
    initialRide: Ride? = null,
    onDismiss: () -> Unit,
    onConfirm: (vehicleEmoji: String, vehicleLabel: String, departureLocation: String, totalSeats: Int, departureTime: Long, returnTime: Long, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var vehicleEmoji by remember { mutableStateOf(initialRide?.vehicleEmoji ?: VEHICLE_EMOJIS.first()) }
    var vehicleLabel by remember { mutableStateOf(initialRide?.vehicleLabel ?: "") }
    var departureLocation by remember { mutableStateOf(initialRide?.departureLocation ?: "") }
    var totalSeats by remember { mutableIntStateOf(initialRide?.totalSeats ?: 4) }
    var departureMillis by remember { mutableLongStateOf(initialRide?.departureTime ?: 0L) }
    var returnMillis by remember { mutableLongStateOf(initialRide?.returnTime ?: 0L) }
    var notes by remember { mutableStateOf(initialRide?.notes ?: "") }

    var showDepDatePicker by remember { mutableStateOf(false) }
    var showDepTimePicker by remember { mutableStateOf(false) }
    var showRetDatePicker by remember { mutableStateOf(false) }
    var showRetTimePicker by remember { mutableStateOf(false) }
    var pendingDepDateMillis by remember { mutableLongStateOf(0L) }
    var pendingRetDateMillis by remember { mutableLongStateOf(0L) }

    val invalidDates = departureMillis > 0 && returnMillis > 0 && departureMillis >= returnMillis
    val canConfirm = vehicleLabel.isNotBlank() && departureLocation.isNotBlank() &&
            departureMillis > 0 && returnMillis > 0 && !invalidDates

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(if (initialRide != null) "Edit Ride" else "Offer a Ride", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            // Vehicle emoji
            Text("Vehicle", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VEHICLE_EMOJIS.forEach { emoji ->
                    val selected = emoji == vehicleEmoji
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        onClick = { vehicleEmoji = emoji },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Vehicle label
            OutlinedTextField(
                value = vehicleLabel,
                onValueChange = { vehicleLabel = it },
                label = { Text("Vehicle description") },
                placeholder = { Text("e.g. Black Pickup Truck") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Departure location
            OutlinedTextField(
                value = departureLocation,
                onValueChange = { departureLocation = it },
                label = { Text("Departure location") },
                placeholder = { Text("e.g. North side of house") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // Total seats counter
            Text("Total seats", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (totalSeats > 1) totalSeats-- },
                    enabled = totalSeats > 1
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease seats")
                }
                Text(
                    "$totalSeats",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { if (totalSeats < 8) totalSeats++ },
                    enabled = totalSeats < 8
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase seats")
                }
            }
            Spacer(Modifier.height(16.dp))

            // Departure date + time
            Text("Departure", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDepDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (departureMillis > 0L) formatVehicleDateTime(departureMillis) else "Set departure date & time")
            }
            Spacer(Modifier.height(12.dp))

            // Return date + time
            Text("Return", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showRetDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (returnMillis > 0L) formatVehicleDateTime(returnMillis) else "Set return date & time")
            }

            if (invalidDates) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Return must be after departure",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(12.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g. Leaving from the north entrance") },
                singleLine = false,
                minLines = 2,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (canConfirm) {
                            onConfirm(vehicleEmoji, vehicleLabel.trim(), departureLocation.trim(), totalSeats, departureMillis, returnMillis, notes.trim())
                        }
                    },
                    enabled = canConfirm
                ) { Text(if (initialRide != null) "Save" else "Confirm") }
            }
        }
    }

    // Departure date picker
    if (showDepDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (departureMillis > 0L) departureMillis else null,
            initialDisplayedMonthMillis = if (departureMillis > 0L) departureMillis else null
        )
        DatePickerDialog(
            onDismissRequest = { showDepDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDepDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDepDatePicker = false
                    showDepTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDepDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Departure time picker
    if (showDepTimePicker) {
        val existing = Calendar.getInstance().also { it.timeInMillis = if (departureMillis > 0L) departureMillis else System.currentTimeMillis() }
        val timePickerState = rememberTimePickerState(
            initialHour = existing.get(Calendar.HOUR_OF_DAY),
            initialMinute = existing.get(Calendar.MINUTE)
        )
        Dialog(onDismissRequest = { showDepTimePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Departure time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDepTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = pendingDepDateMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            departureMillis = cal.timeInMillis
                            showDepTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    // Return date picker
    if (showRetDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (returnMillis > 0L) returnMillis else null,
            initialDisplayedMonthMillis = if (returnMillis > 0L) returnMillis else if (departureMillis > 0L) departureMillis else null
        )
        DatePickerDialog(
            onDismissRequest = { showRetDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingRetDateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showRetDatePicker = false
                    showRetTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showRetDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Return time picker
    if (showRetTimePicker) {
        val existing = Calendar.getInstance().also { it.timeInMillis = if (returnMillis > 0L) returnMillis else System.currentTimeMillis() }
        val timePickerState = rememberTimePickerState(
            initialHour = existing.get(Calendar.HOUR_OF_DAY),
            initialMinute = existing.get(Calendar.MINUTE)
        )
        Dialog(onDismissRequest = { showRetTimePicker = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Return time", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = timePickerState)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRetTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = pendingRetDateMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            returnMillis = cal.timeInMillis
                            showRetTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

private fun formatVehicleDateTime(millis: Long): String =
    SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US).format(Date(millis))

@Composable
private fun PaymentSummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}
