package com.novawerk.berlinfoodmap.ui.pages.map

import com.novawerk.berlinfoodmap.domain.restaurant.CuisineType
import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.BitmapDescriptorFactory

/**
 * Creates a small colored circle bitmap for map markers.
 * Each cuisine type gets a distinct color.
 */
fun cuisineMarkerIcon(cuisineType: CuisineType): BitmapDescriptor {
    val (r, g, b) = when (cuisineType) {
        CuisineType.SICHUAN   -> Triple(220, 53, 34)   // Red — spicy
        CuisineType.CANTONESE -> Triple(255, 167, 38)   // Orange — warm
        CuisineType.HOTPOT    -> Triple(198, 40, 40)    // Dark red — hot
        CuisineType.BBQ       -> Triple(121, 85, 72)    // Brown — smoky
        CuisineType.DIM_SUM   -> Triple(255, 202, 40)   // Yellow — dim sum
        CuisineType.NOODLES   -> Triple(66, 165, 245)   // Blue — noodle
        CuisineType.GENERAL   -> Triple(102, 187, 106)  // Green — general
        CuisineType.OTHER     -> Triple(149, 117, 205)  // Purple — other
    }

    // Create a 48x48 circle marker
    val size = 48
    val centerX = size / 2
    val centerY = size / 2
    val outerRadius = size / 2
    val innerRadius = size / 2 - 3

    val bytes = ByteArray(size * size * 4)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val dx = x - centerX
            val dy = y - centerY
            val distSq = dx * dx + dy * dy
            val i = (y * size + x) * 4

            when {
                distSq <= innerRadius * innerRadius -> {
                    // Inner fill
                    bytes[i] = 230.toByte()     // alpha
                    bytes[i + 1] = r.toByte()
                    bytes[i + 2] = g.toByte()
                    bytes[i + 3] = b.toByte()
                }
                distSq <= outerRadius * outerRadius -> {
                    // Border (white)
                    bytes[i] = 255.toByte()
                    bytes[i + 1] = 255.toByte()
                    bytes[i + 2] = 255.toByte()
                    bytes[i + 3] = 255.toByte()
                }
                else -> {
                    // Transparent
                    bytes[i] = 0
                    bytes[i + 1] = 0
                    bytes[i + 2] = 0
                    bytes[i + 3] = 0
                }
            }
        }
    }

    return BitmapDescriptorFactory.fromBytes(bytes, size, size)
}
