package com.hullcheck.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hullcheck.data.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = HullTeal,
    onPrimary = Color(0xFF00201F),
    primaryContainer = HullTealContainer,
    onPrimaryContainer = HullTealOnContainer,
    secondary = HullAmber,
    onSecondary = Color(0xFF2A1A00),
    secondaryContainer = Color(0xFF4A3410),
    onSecondaryContainer = Color(0xFFFFE2B0),
    tertiary = Color(0xFF7FB6CC),
    onTertiary = Color(0xFF00131D),
    background = HullBackground,
    onBackground = HullTextPrimary,
    surface = HullSurface,
    onSurface = HullTextPrimary,
    surfaceVariant = HullSurfaceElevated,
    onSurfaceVariant = HullTextSecondary,
    surfaceContainerLowest = HullBackground,
    surfaceContainerLow = HullSurface,
    surfaceContainer = HullSurfaceElevated,
    surfaceContainerHigh = HullSurfaceHigh,
    surfaceContainerHighest = HullSurfaceHighest,
    outline = HullOutline,
    outlineVariant = HullOutlineVariant,
    error = StatusDefect,
    onError = Color(0xFF3A0A0A),
    errorContainer = Color(0xFF5A1A1A),
    onErrorContainer = Color(0xFFFFD9D9),
    scrim = Color(0xFF000000),
)

private val LightColors = lightColorScheme(
    primary = HullLightPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = HullLightPrimaryContainer,
    onPrimaryContainer = HullLightOnPrimaryContainer,
    secondary = Color(0xFFB06D00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE0B0),
    onSecondaryContainer = Color(0xFF3A2600),
    tertiary = Color(0xFF2E6E86),
    onTertiary = Color(0xFFFFFFFF),
    background = HullLightBackground,
    onBackground = HullLightTextPrimary,
    surface = HullLightSurface,
    onSurface = HullLightTextPrimary,
    surfaceVariant = HullLightSurfaceVariant,
    onSurfaceVariant = HullLightTextSecondary,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FB),
    surfaceContainer = Color(0xFFEFF3F6),
    surfaceContainerHigh = Color(0xFFE8EEF2),
    surfaceContainerHighest = Color(0xFFE1E8ED),
    outline = HullLightOutline,
    outlineVariant = HullLightOutlineVariant,
    error = Color(0xFFC0322F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF410005),
)

@Composable
fun HullCheckTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HullTypography,
        content = content,
    )
}
