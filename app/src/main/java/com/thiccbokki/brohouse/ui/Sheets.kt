package com.thiccbokki.brohouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
