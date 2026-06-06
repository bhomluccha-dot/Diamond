package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CustomGamerColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = ThemeBackground,
    secondary = SecondaryGold,
    onSecondary = ThemeBackground,
    tertiary = AccentTeal,
    onTertiary = ThemeBackground,
    background = ThemeBackground,
    onBackground = TextLight,
    surface = ThemeSurface,
    onSurface = TextLight,
    surfaceVariant = ThemeSurfaceVariant,
    onSurfaceVariant = TextMuted,
    error = RedError,
    onError = TextLight
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CustomGamerColorScheme,
        typography = Typography,
        content = content
    )
}
