package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TawasulPrimaryDark,
    onPrimary = TawasulOnPrimaryDark,
    primaryContainer = TawasulPrimaryContainerDark,
    onPrimaryContainer = TawasulOnPrimaryContainerDark,
    secondary = TawasulSecondaryDark,
    onSecondary = TawasulOnSecondaryDark,
    secondaryContainer = TawasulSecondaryContainerDark,
    onSecondaryContainer = TawasulOnSecondaryContainerDark,
    background = TawasulBackgroundDark,
    onBackground = TawasulOnBackgroundDark,
    surface = TawasulSurfaceDark,
    onSurface = TawasulOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = TawasulPrimaryLight,
    onPrimary = TawasulOnPrimaryLight,
    primaryContainer = TawasulPrimaryContainerLight,
    onPrimaryContainer = TawasulOnPrimaryContainerLight,
    secondary = TawasulSecondaryLight,
    onSecondary = TawasulOnSecondaryLight,
    secondaryContainer = TawasulSecondaryContainerLight,
    onSecondaryContainer = TawasulOnSecondaryContainerLight,
    background = TawasulBackgroundLight,
    onBackground = TawasulOnBackgroundLight,
    surface = TawasulSurfaceLight,
    onSurface = TawasulOnSurfaceLight
)

@Composable
fun MyApplicationTheme(
    darkModeSetting: String = "AUTO", // "AUTO", "DARK", "LIGHT"
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkModeSetting) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
