package com.smallllm.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// SmallLLM brand identity — an electric indigo primary with a warm coral accent,
// distinct from the old Android Studio template purple. Full Material 3 role set so
// every surface (containers, tonal buttons, chips, bubbles) reads as one system.
// Expressive character comes from MaterialExpressiveTheme + motion + shapes + type;
// this file just supplies a cohesive, accessible color foundation for light & dark.

val BrandLightColorScheme = lightColorScheme(
    primary = Color(0xFF4A5BC4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF030865),
    inversePrimary = Color(0xFFBBC3FF),

    secondary = Color(0xFF5A5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE1F9),
    onSecondaryContainer = Color(0xFF171B2C),

    tertiary = Color(0xFF9C4327),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCD),
    onTertiaryContainer = Color(0xFF3A0B00),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceTint = Color(0xFF4A5BC4),

    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7),

    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFFBF8FF),
    surfaceDim = Color(0xFFDBD9E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFEAE7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
)

val BrandDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBBC3FF),
    onPrimary = Color(0xFF142778),
    primaryContainer = Color(0xFF323F90),
    onPrimaryContainer = Color(0xFFDFE0FF),
    inversePrimary = Color(0xFF4A5BC4),

    secondary = Color(0xFFC3C5DD),
    onSecondary = Color(0xFF2C2F42),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDFE1F9),

    tertiary = Color(0xFFFFB59B),
    onTertiary = Color(0xFF5C1900),
    tertiaryContainer = Color(0xFF7C2D0F),
    onTertiaryContainer = Color(0xFFFFDBCD),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceTint = Color(0xFFBBC3FF),

    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),

    outline = Color(0xFF918F9A),
    outlineVariant = Color(0xFF46464F),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF38393F),
    surfaceDim = Color(0xFF121318),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)
