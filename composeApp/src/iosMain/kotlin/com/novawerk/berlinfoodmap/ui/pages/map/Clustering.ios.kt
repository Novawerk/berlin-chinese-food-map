package com.novawerk.berlinfoodmap.ui.pages.map

import platform.UIKit.UIScreen

// iOS's `Projection.toScreenLocation` returns CGPoint values which are
// in **points** (1pt ≈ 1dp ≈ scale-many px). Multiply by the screen
// scale so coords match the cluster radius (computed via Dp.toPx()).
internal actual fun projectionPixelScale(): Float =
    UIScreen.mainScreen.scale.toFloat()
