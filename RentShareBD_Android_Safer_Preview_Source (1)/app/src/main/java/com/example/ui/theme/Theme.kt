package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = PureWhite,
    primaryContainer = IndigoLight,
    onPrimaryContainer = IndigoDark,
    secondary = TealAccent,
    onSecondary = PureWhite,
    secondaryContainer = TealLight,
    onSecondaryContainer = TealDark,
    background = SlateCanvas,
    onBackground = SlateInk,
    surface = PureWhite,
    onSurface = SlateInk,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = RoseError,
    onError = PureWhite,
    errorContainer = RoseSurface,
    onErrorContainer = RoseError
)

private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = IndigoDark,
    primaryContainer = IndigoPrimary,
    onPrimaryContainer = PureWhite,
    secondary = TealLight,
    onSecondary = TealDark,
    secondaryContainer = TealAccent,
    onSecondaryContainer = PureWhite,
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = RoseSurface,
    onError = RoseError
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun RentShareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
