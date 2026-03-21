package com.bennybokki.frientrip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    houseShare: Double = 0.0,
    expensesShare: Double = 0.0,
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
                    if (houseShare > 0.0 || expensesShare > 0.0) {
                        PaymentSummaryRow("House share", currency.format(houseShare))
                        PaymentSummaryRow("Expenses share", currency.format(expensesShare))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                    PaymentSummaryRow("Total owed", currency.format(computedOwed))
                    PaymentSummaryRow("Already paid", currency.format(amountPaid))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    PaymentSummaryRow(
                        label = "Remaining",
                        value = if (remaining <= 0.0) "Paid up \u2713" else currency.format(remaining),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayExpensesSheet(
    amountDue: Double,
    pendingPaymentStatus: String,
    onDismiss: () -> Unit,
    onSubmitPayment: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)
    var copied by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf(String.format("%.2f", amountDue)) }
    val parsed = amountText.trim().toDoubleOrNull()
    val canSubmit = parsed != null && parsed >= 0.01

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pay Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))

            // Rejection notice
            if (pendingPaymentStatus == "rejected") {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = Color(0xFFB71C1C),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Your last payment submission was rejected by the trip manager.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB71C1C)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Amount due — tappable to copy
            Surface(
                onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(String.format("%.2f", amountDue)))
                    copied = true
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Amount Due",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currency.format(amountDue),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (copied) "Copied to clipboard ✓" else "Tap to copy amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Amount paid field + Submit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Paid") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { if (canSubmit) onSubmitPayment(parsed!!) },
                    enabled = canSubmit
                ) {
                    Text("Submit")
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Pay with",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))

            val paymentApps = listOf(
                Triple("PayPal",  Color(0xFF003087), "https://www.paypal.com/myaccount/transfer/homepage/pay"),
                Triple("Venmo",   Color(0xFF3D95CE), "https://venmo.com/"),
                Triple("Cash App",Color(0xFF00C244), "https://cash.app/"),
                Triple("Zelle",   Color(0xFF6D1ED4), "https://www.zellepay.com/"),
                Triple("GPay",    Color(0xFF000000), "https://pay.google.com/")
            )

            paymentApps.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (label, color, url) ->
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color,
                                contentColor = Color.White
                            )
                        ) {
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                    // If odd row, fill remaining space so the lone button doesn't stretch full-width
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyPaymentSheet(
    member: com.bennybokki.frientrip.data.TripMember,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Verify Payment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                member.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(20.dp))

            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Amount Submitted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currency.format(member.pendingPaymentAmount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C),
                        contentColor = Color.White
                    )
                ) {
                    Text("Reject", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20),
                        contentColor = Color.White
                    )
                ) {
                    Text("Approve", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistorySheet(
    memberName: String,
    events: List<com.bennybokki.frientrip.data.PaymentEvent>,
    isAdmin: Boolean = false,
    onRevertEvent: (com.bennybokki.frientrip.data.PaymentEvent) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)
    val dateFmt = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.US)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    "Payment History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    memberName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No payment history yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(events) { event ->
                        PaymentEventRow(
                            event = event,
                            currency = currency,
                            dateFmt = dateFmt,
                            showRevert = isAdmin && (event.type == "approved" || event.type == "rejected"),
                            onRevert = { onRevertEvent(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentEventRow(
    event: com.bennybokki.frientrip.data.PaymentEvent,
    currency: java.text.NumberFormat,
    dateFmt: java.text.SimpleDateFormat,
    showRevert: Boolean = false,
    onRevert: () -> Unit = {}
) {
    val isApproved = event.type == "approved"
    val isRejected = event.type == "rejected"
    val isReverted = event.type == "reverted"
    val icon = when {
        isApproved -> Icons.Default.CheckCircle
        isRejected -> Icons.Default.Cancel
        isReverted -> Icons.Default.Undo
        else -> Icons.Default.Upload
    }
    val label = when {
        isApproved -> "Approved by ${event.actorName}"
        isRejected -> "Rejected by ${event.actorName}"
        isReverted -> "Reverted by ${event.actorName}"
        else -> "Submitted by ${event.actorName}"
    }
    val iconColor = when {
        isApproved -> Color(0xFF1B5E20)
        isRejected -> Color(0xFFB71C1C)
        isReverted -> Color(0xFF5C6BC0)
        else -> MaterialTheme.colorScheme.primary
    }
    val bgColor = when {
        isApproved -> Color(0xFFE8F5E9)
        isRejected -> Color(0xFFFFEBEE)
        isReverted -> Color(0xFFE8EAF6)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        dateFmt.format(java.util.Date(event.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Text(
                    currency.format(event.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }
            if (showRevert) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                TextButton(
                    onClick = onRevert,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Revert", style = MaterialTheme.typography.labelMedium)
                }
            }
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
