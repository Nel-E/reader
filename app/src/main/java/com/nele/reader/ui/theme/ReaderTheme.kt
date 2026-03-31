package com.nele.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val primaryTeal = Color(0xFF01696F)
private val primaryTealDark = Color(0xFF4F98A3)
private val bgLight = Color(0xFFF7F6F2)
private val bgDark = Color(0xFF171614)
private val surfaceLight = Color(0xFFF9F8F5)
private val surfaceDark = Color(0xFF1C1B19)

private val LightColors = lightColorScheme(
    primary = primaryTeal,
    onPrimary = Color.White,
    background = bgLight,
    onBackground = Color(0xFF28251D),
    surface = surfaceLight,
    onSurface = Color(0xFF28251D),
    surfaceVariant = Color(0xFFEDEAE5),
    onSurfaceVariant = Color(0xFF7A7974),
    outline = Color(0xFFD4D1CA),
)

private val DarkColors = darkColorScheme(
    primary = primaryTealDark,
    onPrimary = Color(0xFF171614),
    background = bgDark,
    onBackground = Color(0xFFCDCCCA),
    surface = surfaceDark,
    onSurface = Color(0xFFCDCCCA),
    surfaceVariant = Color(0xFF22211F),
    onSurfaceVariant = Color(0xFF797876),
    outline = Color(0xFF393836),
)

@Composable
fun ReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
