package com.novawerk.berlinfoodmap.ui.pages.map

import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import eu.buney.maps.LatLng
import eu.buney.maps.Projection
import eu.buney.maps.ScreenPoint

/**
 * Project every restaurant's lat/lng to screen pixels.
 *
 * Must run on the main thread because the Maps SDK's projection isn't
 * documented as thread-safe. Cheap (~tens of microseconds per call) but
 * crosses JNI, so doing all projections in one tight loop here keeps
 * the total cost bounded.
 *
 * Why the [projectionPixelScale] multiplier: the kmp-maps `Projection`
 * doc claims the returned `ScreenPoint` is in pixels, but the iOS
 * implementation actually returns CGPoint values which are in *points*
 * (1pt = scale px on @2x/@3x devices). Android's implementation uses
 * `Point` which is in pixels. We caller-side normalise iOS's coords
 * into pixels so downstream overlap math (computed via `Dp.toPx()`,
 * also pixels) lines up across platforms — without this fix iOS
 * over-collapsed everything because the radius was effectively 3× the
 * projected coord scale.
 */
internal fun projectAll(
    restaurants: List<Restaurant>,
    projection: Projection,
): List<ScreenPoint> {
    val scale = projectionPixelScale()
    return restaurants.map { r ->
        val pt = projection.toScreenLocation(LatLng(r.latitude, r.longitude))
        if (scale == 1f) pt else ScreenPoint(pt.x * scale, pt.y * scale)
    }
}

/**
 * Multiplier to convert this platform's `Projection.toScreenLocation`
 * output into actual pixel coordinates. Android = 1 (already pixels);
 * iOS = `UIScreen.mainScreen.scale` (CGPoint → px).
 */
internal expect fun projectionPixelScale(): Float

/**
 * Spatial-grid overlap detection on pre-projected screen coords —
 * O(n) average. Returns the set of restaurant ids whose full pill card
 * would visually collide with at least one other pill at the current
 * zoom. The map screen renders these as small dot/heart/star markers
 * instead of full pills, so the user sees individual points-of-interest
 * everywhere without the visual noise of overlapping cards.
 *
 * Pure math, no SDK access, safe to call from any dispatcher. Pair with
 * [projectAll] (which must run on Main) to move detection off the UI
 * thread.
 *
 * **Predicate**: AABB rectangle overlap. Each pill's footprint is a
 * `widthsPx[i] × heightPx` box anchored at the bottom-centre of its
 * lat/lng pin (anchor `(0.5, 1)`). Two pills are considered overlapping
 * when their boxes — expanded by [paddingPx] on every side — intersect.
 * Width-aware overlap means short names (narrow pills) tolerate closer
 * neighbours than long names (wide pills), which a uniform circular
 * radius would get wrong in either direction.
 *
 * The screen is divided into square cells of side [maxOverlapPx]
 * (= the largest possible centre-to-centre distance at which two
 * pills can still touch). Any candidate that could overlap point i
 * must have its anchor in one of i's 9 surrounding cells, so we probe
 * only those — keeping the algorithm O(n) average.
 */
internal fun detectDenseRestaurants(
    restaurants: List<Restaurant>,
    projected: List<ScreenPoint>,
    widthsPx: FloatArray,
    heightPx: Float,
    paddingPx: Float,
    maxOverlapPx: Float,
): Set<String> {
    require(restaurants.size == projected.size) {
        "restaurants and projected must have the same size"
    }
    require(restaurants.size == widthsPx.size) {
        "restaurants and widthsPx must have the same size"
    }
    if (restaurants.isEmpty()) return emptySet()

    val cellSize = maxOverlapPx.coerceAtLeast(1f)
    val n = restaurants.size
    val grid = HashMap<Long, MutableList<Int>>(n)

    // Pass 1: index every projected anchor into its grid cell. We need a
    // complete index before checking because dense markers are pairwise —
    // a single-pass "matched-against-existing" approach (like the old
    // bucket clusterer) would miss the case where i is processed before
    // j but j ends up overlapping i.
    for (i in 0 until n) {
        val pt = projected[i]
        val cx = (pt.x / cellSize).toInt()
        val cy = (pt.y / cellSize).toInt()
        grid.getOrPut(cellKey(cx, cy)) { ArrayList(2) }.add(i)
    }

    // Pass 2: for each marker, probe the 9-cell neighbourhood for any
    // other marker whose AABB intersects ours. First hit → mark dense
    // and bail out (we only need to know if it overlaps at least one
    // neighbour, not how many).
    val dense = HashSet<String>()
    for (i in 0 until n) {
        val pt = projected[i]
        val w = widthsPx[i]
        val cx = (pt.x / cellSize).toInt()
        val cy = (pt.y / cellSize).toInt()
        outer@ for (dx in -1..1) {
            for (dy in -1..1) {
                val key = cellKey(cx + dx, cy + dy)
                val candidates = grid[key] ?: continue
                for (j in candidates) {
                    if (i == j) continue
                    val pt2 = projected[j]
                    val w2 = widthsPx[j]
                    // AABB overlap, anchor (0.5, 1): horizontal centres are
                    // (pt.x, pt2.x); vertical extents reach `heightPx` upward
                    // from each anchor's y. Two boxes (expanded by padding)
                    // overlap iff |dx| < (w_a + w_b)/2 + padding AND
                    // |dy| < heightPx + padding.
                    val ddx = if (pt.x >= pt2.x) pt.x - pt2.x else pt2.x - pt.x
                    val ddy = if (pt.y >= pt2.y) pt.y - pt2.y else pt2.y - pt.y
                    val xLimit = (w + w2) / 2f + paddingPx
                    val yLimit = heightPx + paddingPx
                    if (ddx < xLimit && ddy < yLimit) {
                        dense.add(restaurants[i].id)
                        break@outer
                    }
                }
            }
        }
    }
    return dense
}

private fun cellKey(cx: Int, cy: Int): Long =
    (cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
