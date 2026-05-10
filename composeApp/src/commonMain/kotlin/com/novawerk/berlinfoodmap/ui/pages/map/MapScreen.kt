package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.filter_title
import berlinfoodmap.composeapp.generated.resources.map_loading
import coil3.compose.LocalPlatformContext
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.pages.search.RestaurantSearchBar
import eu.buney.maps.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    repository: RestaurantRepository,
    favoritesRepository: FavoritesRepository,
    onNavigateDetail: (String) -> Unit,
) {
    // VM is bound to the current NavBackStackEntry — survives recompose +
    // configuration changes; cleared when the destination leaves the back
    // stack. Properties are Compose state, so reads in composition
    // auto-subscribe via the snapshot system.
    //
    // PlatformContext is captured at first composition and held by the VM.
    // It's the application-scoped Coil context (Android: app Context,
    // iOS: empty marker), so no leak risk from the long lifetime.
    val platformContext = LocalPlatformContext.current
    val viewModel: MapViewModel = viewModel {
        MapViewModel(repository, favoritesRepository, platformContext)
    }
    // Map-control state (location + freshness) lives in its own VM, separate
    // from the restaurant pipeline. Each VM has independent lifecycle and
    // dependencies; both bind to the same NavBackStackEntry so they survive
    // configuration changes together.
    val controlVm: MapControlViewModel = viewModel { MapControlViewModel() }
    val restaurantsFiltered = viewModel.restaurantsFiltered

    var filterSheetOpen by remember { mutableStateOf(false) }
    var districtSheetOpen by remember { mutableStateOf(false) }
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
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    // Reset the nearby-row scroll position whenever the user pans/zooms the
    // map by gesture. Programmatic camera moves (card click, FAB, district
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

    // Recompute the dense-id set on filter changes (immediate) and on
    // zoom-idle (lazy — pan is invariant under our screen-distance
    // detection, since pairwise screen distances are translation-invariant).
    LaunchedEffect(restaurantsFiltered, markerHeightPx, markerPaddingPx, maxOverlapPx) {
        cameraPositionState.projection?.let { projection ->
            viewModel.recomputeDenseIds(
                projection = projection,
                widthEstimator = widthEstimator,
                heightPx = markerHeightPx,
                paddingPx = markerPaddingPx,
                maxOverlapPx = maxOverlapPx,
            )
        }
        snapshotFlow {
            cameraPositionState.isMoving to cameraPositionState.position.zoom
        }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, zoom) -> zoom }
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

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapConfig.properties,
            uiSettings = mapConfig.uiSettings,
            onMapLoaded = { mapLoaded = true },
        ) {
            // Per-restaurant render branch: dense → compact dot, otherwise
            // → full pill. We iterate the entire filtered list (typically
            // ≤200) because the Maps SDK handles off-screen markers
            // efficiently and dot bitmaps come from a 3-entry shared cache,
            // so the marginal cost of every-restaurant emission is small.
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

        // Loading mask covers the empty/grey map until the SDK signals the
        // first frame is rendered (`onMapLoaded`).
        AnimatedVisibility(
            visible = !mapLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(Res.string.map_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        RestaurantSearchBar(
            repository = repository,
            onRestaurantClick = { id ->
                viewModel.allRestaurants.firstOrNull { it.id == id }?.let { r ->
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(r.latitude, r.longitude),
                                16f,
                            ),
                        )
                    }
                }
                onNavigateDetail(id)
            },
            onBrowseDistricts = { districtSheetOpen = true },
            modifier = Modifier.align(Alignment.TopCenter),
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

        // Lift the FABs clear of the bottom NearbyCard row. NearbyCard is
        // ~68dp tall and sits 12dp above the screen bottom; an extra 24dp
        // of breathing room above it puts the FAB visibly above the cards
        // (no horizontal/vertical overlap) without floating too high.
        // When no card row is showing (empty viewport) the FAB sits close
        // to the bottom edge.
        val cardOffset = if (viewModel.visibleRestaurants.isNotEmpty()) 104.dp else 24.dp
        // FABs use a neutral elevated surface (`surfaceContainerHigh`) with a
        // dark `onSurface` icon — quiet chrome that sits on top of the map
        // without competing with brand-red markers or the warm-cream
        // background. This is the standard "neutral floating action" pattern
        // (cf. Google Maps' locate FAB).
        val fabContainer = MaterialTheme.colorScheme.surfaceContainerHigh
        val fabContent = MaterialTheme.colorScheme.onSurface

        FloatingActionButton(
            onClick = {
                if (controlVm.isLocating) return@FloatingActionButton
                scope.launch {
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
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = cardOffset),
            containerColor = fabContainer,
            contentColor = fabContent,
        ) {
            if (controlVm.isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = fabContent,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = "Locate me")
            }
        }

        BadgedBox(
            badge = {
                if (viewModel.activeFilterCount > 0) {
                    Badge { Text(text = "${viewModel.activeFilterCount}") }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = cardOffset),
        ) {
            FloatingActionButton(
                onClick = { filterSheetOpen = true },
                containerColor = fabContainer,
                contentColor = fabContent,
            ) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = stringResource(Res.string.filter_title),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (filterSheetOpen) {
        FilterSheet(
            allRestaurants = viewModel.allRestaurants,
            favorites = viewModel.favorites,
            selectedCuisines = viewModel.selectedCuisines,
            selectedFormats = viewModel.selectedFormats,
            favoritesOnly = viewModel.favoritesOnly,
            featuredOnly = viewModel.featuredOnly,
            openNow = viewModel.openNow,
            // Single commit point — sheet draft becomes live filter only
            // here, then dismiss. Sheet swipe / scrim cancels the draft.
            onApply = { fav, featured, openNow, cuisines, formats ->
                viewModel.toggleFavoritesOnly(fav)
                viewModel.toggleFeaturedOnly(featured)
                viewModel.toggleOpenNow(openNow)
                viewModel.setCuisines(cuisines)
                viewModel.setFormats(formats)
                filterSheetOpen = false
            },
            onDismiss = { filterSheetOpen = false },
        )
    }

    if (districtSheetOpen) {
        DistrictPickerSheet(
            // Pass the full list — district browsing is "where in Berlin
            // do I want to look", independent of current filters. The
            // filter state stays applied after the camera jump.
            restaurants = viewModel.allRestaurants,
            onDistrictSelected = { center ->
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(center, 14f),
                    )
                }
                districtSheetOpen = false
            },
            onDismiss = { districtSheetOpen = false },
        )
    }
}
