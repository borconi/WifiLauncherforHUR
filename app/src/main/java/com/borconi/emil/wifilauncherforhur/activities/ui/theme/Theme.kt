package com.borconi.emil.wifilauncherforhur.activities.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AaGreen,
    onPrimary = AaSlate,
    primaryContainer = AaGreenContainer,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE8FFF6),
    secondary = AaTeal,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF163B3A),
    background = AaSlate,
    surface = AaSurface,
    surfaceVariant = AaSurfaceVariant,
    outline = AaOutline,
)

@Composable
fun WifiLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}
