package com.example.gastofacil.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TopBarHeaderGreen,
    onPrimary = Color.White,
    primaryContainer = SageCardBackground,
    onPrimaryContainer = DarkForestGreen,
    secondary = WarmGoldFab,
    onSecondary = WarmGoldOnFab,
    secondaryContainer = WarmGoldFab,
    onSecondaryContainer = WarmGoldOnFab,
    tertiary = IncomeGreenText,
    onTertiary = Color.White,
    background = SageBackground,
    onBackground = TextPrimary,
    surface = SageBackground,
    onSurface = TextPrimary,
    surfaceVariant = SageCardBackground,
    onSurfaceVariant = DarkForestGreen,
    error = ExpenseRedText,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = ExpenseRedText,
)

private val DarkColorScheme = darkColorScheme(
    primary = TopBarHeaderGreen,
    onPrimary = Color.White,
    primaryContainer = DarkForestGreen,
    onPrimaryContainer = Color.White,
    secondary = WarmGoldFab,
    onSecondary = WarmGoldOnFab,
    secondaryContainer = WarmGoldFab,
    onSecondaryContainer = WarmGoldOnFab,
    tertiary = IncomeGreenText,
    background = Color(0xFF0F231B),
    onBackground = Color(0xFFE8F2EE),
    surface = Color(0xFF142E24),
    onSurface = Color(0xFFE8F2EE),
    surfaceVariant = DarkForestGreen,
    onSurfaceVariant = Color(0xFFE8F2EE),
    error = Color(0xFFE57373),
    onError = Color.Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

@Composable
fun GastoFacilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
