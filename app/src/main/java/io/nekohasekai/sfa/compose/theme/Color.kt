package io.nekohasekai.sfa.compose.theme

import androidx.compose.ui.graphics.Color

// =====================================================================================
// InVPN — minimal monochrome palette (single periwinkle accent), light + dark.
// Replaces the former Greco-Roman palette. Old symbol names are kept and re-valued so
// existing screens keep compiling; new UI should prefer MaterialTheme.colorScheme.
// =====================================================================================

// -- New minimal tokens -------------------------------------------------------
// Light
val MinBgLight = Color(0xFFF7F7F5)
val MinInkLight = Color(0xFF16161A)
val MinSubLight = Color(0x7A16161A) // ink @ ~48%
val MinLineLight = Color(0x1F16161A) // ink @ ~12%
val MinFaintLight = Color(0x1416161A) // ink @ ~8%
val MinAccentLight = Color(0xFF8F91DD)
val MinAccentInkLight = Color(0xFFFFFFFF) // text/icon ON accent
// Dark
val MinBgDark = Color(0xFF0E0E10)
val MinInkDark = Color(0xFFF3F3F1)
val MinSubDark = Color(0x80F3F3F1) // ink @ ~50%
val MinLineDark = Color(0x21F3F3F1) // ink @ ~13%
val MinFaintDark = Color(0x1AF3F3F1) // ink @ ~10%
val MinAccentDark = Color(0xFFAEB0F2)
val MinAccentInkDark = Color(0xFF0E0E10)

// -- Back-compat names (re-valued to the minimal palette) ---------------------
val AegeanBlue = MinAccentLight
val AegeanBlueDark = Color(0xFF6B6DC4)
val AegeanBlueLight = MinAccentDark
val AntiqueGold = MinAccentLight
val AntiqueGoldLight = MinAccentDark
val MarbleBg = MinBgLight
val MarbleSurface = MinBgLight
val InkText = MinInkLight
val InkMuted = MinSubLight

val SingBoxPrimary = MinAccentLight
val SingBoxPrimaryDark = Color(0xFF6B6DC4)
val SingBoxPrimaryLight = MinAccentDark

// -- Functional colors (service status + log viewer + semantic) ---------------
val ServiceRunning = MinAccentLight
val ServiceStopped = Color(0xFF9E9E9E)
val ServiceError = Color(0xFFE5484D)

val LogRed = Color(0xFFFF2158)
val LogGreen = Color(0xFF2ECC71)
val LogYellow = Color(0xFFE5C500)
val LogBlue = Color(0xFF3498DB)
val LogPurple = Color(0xFF8F91DD)
val LogRedLight = Color(0xFFE91E63)
val LogBlueLight = Color(0xFF00A6B2)
val LogWhite = Color(0xFFECECEC)

val SeedColor = MinAccentLight

val SuccessGreen = Color(0xFF30A46C)
val WarningOrange = Color(0xFFFFB224)
val ErrorRed = Color(0xFFE5484D)
val InfoBlue = Color(0xFF3498DB)
