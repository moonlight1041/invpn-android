package io.nekohasekai.sfa.compose.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.nekohasekai.sfa.R

// Cinzel (OFL) — classical Roman-capital display face for InVPN titles/headlines.
val Cinzel =
    FontFamily(
        Font(R.font.cinzel, FontWeight.Normal),
        Font(R.font.cinzel, FontWeight.Medium),
        Font(R.font.cinzel, FontWeight.SemiBold),
        Font(R.font.cinzel, FontWeight.Bold),
    )

// Cinzel on display/headline/title-large (the classical voice); readable sans for body/labels.
val Typography =
    Typography(
        displayLarge = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 60.sp, letterSpacing = 1.sp),
        displayMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 42.sp, lineHeight = 50.sp, letterSpacing = 1.sp),
        displaySmall = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 42.sp, letterSpacing = 0.5.sp),
        headlineLarge = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = 0.5.sp),
        headlineMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 0.5.sp),
        headlineSmall = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = 0.5.sp),
        titleLarge = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.5.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
