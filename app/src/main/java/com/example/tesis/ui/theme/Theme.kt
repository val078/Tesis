// ui/theme/Theme.kt
package com.example.tesis.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema de colores claro con la nueva paleta naranja
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,      // Naranja principal
    onPrimary = BackgroundWhite,  // Texto blanco sobre naranja
    secondary = PinkOrange,       // Naranja rosado
    onSecondary = BackgroundWhite,// Texto blanco sobre naranja rosado
    tertiary = DarkOrange,        // Naranja oscuro
    onTertiary = BackgroundWhite, // Texto blanco sobre naranja oscuro
    background = BackgroundWhite, // Fondo blanco
    surface = BackgroundWhite,    // Superficie blanca
    onBackground = TextDark,      // Texto oscuro sobre fondo
    onSurface = TextDark,         // Texto oscuro sobre superficie
    error = CoralRed,             // Rojo coral para errores
    onError = BackgroundWhite,    // Texto blanco sobre error
)

// Esquema de colores oscuro (opcional)
private val DarkColorScheme = darkColorScheme(
    primary = LightOrange,        // Naranja claro para modo oscuro
    onPrimary = TextDark,         // Texto oscuro sobre naranja claro
    secondary = PinkOrange,       // Naranja rosado
    onSecondary = TextDark,       // Texto oscuro sobre naranja rosado
    tertiary = PrimaryOrange,     // Naranja principal
    onTertiary = BackgroundWhite, // Texto blanco sobre naranja
    background = Color(0xFF1C1B1F), // Fondo oscuro (Color directo)
    surface = Color(0xFF1C1B1F),    // Superficie oscura (Color directo)
    onBackground = BackgroundWhite, // Texto blanco sobre fondo oscuro
    onSurface = BackgroundWhite,    // Texto blanco sobre superficie
    error = CoralRed,               // Rojo coral para errores
    onError = BackgroundWhite,      // Texto blanco sobre error
)

@Composable
fun TesisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}