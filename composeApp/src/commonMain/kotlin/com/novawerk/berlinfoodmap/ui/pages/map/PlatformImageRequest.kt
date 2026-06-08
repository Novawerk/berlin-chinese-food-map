package com.novawerk.berlinfoodmap.ui.pages.map

import coil3.request.ImageRequest

// Marker covers must be drawn into a Compose canvas. On Android Coil can return
// hardware bitmaps that fail to render in software-rasterised paths, so the
// request must opt out via allowHardware(false). iOS uses CGImage and has no
// hardware-bitmap concept, so the actual is a no-op.
expect fun ImageRequest.Builder.disableHardwareBitmap(): ImageRequest.Builder
