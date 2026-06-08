package com.novawerk.berlinfoodmap.ui.pages.map

// Android's `Projection.toScreenLocation` already returns pixel coords
// (Android `Point` = px). No scaling needed.
internal actual fun projectionPixelScale(): Float = 1f
