package com.bennybokki.frientrip.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bennybokki.frientrip.TripViewModel
import com.bennybokki.frientrip.data.TripHistoryEvent
import com.bennybokki.frientrip.ui.theme.ElectricCyan
import com.bennybokki.frientrip.ui.theme.LocalIsDarkTheme
import com.bennybokki.frientrip.ui.theme.NeonGreen
import com.bennybokki.frientrip.ui.theme.NeonPurple
import com.bennybokki.frientrip.ui.theme.StatusPaidBgDark
import com.bennybokki.frientrip.ui.theme.StatusPaidBgLight
import com.bennybokki.frientrip.ui.theme.StatusPendingBgDark
import com.bennybokki.frientrip.ui.theme.StatusPendingBgLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class HistorySection(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val events: List<TripHistoryEvent>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val allEvents by viewModel.history.collectAsState()

    val grouped = allEvents.groupBy { it.category }

    val isDark = LocalIsDarkTheme.current
    val sections = listOf(
        HistorySection(
            key = "expenses",
            label = "Expenses",
            icon = Icons.Default.AttachMoney,
            containerColor = if (isDark) StatusPendingBgDark else StatusPendingBgLight,
            contentColor = ElectricCyan,
            events = grouped["expenses"]?.sortedByDescending { it.timestamp } ?: emptyList()
        ),
        HistorySection(
            key = "supplies",
            label = "Supplies",
            icon = Icons.Default.Checklist,
            containerColor = if (isDark) StatusPaidBgDark else StatusPaidBgLight,
            contentColor = NeonGreen,
            events = grouped["supplies"]?.sortedByDescending { it.timestamp } ?: emptyList()
        ),
        HistorySection(
            key = "payments",
            label = "Payments",
            icon = Icons.Default.Payments,
            containerColor = if (isDark) Color(0xFF1A0A2E) else Color(0xFFF3E5F5),
            contentColor = NeonPurple,
            events = grouped["payments"]?.sortedByDescending { it.timestamp } ?: emptyList()
        )
    ).filter { it.events.isNotEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trip History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (allEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet\nActivity will appear here",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                sections.forEach { section ->
                    item(key = "header_${section.key}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = section.containerColor
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = section.contentColor,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(18.dp)
                                )
                            }
                            Text(
                                section.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = section.containerColor
                            ) {
                                Text(
                                    "${section.events.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = section.contentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    items(section.events, key = { "${section.key}_${it.id}" }) { event ->
                        HistoryEventRow(
                            event = event,
                            accentColor = section.contentColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                        )
                    }

                    item(key = "divider_${section.key}") {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEventRow(
    event: TripHistoryEvent,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline dot
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = accentColor.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxSize()
            ) {}
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                formatTimestamp(event.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        diff < 172_800_000L -> "Yesterday"
        else -> {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            val thisYear = Calendar.getInstance().get(Calendar.YEAR)
            val pattern = if (cal.get(Calendar.YEAR) == thisYear) "MMM d" else "MMM d, yyyy"
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
        }
    }
}
