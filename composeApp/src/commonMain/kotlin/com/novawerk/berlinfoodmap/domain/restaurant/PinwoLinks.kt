package com.novawerk.berlinfoodmap.domain.restaurant

/** pinwo.de homepage — fallback when a restaurant has no coupon page. */
const val PINWO_HOME_URL = "https://pinwo.de"

/**
 * Query parameters appended to every pinwo.de link the app opens, so Pinwo's
 * analytics can attribute the visit to the Berlin Food Map app. Standard UTM
 * keys so any analytics tool picks them up without setup.
 */
internal val PINWO_APP_UTM = listOf(
    "utm_source" to "berlinfoodmap",
    "utm_medium" to "app",
    "utm_campaign" to "coupon",
)

/**
 * The link to open for a Pinwo offer: [url] (a restaurant's coupon page) or the
 * pinwo.de homepage when it's blank, tagged with [PINWO_APP_UTM]. Keeps any
 * existing query and `#fragment`; a key already present in [url] is not
 * overridden.
 */
fun pinwoAppLink(url: String?): String {
    val base = url?.trim()?.takeIf { it.isNotEmpty() } ?: PINWO_HOME_URL
    val hashAt = base.indexOf('#')
    val beforeHash = if (hashAt >= 0) base.substring(0, hashAt) else base
    val fragment = if (hashAt >= 0) base.substring(hashAt) else ""

    val queryAt = beforeHash.indexOf('?')
    val existingKeys = if (queryAt >= 0) {
        beforeHash.substring(queryAt + 1).split('&')
            .map { it.substringBefore('=') }
            .filter { it.isNotEmpty() }
            .toSet()
    } else {
        emptySet()
    }
    val extra = PINWO_APP_UTM
        .filter { (k, _) -> k !in existingKeys }
        .joinToString("&") { (k, v) -> "$k=$v" }
    if (extra.isEmpty()) return base

    val sep = when {
        queryAt < 0 -> "?"
        beforeHash.endsWith('?') || beforeHash.endsWith('&') -> ""
        else -> "&"
    }
    return "$beforeHash$sep$extra$fragment"
}
