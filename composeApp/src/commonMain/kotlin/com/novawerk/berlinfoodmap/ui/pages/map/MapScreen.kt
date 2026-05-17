package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import eu.buney.maps.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    controlVm: MapControlViewModel,
    commands: MapCommands,
    snackbarHostState: SnackbarHostState,
    onNavigateDetail: (String) -> Unit,
) {
    // Both view models are @AppScope DI singletons resolved by AppComponent —
    // same instance across the entire app. They were touched at the top of
    // App() so their `init` blocks ran during the splash hold and the
    // restaurant data + first location fix are already in memory.
    val restaurantsFiltered = viewModel.restaurantsFiltered

    var mapLoaded by remember { mutableStateOf(false) }
    val myLocationIcon = remember(mapLoaded) {
        if (mapLoaded) {
            try { myLocationDotIcon() } catch (_: Exception) { null }
        } else null
    }

    val mapConfig = rememberBerlinMapConfig()

    val cameraPositionState = rememberCameraPositionState {
        position = DEFAULT_BERLIN_CAMERA
    }

    // Cover bitmaps are owned by the VM (loaded via `imageLoader.execute`,
    // not via composable painters) — survives configuration changes and
    // dot ↔ pill collapse/expand cycles as the user zooms.
    val markerCovers = viewModel.markerCovers
    // Descriptor caches stay in composition: rendering goes through a
    // hidden `ComposeView` (Android) that needs a live composition context.
    val descriptorCache = rememberMarkerDescriptorCache()
    val dotDescriptorCache = rememberMarkerDotDescriptorCache()
    LaunchedEffect(restaurantsFiltered) {
        // Drop entries for restaurants no longer in the filter — otherwise
        // descriptors leak bitmap memory until MapScreen disposes.
        val keep = restaurantsFiltered.mapTo(HashSet()) { it.id }
        descriptorCache.keys.toList().forEach { id ->
            if (id !in keep) descriptorCache.remove(id)
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Reset the nearby-row scroll position whenever the user pans/zooms the
    // map by gesture. Programmatic camera moves (card click, locate, district
    // tap) report API_ANIMATION, so they don't trigger this — only direct
    // user input does. Without this, the row would keep its old scroll
    // offset against a freshly-rebuilt list, which is disorienting.
    LaunchedEffect(listState) {
        snapshotFlow { cameraPositionState.isMoving }
            .filter { it && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE }
            .collect {
                if (listState.firstVisibleItemIndex != 0 ||
                    listState.firstVisibleItemScrollOffset != 0
                ) {
                    listState.animateScrollToItem(0)
                }
            }
    }

    val density = LocalDensity.current
    // AABB dense-detection parameters. Each pill has a per-restaurant
    // width (estimated from name + tag chars, capped at MARKER_MAX_WIDTH);
    // the body height is constant. A small visual padding is added so
    // adjacent-but-not-quite-touching pills also collapse to dots —
    // matches what users perceive as "overlapping".
    val markerHeightPx = with(density) { MARKER_BODY_HEIGHT.toPx() }
    val markerPaddingPx = with(density) { 6.dp.toPx() }
    val markerMaxWidthPx = with(density) { MARKER_MAX_WIDTH.toPx() }
    // Worst-case centre-to-centre overlap distance: two MARKER_MAX_WIDTH
    // pills + padding. Used as the spatial-grid cell size.
    val maxOverlapPx = markerMaxWidthPx + markerPaddingPx
    val widthEstimator: (com.novawerk.berlinfoodmap.domain.restaurant.Restaurant) -> Float =
        remember(density) {
            { restaurant ->
                with(density) { estimatedMarkerWidth(restaurant).toPx() }
            }
        }

    // Push viewport bounds into the data VM only when the camera settles.
    // The bottom LazyRow reads `visibleRestaurants` derived from these
    // bounds; updating per-frame during a pinch/pan turns into 60 fps of
    // list rebuilds, which is the dominant source of jank on big zoom
    // changes. Idle-only updates give visible cards a one-gesture lag,
    // which is the standard mobile pattern.
    LaunchedEffect(Unit) {
        snapshotFlow {
            cameraPositionState.isMoving to cameraPositionState.projection?.visibleBounds
        }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, bounds) -> bounds }
            .distinctUntilChanged()
            .collect { viewModel.updateVisibleBounds(it) }
    }

    // Recompute the dense-id set on filter changes (LaunchedEffect re-fires,
    // snapshotFlow re-collects → fresh first emission), on first map load
    // (projection flips from null → non-null), and on zoom-idle. Pan is
    // invariant under our screen-distance detection so we don't watch it.
    LaunchedEffect(restaurantsFiltered, markerHeightPx, markerPaddingPx, maxOverlapPx) {
        snapshotFlow {
            Triple(
                cameraPositionState.projection != null,
                cameraPositionState.isMoving,
                cameraPositionState.position.zoom,
            )
        }
            .filter { (hasProjection, isMoving, _) -> hasProjection && !isMoving }
            .distinctUntilChanged()
            .collect {
                cameraPositionState.projection?.let { projection ->
                    viewModel.recomputeDenseIds(
                        projection = projection,
                        widthEstimator = widthEstimator,
                        heightPx = markerHeightPx,
                        paddingPx = markerPaddingPx,
                        maxOverlapPx = maxOverlapPx,
                    )
                }
            }
    }

    // ── Command bus ────────────────────────────────────────────────────
    // Pan requests come from outside MapScreen (search results, district
    // picker). LaunchedEffect collapses the slot back to null once the
    // animation kicks off so a repeat tap on the same target still fires.
    LaunchedEffect(commands.pendingPan) {
        commands.pendingPan?.let { req ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(req.target, req.zoom),
            )
            commands.clearPan()
        }
    }

    // Locate requests use a monotonically increasing tick so re-taps of the
    // bottom-nav button after a permission denial still re-enter the flow.
    // Skip the initial 0 — that's the default state, not a user action.
    LaunchedEffect(commands.locateTick) {
        if (commands.locateTick == 0) return@LaunchedEffect
        if (controlVm.isLocating) return@LaunchedEffect
        when (val result = controlVm.ensureFreshLocation()) {
            is LocationOutcome.Available -> cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(result.coords, 15f),
            )
            LocationOutcome.PermissionDenied -> snackbarHostState.showSnackbar(
                "Location permission denied",
            )
            LocationOutcome.Unavailable -> snackbarHostState.showSnackbar(
                "Could not determine your location",
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapConfig.properties,
            uiSettings = mapConfig.uiSettings,
            onMapLoaded = { mapLoaded = true },
        ) {
            // Defer marker emission until the SDK has rendered its first
            // frame. Each marker rasterises a Compose bitmap on the main
            // thread, and ~200 of those compete with the Maps SDK for the
            // initial paint — emitting after `onMapLoaded` lets the map
            // tiles appear first, with pins dropping in shortly after.
            if (mapLoaded) {
                val denseIds = viewModel.denseIds
                val favorites = viewModel.favorites
                restaurantsFiltered.forEach { restaurant ->
                    val isFavorite = restaurant.id in favorites
                    if (restaurant.id in denseIds) {
                        val kind = when {
                            isFavorite -> MarkerDotKind.FAVORITE
                            restaurant.featured -> MarkerDotKind.FEATURED
                            else -> MarkerDotKind.REGULAR
                        }
                        MarkerDot(
                            restaurant = restaurant,
                            kind = kind,
                            descriptorCache = dotDescriptorCache,
                            onClick = { onNavigateDetail(restaurant.id) },
                        )
                    } else {
                        RestaurantMarker(
                            restaurant = restaurant,
                            cover = markerCovers[restaurant.id],
                            isFavorite = isFavorite,
                            descriptorCache = descriptorCache,
                            onClick = { onNavigateDetail(restaurant.id) },
                        )
                    }
                }
            }

            controlVm.myLocation?.let { loc ->
                val state = rememberMarkerState(key = "__my_location__", position = loc)
                Marker(
                    state = state,
                    icon = myLocationIcon,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 10f,
                )
            }
        }

        // Hide the underlying MapView's first frame, which the Google Maps
        // SDK paints at (0, 0) — somewhere in the Gulf of Guinea — before
        // applying the camera position from `rememberCameraPositionState`.
        // Plain brand surface, no spinner, fades out the moment the SDK
        // signals first render so we don't bleed perceived load time.
        AnimatedVisibility(
            visible = !mapLoaded,
            enter = androidx.compose.animation.EnterTransition.None,
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            )
        }

        if (viewModel.visibleRestaurants.isNotEmpty()) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(viewModel.visibleRestaurants, key = { it.id }) { restaurant ->
                    NearbyCard(
                        restaurant = restaurant,
                        isFavorite = restaurant.id in viewModel.favorites,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(restaurant.latitude, restaurant.longitude),
                                        16f,
                                    ),
                                )
                            }
                        },
                        onLongClick = { onNavigateDetail(restaurant.id) },
                    )
                }
            }
        }
    }
}
