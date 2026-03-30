package com.bennybokki.frientrip.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.*

// ── Date picker dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
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
fun AppTimePickerDialog(
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

fun formatDate(millis: Long): String =
    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(millis))

fun formatTime(millis: Long): String =
    SimpleDateFormat("hh:mm a", Locale.US).format(Date(millis))

/** Keep existing time, replace date portion. */
fun mergeDateTime(dateMillis: Long, existingMillis: Long): Long {
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
fun mergeTime(existingMillis: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = if (existingMillis > 0) existingMillis else System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
