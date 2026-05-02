package com.novawerk.berlinfoodmap.ui.pages.map

import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.BitmapDescriptorFactory
import kotlin.math.sqrt

/**
 * Standard "my-location" dot — blue inner disc, white border, soft halo.
 * Circular shape (vs the Compose card markers for restaurants) reinforces the
 * convention that this is the user, not a place.
 */
fun myLocationDotIcon(): BitmapDescriptor {
    val size = 56
    val center = size / 2.0
    val haloRadius = 26.0
    val borderOuterRadius = 18.0
    val innerRadius = 14.0

    val bytes = ByteArray(size * size * 4)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val i = (y * size + x) * 4
            val dx = x - center
            val dy = y - center
            val dist = sqrt(dx * dx + dy * dy)
            when {
                dist <= innerRadius -> writePixel(bytes, i, 255, 0x25, 0x63, 0xEB)
                dist <= borderOuterRadius -> writePixel(bytes, i, 255, 255, 255, 255)
                dist <= haloRadius -> writePixel(bytes, i, 64, 0x25, 0x63, 0xEB)
                else -> writePixel(bytes, i, 0, 0, 0, 0)
            }
        }
    }
    return BitmapDescriptorFactory.fromBytes(bytes, size, size)
}

private fun writePixel(buf: ByteArray, i: Int, a: Int, r: Int, g: Int, b: Int) {
    buf[i] = a.toByte()
    buf[i + 1] = r.toByte()
    buf[i + 2] = g.toByte()
    buf[i + 3] = b.toByte()
}
