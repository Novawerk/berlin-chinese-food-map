package com.novawerk.berlinfoodmap.ui.pages.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.novawerk.berlinfoodmap.ui.theme.LocalIsDarkTheme
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
 *  - Style follows the active light/dark theme (see [BERLIN_MAP_STYLE_JSON_LIGHT]
 *    and [BERLIN_MAP_STYLE_JSON_DARK]).
 *  - Zoom is clamped to 10..18: below 10 the city falls off-screen and the
 *    cluster math gets noisy; above 18 the Maps SDK starts asking for
 *    detail it doesn't have for our coverage area.
 *  - Buildings / traffic / indoor maps are off so the map reads cleaner
 *    against the warm cream / dark surface.
 *  - Rotation and tilt gestures are off — restaurant pins are billboards,
 *    they don't make sense at an angle.
 */
internal data class BerlinMapConfig(
    val properties: MapProperties,
    val uiSettings: MapUiSettings,
)

@Composable
internal fun rememberBerlinMapConfig(): BerlinMapConfig {
    val isDark = LocalIsDarkTheme.current
    val style = remember(isDark) {
        val json = if (isDark) BERLIN_MAP_STYLE_JSON_DARK else BERLIN_MAP_STYLE_JSON_LIGHT
        try {
            MapStyleOptions.fromJson(json)
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
 * Pinwo light map style — desaturated cream/stone base from the brandbook so
 * brand-red pins read as the dominant element. Hides default POIs and tones
 * road labels.
 *
 * `landscape.man_made` is intentionally a hair darker than the cream surface
 * so building footprints stay legible at zoom 18 — without that contrast,
 * man-made parcels and `road.local` blend into a single white field.
 */
val BERLIN_MAP_STYLE_JSON_LIGHT = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#FFFCF8" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#514646" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#FFFCF8" }] },
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
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#E8E4DD" }] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#C6C2BD" }] },
  { "featureType": "road.arterial", "elementType": "geometry", "stylers": [{ "color": "#EFEBE2" }] },
  { "featureType": "road.local", "elementType": "geometry", "stylers": [{ "color": "#FFFFFF" }] },
  { "featureType": "road.local", "elementType": "geometry.stroke", "stylers": [{ "color": "#E0DCD4" }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#D6E0E8" }] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#7A8AA0" }] },
  { "featureType": "landscape.natural", "elementType": "geometry", "stylers": [{ "color": "#F6F1E8" }] },
  { "featureType": "landscape.man_made", "elementType": "geometry", "stylers": [{ "color": "#EDE8DE" }] },
  { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#DDE3CE" }] },
  { "featureType": "poi.park", "elementType": "labels", "stylers": [{ "visibility": "simplified" }] },
  { "featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{ "color": "#C6C2BD" }] }
]
""".trimIndent()

/**
 * Pinwo dark map style — anchored on the dark `surface` (`#221C1C`) with
 * slightly lighter parcels and roads so layered features stay legible. Same
 * POI / transit visibility rules as the light style.
 */
val BERLIN_MAP_STYLE_JSON_DARK = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#221C1C" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#C6C2BD" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#1A1515" }] },
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
  { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#9A8F8F" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#4A3F3F" }] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#5C5050" }] },
  { "featureType": "road.arterial", "elementType": "geometry", "stylers": [{ "color": "#3A3232" }] },
  { "featureType": "road.local", "elementType": "geometry", "stylers": [{ "color": "#2F2828" }] },
  { "featureType": "road.local", "elementType": "geometry.stroke", "stylers": [{ "color": "#3A3232" }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#0F1A24" }] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#5A6B80" }] },
  { "featureType": "landscape.natural", "elementType": "geometry", "stylers": [{ "color": "#1E1818" }] },
  { "featureType": "landscape.man_made", "elementType": "geometry", "stylers": [{ "color": "#2B2424" }] },
  { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#1F2A1A" }] },
  { "featureType": "poi.park", "elementType": "labels", "stylers": [{ "visibility": "simplified" }] },
  { "featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{ "color": "#514646" }] }
]
""".trimIndent()
