package com.bennybokki.frientrip.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import kotlin.math.absoluteValue

/**
 * A Card wrapper that adds vivid accent borders.
 *
 * - **Dark mode**: 1dp neon border on `#121826` background, 0dp elevation (glowing effect).
 * - **Light mode**: 1dp border at 25% opacity on `#F8FAFC` background, 2dp elevation.
 *
 * [accentIndex] selects the border color from [VividAccents] (cycles via modulo).
 */
@Composable
fun VividCard(
    modifier: Modifier = Modifier,
    accentIndex: Int = 0,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val accentColor = VividAccents[accentIndex.absoluteValue % VividAccents.size]
    val border = if (isDark) {
        BorderStroke(2.dp, accentColor)
    } else {
        BorderStroke(2.dp, accentColor.copy(alpha = 0.30f))
    }
    val containerColor = if (isDark) DarkCardSurface else Color(0xFFF8FAFC)
    val elevation = if (isDark) 1.dp else 6.dp

    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = shape,
            border = border,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            modifier = modifier,
            content = content
        )
    } else {
        Card(
            shape = shape,
            border = border,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            modifier = modifier,
            content = content
        )
    }
}

/** Semantic status types for [VividStatusBadge]. */
enum class VividStatus { PAID, DUE, PENDING }

/**
 * Theme-aware status pill that uses the Vivid Pulse semantic colors.
 *
 * - **PAID**: Neon Green (dark) / deep green (light)
 * - **DUE**: Vivid Pink (dark) / deep pink (light)
 * - **PENDING**: Electric Cyan (dark) / deep cyan (light)
 */
@Composable
fun VividStatusBadge(label: String, status: VividStatus) {
    val isDark = LocalIsDarkTheme.current
    val (textColor, bgColor) = when (status) {
        VividStatus.PAID -> if (isDark) StatusPaidDark to StatusPaidBgDark
                            else StatusPaidLight to StatusPaidBgLight
        VividStatus.DUE -> if (isDark) StatusDueDark to StatusDueBgDark
                           else StatusDueLight to StatusDueBgLight
        VividStatus.PENDING -> if (isDark) StatusPendingDark to StatusPendingBgDark
                               else StatusPendingLight to StatusPendingBgLight
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
