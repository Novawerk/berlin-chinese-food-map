package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.favorites_title
import berlinfoodmap.composeapp.generated.resources.filter_clear
import berlinfoodmap.composeapp.generated.resources.filter_open_now
import berlinfoodmap.composeapp.generated.resources.filters_active_multi
import berlinfoodmap.composeapp.generated.resources.marker_location
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import eu.buney.maps.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
    // Render the design's red-wine arrow PNG into a marker bitmap via the
    // same compose-to-bitmap pipeline the restaurant pills use.
    val myLocationIcon: eu.buney.maps.BitmapDescriptor? = if (mapLoaded) {
        rememberStableComposeBitmapDescriptor("__my_location__") {
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(
                    Res.drawable.marker_location,
                ),
                contentDescription = null,
                modifier = androidx.compose.ui.Modifier.size(28.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
        }
    } else null

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
    LaunchedEffect(viewModel.allRestaurants) {
        // Evict descriptors only when a restaurant leaves the dataset, NOT
        // when it's merely filtered out. Keying this on the filtered list
        // threw away every hidden pill the moment a tag filter was applied,
        // so clearing the filter (e.g. tapping the active-filter chip) had to
        // re-rasterise ~180 pills synchronously on the UI thread (~5-15ms
        // each) — the map-wide stutter. The cache ceiling is the full dataset
        // either way, which is just the unfiltered steady state.
        val keep = viewModel.allRestaurants.mapTo(HashSet()) { it.id }
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
                val classifiedIds = viewModel.classifiedIds
                val favorites = viewModel.favorites
                restaurantsFiltered.forEach { restaurant ->
                    val isFavorite = restaurant.id in favorites
                    // Compact dot when the marker is known dense, OR when it
                    // appeared after a filter change and the recompute hasn't
                    // classified it yet — defaulting those to dots avoids a
                    // flash of full pills collapsing to dots (the big→small
                    // blink on clearing a filter); they bloom up to pills on
                    // the next recompute if sparse. Cold start (nothing
                    // classified) falls back to pills to match first paint.
                    val isDot = when {
                        restaurant.id in classifiedIds -> restaurant.id in denseIds
                        classifiedIds.isEmpty() -> false
                        else -> true
                    }
                    if (isDot) {
                        val kind = when {
                            isFavorite && restaurant.hasDiscount -> MarkerDotKind.DISCOUNT_FAVORITE
                            restaurant.hasDiscount -> MarkerDotKind.DISCOUNT
                            isFavorite -> MarkerDotKind.FAVORITE
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
                    // Compass heading from Compass library; null falls back to
                    // 0° (arrow points north). The bitmap is designed as an
                    // upward arrow at 0° so the rotation reads as heading.
                    rotation = controlVm.bearing ?: 0f,
                    zIndex = 10f,
                )

                // Walking-radius rings, toggled by the locate FAB. Radii are
                // 350 m (≈ 5 min walk at 4.2 km/h) and 1000 m (≈ 15 min).
                // Stroke pattern Dash/Gap renders as a dashed line on Android;
                // iOS ignores `strokePattern` (the kmp-maps library wraps
                // MKCircleRenderer which has no strokePattern equivalent) and
                // falls back to a solid stroke — see the upstream Circle.kt
                // docstring. Acceptable for v1; revisit if needed.
                if (controlVm.walkingRadiusVisible) {
                    val ringColor = androidx.compose.ui.graphics.Color(0xFF780A09)
                    val ringPattern = listOf(
                        eu.buney.maps.PatternItem.Dash(20f),
                        eu.buney.maps.PatternItem.Gap(10f),
                    )
                    Circle(
                        center = loc,
                        radius = 350.0,
                        fillColor = androidx.compose.ui.graphics.Color.Transparent,
                        strokeColor = ringColor,
                        strokeWidth = 4f,
                        strokePattern = ringPattern,
                        zIndex = 5f,
                    )
                    Circle(
                        center = loc,
                        radius = 1000.0,
                        fillColor = androidx.compose.ui.graphics.Color.Transparent,
                        strokeColor = ringColor,
                        strokeWidth = 4f,
                        strokePattern = ringPattern,
                        zIndex = 5f,
                    )
                }
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

        // Bottom-right locate FAB (per May-24 design PDF page 4). First
        // tap centres on user's location AND turns the walking-radius
        // rings on; second tap turns them off without re-centring. The
        // FAB shows a busy spinner overlay while a sensor fetch is in
        // flight. Bottom padding clears the restaurant-cards row entirely
        // — NearbyCard is ~56 dp tall + the LazyRow's 12 dp bottom pad +
        // ~12 dp breathing room = ~80 dp; we use a slightly higher
        // 100 dp so the FAB sits cleanly above the cards instead of
        // touching them.
        val hasCards = viewModel.visibleRestaurants.isNotEmpty()
        LocateFab(
            isLocating = controlVm.isLocating,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = if (hasCards) 100.dp else 16.dp),
            onClick = {
                if (controlVm.walkingRadiusVisible) {
                    controlVm.toggleWalkingRadius()
                } else {
                    controlVm.toggleWalkingRadius()
                    commands.requestLocate()
                }
            },
        )

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

        // Active-filter chip — appears once a tag (or the favorites / open-now
        // toggle) is applied from the search sheet. Tapping it clears every
        // filter; this is the "cancel" affordance that was missing after the
        // sheet dismissed. Neutral surface chrome per the map-FAB convention so
        // it stays quiet against the red marker pins.
        if (viewModel.activeFilterCount > 0) {
            ActiveFilterChip(
                label = activeFilterLabel(viewModel),
                onClear = { viewModel.resetFilters() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun activeFilterLabel(vm: MapViewModel): String {
    val count = vm.activeFilterCount
    return when {
        count == 1 && vm.selectedCuisines.size == 1 ->
            tagDisplayName(vm.selectedCuisines.first())
        count == 1 && vm.selectedFormats.size == 1 ->
            tagDisplayName(vm.selectedFormats.first())
        count == 1 && vm.favoritesOnly -> stringResource(Res.string.favorites_title)
        count == 1 && vm.openNow -> stringResource(Res.string.filter_open_now)
        else -> stringResource(Res.string.filters_active_multi, count)
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // White (`surface`) container keeps the chip off the reserved brand-red
    // `primary` fill that the marker pins use, and reads cleanly over the
    // map's light tiles — surfaceContainerHigh tinted mauve/purple in the
    // expressive light scheme, which looked off. The "active" state is
    // signalled by decoration instead of a coloured fill: a brand-coloured
    // outline ring plus a brand-tinted leading filter glyph. A dedicated
    // circular close affordance makes "tap to clear" obvious.
    Surface(
        onClick = onClear,
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.filter_clear),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
