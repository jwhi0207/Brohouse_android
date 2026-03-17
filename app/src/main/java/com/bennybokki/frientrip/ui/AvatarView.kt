package com.bennybokki.frientrip.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 10 distinct avatar background colors — index stored as avatarSeed in Firestore. */
val AVATAR_COLORS = listOf(
    Color(0xFF5C6BC0), // Indigo
    Color(0xFF42A5F5), // Blue
    Color(0xFF26C6DA), // Cyan
    Color(0xFF26A69A), // Teal
    Color(0xFF66BB6A), // Green
    Color(0xFFFFA726), // Orange
    Color(0xFFEF5350), // Red
    Color(0xFFEC407A), // Pink
    Color(0xFFAB47BC), // Purple
    Color(0xFF8D6E63), // Brown
)

/**
 * Circular avatar showing the first letter of [name] on a solid color background.
 *
 * The color is chosen by [seed] % 10 so existing random seeds map gracefully to
 * a deterministic color, and newly chosen palette indices (0–9) map directly.
 */
@Composable
fun AvatarView(seed: Long, name: String = "", size: Dp = 52.dp) {
    val colorIndex = (seed % 10L).toInt().let { if (it < 0) it + 10 else it }
    val bgColor = AVATAR_COLORS[colorIndex]
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val fontSize = with(LocalDensity.current) { (size.toPx() * 0.42f).toSp() }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            lineHeight = fontSize,
        )
    }
}
