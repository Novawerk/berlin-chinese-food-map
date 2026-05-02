package com.novawerk.berlinfoodmap.ui.pages.map

/**
 * Pinwo map style — desaturated cream/stone base from the brandbook so brand-red
 * pins read as the dominant element. Hides default POIs and tones road labels.
 */
val BERLIN_MAP_STYLE_JSON = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#f8f1de" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#514646" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#f8f1de" }] },
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
  { "featureType": "road.local", "elementType": "geometry", "stylers": [{ "color": "#ffffff" }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#d6e0e8" }] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#7a8aa0" }] },
  { "featureType": "landscape.natural", "elementType": "geometry", "stylers": [{ "color": "#f3ead5" }] },
  { "featureType": "landscape.man_made", "elementType": "geometry", "stylers": [{ "color": "#f8f1de" }] },
  { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#ebdfc6" }] },
  { "featureType": "poi.park", "elementType": "labels", "stylers": [{ "visibility": "simplified" }] },
  { "featureType": "administrative", "elementType": "geometry.stroke", "stylers": [{ "color": "#c6c2bd" }] }
]
""".trimIndent()
