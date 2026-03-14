package com.thiccbokki.brohouse.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.thiccbokki.brohouse.TripViewModel
import com.thiccbokki.brohouse.data.SupplyItem
import com.thiccbokki.brohouse.data.TripMember
import com.thiccbokki.brohouse.data.ClaimEntry
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

val SUPPLY_CATEGORIES = listOf("Food", "Disposables", "Entertainment", "Drugs & Paraphernalia", "Other")

val QUANTITY_PRESETS = listOf("enough", "big ole box", "costco")

data class QuickAddItem(val name: String, val category: String, val quantity: String = "")

val QUICK_ADD_ITEMS = listOf(
    QuickAddItem("Burgers", "Food", "enough"),
    QuickAddItem("Buns", "Food", "enough"),
    QuickAddItem("Hot Dogs", "Food", "enough"),
    QuickAddItem("Hot Dog Buns", "Food", "enough"),
    QuickAddItem("Chili", "Food", "enough"),
    QuickAddItem("Ketchup", "Food"),
    QuickAddItem("Mustard", "Food"),
    QuickAddItem("Eggs", "Food", "enough"),
    QuickAddItem("Bacon", "Food", "enough"),
    QuickAddItem("Coffee", "Food"),
    QuickAddItem("Garbage Bags", "Disposables", "big ole box"),
    QuickAddItem("Plastic Cups", "Disposables", "costco"),
    QuickAddItem("Plastic Utensils", "Disposables", "costco"),
    QuickAddItem("Bluetooth Speaker", "Entertainment", "1"),
    QuickAddItem("Weed", "Drugs & Paraphernalia"),
    QuickAddItem("Shrooms", "Drugs & Paraphernalia"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliesScreen(
    viewModel: TripViewModel,
    isAdmin: Boolean,
    onNavigateBack: () -> Unit
) {
    val supplyItems by viewModel.supplyItems.collectAsState()
    val members by viewModel.members.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var claimItem by remember { mutableStateOf<SupplyItem?>(null) }
    var manageClaimItem by remember { mutableStateOf<SupplyItem?>(null) }
    var deleteItem by remember { mutableStateOf<SupplyItem?>(null) }
    var pendingQuickAddName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingQuickAddName, supplyItems) {
        val name = pendingQuickAddName ?: return@LaunchedEffect
        val found = supplyItems.find { it.name.equals(name, ignoreCase = true) }
        if (found != null) {
            claimItem = found
            pendingQuickAddName = null
        }
    }

    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val grouped = supplyItems.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplies") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showAddSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Item")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val existingNames = remember(supplyItems) {
            supplyItems.map { it.name.lowercase() }.toSet()
        }
        val availableQuickAdds = QUICK_ADD_ITEMS.filter { it.name.lowercase() !in existingNames }

        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            // Quick Add — admin only
            if (isAdmin && availableQuickAdds.isNotEmpty()) {
                item(key = "quick_add") {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "Quick Add",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        availableQuickAdds.chunked(3).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                row.forEach { quickItem ->
                                    ElevatedFilterChip(
                                        selected = false,
                                        onClick = {
                                            viewModel.addSupplyItem(
                                                quickItem.name,
                                                quickItem.category,
                                                quickItem.quantity
                                            )
                                            pendingQuickAddName = quickItem.name
                                        },
                                        label = { Text(quickItem.name) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }

            if (supplyItems.isEmpty()) {
                item(key = "empty") {
                    Text(
                        if (isAdmin) "No supplies yet — tap + or quick-add above!"
                        else "No supplies yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            SUPPLY_CATEGORIES.forEach { category ->
                val categoryItems = grouped[category] ?: return@forEach
                val isCollapsed = collapsedCategories[category] != false

                item(key = "header_$category") {
                    val claimedCount = categoryItems.count { it.isClaimed }
                    CategoryHeader(
                        category = category,
                        claimedCount = claimedCount,
                        totalCount = categoryItems.size,
                        isCollapsed = isCollapsed,
                        onToggle = { collapsedCategories[category] = !isCollapsed }
                    )
                }

                if (!isCollapsed) {
                    item(key = "category_items_$category") {
                        ReorderableCategory(
                            items = categoryItems,
                            isAdmin = isAdmin,
                            onReorder = { reordered -> viewModel.reorderSupplyItems(category, reordered) },
                            onClaim = { claimItem = it },
                            onManageClaims = { manageClaimItem = it },
                            onDelete = { deleteItem = it }
                        )
                    }
                }
            }
        }
    }

    if (isAdmin && showAddSheet) {
        AddSupplyItemSheet(
            onDismiss = { showAddSheet = false },
            onSave = { name, category, quantity ->
                viewModel.addSupplyItem(name, category, quantity)
                showAddSheet = false
            }
        )
    }

    claimItem?.let { item ->
        ClaimDialog(
            item = item,
            members = members,
            onDismiss = { claimItem = null },
            onClaim = { member, quantity ->
                viewModel.claimSupplyItem(item, member, quantity)
                claimItem = null
            }
        )
    }

    manageClaimItem?.let { item ->
        ManageClaimsDialog(
            item = item,
            members = members,
            onDismiss = { manageClaimItem = null },
            onRemoveClaim = { uid, displayName ->
                viewModel.unclaimSupplyItem(item, uid, displayName)
                val updated = item.removeClaim(uid, displayName)
                manageClaimItem = if (updated.claimedNames.isEmpty()) null else updated
            },
            onAddMore = {
                manageClaimItem = null
                claimItem = item
            }
        )
    }

    if (isAdmin) {
        deleteItem?.let { item ->
            AlertDialog(
                onDismissRequest = { deleteItem = null },
                title = { Text("Delete Item") },
                text = { Text("Remove \"${item.name}\" from the supplies list?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteSupplyItem(item)
                        deleteItem = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteItem = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    category: String,
    claimedCount: Int,
    totalCount: Int,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isCollapsed) -90f else 0f,
        label = "caret_rotation"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = if (isCollapsed) "Expand" else "Collapse",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp).rotate(rotation)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = category,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$claimedCount/$totalCount",
            style = MaterialTheme.typography.labelMedium,
            color = if (claimedCount < totalCount) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ReorderableCategory(
    items: List<SupplyItem>,
    isAdmin: Boolean,
    onReorder: (List<SupplyItem>) -> Unit,
    onClaim: (SupplyItem) -> Unit,
    onManageClaims: (SupplyItem) -> Unit,
    onDelete: (SupplyItem) -> Unit
) {
    var localItems by remember(items) { mutableStateOf(items) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = with(LocalDensity.current) { 60.dp.toPx() }

    Column {
        localItems.forEachIndexed { index, item ->
            val isDragging = index == draggedIndex
            key(item.id) {
                SwipeToDismissItem(
                    item = item,
                    isAdmin = isAdmin,
                    isDragging = isDragging,
                    dragOffset = if (isDragging) dragOffset else 0f,
                    onClaim = { onClaim(item) },
                    onManageClaims = { onManageClaims(item) },
                    onDelete = { onDelete(item) },
                    onDragStart = { draggedIndex = index; dragOffset = 0f },
                    onDrag = { delta ->
                        dragOffset += delta
                        val targetIndex = draggedIndex + (dragOffset / itemHeightPx).roundToInt()
                        val clampedTarget = targetIndex.coerceIn(0, localItems.lastIndex)
                        if (clampedTarget != draggedIndex) {
                            val mutable = localItems.toMutableList()
                            val movedItem = mutable.removeAt(draggedIndex)
                            mutable.add(clampedTarget, movedItem)
                            localItems = mutable
                            dragOffset -= (clampedTarget - draggedIndex) * itemHeightPx
                            draggedIndex = clampedTarget
                        }
                    },
                    onDragEnd = { draggedIndex = -1; dragOffset = 0f; onReorder(localItems) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    item: SupplyItem,
    isAdmin: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    onClaim: () -> Unit,
    onManageClaims: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (isAdmin && value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        }
    )
    Box(
        modifier = Modifier.then(
            if (isDragging) Modifier.zIndex(1f).graphicsLayer { translationY = dragOffset }
            else Modifier
        )
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                if (isAdmin) {
                    val color by animateColorAsState(
                        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface,
                        label = "swipe_bg"
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError)
                    }
                }
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = isAdmin && !isDragging
        ) {
            SupplyItemRow(
                item = item,
                isAdmin = isAdmin,
                isDragging = isDragging,
                onClick = onClaim,
                onChipClick = onManageClaims,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd
            )
        }
    }
}

@Composable
private fun SupplyItemRow(
    item: SupplyItem,
    isAdmin: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onChipClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val elevation = if (isDragging) 8.dp else 0.dp
    Surface(tonalElevation = elevation, shadowElevation = elevation) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAdmin) {
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onDragStart() },
                                onDrag = { change, offset -> change.consume(); onDrag(offset.y) },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragEnd
                            )
                        }
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                if (item.isClaimed) {
                    item.claimEntries.forEach { entry ->
                        val label = if (entry.quantity.isNotBlank()) "${entry.name}: ${entry.quantity}" else entry.name
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else if (item.quantity.isNotBlank()) {
                    Text(
                        item.quantity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            val names = item.claimedNames
            if (names.isNotEmpty()) {
                SuggestionChip(
                    onClick = onChipClick,
                    label = { Text(if (names.size == 1) names.first() else "${names.size} claimed") }
                )
            } else {
                AssistChip(onClick = onClick, label = { Text("Unclaimed") })
            }
        }
    }
}

@Composable
private fun ClaimDialog(
    item: SupplyItem,
    members: List<TripMember>,
    onDismiss: () -> Unit,
    onClaim: (TripMember, String) -> Unit
) {
    var selectedMember by remember { mutableStateOf<TripMember?>(null) }
    var quantity by remember { mutableStateOf(item.quantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedMember == null) "Who's bringing this?" else "Claim ${item.name}") },
        text = {
            if (members.isEmpty()) {
                Text("Add some people first!")
            } else if (selectedMember == null) {
                Column {
                    members.forEach { member ->
                        TextButton(
                            onClick = { selectedMember = member },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(member.displayName, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                Column {
                    Text("${selectedMember!!.displayName} is bringing this")
                    Spacer(Modifier.height(12.dp))
                    QuantityField(value = quantity, onValueChange = { quantity = it })
                }
            }
        },
        confirmButton = {
            if (selectedMember != null) {
                Button(onClick = { onClaim(selectedMember!!, quantity.trim()) }) { Text("Claim") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        dismissButton = {
            if (selectedMember != null) {
                TextButton(onClick = { selectedMember = null }) { Text("Back") }
            }
        }
    )
}

@Composable
private fun ManageClaimsDialog(
    item: SupplyItem,
    members: List<TripMember>,
    onDismiss: () -> Unit,
    onRemoveClaim: (uid: String, displayName: String) -> Unit,
    onAddMore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name) },
        text = {
            Column {
                Text("Claimed by:", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                item.claimEntries.forEach { entry ->
                    val member = members.find { it.displayName == entry.name }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                            if (entry.quantity.isNotBlank()) {
                                Text(
                                    entry.quantity,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        TextButton(onClick = {
                            onRemoveClaim(member?.uid ?: "", entry.name)
                        }) { Text("Remove") }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onAddMore) { Text("Add Person") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupplyItemSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, quantity: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SUPPLY_CATEGORIES.first()) }
    var quantity by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val trimmedName = name.trim()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Add Supply Item", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(12.dp))
            Text("Category", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            SUPPLY_CATEGORIES.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    row.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            QuantityField(value = quantity, onValueChange = { quantity = it })
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(trimmedName, selectedCategory, quantity.trim()) },
                    enabled = trimmedName.isNotEmpty()
                ) { Text("Save") }
            }
        }
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun QuantityField(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text("Quantity", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QUANTITY_PRESETS.forEach { preset ->
                FilterChip(
                    selected = value == preset,
                    onClick = { onValueChange(if (value == preset) "" else preset) },
                    label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Or type your own") },
            placeholder = { Text("e.g. 12, 2 bags, a couple") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
