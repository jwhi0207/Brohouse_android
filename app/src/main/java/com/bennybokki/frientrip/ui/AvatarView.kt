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
import androidx.compose.ui.layout.ContentScale
import com.bennybokki.frientrip.ui.theme.ElectricCyan
import com.bennybokki.frientrip.ui.theme.ElectricLime
import com.bennybokki.frientrip.ui.theme.MintGreen
import com.bennybokki.frientrip.ui.theme.NeonGreen
import com.bennybokki.frientrip.ui.theme.NeonPurple
import com.bennybokki.frientrip.ui.theme.VividPink
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** 10 distinct avatar background colors — Vivid Pulse palette. */
val AVATAR_COLORS = listOf(
    NeonPurple,            // Purple
    ElectricCyan,          // Cyan
    MintGreen,             // Mint
    NeonGreen,             // Green
    ElectricLime,          // Lime
    VividPink,             // Pink
    Color(0xFFFF4D6A),     // Light Pink
    Color(0xFFE040FB),     // Light Purple
    Color(0xFFD050F0),     // Mid Purple
    Color(0xFF8D6E63),     // Brown (variety)
)

/** 12 fixed DiceBear seeds — each produces a distinct pixel art character. */
val AVATAR_SEEDS = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L)

/**
 * Circular avatar using DiceBear Pixel Art style.
 *
 * [seed] determines the pixel art character (passed to DiceBear).
 * [colorIndex] determines the background color (0–9 from [AVATAR_COLORS]).
 * The colored initial is shown as a fallback while the image loads.
 */
@Composable
fun AvatarView(seed: Long, colorIndex: Int, name: String = "", size: Dp = 52.dp) {
    val bgColor = AVATAR_COLORS[colorIndex.coerceIn(0, AVATAR_COLORS.lastIndex)]
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
        // Fallback initial shown while image loads or on error
        Text(
            text = initial,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            lineHeight = fontSize,
        )
        AsyncImage(
            model = "https://api.dicebear.com/9.x/pixel-art/png?seed=$seed&size=128",
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}
