package com.example.brohouse.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val trimmed = name.trim()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Add Person", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrect = false
                ),
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
                    onClick = { onSave(trimmed) },
                    enabled = trimmed.isNotEmpty()
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
fun EditNightsSheet(currentNights: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var nightsText by remember { mutableStateOf(if (currentNights > 0) "$currentNights" else "") }
    val focusRequester = remember { FocusRequester() }
    val parsed = nightsText.toIntOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Edit Nights", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = nightsText,
                onValueChange = { nightsText = it.filter(Char::isDigit).take(1) },
                label = { Text("Nights") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text("Currently: $currentNights ${if (currentNights == 1) "night" else "nights"}") },
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
fun AddPaymentSheet(currentOwed: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var amountText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val parsed = amountText.trim().toDoubleOrNull()?.takeIf { it >= 0 }
    val currentFormatted = String.format("$%.2f", currentOwed)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Add Payment", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                prefix = { Text("$") },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Currently owes: $currentFormatted") },
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
                ) { Text("Save") }
            }
        }
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}
