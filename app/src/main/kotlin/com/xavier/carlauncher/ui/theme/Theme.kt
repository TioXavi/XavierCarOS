package com.xavier.carlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Dynamic Theme Accessors (Reactively bind to current active theme) ──────────
val ColorPrimary: Color
    @Composable get() = XavierTheme.colors.primary

val ColorPrimaryVariant: Color
    @Composable get() = XavierTheme.colors.primaryVariant

val ColorSecondary: Color
    @Composable get() = XavierTheme.colors.secondary

val ColorBackground: Color
    @Composable get() = XavierTheme.colors.background

val ColorSurface: Color
    @Composable get() = XavierTheme.colors.surface

val ColorSurfaceDeep: Color
    @Composable get() = XavierTheme.colors.surfaceDeep

val ColorOnPrimary: Color
    @Composable get() = XavierTheme.colors.textPrimary

val ColorOnBackground: Color
    @Composable get() = XavierTheme.colors.textPrimary

val ColorOnSurface: Color
    @Composable get() = XavierTheme.colors.textPrimary

val ColorGrayMid: Color
    @Composable get() = XavierTheme.colors.textSecondary

val ColorGrayDark: Color
    @Composable get() = XavierTheme.colors.textTertiary

val ColorMusicGradientStart: Color
    @Composable get() = XavierTheme.colors.musicGradientStart

val ColorMusicGradientEnd: Color
    @Composable get() = XavierTheme.colors.musicGradientEnd

val ColorBorder: Color
    @Composable get() = XavierTheme.colors.border

val ColorNeonBlue: Color
    @Composable get() = XavierTheme.colors.secondary

val ColorAccentGreen = Color(0xFF22C55E)

// ── Typography ────────────────────────────────────────────────────────────────
val XavierTypography = Typography(
    displayLarge = TextStyle(
        fontSize   = 48.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontSize   = 40.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontSize   = 32.sp,
        fontWeight = FontWeight.Bold,
        color      = Color.White
    ),
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
        color      = Color(0xFFB0BEC5)
    ),
    bodyLarge = TextStyle(
        fontSize   = 16.sp,
        fontWeight = FontWeight.Normal,
        color      = Color.White
    ),
    bodyMedium = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Normal,
        color      = Color(0xFFB0BEC5)
    ),
    bodySmall = TextStyle(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Normal,
        color      = Color(0xFF607D8B)
    ),
    labelLarge = TextStyle(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Medium,
        color      = Color.White
    ),
    labelMedium = TextStyle(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium,
        color      = Color(0xFFB0BEC5)
    ),
    labelSmall = TextStyle(
        fontSize   = 10.sp,
        fontWeight = FontWeight.Normal,
        color      = Color(0xFF607D8B)
    ),
)

// ── Theme composable ──────────────────────────────────────────────────────────
@Composable
fun XavierCarOSTheme(
    theme: AppTheme = ThemeManager.currentTheme.collectAsState().value,
    content: @Composable () -> Unit
) {
    val themeColors = ThemeManager.getColors(theme)

    val colorScheme = if (themeColors.isDark) {
        darkColorScheme(
            primary            = themeColors.primary,
            onPrimary          = themeColors.textPrimary,
            primaryContainer   = themeColors.primaryVariant,
            onPrimaryContainer = themeColors.textPrimary,
            secondary          = themeColors.secondary,
            onSecondary        = Color.Black,
            background         = themeColors.background,
            onBackground       = themeColors.textPrimary,
            surface            = themeColors.surface,
            onSurface          = themeColors.textPrimary,
            surfaceVariant     = themeColors.surfaceDeep,
            onSurfaceVariant   = themeColors.textSecondary,
            outline            = themeColors.border,
            error              = Color(0xFFEF4444),
            onError            = Color.White,
        )
    } else {
        lightColorScheme(
            primary            = themeColors.primary,
            onPrimary          = Color.White,
            primaryContainer   = themeColors.primaryVariant,
            onPrimaryContainer = themeColors.primary,
            secondary          = themeColors.secondary,
            onSecondary        = Color.White,
            background         = themeColors.background,
            onBackground       = themeColors.textPrimary,
            surface            = themeColors.surface,
            onSurface          = themeColors.textPrimary,
            surfaceVariant     = themeColors.surfaceDeep,
            onSurfaceVariant   = themeColors.textSecondary,
            outline            = themeColors.border,
            error              = Color(0xFFEF4444),
            onError            = Color.White,
        )
    }

    CompositionLocalProvider(LocalCarThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = XavierTypography,
            content     = content
        )
    }
}
