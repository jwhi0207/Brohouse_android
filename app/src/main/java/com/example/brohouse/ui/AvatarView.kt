package com.thiccbokki.brohouse.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Deterministic xorshift64 — mirrors the iOS SeededGenerator exactly.
private class SeededGenerator(seed: Long) {
    private var state: Long = if (seed == 0L) 1L else seed

    init {
        repeat(8) { next() }
    }

    fun next(): Long {
        state = state xor (state shl 13)
        state = state xor (state ushr 7)
        state = state xor (state shl 17)
        return state
    }

    // Matches Swift's: Double(next()) / Double(UInt64.max)
    fun nextDouble(): Double = next().toULong().toDouble() / ULong.MAX_VALUE.toDouble()
}

private fun avatarColor(rng: SeededGenerator): Color {
    val hue = (rng.nextDouble() * 360.0).toFloat()
    val sat = (0.5 + rng.nextDouble() * 0.5).toFloat()
    val bri = (0.55 + rng.nextDouble() * 0.45).toFloat()
    return Color.hsv(hue, sat, bri)
}

@Composable
fun AvatarView(seed: Long, size: Dp = 52.dp) {
    Canvas(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
    ) {
        drawAvatar(seed = seed)
    }
}

private fun DrawScope.drawAvatar(seed: Long) {
    val rng = SeededGenerator(seed)
    val w = this.size.width
    val h = this.size.height

    val bg1 = avatarColor(rng)
    val bg2 = avatarColor(rng)

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(bg1, bg2),
            start = Offset.Zero,
            end = Offset(w, h)
        )
    )

    val shapeCount = 4 + (rng.next().toULong() % 3uL).toInt()
    repeat(shapeCount) {
        val cx = (rng.nextDouble() * w * 1.6 - w * 0.3).toFloat()
        val cy = (rng.nextDouble() * h * 1.6 - h * 0.3).toFloat()
        val r  = (0.2 + rng.nextDouble() * 0.45).toFloat() * w
        val shapeColor = avatarColor(rng)
        val alpha = (0.22 + rng.nextDouble() * 0.38).toFloat()
        drawCircle(
            color = shapeColor.copy(alpha = alpha),
            radius = r,
            center = Offset(cx, cy)
        )
    }
}
