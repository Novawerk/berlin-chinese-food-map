package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.CuisineType
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.cuisineDisplayName
import eu.buney.maps.GoogleMap
import eu.buney.maps.LatLng
import eu.buney.maps.LatLngBounds
import eu.buney.maps.MapProperties
import eu.buney.maps.MapStyleOptions
import eu.buney.maps.MapUiSettings
import eu.buney.maps.Marker
import eu.buney.maps.MarkerComposable
import eu.buney.maps.CameraPosition
import eu.buney.maps.CameraUpdateFactory
import eu.buney.maps.Projection
import eu.buney.maps.rememberCameraPositionState
import eu.buney.maps.rememberMarkerState
import eu.buney.maps.rememberUpdatedMarkerState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.all
import berlinfoodmap.composeapp.generated.resources.filter_cuisine
import berlinfoodmap.composeapp.generated.resources.filter_district
import berlinfoodmap.composeapp.generated.resources.filter_reset
import berlinfoodmap.composeapp.generated.resources.filter_title
import berlinfoodmap.composeapp.generated.resources.search_hint

private val BERLIN_BOUNDS = LatLngBounds(
    southwest = LatLng(52.33, 13.08),
    northeast = LatLng(52.68, 13.76),
)
private val BERLIN_CENTER = LatLng(52.52, 13.405)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    repository: RestaurantRepository,
    onNavigateDetail: (String) -> Unit,
    onNavigateSearch: () -> Unit,
) {
    var allRestaurants by remember { mutableStateOf<List<Restaurant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedCuisine by remember { mutableStateOf<CuisineType?>(null) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var selectedRestaurantId by remember { mutableStateOf<String?>(null) }
    var filterSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allRestaurants = repository.getAll()
        loading = false
    }

    var mapLoaded by remember { mutableStateOf(false) }
    val myLocationIcon = remember(mapLoaded) {
        if (mapLoaded) {
            try { myLocationDotIcon() } catch (_: Exception) { null }
        } else null
    }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val mapStyleOptions = remember {
        try {
            MapStyleOptions.fromJson(BERLIN_MAP_STYLE_JSON)
        } catch (_: Exception) {
            null
        }
    }

    val mapProperties = remember(mapStyleOptions) {
        MapProperties(
            mapStyleOptions = mapStyleOptions,
            minZoomPreference = 10f,
            maxZoomPreference = 18f,
            isBuildingEnabled = false,
            isTrafficEnabled = false,
            isIndoorEnabled = false,
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(target = BERLIN_CENTER, zoom = 12f)
    }

    val districts = remember(allRestaurants) {
        allRestaurants.map { it.address.district }.distinct().sorted()
    }
    val activeFilterCount = (if (selectedCuisine != null) 1 else 0) +
        (if (selectedDistrict != null) 1 else 0)

    val restaurantsFiltered = remember(allRestaurants, selectedCuisine, selectedDistrict) {
        allRestaurants.filter { r ->
            (selectedCuisine == null || r.cuisineType == selectedCuisine) &&
                (selectedDistrict == null || r.address.district == selectedDistrict)
        }
    }

    // Restaurants within the current map viewport — re-derives on camera movement.
    val visibleRestaurants by remember(restaurantsFiltered) {
        derivedStateOf {
            cameraPositionState.position // subscribe to camera changes
            val bounds = cameraPositionState.projection?.visibleBounds
            if (bounds == null) restaurantsFiltered
            else restaurantsFiltered.filter { r ->
                r.latitude in bounds.southwest.latitude..bounds.northeast.latitude &&
                    r.longitude in bounds.southwest.longitude..bounds.northeast.longitude
            }
        }
    }

    val listState = rememberLazyListState()
    val locationRequester = rememberLocationRequester()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var locating by remember { mutableStateOf(false) }

    // Cluster nearby restaurants when their on-screen positions would overlap.
    val density = LocalDensity.current
    val clusterRadiusPx = with(density) { 72.dp.toPx() }

    // Cluster only when the camera settles. Re-rasterizing MarkerComposables
    // every frame during pan/zoom is the real source of jank.
    var clusters by remember(restaurantsFiltered) {
        mutableStateOf(
            restaurantsFiltered.map {
                RestaurantCluster(listOf(it), LatLng(it.latitude, it.longitude))
            }
        )
    }
    LaunchedEffect(restaurantsFiltered, clusterRadiusPx) {
        snapshotFlow {
            // Recompute when the map idles at a new position. isMoving=true
            // values pass through but the filter below drops them.
            cameraPositionState.isMoving to cameraPositionState.position
        }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, pos) -> pos }
            .distinctUntilChanged()
            .collect {
                val projection = cameraPositionState.projection
                if (projection == null) {
                    clusters = restaurantsFiltered.map {
                        RestaurantCluster(listOf(it), LatLng(it.latitude, it.longitude))
                    }
                    return@collect
                }
                // Viewport prefilter — drop restaurants outside the visible region
                // (with a 20% margin so markers near the edges still cluster
                // correctly with on-screen ones). Skips toScreenLocation calls
                // on hidden points entirely.
                val bounds = projection.visibleBounds
                val latPad = (bounds.northeast.latitude - bounds.southwest.latitude) * 0.2
                val lngPad = (bounds.northeast.longitude - bounds.southwest.longitude) * 0.2
                val minLat = bounds.southwest.latitude - latPad
                val maxLat = bounds.northeast.latitude + latPad
                val minLng = bounds.southwest.longitude - lngPad
                val maxLng = bounds.northeast.longitude + lngPad
                val candidates = restaurantsFiltered.filter { r ->
                    r.latitude in minLat..maxLat && r.longitude in minLng..maxLng
                }
                clusters = clusterByScreenDistance(candidates, projection, clusterRadiusPx)
            }
    }

    LaunchedEffect(selectedRestaurantId, visibleRestaurants) {
        val id = selectedRestaurantId ?: return@LaunchedEffect
        val idx = visibleRestaurants.indexOfFirst { it.id == id }
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = { selectedRestaurantId = null },
            onMapLoaded = { mapLoaded = true },
        ) {
            clusters.forEach { cluster ->
                if (cluster.items.size == 1) {
                    val restaurant = cluster.items.first()
                    val selected = restaurant.id == selectedRestaurantId
                    val state = rememberUpdatedMarkerState(
                        position = LatLng(restaurant.latitude, restaurant.longitude),
                    )
                    MarkerComposable(
                        keys = arrayOf<Any>(restaurant.id, selected),
                        state = state,
                        title = restaurant.name.zh,
                        snippet = restaurant.name.en,
                        anchor = Offset(0.5f, 1f),
                        onClick = {
                            selectedRestaurantId = restaurant.id
                            true
                        },
                    ) {
                        MiniRestaurantCard(restaurant, selected)
                    }
                } else {
                    val currentZoom = cameraPositionState.position.zoom
                    val targetZoom = (currentZoom + 2f).coerceAtMost(18f)
                    val state = rememberUpdatedMarkerState(position = cluster.center)
                    MarkerComposable(
                        keys = arrayOf<Any>(cluster.items.size, cluster.center),
                        state = state,
                        anchor = Offset(0.5f, 0.5f),
                        onClick = {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(cluster.center, targetZoom),
                                )
                            }
                            true
                        },
                    ) {
                        ClusterBadge(cluster.items.size)
                    }
                }
            }

            myLocation?.let { loc ->
                val state = rememberMarkerState(key = "__my_location__", position = loc)
                Marker(
                    state = state,
                    icon = myLocationIcon,
                    anchor = Offset(0.5f, 0.5f),
                    zIndex = 10f,
                )
            }
        }

        // Top: floating search bar only — filtering is now in the bottom-left FAB.
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

        // Bottom: horizontal cards for restaurants in the current viewport
        if (visibleRestaurants.isNotEmpty()) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visibleRestaurants, key = { it.id }) { restaurant ->
                    NearbyCard(
                        restaurant = restaurant,
                        selected = restaurant.id == selectedRestaurantId,
                        onClick = { onNavigateDetail(restaurant.id) },
                    )
                }
            }
        }

        // My-location FAB — anchored above the card row, branded red.
        val cardOffset = if (visibleRestaurants.isNotEmpty()) 132.dp else 24.dp
        SmallFloatingActionButton(
            onClick = {
                if (locating) return@SmallFloatingActionButton
                scope.launch {
                    locating = true
                    val result = locationRequester.request()
                    locating = false
                    when (result) {
                        is LocationResult.Success -> {
                            val target = LatLng(result.latitude, result.longitude)
                            myLocation = target
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(target, 15f),
                            )
                        }
                        LocationResult.PermissionDenied -> snackbarHostState.showSnackbar(
                            "Location permission denied",
                        )
                        LocationResult.Unavailable -> snackbarHostState.showSnackbar(
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
            if (locating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Filled.MyLocation, contentDescription = "Locate me")
            }
        }

        // Filter FAB — bottom-left, opens a multi-section sheet.
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(text = "$activeFilterCount") }
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
            districts = districts,
            selectedCuisine = selectedCuisine,
            selectedDistrict = selectedDistrict,
            onCuisineSelected = { selectedCuisine = it; selectedRestaurantId = null },
            onDistrictSelected = { selectedDistrict = it; selectedRestaurantId = null },
            onReset = {
                selectedCuisine = null
                selectedDistrict = null
                selectedRestaurantId = null
            },
            onDismiss = { filterSheetOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    districts: List<String>,
    selectedCuisine: CuisineType?,
    selectedDistrict: String?,
    onCuisineSelected: (CuisineType?) -> Unit,
    onDistrictSelected: (String?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.filter_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReset) {
                    Text(stringResource(Res.string.filter_reset))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.filter_cuisine),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = selectedCuisine == null,
                    onClick = { onCuisineSelected(null) },
                    label = { Text(stringResource(Res.string.all)) },
                )
                CuisineType.entries.forEach { cuisine ->
                    FilterChip(
                        selected = selectedCuisine == cuisine,
                        onClick = { onCuisineSelected(cuisine) },
                        label = { Text(cuisineDisplayName(cuisine)) },
                    )
                }
            }

            if (districts.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(Res.string.filter_district),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilterChip(
                        selected = selectedDistrict == null,
                        onClick = { onDistrictSelected(null) },
                        label = { Text(stringResource(Res.string.all)) },
                    )
                    districts.forEach { district ->
                        FilterChip(
                            selected = selectedDistrict == district,
                            onClick = { onDistrictSelected(district) },
                            label = { Text(district) },
                        )
                    }
                }
            }
        }
    }
}

