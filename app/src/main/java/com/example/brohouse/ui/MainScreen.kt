package com.example.brohouse.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.brohouse.MainViewModel
import com.example.brohouse.data.HouseDetails
import com.example.brohouse.data.Person
import com.example.brohouse.data.SupplyItem
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToHouseDetails: () -> Unit,
    onNavigateToSupplies: () -> Unit
) {
    val people by viewModel.people.collectAsState()
    val houseDetails by viewModel.houseDetails.collectAsState()
    val supplyItems by viewModel.supplyItems.collectAsState()

    var showAddPerson by remember { mutableStateOf(false) }
    var editNightsPerson by remember { mutableStateOf<Person?>(null) }
    var addPaymentPerson by remember { mutableStateOf<Person?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt Tracker") },
                actions = {
                    IconButton(onClick = { showAddPerson = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Person")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                HouseDetailsCard(
                    details = houseDetails,
                    guestCount = people.size,
                    onClick = onNavigateToHouseDetails,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                SuppliesCard(
                    supplyItems = supplyItems,
                    onClick = onNavigateToSupplies,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (people.isEmpty()) {
                item {
                    Text(
                        text = "Nobody wants to go T_T",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                items(people, key = { it.id }) { person ->
                    PersonRowView(
                        person = person,
                        onEditNights = { editNightsPerson = person },
                        onAddPayment = { addPaymentPerson = person }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 80.dp))
                }
            }
        }
    }

    if (showAddPerson) {
        AddPersonSheet(
            onDismiss = { showAddPerson = false },
            onSave = { name ->
                viewModel.addPerson(name)
                showAddPerson = false
            }
        )
    }

    editNightsPerson?.let { person ->
        EditNightsSheet(
            currentNights = person.nightsStayed,
            onDismiss = { editNightsPerson = null },
            onSave = { nights ->
                viewModel.updateNights(person, nights)
                editNightsPerson = null
            }
        )
    }

    addPaymentPerson?.let { person ->
        AddPaymentSheet(
            currentOwed = person.moneyOwed,
            onDismiss = { addPaymentPerson = null },
            onSave = { amount ->
                viewModel.addPayment(person, amount)
                addPaymentPerson = null
            }
        )
    }
}

@Composable
fun PersonRowView(
    person: Person,
    onEditNights: () -> Unit,
    onAddPayment: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(seed = person.avatarSeed)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(person.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.NightlightRound,
                        contentDescription = null,
                        tint = Color(0xFF3F51B5),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    val n = person.nightsStayed
                    Text(
                        "$n ${if (n == 1) "night" else "nights"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AttachMoney,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        NumberFormat.getCurrencyInstance(Locale.US).format(person.moneyOwed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit Nights") },
                    leadingIcon = { Icon(Icons.Filled.NightlightRound, null) },
                    onClick = { showMenu = false; onEditNights() }
                )
                DropdownMenuItem(
                    text = { Text("Add Payment") },
                    leadingIcon = { Icon(Icons.Filled.AttachMoney, null) },
                    onClick = { showMenu = false; onAddPayment() }
                )
            }
        }
    }
}

@Composable
fun HouseDetailsCard(
    details: HouseDetails?,
    guestCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalNights = details?.totalNights ?: 0
    val totalCost = details?.totalCost ?: 0.0
    val thumbnailData = details?.thumbnailData

    val bitmap = remember(thumbnailData) {
        thumbnailData?.let { data ->
            runCatching { BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap() }.getOrNull()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Text(
                "House Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "House photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                            Text(
                                "House Photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatCell(
                    label = "Total Nights",
                    value = "$totalNights",
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                StatCell(
                    label = "Total Cost",
                    value = NumberFormat.getCurrencyInstance(Locale.US).format(totalCost),
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.height(36.dp))
                StatCell(
                    label = "No. of Guests",
                    value = "$guestCount",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun SuppliesCard(
    supplyItems: List<SupplyItem>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unclaimed = supplyItems.count { it.claimedByPersonId == null }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Supplies", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (supplyItems.isEmpty()) "No items yet — tap to add supplies"
                else "${supplyItems.size} ${if (supplyItems.size == 1) "item" else "items"}, $unclaimed unclaimed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
