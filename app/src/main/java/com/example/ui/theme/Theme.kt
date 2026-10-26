package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private fun buildCustomColorScheme(accentHex: Long, isDark: Boolean): ColorScheme {
    val accent = Color(accentHex)
    return if (isDark) {
        val softPrimary = Color(
            red = (accent.red * 0.65f + 0.35f).coerceIn(0f, 1f),
            green = (accent.green * 0.65f + 0.35f).coerceIn(0f, 1f),
            blue = (accent.blue * 0.65f + 0.35f).coerceIn(0f, 1f),
            alpha = 1f
        )
        val container = Color(
            red = (accent.red * 0.35f).coerceIn(0f, 1f),
            green = (accent.green * 0.35f).coerceIn(0f, 1f),
            blue = (accent.blue * 0.35f).coerceIn(0f, 1f),
            alpha = 1f
        )
        darkColorScheme(
            primary = softPrimary,
            onPrimary = if (softPrimary.luminance() > 0.45f) Color(0xFF090D16) else Color.White,
            primaryContainer = container,
            onPrimaryContainer = Color(0xFFE0E7FF),
            secondary = SecondaryTealDark,
            onSecondary = Color(0xFF042F2E),
            secondaryContainer = SecondaryContainerTealDark,
            onSecondaryContainer = OnSecondaryContainerTealDark,
            tertiary = TertiaryAmberDark,
            onTertiary = Color(0xFF451A03),
            tertiaryContainer = TertiaryContainerAmberDark,
            onTertiaryContainer = OnTertiaryContainerAmberDark,
            background = SurfaceDark,
            onBackground = OnSurfaceDark,
            surface = SurfaceDark,
            onSurface = OnSurfaceDark,
            surfaceVariant = SurfaceVariantDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceContainerHigh = Color(0xFF1E293B),
            surfaceContainerHighest = Color(0xFF334155)
        )
    } else {
        val container = Color(
            red = (accent.red * 0.15f + 0.85f).coerceIn(0f, 1f),
            green = (accent.green * 0.15f + 0.85f).coerceIn(0f, 1f),
            blue = (accent.blue * 0.15f + 0.85f).coerceIn(0f, 1f),
            alpha = 1f
        )
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = Color(0xFF1E1B4B),
            secondary = SecondaryTealLight,
            onSecondary = Color.White,
            secondaryContainer = SecondaryContainerTealLight,
            onSecondaryContainer = OnSecondaryContainerTealLight,
            tertiary = TertiaryAmberLight,
            onTertiary = Color.White,
            tertiaryContainer = TertiaryContainerAmberLight,
            onTertiaryContainer = OnTertiaryContainerAmberLight,
            background = SurfaceLight,
            onBackground = OnSurfaceLight,
            surface = SurfaceLight,
            onSurface = OnSurfaceLight,
            surfaceVariant = SurfaceVariantLight,
            onSurfaceVariant = OnSurfaceVariantLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = Color(0xFFF1F5F9),
            surfaceContainerHighest = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun ExamPulseTheme(
    themeMode: String = "auto",
    accentColorHex: Long = 0xFF4F46E5L,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> buildCustomColorScheme(accentColorHex, isDark)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ExamPulseTheme(
        themeMode = if (darkTheme) "dark" else "light",
        dynamicColor = dynamicColor,
        content = content
    )
}
