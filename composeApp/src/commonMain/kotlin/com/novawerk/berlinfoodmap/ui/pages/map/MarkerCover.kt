package com.novawerk.berlinfoodmap.ui.pages.map

import coil3.Image

/**
 * Outcome of attempting to load the cover photo for one marker.
 *
 * The marker layer treats *settled* (Loaded / Failed / NoUrl) uniformly
 * when deciding whether to emit. Map entry presence in the cover cache
 * means "load was attempted" (or there was no URL to begin with); an
 * absent entry means "still in flight".
 */
internal sealed interface MarkerCover {
    data object NoUrl : MarkerCover
    data object Failed : MarkerCover
    data class Loaded(val image: Image) : MarkerCover
}
