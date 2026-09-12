package com.roleta.app.ui.theme

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
    primary = Amber80,
    onPrimary = Amber20,
    primaryContainer = Amber30,
    onPrimaryContainer = Amber90,
    secondary = AmberGrey80,
    onSecondary = Color(0xFF402D17),
    secondaryContainer = Color(0xFF59432B),
    onSecondaryContainer = AmberGrey90,
    tertiary = Olive80,
    onTertiary = Color(0xFF2E3408),
    tertiaryContainer = Color(0xFF43491D),
    onTertiaryContainer = Color(0xFFDFE7AE),
)

private val LightColorScheme = lightColorScheme(
    primary = Amber40,
    onPrimary = Color.White,
    primaryContainer = Amber90,
    onPrimaryContainer = Amber10,
    secondary = AmberGrey40,
    onSecondary = Color.White,
    secondaryContainer = AmberGrey90,
    onSecondaryContainer = Color(0xFF291806),
    tertiary = Olive40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFE7AE),
    onTertiaryContainer = Color(0xFF1D2000),
)

@Composable
fun RoletaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
