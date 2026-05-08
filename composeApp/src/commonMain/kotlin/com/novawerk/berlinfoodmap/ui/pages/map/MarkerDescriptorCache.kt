package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import eu.buney.maps.BitmapDescriptor

/**
 * One bitmap descriptor + the visual state it was rendered for. The
 * coverReady flag lets us evict the placeholder-pill descriptor once the
 * photo loads; the isFavorite flag re-rasterises the pill with/without
 * the heart marker when the user toggles their saved set.
 */
internal data class CachedMarkerDescriptor(
    val coverReady: Boolean,
    val isFavorite: Boolean,
    val descriptor: BitmapDescriptor,
)

/**
 * Hoisted bitmap descriptor cache, keyed by restaurant id. Lives for the
 * whole MapScreen — survives cluster regroupings.
 *
 * Without this, each transition between single-marker and multi-cluster
 * forces `RestaurantMarker` to re-rasterise the pill via
 * `rememberStableComposeBitmapDescriptor` (creates an off-screen
 * ComposeView, measures, lays out, draws to bitmap — ~5-15ms per marker).
 * For 30+ markers reshuffling on every zoom that's hundreds of ms of
 * synchronous work on the UI thread.
 *
 * With this cache, each restaurant's pill is rasterised at most twice
 * across the session: once before the cover photo loads (placeholder
 * variant) and once after (photo variant). Subsequent cluster transitions
 * are pure cache hits.
 */
@Composable
internal fun rememberMarkerDescriptorCache(): SnapshotStateMap<String, CachedMarkerDescriptor> =
    remember { mutableStateMapOf() }

/**
 * Hoisted cluster-bitmap cache, keyed on the *displayed* count (clamped to
 * 100 because ≥100 collapses to "99+"). Cluster badges don't depend on
 * cluster identity or centroid — only the rendered number — so a single
 * bitmap per count is shared across every cluster of that size and survives
 * cluster regroupings.
 *
 * Without this, every zoom-idle that changes the cluster grouping forces
 * each `ClusterMarker` slot to re-rasterise via the off-screen ComposeView
 * path (~5–15 ms per cluster). On big zoom-out transitions where 30+
 * clusters appear, that's a noticeable hitch.
 */
@Composable
internal fun rememberClusterDescriptorCache(): SnapshotStateMap<Int, BitmapDescriptor> =
    remember { mutableStateMapOf() }
