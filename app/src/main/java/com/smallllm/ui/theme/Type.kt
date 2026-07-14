package com.smallllm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// Full Material 3 type scale (all roles come populated from the default Typography()),
// with the Expressive touch of heavier, more emphatic display/headline/title weights so
// prominent text — screen titles, model names — carries visual energy. Body/label roles
// keep their readable defaults.
private val base = Typography()

val AppTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)
