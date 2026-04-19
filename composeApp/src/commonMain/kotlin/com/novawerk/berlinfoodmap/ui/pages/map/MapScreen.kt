package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.restaurant.CuisineType
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.CuisineChips
import eu.buney.maps.GoogleMap
import eu.buney.maps.LatLng
import eu.buney.maps.LatLngBounds
import eu.buney.maps.MapProperties
import eu.buney.maps.MapStyleOptions
import eu.buney.maps.MapUiSettings
import eu.buney.maps.Marker
import eu.buney.maps.CameraPosition
import eu.buney.maps.rememberCameraPositionState
import eu.buney.maps.rememberMarkerState
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.search_hint

// Berlin bounding box — restricts camera to greater Berlin area
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
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

    LaunchedEffect(Unit) {
        allRestaurants = repository.getAll()
        loading = false
    }

    val filteredRestaurants = remember(allRestaurants, selectedCuisine) {
        if (selectedCuisine == null) allRestaurants
        else allRestaurants.filter { it.cuisineType == selectedCuisine }
    }

    // Marker icons created after map loads (BitmapDescriptorFactory needs map init)
    var mapLoaded by remember { mutableStateOf(false) }
    val markerIcons = remember(mapLoaded) {
        if (mapLoaded) {
            try {
                CuisineType.entries.associateWith { cuisineMarkerIcon(it) }
            } catch (_: Exception) {
                emptyMap()
            }
        } else emptyMap()
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Map style — clean, no default POI markers
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
            minZoomPreference = 10f,  // Can't zoom out beyond Berlin
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

    Box(modifier = Modifier.fillMaxSize()) {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition(target = BERLIN_CENTER, zoom = 12f)
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapClick = { selectedRestaurant = null },
            onMapLoaded = { mapLoaded = true },
        ) {
            filteredRestaurants.forEach { restaurant ->
                val markerState = rememberMarkerState(
                    key = restaurant.id,
                    position = LatLng(restaurant.latitude, restaurant.longitude),
                )
                Marker(
                    state = markerState,
                    title = restaurant.name.zh,
                    snippet = restaurant.name.en,
                    icon = markerIcons[restaurant.cuisineType] ?: markerIcons.values.firstOrNull(),
                    onClick = {
                        selectedRestaurant = restaurant
                        true
                    },
                )
            }
        }

        // Search bar overlay at top
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            placeholder = { Text(stringResource(Res.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
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

        // Bottom panel: selected restaurant + cuisine chips
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            // Selected restaurant preview card
            selectedRestaurant?.let { restaurant ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateDetail(restaurant.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = restaurant.name.zh,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = restaurant.name.en,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${restaurant.address.district} · ${restaurant.visitCount} visited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            ) {
                CuisineChips(
                    selected = selectedCuisine,
                    onSelected = {
                        selectedCuisine = it
                        selectedRestaurant = null
                    },
                )
            }
        }
    }
}
