package com.bennybokki.frientrip.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.bennybokki.frientrip.TripViewModel
import com.bennybokki.frientrip.data.SharedExpense
import com.bennybokki.frientrip.data.SupplyItem
import com.bennybokki.frientrip.data.TripMember
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun categoryIcon(category: String): ImageVector = when (category) {
    "Food"                    -> Icons.Default.Restaurant
    "Disposables"             -> Icons.Default.ShoppingBag
    "Entertainment"           -> Icons.Default.SportsEsports
    "Outdoor & Games"         -> Icons.Default.SportsBaseball
    else                      -> Icons.Default.Category
}

val SUPPLY_CATEGORIES = listOf("Food", "Disposables", "Entertainment", "Outdoor & Games", "Other")

private val PICKER_UNITS = listOf(
    "", "pieces", "dozen", "packs", "boxes",
    "bags", "cases", "bottles", "cans", "lbs", "oz", "gallons", "liters"
)
private val QUANTITY_PATTERN = Regex("""^(\d{1,2})\s*(.*)$""")

data class QuickAddItem(val name: String, val category: String, val quantity: String = "")

val QUICK_ADD_ITEMS = listOf(
    QuickAddItem("Burgers", "Food"),
    QuickAddItem("Buns", "Food"),
    QuickAddItem("Hot Dogs", "Food"),
    QuickAddItem("Hot Dog Buns", "Food"),
    QuickAddItem("Chili", "Food"),
    QuickAddItem("Ketchup", "Food"),
    QuickAddItem("Mustard", "Food"),
    QuickAddItem("Eggs", "Food"),
    QuickAddItem("Bacon", "Food"),
    QuickAddItem("Coffee", "Food"),
    QuickAddItem("Garbage Bags", "Disposables"),
    QuickAddItem("Plastic Cups", "Disposables"),
    QuickAddItem("Plastic Utensils", "Disposables"),
    QuickAddItem("Bluetooth Speaker", "Entertainment"),
    QuickAddItem("Cards", "Outdoor & Games"),
    QuickAddItem("Board Games", "Outdoor & Games"),
    QuickAddItem("S'mores Kit", "Food"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliesScreen(
    viewModel: TripViewModel,
    isAdmin: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val supplyItems by viewModel.supplyItems.collectAsState()
    val members by viewModel.members.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<SupplyItem?>(null) }
    var deleteItem by remember { mutableStateOf<SupplyItem?>(null) }
    var billToGroupItem by remember { mutableStateOf<SupplyItem?>(null) }
    var pendingQuickAddName by remember { mutableStateOf<String?>(null) }

    // Keep detailItem in sync with live data so claims update while sheet is open
    val liveDetailItem = detailItem?.let { current ->
        supplyItems.find { it.id == current.id }
    }
    LaunchedEffect(liveDetailItem) {
        if (liveDetailItem != null) detailItem = liveDetailItem
    }

    LaunchedEffect(pendingQuickAddName, supplyItems) {
        val name = pendingQuickAddName ?: return@LaunchedEffect
        val found = supplyItems.find { it.name.equals(name, ignoreCase = true) }
        if (found != null) {
            detailItem = found
            pendingQuickAddName = null
        }
    }

    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val grouped = supplyItems.groupBy { it.category }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Supplies",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { /* TODO: search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val existingNames = remember(supplyItems) {
            supplyItems.map { it.name.lowercase() }.toSet()
        }
        val availableQuickAdds = QUICK_ADD_ITEMS.filter { it.name.lowercase() !in existingNames }

        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Quick Add ─────────────────────────────────────────────────────
            if (availableQuickAdds.isNotEmpty()) {
                item(key = "quick_add") {
                    Column {
                        Text(
                            "QUICK ADD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = TextUnit(1.5f, TextUnitType.Sp),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableQuickAdds, key = { it.name }) { quickItem ->
                                Surface(
                                    onClick = {
                                        viewModel.addSupplyItem(
                                            quickItem.name,
                                            quickItem.category,
                                            quickItem.quantity
                                        )
                                        pendingQuickAddName = quickItem.name
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            quickItem.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (supplyItems.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "No supplies yet — tap + or quick-add above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Categories ────────────────────────────────────────────────────
            SUPPLY_CATEGORIES.forEach { category ->
                val categoryItems = grouped[category] ?: return@forEach
                val isCollapsed = collapsedCategories[category] != false
                val claimedCount = categoryItems.count { it.isClaimed }

                item(key = "cat_$category") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        CategoryHeader(
                            category = category,
                            claimedCount = claimedCount,
                            totalCount = categoryItems.size,
                            isCollapsed = isCollapsed,
                            onToggle = { collapsedCategories[category] = !isCollapsed }
                        )
                        if (!isCollapsed) {
                            ReorderableCategory(
                                items = categoryItems,
                                isAdmin = isAdmin,
                                onReorder = { reordered -> viewModel.reorderSupplyItems(category, reordered) },
                                onClaim = { detailItem = it },
                                onManageClaims = { detailItem = it },
                                onDelete = { deleteItem = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val existingNamesForSheet = remember(supplyItems) {
            supplyItems.map { it.name.lowercase() }.toSet()
        }
        AddSupplyItemSheet(
            existingNames = existingNamesForSheet,
            onDismiss = { showAddSheet = false },
            onSave = { name, category, quantity ->
                viewModel.addSupplyItem(name, category, quantity)
                showAddSheet = false
            },
            onSaveAndClaim = { name, category, quantity ->
                viewModel.addSupplyItem(name, category, quantity)
                pendingQuickAddName = name
                showAddSheet = false
            }
        )
    }

    detailItem?.let { item ->
        val currentMember = members.find { it.uid == viewModel.currentUid }
        val linkedExpense = expenses.find { it.linkedSupplyId == item.id }
        ItemDetailSheet(
            item = item,
            currentMember = currentMember,
            members = members,
            isAdmin = isAdmin,
            linkedExpense = linkedExpense,
            onDismiss = { detailItem = null },
            onClaim = { member, quantity ->
                viewModel.claimSupplyItem(item, member, quantity)
            },
            onRemoveClaim = { uid, displayName ->
                viewModel.unclaimSupplyItem(item, uid, displayName)
            },
            onBillToGroup = {
                detailItem = null
                billToGroupItem = item
            }
        )
    }

    billToGroupItem?.let { item ->
        AddExpenseSheet(
            initialDescription = item.name,
            initialCategory = "supply",
            initialLinkedSupplyId = item.id,
            onDismiss = { billToGroupItem = null },
            onSubmitFull = { description, amount, splitMethod, category, linkedSupplyId ->
                viewModel.submitExpense(description, amount, splitMethod, category, linkedSupplyId)
                billToGroupItem = null
            }
        )
    }

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
    val progress = if (totalCount > 0) claimedCount.toFloat() / totalCount else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$claimedCount/$totalCount claimed",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp).rotate(rotation)
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun ReorderableCategory(
    items: List<SupplyItem>,
    isAdmin: Boolean = false,
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
                    isDragging = isDragging,
                    isAdmin = isAdmin,
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
    isDragging: Boolean,
    isAdmin: Boolean = false,
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
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
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
            },
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = !isDragging && isAdmin
        ) {
            SupplyItemRow(
                item = item,
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
                OutlinedButton(
                    onClick = onClick,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        "Claim",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailSheet(
    item: SupplyItem,
    currentMember: TripMember?,
    members: List<TripMember>,
    isAdmin: Boolean = false,
    linkedExpense: SharedExpense? = null,
    onDismiss: () -> Unit,
    onClaim: (TripMember, String) -> Unit,
    onRemoveClaim: (uid: String, displayName: String) -> Unit,
    onBillToGroup: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showClaimPicker by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            if (!showClaimPicker) {
                // ── Item detail view ─────────────────────────────────────
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = categoryIcon(item.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            item.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Claims section ───────────────────────────────────────
                if (item.isClaimed) {
                    Text(
                        "CLAIMED BY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = TextUnit(1.5f, TextUnitType.Sp)
                    )
                    Spacer(Modifier.height(12.dp))

                    item.claimEntries.forEach { entry ->
                        val member = members.find { it.displayName == entry.name }
                        val isMe = member?.uid == currentMember?.uid
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMe)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isMe) "${entry.name} (you)" else entry.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (entry.quantity.isNotBlank()) {
                                        Text(
                                            entry.quantity,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                if (isMe || isAdmin) {
                                    TextButton(onClick = {
                                        onRemoveClaim(member?.uid ?: "", entry.name)
                                    }) {
                                        Text("Remove", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                } else {
                    // No claims yet
                    Text(
                        "No one has claimed this item yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(20.dp))
                }

                // ── Bill to Group section ─────────────────────────────────
                if (item.isClaimed) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(Modifier.height(12.dp))
                    if (linkedExpense != null) {
                        val currency = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.US)
                        val statusText = if (linkedExpense.approved) "Approved" else "Pending"
                        val statusColor = if (linkedExpense.approved) Color(0xFF2E7D32) else Color(0xFFE65100)
                        val bgColor = if (linkedExpense.approved) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Billed to Group", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    currency.format(linkedExpense.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = bgColor
                            ) {
                                Text(
                                    statusText.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onBillToGroup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bill to Group")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── Claim button ─────────────────────────────────────────
                if (currentMember == null) {
                    Text(
                        "You need to be a trip member to claim items.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Button(
                        onClick = { showClaimPicker = true },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (item.isClaimed) "Add My Claim" else "Claim This Item",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                // ── Quantity picker view ─────────────────────────────────
                Text(
                    "Claim ${item.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "How much are you bringing?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(24.dp))

                QuantityField(value = quantity, onValueChange = { quantity = it })
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showClaimPicker = false },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Back", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = {
                            if (currentMember != null) {
                                onClaim(currentMember, quantity.trim())
                                showClaimPicker = false
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(2f).height(52.dp)
                    ) {
                        Text("Confirm Claim", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupplyItemSheet(
    existingNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, quantity: String) -> Unit,
    onSaveAndClaim: (name: String, category: String, quantity: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SUPPLY_CATEGORIES.first()) }
    var quantity by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val trimmedName = name.trim()
    val isDuplicate = trimmedName.lowercase() in existingNames

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier
            .fillMaxHeight(0.85f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
        ) {
            Text("Add Supply Item", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                singleLine = true,
                isError = isDuplicate,
                supportingText = if (isDuplicate) {{ Text("\"$trimmedName\" is already on the list") }} else null,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                OutlinedButton(
                    onClick = { onSave(trimmedName, selectedCategory, quantity.trim()) },
                    enabled = trimmedName.isNotEmpty() && !isDuplicate,
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                Button(
                    onClick = { onSaveAndClaim(trimmedName, selectedCategory, quantity.trim()) },
                    enabled = trimmedName.isNotEmpty() && !isDuplicate,
                    modifier = Modifier.weight(1.5f)
                ) { Text("Save & Claim") }
            }
        }
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

@Composable
private fun QuantityField(value: String, onValueChange: (String) -> Unit) {
    // Try to parse existing value back into number + unit
    val match = remember(value) { QUANTITY_PATTERN.matchEntire(value.trim()) }
    val parsedNumber = match?.groupValues?.get(1)?.toIntOrNull()
    val parsedUnit = match?.groupValues?.get(2)?.trim()?.lowercase()
    val unitIndex = if (parsedUnit != null) PICKER_UNITS.indexOfFirst { it == parsedUnit }.takeIf { it >= 0 } else null
    val canUsePicker = parsedNumber != null && parsedNumber in 1..99 && (unitIndex != null || parsedUnit.isNullOrEmpty())

    val initialNumber = if (canUsePicker && parsedNumber != null) parsedNumber else 1
    val initialUnitIndex = unitIndex ?: 0

    var useCustomText by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(initialNumber) }
    var unitIdx by remember { mutableIntStateOf(initialUnitIndex) }
    var customText by remember { mutableStateOf(if (useCustomText) value else "") }

    fun emitValue() {
        val unit = PICKER_UNITS[unitIdx]
        onValueChange("$count $unit".trim())
    }

    // Emit the picker's initial value so the parent has a non-empty quantity
    LaunchedEffect(Unit) {
        if (value.isEmpty()) emitValue()
    }

    Column {
        Text("Quantity", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))

        if (!useCustomText) {
            // ── Current value display ────────────────────────────────────
            val unitLabel = PICKER_UNITS[unitIdx].ifEmpty { "" }
            Text(
                text = if (unitLabel.isNotEmpty()) "$count $unitLabel" else count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // ── Horizontal scroll ruler ──────────────────────────────────
            HorizontalRulerPicker(
                selectedValue = count,
                range = 1..99,
                onValueChange = { count = it; emitValue() }
            )

            Spacer(Modifier.height(16.dp))

            // ── Unit dropdown ─────────────────────────────────────────────
            UnitDropdown(
                selectedIndex = unitIdx,
                onSelected = { unitIdx = it; emitValue() }
            )

            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { useCustomText = true; customText = ""; onValueChange("") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Type custom amount") }
        } else {
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it; onValueChange(it) },
                label = { Text("Quantity") },
                placeholder = { Text("e.g. enough, a couple, big ole box") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = {
                    useCustomText = false
                    count = 1
                    unitIdx = 0
                    emitValue()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Use picker") }
        }
    }
}

@Composable
private fun HorizontalRulerPicker(
    selectedValue: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    val itemWidthDp = 48.dp
    val density = LocalDensity.current
    val itemWidthPx = with(density) { itemWidthDp.toPx() }

    // Calculate padding items needed to center first/last real item
    // We use a large fixed count; the spacer width is set at layout time
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedValue - range.first
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Track the center item continuously (during and after scroll)
    var centerIndex by remember { mutableIntStateOf(selectedValue - range.first) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset +
                (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - viewportCenter)
            }?.index ?: 0
        }.collect { idx ->
            centerIndex = idx
        }
    }

    // Emit value when scroll settles
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val value = (centerIndex + range.first).coerceIn(range)
                if (value != selectedValue) onValueChange(value)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        // Half the viewport width in px, used for side padding
        val halfViewportPx = with(density) { (maxWidth / 2).toPx() }
        val sidePadDp = with(density) { (halfViewportPx - itemWidthPx / 2).toDp() }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = sidePadDp)
        ) {
            items(range.count()) { index ->
                val number = range.first + index

                // Distance from center: 0 = centered, 1+ = off-screen
                val distFromCenter = kotlin.math.abs(index - centerIndex).toFloat()
                val proximity = (1f - (distFromCenter / 3f)).coerceIn(0f, 1f)

                // Animated scale and alpha based on proximity to center
                val scale by animateFloatAsState(
                    targetValue = 1f + proximity * 0.7f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale_$number"
                )
                val alpha by animateFloatAsState(
                    targetValue = 0.3f + proximity * 0.7f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "alpha_$number"
                )
                val isCenter = index == centerIndex

                Box(
                    modifier = Modifier
                        .width(itemWidthDp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // Bubble background for center item
                    if (isCenter) {
                        Box(
                            modifier = Modifier
                                .size((40 * scale).dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .background(bubbleColor, CircleShape)
                        )
                    }
                    Text(
                        text = number.toString(),
                        style = if (isCenter) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) primaryColor else dimColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    // Build alphabetically sorted entries, keeping index mapping back to PICKER_UNITS
    val sortedEntries = remember {
        PICKER_UNITS.mapIndexed { index, unit -> index to unit.ifEmpty { "(none)" } }
            .sortedBy { it.second.lowercase() }
    }
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = PICKER_UNITS[selectedIndex].ifEmpty { "(none)" }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .widthIn(min = 180.dp, max = 240.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortedEntries.forEach { (originalIndex, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelected(originalIndex)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleFlowRow(
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val hSpacingPx = with(LocalDensity.current) { horizontalSpacing.roundToPx() }
    val vSpacingPx = with(LocalDensity.current) { verticalSpacing.roundToPx() }

    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val maxWidth = constraints.maxWidth

        var x = 0
        var y = 0
        var rowHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEach { placeable ->
            if (x + placeable.width > maxWidth && x > 0) {
                x = 0
                y += rowHeight + vSpacingPx
                rowHeight = 0
            }
            positions.add(x to y)
            rowHeight = maxOf(rowHeight, placeable.height)
            x += placeable.width + hSpacingPx
        }

        val totalHeight = y + rowHeight
        layout(maxWidth, totalHeight) {
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(positions[i].first, positions[i].second)
            }
        }
    }
}
