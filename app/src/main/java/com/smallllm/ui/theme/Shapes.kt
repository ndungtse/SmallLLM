package com.smallllm.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Expressive shape scale — rounder and more generous than the Material 3 defaults
// (which top out around 4/8/12/16/28dp). Bigger radii give cards, chips, buttons and
// bubbles the soft, tactile feel that defines M3 Expressive. Morphing accent shapes
// (MaterialShapes / RoundedPolygon) are applied per-component rather than in the theme.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
