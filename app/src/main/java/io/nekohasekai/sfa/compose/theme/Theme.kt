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

// InVPN light scheme — marble background, Aegean blue primary, antique gold secondary.
private val LightColorScheme =
    lightColorScheme(
        primary = AegeanBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD7E4EF),
        onPrimaryContainer = AegeanBlueDark,
        secondary = AntiqueGold,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEFE2C6),
        onSecondaryContainer = Color(0xFF4A3A14),
        tertiary = AegeanBlueLight,
        background = MarbleBg,
        onBackground = InkText,
        surface = MarbleSurface,
        onSurface = InkText,
        surfaceVariant = Color(0xFFE9E3D6),
        onSurfaceVariant = InkMuted,
        outline = Color(0xFFC7BFAD),
        outlineVariant = Color(0xFFDED7C7),
        error = ErrorRed,
        onError = Color.White,
    )

// InVPN dark scheme — classical charcoal with blue/gold accents.
private val DarkColorScheme =
    darkColorScheme(
        primary = AegeanBlueLight,
        onPrimary = Color(0xFF071520),
        secondary = AntiqueGoldLight,
        onSecondary = Color(0xFF241A06),
        tertiary = AegeanBlueLight,
        background = Color(0xFF14161A),
        onBackground = Color(0xFFE8E3D7),
        surface = Color(0xFF1C1F24),
        onSurface = Color(0xFFE8E3D7),
        surfaceVariant = Color(0xFF2A2E35),
        onSurfaceVariant = Color(0xFFBEB8A9),
        outline = Color(0xFF4B4F57),
        error = Color(0xFFE0A0A0),
    )

@Composable
fun SFATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // InVPN uses its own fixed palette — dynamic (Material You) disabled on purpose.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
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
