package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KundarkiLightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = CrimsonSoft,
    onPrimaryContainer = CrimsonPrimary,
    secondary = FreshGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = FreshGreenSoft,
    onSecondaryContainer = FreshGreenDark,
    tertiary = WarmAmber,
    onTertiary = InkPrimary,
    tertiaryContainer = WarmAmberSoft,
    onTertiaryContainer = InkPrimary,
    background = CanvasLight,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder,
    error = CrimsonPrimary,
    onError = SurfaceWhite
)

private val KundarkiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KundarkiDeliveryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KundarkiLightColorScheme,
        typography = KundarkiTypography,
        shapes = KundarkiShapes,
        content = content
    )
}
