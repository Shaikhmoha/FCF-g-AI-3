package com.fcf.videoconverter.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val FcfPurplePrimary = Color(0xFF7C3AED)
val FcfPurpleDeep = Color(0xFF5B21B6)
val FcfPurpleLight = Color(0xFFA78BFA)

private val FcfLightColors = lightColorScheme(
    primary = FcfPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626)
)

private val FcfDarkColors = darkColorScheme(
    primary = FcfPurpleLight,
    onPrimary = Color(0xFF1E1038),
    primaryContainer = Color(0xFF3B1F75),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),
    background = Color(0xFF090B12),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111522),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1B2032),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF282E42),
    error = Color(0xFFF87171)
)

private val FcfTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun FcfTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FcfDarkColors else FcfLightColors,
        typography = FcfTypography,
        content = content
    )
}
