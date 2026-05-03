package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.buney.maps.CameraPosition
import eu.buney.maps.LatLng
import eu.buney.maps.MapProperties
import eu.buney.maps.MapStyleOptions
import eu.buney.maps.MapUiSettings

// City-centre default for the camera when MapScreen first composes — falls
// roughly on Brandenburger Tor at zoom 12, framing the dense ring of
// inner-city Bezirke without panning.
private val BERLIN_CENTER = LatLng(52.52, 13.405)
internal val DEFAULT_BERLIN_CAMERA = CameraPosition(target = BERLIN_CENTER, zoom = 12f)

/**
 * Static GoogleMap configuration for the Berlin map screen.
 *
 *  - Style is the brandbook desaturated palette (see [BERLIN_MAP_STYLE_JSON]).
 *  - Zoom is clamped to 10..18: below 10 the city falls off-screen and the
 *    cluster math gets noisy; above 18 the Maps SDK starts asking for
 *    detail it doesn't have for our coverage area.
 *  - Buildings / traffic / indoor maps are off so the map reads cleaner
 *    against the warm cream surface.
 *  - Rotation and tilt gestures are off — restaurant pins are billboards,
 *    they don't make sense at an angle.
 */
internal data class BerlinMapConfig(
    val properties: MapProperties,
    val uiSettings: MapUiSettings,
)

@Composable
internal fun rememberBerlinMapConfig(): BerlinMapConfig {
    val style = remember {
        try {
            MapStyleOptions.fromJson(BERLIN_MAP_STYLE_JSON)
        } catch (_: Exception) {
            null
        }
    }
    val properties = remember(style) {
        MapProperties(
            mapStyleOptions = style,
            minZoomPreference = 10f,
            maxZoomPreference = 18f,
            isBuildingEnabled = false,
            isTrafficEnabled = false,
            isIndoorEnabled = false,
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
        )
    }
    return remember(properties, uiSettings) {
        BerlinMapConfig(properties, uiSettings)
    }
}

/**
 * Pinwo map style — desaturated cream/stone base from the brandbook so brand-red
 * pins read as the dominant element. Hides default POIs and tones road labels.
 */
val BERLIN_MAP_STYLE_JSON = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#ffffff" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#514646" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#ffffff" }] },
  { "featureType": "poi.business", "stylers": [{ "visibility": "off" }] },
  { "featureType": "poi.attraction", "stylers": [{ "visibility": "off" }] },
  { "featureType": "poi.government", "stylers": [{ "visibility": "off" }] },
  { "featureType": "poi.medical", "stylers": [{ "visibility": "off" }] },
  { "featureType": "poi.place_of_worship", "stylers": [{ "visibility": "off" }] },
  { "featureType": "poi.school", "stylers": [{ "visibility": "off" }] },
  { "featureType": "poi.sports_complex", "stylers": [{ "visibility": "off" }] },
  { "featureType": "transit", "stylers": [{ "visibility": "simplified" }] },
  { "featureType": "transit.station.bus", "stylers": [{ "visibility": "off" }] },
  { "featureType": "road", "elementType": "labels.icon", "stylers": [{ "visibility": "off" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#e8e4dd" }] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#c6c2bd" }] },
  { "featureType": "road.arterial", "elementType": "geometry", "stylers": [{ "color": "#efebe2" }] },
  { "featureType": "road.local", "elementType": "geometry", "stylers": [{ "color": "#f5f5f5" }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#d6e0e8" }] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#7a8aa0" }] },
  { "featureType": "landscape.natural", "elementType": "geometry", "stylers": [{ "color": "#fafafa" }] },
  { "featureType": "landscape.man_made", "elementType": "geometry", "stylers": [{ "color": "#ffffff" }] },
  { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#f0ede5" }] },
  { "featureType": "poi.park", "elementType": "labels", "stylers": [{ "visibility": "simplified" }] },
  { "featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{ "color": "#c6c2bd" }] }
]
""".trimIndent()
