package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = HabeshaEmeraldDark,
    onPrimary = OnHabeshaEmeraldDark,
    primaryContainer = HabeshaEmeraldContainerDark,
    onPrimaryContainer = OnHabeshaEmeraldContainerDark,
    secondary = TibebGoldDark,
    onSecondary = OnTibebGoldDark,
    secondaryContainer = TibebGoldContainerDark,
    onSecondaryContainer = OnTibebGoldContainerDark,
    tertiary = HabeshaCrimsonDark,
    onTertiary = OnHabeshaCrimsonDark,
    tertiaryContainer = HabeshaCrimsonContainerDark,
    onTertiaryContainer = OnHabeshaCrimsonContainerDark,
    background = ObsidianBackgroundDark,
    onBackground = CrispIvoryInkDark,
    surface = StudioSurfaceDark,
    onSurface = CrispIvoryInkDark,
    surfaceVariant = StudioVariantDark,
    onSurfaceVariant = CrispIvoryInkDark.copy(alpha = 0.78f),
    outline = StudioOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = HabeshaEmeraldLight,
    onPrimary = OnHabeshaEmeraldLight,
    primaryContainer = HabeshaEmeraldContainerLight,
    onPrimaryContainer = OnHabeshaEmeraldContainerLight,
    secondary = TibebGoldLight,
    onSecondary = OnTibebGoldLight,
    secondaryContainer = TibebGoldContainerLight,
    onSecondaryContainer = OnTibebGoldContainerLight,
    tertiary = HabeshaCrimsonLight,
    onTertiary = OnHabeshaCrimsonLight,
    tertiaryContainer = HabeshaCrimsonContainerLight,
    onTertiaryContainer = OnHabeshaCrimsonContainerLight,
    background = IvoryBackgroundLight,
    onBackground = CharcoalInkLight,
    surface = CardSurfaceLight,
    onSurface = CharcoalInkLight,
    surfaceVariant = WarmSandVariantLight,
    onSurfaceVariant = CharcoalInkLight.copy(alpha = 0.74f),
    outline = WarmSlateOutlineLight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GirmaicologyTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
