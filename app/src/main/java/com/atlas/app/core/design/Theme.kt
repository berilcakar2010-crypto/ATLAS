package com.atlas.app.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AtlasColorScheme = darkColorScheme(
    primary = AtlasColors.Brass,
    onPrimary = AtlasColors.Surface2,
    primaryContainer = AtlasColors.Burgundy,
    onPrimaryContainer = AtlasColors.Ivory,
    secondary = AtlasColors.Taupe,
    onSecondary = AtlasColors.Surface2,
    secondaryContainer = AtlasColors.Charcoal,
    onSecondaryContainer = AtlasColors.Ivory,
    background = AtlasColors.Surface2,
    onBackground = AtlasColors.Ivory,
    surface = AtlasColors.Charcoal,
    onSurface = AtlasColors.Ivory,
    surfaceVariant = AtlasColors.Charcoal,
    onSurfaceVariant = AtlasColors.TaupeText,
    surfaceTint = Color.Transparent,
    surfaceDim = AtlasColors.Surface2,
    surfaceBright = Color(0xFF353338),
    surfaceContainerLowest = Color(0xFF1C1A1E),
    surfaceContainerLow = Color(0xFF252328),
    surfaceContainer = AtlasColors.Charcoal,
    surfaceContainerHigh = Color(0xFF2F2D32),
    surfaceContainerHighest = Color(0xFF353338),
    outline = AtlasColors.Taupe,
    outlineVariant = AtlasColors.LineSoft,
    error = AtlasColors.Rose,
    onError = AtlasColors.Surface2,
)

private val AtlasShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun AtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtlasColorScheme,
        typography = AtlasTypography,
        shapes = AtlasShapes,
        content = content,
    )
}
