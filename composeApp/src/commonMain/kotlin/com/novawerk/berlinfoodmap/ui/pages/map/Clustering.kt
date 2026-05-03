package com.novawerk.berlinfoodmap.ui.pages.map

import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import eu.buney.maps.LatLng
import eu.buney.maps.Projection
import eu.buney.maps.ScreenPoint

internal data class RestaurantCluster(
    val items: List<Restaurant>,
    val center: LatLng,
)

/**
 * Structural equality on cluster *grouping* — two cluster lists are
 * equivalent if the same restaurants are grouped into the same buckets,
 * regardless of cluster order or centroid coordinates.
 */
internal fun sameClusterGrouping(
    a: List<RestaurantCluster>,
    b: List<RestaurantCluster>,
): Boolean {
    if (a.size != b.size) return false
    val aSets = a.mapTo(HashSet(a.size)) { it.items.mapTo(HashSet(it.items.size)) { r -> r.id } }
    val bSets = b.mapTo(HashSet(b.size)) { it.items.mapTo(HashSet(it.items.size)) { r -> r.id } }
    return aSets == bSets
}

/**
 * Project every restaurant's lat/lng to screen pixels.
 *
 * Must run on the main thread because the Maps SDK's projection isn't
 * documented as thread-safe. Cheap (~tens of microseconds per call) but
 * crosses JNI, so doing all projections in one tight loop here keeps the
 * total cost bounded.
 */
internal fun projectAll(
    restaurants: List<Restaurant>,
    projection: Projection,
): List<ScreenPoint> = restaurants.map { r ->
    projection.toScreenLocation(LatLng(r.latitude, r.longitude))
}

/**
 * Spatial-grid clustering on pre-projected screen coords — O(n) average.
 *
 * Pure math, no SDK access, safe to call from any dispatcher. Pair with
 * [projectAll] (which must run on Main) to move clustering off the UI
 * thread.
 *
 * The screen is divided into square cells of side [radiusPx]. For each
 * point, any cluster within [radiusPx] of it must have its anchor in one
 * of the 9 surrounding cells. We probe only those cells instead of every
 * existing cluster, eliminating the O(n × k) blowup the naive algorithm
 * has when many markers are visible.
 */
internal fun clusterFromProjected(
    restaurants: List<Restaurant>,
    projected: List<ScreenPoint>,
    radiusPx: Float,
): List<RestaurantCluster> {
    require(restaurants.size == projected.size) {
        "restaurants and projected must have the same size"
    }
    if (restaurants.isEmpty()) return emptyList()

    val cellSize = radiusPx
    val r2 = radiusPx * radiusPx
    val grid = HashMap<Long, MutableList<Int>>(restaurants.size)
    val anchorsX = FloatArray(restaurants.size)
    val anchorsY = FloatArray(restaurants.size)
    val buckets = ArrayList<MutableList<Restaurant>>(restaurants.size)

    for (i in restaurants.indices) {
        val r = restaurants[i]
        val pt = projected[i]
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
