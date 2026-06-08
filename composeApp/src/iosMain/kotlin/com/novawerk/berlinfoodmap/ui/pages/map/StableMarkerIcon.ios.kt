package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.GoogleMapComposable
import eu.buney.maps.rememberComposeBitmapDescriptor

/**
 * iOS keys the bitmap remember correctly (no content lambda in keys),
 * so we delegate to the library's implementation.
 *
 * However: the iOS implementation uses `ImageComposeScene` /
 * `renderComposeScene` to rasterise the content, and that's a fully
 * **detached** Compose scene — none of our app-level CompositionLocals
 * (MaterialTheme, LocalAppLocale, …) reach inside. Without re-providing
 * them, every `MaterialTheme.colorScheme.*` read inside the marker
 * content silently falls back to Material 3's default Baseline scheme,
 * which is purple — visible as purple cluster badges and tag chips on
 * iOS while Android stays brand-red.
 *
 * Fix: capture the parent composition's `MaterialTheme` slice here and
 * replay it inside the bitmap-render lambda. Kotlin closure capture
 * carries the values across the detached scene.
 *
 * (Android's actual hooks in via `setParentCompositionContext`, which
 * preserves locals naturally, so this dance isn't needed there.)
 */
@Composable
@GoogleMapComposable
actual fun rememberStableComposeBitmapDescriptor(
    vararg keys: Any,
    content: @Composable () -> Unit,
): BitmapDescriptor {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes
    return rememberComposeBitmapDescriptor(keys = keys) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