data class RestaurantCluster(
    val items: List<Restaurant>,
    val center: LatLng,
)

/**
 * Spatial-grid clustering — O(n) average case.
 *
 * The screen is divided into square cells of side [radiusPx]. For each point,
 * any cluster within [radiusPx] of it must have its anchor in one of the 9
 * surrounding cells. We probe only those cells instead of every existing
 * cluster, eliminating the O(n × k) blowup the naive algorithm has when many
 * markers are visible.
 */
private fun clusterByScreenDistance(
    items: List<Restaurant>,
    projection: Projection,
    radiusPx: Float,
): List<RestaurantCluster> {
    if (items.isEmpty()) return emptyList()

    val cellSize = radiusPx
    val r2 = radiusPx * radiusPx
    val grid = HashMap<Long, MutableList<Int>>(items.size)
    val anchorsX = FloatArray(items.size)
    val anchorsY = FloatArray(items.size)
    val buckets = ArrayList<MutableList<Restaurant>>(items.size)

    for (r in items) {
        val pt = projection.toScreenLocation(LatLng(r.latitude, r.longitude))
        val cx = (pt.x / cellSize).toInt()
        val cy = (pt.y / cellSize).toInt()

        var matched = -1
        outer@ for (dx in -1..1) {
            for (dy in -1..1) {
                val key = cellKey(cx + dx, cy + dy)
                val candidates = grid[key] ?: continue
                for (idx in candidates) {
                    val ax = anchorsX[idx]
                    val ay = anchorsY[idx]
                    val ddx = pt.x - ax
                    val ddy = pt.y - ay
                    if (ddx * ddx + ddy * ddy <= r2) {
                        matched = idx
                        break@outer
                    }
                }
            }
        }

        if (matched >= 0) {
            buckets[matched].add(r)
        } else {
            val newIdx = buckets.size
            buckets.add(mutableListOf(r))
            anchorsX[newIdx] = pt.x
            anchorsY[newIdx] = pt.y
            grid.getOrPut(cellKey(cx, cy)) { ArrayList(2) }.add(newIdx)
        }
    }

    return buckets.map { bucket ->
        if (bucket.size == 1) {
            val only = bucket[0]
            RestaurantCluster(bucket, LatLng(only.latitude, only.longitude))
        } else {
            var sumLat = 0.0
            var sumLng = 0.0
            for (b in bucket) { sumLat += b.latitude; sumLng += b.longitude }
            RestaurantCluster(bucket, LatLng(sumLat / bucket.size, sumLng / bucket.size))
        }
    }
}

private fun cellKey(cx: Int, cy: Int): Long =
    (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)

@Composable
private fun MiniRestaurantCard(restaurant: Restaurant, selected: Boolean) {
    val container = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface
    val onContainer = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val accent = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.primary
    val border = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accent, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = restaurant.name.zh,
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = cuisineDisplayName(restaurant.cuisineType),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) onContainer.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ClusterBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else "$count",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun NearbyCard(
    restaurant: Restaurant,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = restaurant.name.zh,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = restaurant.name.en,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${restaurant.address.district} · ${cuisineDisplayName(restaurant.cuisineType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${restaurant.viewCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
