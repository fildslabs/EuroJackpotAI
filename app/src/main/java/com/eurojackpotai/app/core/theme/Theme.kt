package com.eurojackpotai.app.core.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Navy30,
    onPrimary = Navy95,
    secondary = Gold60,
    onSecondary = Navy10,
    tertiary = ColdBlue,
    background = Navy95,
    onBackground = NeutralGrey10,
    surface = WhiteSurface,
    onSurface = NeutralGrey10,
    surfaceVariant = NeutralGrey90,
    onSurfaceVariant = NeutralGrey40,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Navy80,
    onPrimary = Navy10,
    secondary = Gold80,
    onSecondary = Navy10,
    tertiary = ColdBlue,
    background = Navy10,
    onBackground = Navy95,
    surface = Navy20,
    onSurface = Navy95,
    surfaceVariant = Navy30,
    onSurfaceVariant = Navy80,
    error = ErrorRedDark
)

/**
 * Root theme for the app. Dynamic color (Material You) is used on Android 12+ when
 * available so the app still feels native, but always falls back to the fixed brand
 * palette above — dynamic color must never break the hot/cold red-blue semantics used
 * elsewhere in the app.
 */
@Composable
fun EuroJackpotAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalView.current.context)
            else dynamicLightColorScheme(LocalView.current.context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
