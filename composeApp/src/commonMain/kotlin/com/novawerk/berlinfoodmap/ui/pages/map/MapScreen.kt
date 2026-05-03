package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.filter_title
import berlinfoodmap.composeapp.generated.resources.map_loading
import berlinfoodmap.composeapp.generated.resources.search_hint
import coil3.compose.LocalPlatformContext
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
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
    onNavigateDetail: (String) -> Unit,
    onNavigateSearch: () -> Unit,
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
    val viewModel: MapViewModel = viewModel { MapViewModel(repository, platformContext) }
    // Map-control state (location + freshness) lives in its own VM, separate
    // from the restaurant pipeline. Each VM has independent lifecycle and
    // dependencies; both bind to the same NavBackStackEntry so they survive
    // configuration changes together.
    val controlVm: MapControlViewModel = viewModel { MapControlViewModel() }
    val restaurantsFiltered = viewModel.restaurantsFiltered

    var filterSheetOpen by remember { mutableStateOf(false) }
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
    // cluster regroupings.
    val markerCovers = viewModel.markerCovers
    // Descriptor cache stays in composition: rendering goes through a
    // hidden `ComposeView` (Android) that needs a live composition context.
    val descriptorCache = rememberMarkerDescriptorCache()
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

    val density = LocalDensity.current
    val clusterRadiusPx = with(density) { 72.dp.toPx() }

    // Push viewport bounds into the data VM so it can derive
    // `visibleRestaurants` itself — composable's only job here is to
    // forward the (composable-bound) camera state.
    LaunchedEffect(Unit) {
        snapshotFlow { cameraPositionState.projection?.visibleBounds }
            .collect { viewModel.updateVisibleBounds(it) }
    }

    // Recompute clusters on filter changes (immediate) and on zoom-idle
    // (lazy — pan is invariant under our screen-distance clustering).
    LaunchedEffect(restaurantsFiltered, clusterRadiusPx) {
        cameraPositionState.projection?.let { projection ->
            viewModel.recomputeClusters(projection, clusterRadiusPx)
        }
        snapshotFlow {
            cameraPositionState.isMoving to cameraPositionState.position.zoom
        }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, zoom) -> zoom }
            .distinctUntilChanged()
            .collect {
                cameraPositionState.projection?.let { projection ->
                    viewModel.recomputeClusters(projection, clusterRadiusPx)
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
            viewModel.clusters.forEach { cluster ->
                if (cluster.items.size == 1) {
                    val restaurant = cluster.items.first()
                    RestaurantMarker(
                        restaurant = restaurant,
                        cover = markerCovers[restaurant.id],
                        descriptorCache = descriptorCache,
                        onClick = { onNavigateDetail(restaurant.id) },
                    )
                } else {
                    ClusterMarker(
                        cluster = cluster,
                        onClick = {
                            scope.launch {
                                val targetZoom = (cameraPositionState.position.zoom + 2f)
                                    .coerceAtMost(18f)
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(cluster.center, targetZoom),
                                )
                            }
                        },
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

        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            placeholder = { Text(stringResource(Res.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onNavigateSearch() },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = MaterialTheme.shapes.extraLarge,
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
                        onClick = { onNavigateDetail(restaurant.id) },
                    )
                }
            }
        }

        // 76dp clears the trimmed NearbyCard (44dp cover + 12dp card padding +
        // 12dp LazyRow bottom inset) by ~8dp. When no card row is showing
        // (empty viewport) the FABs sit close to the bottom edge.
        val cardOffset = if (viewModel.visibleRestaurants.isNotEmpty()) 76.dp else 24.dp
        SmallFloatingActionButton(
            onClick = {
                if (controlVm.isLocating) return@SmallFloatingActionButton
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
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            if (controlVm.isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
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
            SmallFloatingActionButton(
                onClick = { filterSheetOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
            selectedCuisine = viewModel.selectedCuisine,
            selectedFormat = viewModel.selectedFormat,
            // Cuisine + format: pick → auto-dismiss. Reset stays in the
            // sheet so the user can clear without reopening.
            onCuisineSelected = {
                viewModel.setCuisine(it)
                filterSheetOpen = false
            },
            onFormatSelected = {
                viewModel.setFormat(it)
                filterSheetOpen = false
            },
            onDistrictSelected = { center ->
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(center, 14f),
                    )
                }
                filterSheetOpen = false
            },
            onReset = viewModel::resetFilters,
            onDismiss = { filterSheetOpen = false },
        )
    }
}
