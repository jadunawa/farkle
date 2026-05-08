package com.orangezest.farkle.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = OrangeZestPrimary,
    onPrimary = OrangeZestOnPrimary,
    primaryContainer = OrangeZestPrimaryContainer,
    secondary = OrangeZestSecondary,
    background = OrangeZestBackground,
    surface = OrangeZestSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = OrangeZestPrimaryDark,
    onPrimary = OrangeZestOnPrimaryDark,
    primaryContainer = OrangeZestPrimaryContainerDark,
    secondary = OrangeZestSecondaryDark,
    background = OrangeZestBackgroundDark,
    surface = OrangeZestSurfaceDark,
)

@Composable
fun FarkleTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FarkleTypography,
        content = content,
    )
}
