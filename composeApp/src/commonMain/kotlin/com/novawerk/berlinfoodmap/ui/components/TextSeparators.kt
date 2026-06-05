package com.novawerk.berlinfoodmap.ui.components

/** Middle-dot glyph, used as a standalone separator between two text elements. */
internal const val MIDDOT = "·"

/**
 * Middle-dot with surrounding spaces, for joining inline items inside a single
 * string (e.g. `tags.joinToString(MIDDOT_SEP)`). Single source for the card/pill
 * separator so a design change is one edit.
 */
internal const val MIDDOT_SEP = " $MIDDOT "
