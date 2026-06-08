package com.novawerk.berlinfoodmap.domain.common

data class Localizable(
    val en: String,
    val zh: String,
    val de: String? = null,
)

/**
 * Pick the best string for the given locale tag (e.g. "zh", "en",
 * "zh_CN", "en-US"). Falls back to the other supported language if the
 * preferred one is blank, so a description that exists only in one
 * language still shows up.
 *
 * `de` is treated as restaurant-data-only (the German name); it's not a
 * UI language and isn't selected as a fallback.
 */
fun Localizable.preferred(locale: String): String =
    if (locale.startsWith("zh", ignoreCase = true)) {
        zh.takeIf { it.isNotBlank() } ?: en
    } else {
        en.takeIf { it.isNotBlank() } ?: zh
    }
