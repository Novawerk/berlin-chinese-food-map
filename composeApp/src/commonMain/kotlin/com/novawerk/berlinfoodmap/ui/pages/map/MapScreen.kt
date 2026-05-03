package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.restaurant.Tag
import com.novawerk.berlinfoodmap.domain.restaurant.previewImageUrl
import com.novawerk.berlinfoodmap.ui.components.TagChipsGroup
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
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
import berlinfoodmap.composeapp.generated.resources.filter_district
import berlinfoodmap.composeapp.generated.resources.filter_reset
import berlinfoodmap.composeapp.generated.resources.filter_title
import berlinfoodmap.composeapp.generated.resources.map_loading
import berlinfoodmap.composeapp.generated.resources.search_hint
import berlinfoodmap.composeapp.generated.resources.tag_family_format
import berlinfoodmap.composeapp.generated.resources.tag_family_regional

private val BERLIN_BOUNDS = LatLngBounds(
    southwest = LatLng(52.33, 13.08),
    northeast = LatLng(52.68, 13.76),
)
private val BERLIN_CENTER = LatLng(52.52, 13.405)

// Cover-thumbnail decode sizes for the two on-map surfaces. Each is the
// rendered side length × 4 (xxxhdpi safety margin). The source photos
// hosted in Firebase Storage are up to 1600 px wide; coil's `.size()`
// downsamples on decode, so passing these saves memory + CPU per marker.
private const val MARKER_COVER_PX = 128   // MiniRestaurantCard at 32 dp
private const val NEARBY_COVER_PX = 192   // NearbyCard at 44 dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    repository: RestaurantRepository,
    onNavigateDetail: (String) -> Unit,
    onNavigateSearch: () -> Unit,
) {
    // Collect via Flow so persistent-cache results paint immediately and the
    // server refresh follows asynchronously — no full-screen blocking spinner.
    val allRestaurants by remember(repository) {
        repository.observeAll()
    }.collectAsState(initial = emptyList())

    var selectedTags by remember { mutableStateOf<Set<Tag>>(emptySet()) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var filterSheetOpen by remember { mutableStateOf(false) }

    var mapLoaded by remember { mutableStateOf(false) }
    val myLocationIcon = remember(mapLoaded) {
        if (mapLoaded) {
            try { myLocationDotIcon() } catch (_: Exception) { null }
        } else null
    }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }

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
    val activeFilterCount = selectedTags.size +
        (if (selectedDistrict != null) 1 else 0)

    val restaurantsFiltered = remember(allRestaurants, selectedTags, selectedDistrict) {
        allRestaurants.filter { r ->
            val matchesTags = selectedTags.isEmpty() || r.tags.any { it in selectedTags }
            val matchesDistrict = selectedDistrict == null || r.address.district == selectedDistrict
            matchesTags && matchesDistrict
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
    //
    // No key on the remember — keeping the previous clusters around while the
    // LaunchedEffect below recomputes them avoids a flicker frame where every
    // restaurant in the new filter renders as its own (un-clustered) marker.
    // For 200 markers each carrying a label card, that frame costs >100ms.
    var clusters by remember { mutableStateOf<List<RestaurantCluster>>(emptyList()) }
    LaunchedEffect(restaurantsFiltered, clusterRadiusPx) {
        // Recompute synchronously on filter change so the new cluster set
        // lands in the same state-batch instead of waiting for the next
        // camera-idle event.
        cameraPositionState.projection?.let { projection ->
            clusters = clusterForViewport(restaurantsFiltered, projection, clusterRadiusPx)
        }

        snapshotFlow {
            // Recompute when the map idles at a new position. isMoving=true
            // values pass through but the filter below drops them.
            cameraPositionState.isMoving to cameraPositionState.position
        }
            .filter { (isMoving, _) -> !isMoving }
            .map { (_, pos) -> pos }
            .distinctUntilChanged()
            .collect {
                val projection = cameraPositionState.projection ?: return@collect
                clusters = clusterForViewport(restaurantsFiltered, projection, clusterRadiusPx)
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = { mapLoaded = true },
        ) {
            clusters.forEach { cluster ->
                if (cluster.items.size == 1) {
                    val restaurant = cluster.items.first()
                    val state = rememberUpdatedMarkerState(
                        position = LatLng(restaurant.latitude, restaurant.longitude),
                    )
                    // Preload the cover thumb. The marker is rasterised once
                    // per `keys` change, so we feed `coverReady` as a key —
                    // when the image arrives the marker re-rasterises with
                    // the loaded photo.
                    //
                    // `allowHardware(false)`: MarkerComposable rasterises
                    // onto a software canvas; coil's default hardware bitmap
                    // crashes that path.
                    //
                    // `size(MARKER_COVER_PX)`: marker thumbnail is 32dp.
                    // Decoding the source 1600px JPEG at full size is
                    // hundreds of KB of memory per marker; 128px covers
                    // 4× density screens and downsamples on decode.
                    val coverUrl = restaurant.previewImageUrl()
                    val platformContext = LocalPlatformContext.current
                    val coverPainter = coverUrl?.let {
                        rememberAsyncImagePainter(
                            ImageRequest.Builder(platformContext)
                                .data(it)
                                .allowHardware(false)
                                .size(MARKER_COVER_PX, MARKER_COVER_PX)
                                .build(),
                        )
                    }
                    val coverReady = coverPainter
                        ?.state
                        ?.collectAsState()
                        ?.value is AsyncImagePainter.State.Success
                    MarkerComposable(
                        keys = arrayOf<Any>(restaurant.id, coverReady),
                        state = state,
                        title = restaurant.name.zh,
                        snippet = restaurant.name.en,
                        anchor = Offset(0.5f, 1f),
                        onClick = {
                            onNavigateDetail(restaurant.id)
                            true
                        },
                    ) {
                        MiniRestaurantCard(
                            restaurant = restaurant,
                            coverPainter = coverPainter.takeIf { coverReady },
                            hasCoverUrl = coverUrl != null,
                        )
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

        // Loading mask covers the empty/grey map until the SDK signals the
        // first frame is rendered (`onMapLoaded`). Fades out so the transition
        // doesn't feel abrupt.
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
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(Res.string.map_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                        onClick = { onNavigateDetail(restaurant.id) },
                    )
                }
            }
        }

        // My-location FAB — anchored above the card row, branded red.
        // 76dp clears the trimmed NearbyCard (44dp cover + 12dp card padding +
        // 12dp LazyRow bottom inset) by ~8dp. When no card row is showing
        // (empty viewport) the FABs sit close to the bottom edge.
        val cardOffset = if (visibleRestaurants.isNotEmpty()) 76.dp else 24.dp
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
            selectedTags = selectedTags,
            selectedDistrict = selectedDistrict,
            onTagToggle = { tag ->
                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
            },
            onTagsClear = { selectedTags = emptySet() },
            onDistrictSelected = { selectedDistrict = it },
            onReset = {
                selectedTags = emptySet()
                selectedDistrict = null
            },
            onDismiss = { filterSheetOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    districts: List<String>,
    selectedTags: Set<Tag>,
    selectedDistrict: String?,
    onTagToggle: (Tag) -> Unit,
    onTagsClear: () -> Unit,
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
            TagChipsGroup(
                selected = selectedTags,
                onToggle = onTagToggle,
                onClear = onTagsClear,
                regionalLabel = stringResource(Res.string.tag_family_regional),
                formatLabel = stringResource(Res.string.tag_family_format),
            )

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
 * Viewport-prefilter + spatial clustering. Points outside the visible region
 * (with a 20% margin so edge markers still cluster correctly with on-screen
 * neighbours) are skipped before [clusterByScreenDistance] runs, avoiding
 * `toScreenLocation` calls on points the user can't see.
 */
private fun clusterForViewport(
    restaurants: List<Restaurant>,
    projection: Projection,
    radiusPx: Float,
): List<RestaurantCluster> {
    val bounds = projection.visibleBounds
    val latPad = (bounds.northeast.latitude - bounds.southwest.latitude) * 0.2
    val lngPad = (bounds.northeast.longitude - bounds.southwest.longitude) * 0.2
    val minLat = bounds.southwest.latitude - latPad
    val maxLat = bounds.northeast.latitude + latPad
    val minLng = bounds.southwest.longitude - lngPad
    val maxLng = bounds.northeast.longitude + lngPad
    val candidates = restaurants.filter { r ->
        r.latitude in minLat..maxLat && r.longitude in minLng..maxLng
    }
    return clusterByScreenDistance(candidates, projection, radiusPx)
}

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

/**
 * Pill-shaped marker label. Leading thumbnail behaviour:
 *
 *  - cover loaded → render the photo
 *  - cover loading (URL exists, painter not ready) → empty placeholder
 *    so the marker re-rasterises to the photo without flashing an icon
 *  - no cover URL at all → fallback Material restaurant icon
 *
 * Subtitle line shows all tags joined with `·`, with a star prefix for
 * editorially-featured restaurants.
 */
@Composable
private fun MiniRestaurantCard(
    restaurant: Restaurant,
    coverPainter: Painter?,
    hasCoverUrl: Boolean,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (coverPainter != null) {
                Image(
                    painter = coverPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                )
            } else if (hasCoverUrl) {
                // URL is set but the photo hasn't decoded yet — empty
                // circle. Once coverReady flips, the marker re-rasterises
                // and the photo lands without an interim icon flash.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (restaurant.featured) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = restaurant.name.zh,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (restaurant.tags.isNotEmpty()) {
                    val labels = restaurant.tags.map { tagDisplayName(it) }
                    Text(
                        text = labels.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
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
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Square cover thumbnail. Generic Material icon as fallback so a
            // freshly added restaurant without a placeId still renders cleanly.
            // ImageRequest.size() decodes the source 1600px JPEG down to
            // ~192px (4× the rendered 44dp side) — saves ~95% of the bitmap
            // memory when 30+ NearbyCards scroll past in the viewport row.
            val coverUrl = restaurant.previewImageUrl()
            val ctx = LocalPlatformContext.current
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(coverUrl)
                            .size(NEARBY_COVER_PX, NEARBY_COVER_PX)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restaurant.name.zh,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (restaurant.featured) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = restaurant.name.en,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (restaurant.tags.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    val tagLine = restaurant.tags.map { tagDisplayName(it) }.joinToString(" · ")
                    Text(
                        text = tagLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
