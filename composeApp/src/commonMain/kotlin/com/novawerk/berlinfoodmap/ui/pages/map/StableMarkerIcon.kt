package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.GoogleMapComposable

/**
 * Drop-in replacement for `eu.buney.maps.rememberComposeBitmapDescriptor` with
 * correct cache invalidation on Android.
 *
 * The library's Android implementation captures the `content` lambda into the
 * `remember` keys via `rememberUpdatedState`. Each recomposition of the
 * caller produces a new lambda instance, which fails reference equality —
 * `remember(..., currentContent, *keys)` invalidates and the bitmap is
 * re-rendered every recomposition. The user-supplied `keys` parameter is
 * effectively ignored.
 *
 * Visible symptom: the marker icon flickers whenever anything inside the
 * GoogleMap content lambda triggers recomposition (cluster reassignment,
 * camera-state reads, painter state transitions, etc.).
 *
 * This wrapper keeps the same rendering path but excludes the content lambda
 * from the cache key. The latest closure values are still captured via
 * `rememberUpdatedState`, but the bitmap is re-rendered only when the
 * caller's explicit keys change.
 *
 * iOS already implements this correctly; the iOS actual delegates straight
 * to the library.
 */
@Composable
@GoogleMapComposable
expect fun rememberStableComposeBitmapDescriptor(
    vararg keys: Any,
    content: @Composable () -> Unit,
): BitmapDescriptor
