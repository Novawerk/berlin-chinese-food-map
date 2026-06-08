package com.novawerk.berlinfoodmap.ui.pages.map

import coil3.request.ImageRequest
import coil3.request.allowHardware

actual fun ImageRequest.Builder.disableHardwareBitmap(): ImageRequest.Builder =
    allowHardware(false)
