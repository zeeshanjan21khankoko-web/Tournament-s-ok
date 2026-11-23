package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ProTournamentDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = VoidBlack,
    primaryContainer = ObsidianElevated,
    onPrimaryContainer = NeonCyan,
    secondary = ElectricPurple,
    onSecondary = TextPrimary,
    secondaryContainer = NeonViolet,
    onSecondaryContainer = TextPrimary,
    tertiary = CyberMagenta,
    onTertiary = TextPrimary,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = ObsidianElevated,
    error = DangerCrimson,
    onError = VoidBlack
)

val ProTournamentShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ProTournamentTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProTournamentDarkColorScheme,
        typography = ProTournamentTypography,
        shapes = ProTournamentShapes,
        content = content
    )
}
