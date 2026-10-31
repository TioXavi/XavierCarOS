package com.xavier.carlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Brand Colors ──────────────────────────────────────────────────────────────
val ColorPrimary            = Color(0xFF1D4ED8)
val ColorPrimaryVariant     = Color(0xFF1E3A8A)
val ColorSecondary          = Color(0xFF60A5FA)

val ColorBackground         = Color(0xFF060B19)
val ColorSurface            = Color(0xFF0D1B3A)
val ColorSurfaceDeep        = Color(0xFF091326)

val ColorOnPrimary          = Color.White
val ColorOnBackground       = Color.White
val ColorOnSurface          = Color.White

val ColorGrayMid            = Color(0xFFB0BEC5)
val ColorGrayDark           = Color(0xFF607D8B)

val ColorMusicGradientStart = Color(0xFF3B0764)
val ColorMusicGradientEnd   = Color(0xFF1E1B4B)

val ColorBorder             = Color(0xFF1E3A8A)
val ColorNeonBlue           = Color(0xFF60A5FA)
val ColorAccentGreen        = Color(0xFF22C55E)

// ── Dark color scheme ─────────────────────────────────────────────────────────
private val XavierDarkColorScheme = darkColorScheme(
    primary            = ColorPrimary,
    onPrimary          = ColorOnPrimary,
    primaryContainer   = ColorPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary          = ColorSecondary,
    onSecondary        = Color.Black,
    background         = ColorBackground,
    onBackground       = ColorOnBackground,
    surface            = ColorSurface,
    onSurface          = ColorOnSurface,
    surfaceVariant     = ColorSurfaceDeep,
    onSurfaceVariant   = ColorGrayMid,
    outline            = ColorBorder,
    error              = Color(0xFFEF4444),
    onError            = Color.White,
)

// ── Typography ────────────────────────────────────────────────────────────────
val XavierTypography = Typography(
    // Huge clock on status bar
    displayLarge = TextStyle(
        fontSize   = 48.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White,
        letterSpacing = 0.sp
    ),
    // Speedometer / large number
    displayMedium = TextStyle(
        fontSize   = 40.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White,
        letterSpacing = (-0.5).sp
    ),
    // Section headings inside cards
    displaySmall = TextStyle(
        fontSize   = 32.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White
    ),
    // Greeting headline
    headlineLarge = TextStyle(
        fontSize   = 28.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White
    ),
    headlineMedium = TextStyle(
        fontSize   = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color      = Color.White
    ),
    headlineSmall = TextStyle(
        fontSize   = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color      = Color.White
    ),
    // Card titles
    titleLarge = TextStyle(
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White
    ),
    titleMedium = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Medium,
        color      = Color.White
    ),
    titleSmall = TextStyle(
        fontSize   = 13.sp,
        fontWeight = FontWeight.Medium,
        color      = ColorGrayMid
    ),
    // Body text
    bodyLarge = TextStyle(
        fontSize   = 16.sp,
        fontWeight = FontWeight.Normal,
        color      = Color.White
    ),
    bodyMedium = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Normal,
        color      = ColorGrayMid
    ),
    bodySmall = TextStyle(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Normal,
        color      = ColorGrayDark
    ),
    // Labels
    labelLarge = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Medium,
        color      = Color.White
    ),
    labelMedium = TextStyle(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium,
        color      = ColorGrayMid
    ),
    labelSmall = TextStyle(
        fontSize   = 10.sp,
        fontWeight = FontWeight.Normal,
        color      = ColorGrayDark
    ),
)

// ── Theme composable ──────────────────────────────────────────────────────────
@Composable
fun XavierCarOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XavierDarkColorScheme,
        typography  = XavierTypography,
        content     = content
    )
}
