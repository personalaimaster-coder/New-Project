package com.example.petmeds.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary            = Brand.DarkBlue,
    onPrimary          = Brand.DarkBlueOn,
    primaryContainer   = Brand.Lavender,
    onPrimaryContainer = Brand.LavenderOn,
    secondary          = Brand.Slate700,
    onSecondary        = Brand.Canvas,
    secondaryContainer = Brand.Slate100,
    onSecondaryContainer = Brand.Slate900,
    tertiary           = Brand.Coral,
    onTertiary         = Brand.CoralOn,
    tertiaryContainer  = Color(0xFFFFE0D0),
    onTertiaryContainer = Brand.CoralOn,
    error              = Brand.DangerRed,
    onError            = Brand.Canvas,
    errorContainer     = Brand.DangerRedLight,
    onErrorContainer   = Brand.DangerRedOn,
    background         = Brand.Slate50,
    onBackground       = Brand.Slate900,
    surface            = Brand.Canvas,
    onSurface          = Brand.Slate900,
    surfaceVariant     = Brand.Slate100,
    onSurfaceVariant   = Brand.Slate700,
    outline            = Brand.Slate300,
    outlineVariant     = Brand.Slate200,
)

private val DarkColors = darkColorScheme(
    primary            = Brand.Lavender,
    onPrimary          = Brand.LavenderOn,
    primaryContainer   = Brand.DarkBlue,
    onPrimaryContainer = Brand.Canvas,
    secondary          = Brand.Slate300,
    onSecondary        = Brand.Slate900,
    secondaryContainer = Brand.Slate700,
    onSecondaryContainer = Brand.Slate100,
    tertiary           = Brand.Coral,
    onTertiary         = Brand.CoralOn,
    error              = Color(0xFFFF8A80),
    onError            = Brand.DangerRedOn,
    errorContainer     = Brand.DangerRedOn,
    onErrorContainer   = Brand.DangerRedLight,
    background         = Brand.CanvasDark,
    onBackground       = Brand.Slate100,
    surface            = Brand.SurfaceDark,
    onSurface          = Brand.Slate200,
    surfaceVariant     = Color(0xFF1E293B),
    onSurfaceVariant   = Brand.Slate300,
    outline            = Brand.Slate700,
    outlineVariant     = Brand.Slate500,
)

val PetMedsShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
)

private fun Color(hex: Long) = androidx.compose.ui.graphics.Color(hex.toUInt().toLong())

@Composable
fun PetMedsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else      -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = PetMedsTypography,
        shapes      = PetMedsShapes,
        content     = content,
    )
}
