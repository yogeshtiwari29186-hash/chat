package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MeshTealPrimary,
    onPrimary = Color.Black,
    primaryContainer = MeshDarkBubbleOutgoing,
    onPrimaryContainer = Color.White,
    secondary = MeshPulseAccent,
    onSecondary = Color.Black,
    secondaryContainer = MeshDarkSurfaceVariant,
    onSecondaryContainer = MeshTextPrimaryDark,
    background = MeshDarkBackground,
    onBackground = MeshTextPrimaryDark,
    surface = MeshDarkSurface,
    onSurface = MeshTextPrimaryDark,
    surfaceVariant = MeshDarkSurfaceVariant,
    onSurfaceVariant = MeshTextSecondaryDark,
    error = StatusFailedRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MeshTealDark,
    onPrimary = Color.White,
    primaryContainer = MeshLightBubbleOutgoing,
    onPrimaryContainer = MeshTextPrimaryLight,
    secondary = MeshTealPrimary,
    onSecondary = Color.White,
    secondaryContainer = MeshLightSurfaceVariant,
    onSecondaryContainer = MeshTextPrimaryLight,
    background = MeshLightBackground,
    onBackground = MeshTextPrimaryLight,
    surface = MeshLightSurface,
    onSurface = MeshTextPrimaryLight,
    surfaceVariant = MeshLightSurfaceVariant,
    onSurfaceVariant = MeshTextSecondaryLight,
    error = StatusFailedRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent messaging palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
