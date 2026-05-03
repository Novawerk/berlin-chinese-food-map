package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.GoogleMapComposable
import eu.buney.maps.rememberComposeBitmapDescriptor

// iOS already keys the bitmap remember on `keys` only (no content lambda
// in keys), so the library's implementation is correct here.
@Composable
@GoogleMapComposable
actual fun rememberStableComposeBitmapDescriptor(
    vararg keys: Any,
    content: @Composable () -> Unit,
): BitmapDescriptor = rememberComposeBitmapDescriptor(keys = keys, content = content)
