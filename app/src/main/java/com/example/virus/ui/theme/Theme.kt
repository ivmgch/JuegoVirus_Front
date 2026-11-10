package com.example.virus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema de colores enfocado en tonos de morado y beige (sin fondos oscuros)
private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = PurpleSoftTint,
    onPrimaryContainer = PurpleDeep,
    secondary = BeigeDeep,
    onSecondary = Color.White,
    secondaryContainer = BeigeLight,
    onSecondaryContainer = PurpleDeep,
    tertiary = PurpleMedium,
    onTertiary = Color.White,
    tertiaryContainer = PurpleSoftTint,
    onTertiaryContainer = PurpleDeep,
    background = BeigeBackground,
    onBackground = TextPrimary,
    surface = BeigeSurface,
    onSurface = TextPrimary,
    surfaceVariant = BeigeSurfaceAlt,
    onSurfaceVariant = TextSecondary,
    outline = BorderBeige,
    outlineVariant = BorderLavender
)

@Composable
fun VirusTheme(
    // Por petición del usuario ("sin fondos oscuros"), se prioriza el esquema claro en morado y beige
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Íconos oscuros en barra de estado y navegación sobre fondo beige claro
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
