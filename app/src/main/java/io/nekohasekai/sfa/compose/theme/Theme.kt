package io.nekohasekai.sfa.compose.theme

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

// InVPN minimal — light: near-white #F7F7F5 ground, ink #16161A, periwinkle accent.
private val LightColorScheme =
    lightColorScheme(
        primary = MinAccentLight,
        onPrimary = MinAccentInkLight,
        primaryContainer = MinFaintLight,
        onPrimaryContainer = MinInkLight,
        inversePrimary = MinAccentDark,
        secondary = MinInkLight,
        onSecondary = MinBgLight,
        secondaryContainer = MinFaintLight,
        onSecondaryContainer = MinInkLight,
        tertiary = MinAccentLight,
        onTertiary = MinAccentInkLight,
        tertiaryContainer = MinFaintLight,
        onTertiaryContainer = MinInkLight,
        background = MinBgLight,
        onBackground = MinInkLight,
        surface = MinBgLight,
        onSurface = MinInkLight,
        surfaceVariant = MinFaintLight,
        onSurfaceVariant = MinSubLight,
        surfaceTint = Color.Transparent,
        inverseSurface = MinInkLight,
        inverseOnSurface = MinBgLight,
        surfaceBright = MinBgLight,
        surfaceDim = Color(0xFFEDEDEA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = MinBgLight,
        surfaceContainer = Color(0xFFF2F2EF),
        surfaceContainerHigh = Color(0xFFEDEDEA),
        surfaceContainerHighest = Color(0xFFE8E8E4),
        outline = MinLineLight,
        outlineVariant = MinFaintLight,
        error = ErrorRed,
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDE),
        onErrorContainer = Color(0xFF6E1212),
        scrim = Color(0xCC000000),
    )

// InVPN minimal — dark: near-black #0E0E10 ground, off-white ink, lighter periwinkle.
private val DarkColorScheme =
    darkColorScheme(
        primary = MinAccentDark,
        onPrimary = MinAccentInkDark,
        primaryContainer = MinFaintDark,
        onPrimaryContainer = MinInkDark,
        inversePrimary = MinAccentLight,
        secondary = MinInkDark,
        onSecondary = MinBgDark,
        secondaryContainer = MinFaintDark,
        onSecondaryContainer = MinInkDark,
        tertiary = MinAccentDark,
        onTertiary = MinAccentInkDark,
        tertiaryContainer = MinFaintDark,
        onTertiaryContainer = MinInkDark,
        background = MinBgDark,
        onBackground = MinInkDark,
        surface = MinBgDark,
        onSurface = MinInkDark,
        surfaceVariant = MinFaintDark,
        onSurfaceVariant = MinSubDark,
        surfaceTint = Color.Transparent,
        inverseSurface = MinInkDark,
        inverseOnSurface = MinBgDark,
        surfaceBright = Color(0xFF26262A),
        surfaceDim = MinBgDark,
        surfaceContainerLowest = Color(0xFF090909),
        surfaceContainerLow = Color(0xFF131316),
        surfaceContainer = Color(0xFF161619),
        surfaceContainerHigh = Color(0xFF1E1E22),
        surfaceContainerHighest = Color(0xFF26262A),
        outline = MinLineDark,
        outlineVariant = MinFaintDark,
        error = Color(0xFFE5808A),
        onError = Color(0xFF1A0A0C),
        errorContainer = Color(0xFF5A2326),
        onErrorContainer = Color(0xFFF6D6D8),
        scrim = Color(0xCC000000),
    )

@Composable
fun SFATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // InVPN uses its own fixed minimal palette — dynamic (Material You) disabled on purpose.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
