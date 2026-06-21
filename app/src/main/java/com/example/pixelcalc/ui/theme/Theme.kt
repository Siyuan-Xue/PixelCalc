package com.example.pixelcalc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PixelColorScheme = darkColorScheme(
    primary = PixelClay,
    onPrimary = PixelInk,
    secondary = PixelSage,
    onSecondary = PixelInk,
    tertiary = PixelAmber,
    onTertiary = PixelInk,
    background = PixelInk,
    onBackground = PixelPaper,
    surface = PixelButtonSurface,
    onSurface = PixelPaper,
    surfaceVariant = PixelButtonAlt,
    onSurfaceVariant = PixelPaper,
    error = PixelError,
    onError = PixelInk,
    outline = PixelLine,
)

@Composable
fun PixelCalcTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = Typography,
        content = content
    )
}
