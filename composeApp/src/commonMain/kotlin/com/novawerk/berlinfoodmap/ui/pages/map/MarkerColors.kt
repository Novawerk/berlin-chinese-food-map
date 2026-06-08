package com.novawerk.berlinfoodmap.ui.pages.map

import eu.buney.maps.BitmapDescriptor
import eu.buney.maps.BitmapDescriptorFactory
import kotlin.math.sqrt

/**
 * "My-location" marker — red-wine circle with a white upward-pointing
 * triangle inside, per the May-24 Pinwo design spec. The marker is
 * rotated to the user's compass heading by [MapScreen] via the kmp-maps
 * `Marker(rotation = ...)` parameter, so the arrow points the way the
 * user is facing.
 *
 *  - Outer fill: #780A09 (PinwoWine — same as splash background)
 *  - Inner arrow: white triangle, ~60% of the circle's diameter
 *  - 1px white border keeps it crisp against reddish map tiles
 *
 * Designed as an upward-pointing arrow at 0° rotation; with `rotation =
 * bearing` on the `Marker`, it spins on the device's heading.
 */
fun myLocationDotIcon(): BitmapDescriptor {
    val size = 64
    val center = size / 2.0
    val haloRadius = 30.0
    val borderOuterRadius = 22.0

    val bytes = ByteArray(size * size * 4)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val i = (y * size + x) * 4
            val dx = x - center
            val dy = y - center
            val dist = sqrt(dx * dx + dy * dy)

            when {
                // Inside the arrow shape (drawn over the wine fill).
                isInArrow(x, y, size) -> writePixel(bytes, i, 255, 255, 255, 255)
                // Wine fill disc.
                dist <= borderOuterRadius - 1 -> writePixel(bytes, i, 255, 0x78, 0x0A, 0x09)
                // White border ring.
                dist <= borderOuterRadius -> writePixel(bytes, i, 255, 255, 255, 255)
                // Soft halo.
                dist <= haloRadius -> writePixel(bytes, i, 48, 0x78, 0x0A, 0x09)
                else -> writePixel(bytes, i, 0, 0, 0, 0)
            }
        }
    }
    return BitmapDescriptorFactory.fromBytes(bytes, size, size)
}

/**
 * Isosceles triangle pointing up — base at the bottom-third of the
 * marker, apex near the top. Drawn at integer coordinates against the
 * 64x64 canvas. Returns true when the pixel is inside the triangle.
 */
private fun isInArrow(x: Int, y: Int, size: Int): Boolean {
    val cx = size / 2
    val apexY = (size * 0.30).toInt()
    val baseY = (size * 0.65).toInt()
    val halfBase = (size * 0.20).toInt()

    if (y < apexY || y > baseY) return false

    // Linear interpolation: at apexY the width is 0, at baseY it's
    // 2 * halfBase. Pixel is inside if |x - cx| ≤ current half-width.
    val t = (y - apexY).toDouble() / (baseY - apexY).toDouble()
    val halfWidth = (halfBase * t).toInt()
    return kotlin.math.abs(x - cx) <= halfWidth
}

private fun writePixel(buf: ByteArray, i: Int, a: Int, r: Int, g: Int, b: Int) {
    buf[i] = a.toByte()
    buf[i + 1] = r.toByte()
    buf[i + 2] = g.toByte()
    buf[i + 3] = b.toByte()
}
