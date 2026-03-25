package com.bennybokki.frientrip.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** True when the app is rendering in dark mode. Read via `LocalIsDarkTheme.current`. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary             = Color(0xFF0090C0),
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFD6F2FF),
    onPrimaryContainer  = Color(0xFF001D2B),

    secondary           = Color(0xFFAA00BB),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFF5D0FF),
    onSecondaryContainer = Color(0xFF2E0036),

    tertiary            = Color(0xFFD41654),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFFFD9E4),
    onTertiaryContainer = Color(0xFF3F001D),

    background          = LightBackground,
    onBackground        = LightOnSurface,
    surface             = LightSurface,
    onSurface           = LightOnSurface,
    surfaceVariant      = LightSurfaceVariant,
    onSurfaceVariant    = LightOnSurfaceVar,
    outline             = LightOutline,
    outlineVariant      = LightOutlineVariant,

    error               = LightError,
    onError             = OnLightError,
    errorContainer      = LightErrorContainer,
    onErrorContainer    = OnLightErrorContainer,

    inverseSurface      = Color(0xFF2F3033),
    inverseOnSurface    = Color(0xFFF1F0F4),
    inversePrimary      = ElectricCyan,
)

private val DarkColorScheme = darkColorScheme(
    primary             = ElectricCyan,
    onPrimary           = Color(0xFF003544),
    primaryContainer    = Color(0xFF004D65),
    onPrimaryContainer  = Color(0xFFB8EAFF),

    secondary           = NeonPurple,
    onSecondary         = Color(0xFF3F0044),
    secondaryContainer  = Color(0xFF5A0062),
    onSecondaryContainer = Color(0xFFF5D0FF),

    tertiary            = VividPink,
    onTertiary          = Color(0xFF44001D),
    tertiaryContainer   = Color(0xFF66002E),
    onTertiaryContainer = Color(0xFFFFD9E4),

    background          = DarkBackground,
    onBackground        = DarkOnSurface,
    surface             = DarkSurface,
    onSurface           = DarkOnSurface,
    surfaceVariant      = DarkSurfaceVariant,
    onSurfaceVariant    = DarkOnSurfaceVar,
    outline             = DarkOutline,
    outlineVariant      = DarkOutlineVariant,

    error               = DarkError,
    onError             = OnDarkError,
    errorContainer      = DarkErrorContainer,
    onErrorContainer    = OnDarkErrorContainer,

    inverseSurface      = DarkOnSurface,
    inverseOnSurface    = DarkSurface,
    inversePrimary      = Color(0xFF0090C0),
)

@Composable
fun FrientripTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION") // statusBarColor deprecated in API 35; edge-to-edge migration pending
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
