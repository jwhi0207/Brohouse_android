package com.bennybokki.frientrip.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary             = Blue40,
    onPrimary           = OnBlue40,
    primaryContainer    = BlueContainer,
    onPrimaryContainer  = OnBlueContainer,

    secondary           = BlueGrey40,
    onSecondary         = OnBlueGrey40,
    secondaryContainer  = BlueGreyContainer,
    onSecondaryContainer = OnBlueGreyContainer,

    tertiary            = Teal40,
    onTertiary          = OnTeal40,
    tertiaryContainer   = TealContainer,
    onTertiaryContainer = OnTealContainer,

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
    inversePrimary      = Blue80,
)

private val DarkColorScheme = darkColorScheme(
    primary             = Blue80,
    onPrimary           = OnBlue80,
    primaryContainer    = BlueDarkContainer,
    onPrimaryContainer  = OnBlueDarkContainer,

    secondary           = BlueGrey80,
    onSecondary         = OnBlueGrey80,
    secondaryContainer  = BlueGreyDarkContainer,
    onSecondaryContainer = OnBlueGreyDarkContainer,

    tertiary            = Teal80,
    onTertiary          = OnTeal80,
    tertiaryContainer   = TealDarkContainer,
    onTertiaryContainer = OnTealDarkContainer,

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
    inversePrimary      = Blue40,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
