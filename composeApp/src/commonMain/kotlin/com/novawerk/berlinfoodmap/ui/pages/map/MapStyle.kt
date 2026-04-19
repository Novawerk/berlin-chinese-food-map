package com.novawerk.berlinfoodmap.ui.pages.map

/**
 * Custom Google Maps style JSON — clean, minimal style for a food map.
 * Hides default POIs (businesses, transit), tones down road labels,
 * and uses a warm neutral palette.
 *
 * Generated via https://mapstyle.withgoogle.com/
 */
val BERLIN_MAP_STYLE_JSON = """
[
  {
    "featureType": "poi.business",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "poi.attraction",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "poi.government",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "poi.medical",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "poi.place_of_worship",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "poi.school",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "poi.sports_complex",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "transit",
    "stylers": [{ "visibility": "simplified" }]
  },
  {
    "featureType": "transit.station.bus",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "road",
    "elementType": "labels.icon",
    "stylers": [{ "visibility": "off" }]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [{ "color": "#e8d6c8" }]
  },
  {
    "featureType": "road.arterial",
    "elementType": "geometry",
    "stylers": [{ "color": "#f0e6dc" }]
  },
  {
    "featureType": "road.local",
    "elementType": "geometry",
    "stylers": [{ "color": "#f7f2ed" }]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [{ "color": "#c9daf8" }]
  },
  {
    "featureType": "landscape.natural",
    "elementType": "geometry",
    "stylers": [{ "color": "#e8f0e0" }]
  },
  {
    "featureType": "landscape.man_made",
    "elementType": "geometry",
    "stylers": [{ "color": "#f5f0eb" }]
  },
  {
    "featureType": "poi.park",
    "elementType": "geometry",
    "stylers": [{ "color": "#d4e8c4" }]
  },
  {
    "featureType": "poi.park",
    "elementType": "labels",
    "stylers": [{ "visibility": "simplified" }]
  }
]
""".trimIndent()
