package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import eu.buney.maps.BitmapDescriptor

/**
 * One bitmap descriptor + the visual state it was rendered for. The
 * `coverReady` flag lets us evict the placeholder-pill descriptor once
 * the photo loads; `isFavorite` re-rasterises the pill with/without
 * the floating heart badge when the user toggles their saved set.
 */
internal data class CachedMarkerDescriptor(
    val coverReady: Boolean,
    val isFavorite: Boolean,
    val descriptor: BitmapDescriptor,
)

/**
 * Hoisted bitmap descriptor cache for full pill markers, keyed by
 * restaurant id. Lives for the whole MapScreen — survives the
 * dot ↔ pill collapse/expand transitions that happen as the user zooms.
 *
 * Without this, each transition between dot and pill forces
 * `RestaurantMarker` to re-rasterise the pill via
 * `rememberStableComposeBitmapDescriptor` (creates an off-screen
 * ComposeView, measures, lays out, draws to bitmap — ~5-15ms per marker).
 * For 30+ markers reshuffling on every zoom that's hundreds of ms of
 * synchronous work on the UI thread.
 *
 * With this cache, each restaurant's pill is rasterised at most twice
 * across the session: once before the cover photo loads (placeholder
 * variant) and once after (photo variant). Subsequent zoom transitions
 * that pull the marker back from dot to pill are pure cache hits.
 */
@Composable
internal fun rememberMarkerDescriptorCache(): SnapshotStateMap<String, CachedMarkerDescriptor> =
    remember { mutableStateMapOf() }

/**
 * Hoisted cache for the dense-marker dot bitmaps, keyed on
 * [MarkerDotKind]. Three entries max for the whole session — every
 * dense restaurant of the same variant shares one bitmap.
 */
@Composable
internal fun rememberMarkerDotDescriptorCache(): SnapshotStateMap<MarkerDotKind, BitmapDescriptor> =
    remember { mutableStateMapOf() }
