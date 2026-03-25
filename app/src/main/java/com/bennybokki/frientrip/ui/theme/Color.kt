package com.bennybokki.frientrip.ui.theme

import androidx.compose.ui.graphics.Color

// ── Vivid Pulse accents ─────────────────────────────────────────────────────
val NeonPurple    = Color(0xFFCE00E0)
val VividPink     = Color(0xFFFF206E)
val ElectricLime  = Color(0xFFE1FF00)
val NeonGreen     = Color(0xFF0EFB22)
val ElectricCyan  = Color(0xFF00BBF9)
val MintGreen     = Color(0xFF00F5D4)

/** Ordered list for cycling card borders and other multi-accent uses. */
val VividAccents = listOf(ElectricCyan, VividPink, NeonGreen, NeonPurple, ElectricLime, MintGreen)

// ── Semantic status colors (dark / light pairs) ─────────────────────────────
val StatusPaidDark       = NeonGreen
val StatusPaidBgDark     = Color(0xFF0A2E0D)
val StatusPaidLight      = Color(0xFF059212)
val StatusPaidBgLight    = Color(0xFFE8FBE9)

val StatusDueDark        = VividPink
val StatusDueBgDark      = Color(0xFF2E0A18)
val StatusDueLight       = Color(0xFFD41654)
val StatusDueBgLight     = Color(0xFFFFE8EF)

val StatusPendingDark    = ElectricCyan
val StatusPendingBgDark  = Color(0xFF0A1E2E)
val StatusPendingLight   = Color(0xFF0090C0)
val StatusPendingBgLight = Color(0xFFE3F6FD)

// ── Light scheme ─────────────────────────────────────────────────────────────
val Blue40          = Color(0xFF0061A4)
val BlueContainer   = Color(0xFFD1E4FF)
val OnBlue40        = Color(0xFFFFFFFF)
val OnBlueContainer = Color(0xFF001D36)

val BlueGrey40          = Color(0xFF535F70)
val BlueGreyContainer   = Color(0xFFD7E3F7)
val OnBlueGrey40        = Color(0xFFFFFFFF)
val OnBlueGreyContainer = Color(0xFF101C2B)

val Teal40          = Color(0xFF006874)
val TealContainer   = Color(0xFF97F0FF)
val OnTeal40        = Color(0xFFFFFFFF)
val OnTealContainer = Color(0xFF001F24)

val LightSurface        = Color(0xFFFFFFFF)
val LightBackground     = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF8FAFC)
val LightOnSurface      = Color(0xFF0F172A)
val LightOnSurfaceVar   = Color(0xFF475569)
val LightOutline        = Color(0xFF94A3B8)
val LightOutlineVariant = Color(0xFFE2E8F0)

val LightError          = Color(0xFFBA1A1A)
val LightErrorContainer = Color(0xFFFFDAD6)
val OnLightError        = Color(0xFFFFFFFF)
val OnLightErrorContainer = Color(0xFF410002)

// ── Dark scheme ───────────────────────────────────────────────────────────────
val Blue80          = Color(0xFF9ECAFF)
val BlueDarkContainer   = Color(0xFF004A77)
val OnBlue80        = Color(0xFF003258)
val OnBlueDarkContainer = Color(0xFFD1E4FF)

val BlueGrey80          = Color(0xFFBBC8DB)
val BlueGreyDarkContainer   = Color(0xFF3C4858)
val OnBlueGrey80        = Color(0xFF253140)
val OnBlueGreyDarkContainer = Color(0xFFD7E3F7)

val Teal80          = Color(0xFF4FD8EB)
val TealDarkContainer   = Color(0xFF004F58)
val OnTeal80        = Color(0xFF00363D)
val OnTealDarkContainer = Color(0xFF97F0FF)

val DarkSurface        = Color(0xFF05070A)
val DarkBackground     = Color(0xFF05070A)
val DarkCardSurface    = Color(0xFF121826)
val DarkSurfaceVariant = Color(0xFF121826)
val DarkOnSurface      = Color(0xFFE2E8F0)
val DarkOnSurfaceVar   = Color(0xFFA0AEC0)
val DarkOutline        = Color(0xFF4A5568)
val DarkOutlineVariant = Color(0xFF2D3748)

val DarkError          = Color(0xFFFFB4AB)
val DarkErrorContainer = Color(0xFF93000A)
val OnDarkError        = Color(0xFF690005)
val OnDarkErrorContainer = Color(0xFFFFDAD6)
