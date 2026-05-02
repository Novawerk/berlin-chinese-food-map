package com.novawerk.berlinfoodmap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.Font
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.noto_sans_sc_bold
import berlinfoodmap.composeapp.generated.resources.noto_sans_sc_medium
import berlinfoodmap.composeapp.generated.resources.noto_sans_sc_regular
import berlinfoodmap.composeapp.generated.resources.source_sans_3_bold
import berlinfoodmap.composeapp.generated.resources.source_sans_3_medium
import berlinfoodmap.composeapp.generated.resources.source_sans_3_regular
import berlinfoodmap.composeapp.generated.resources.source_sans_3_semibold

// Pinwo brand palette — Brandbook V2
internal val PinwoRed = Color(0xFFB51F1F)        // Core red — primary
internal val PinwoWine = Color(0xFF780A09)       // Deep red — secondary
internal val PinwoOlive = Color(0xFF848419)      // Core olive — tertiary
internal val PinwoOliveDark = Color(0xFF66753C)
internal val PinwoCream = Color(0xFFF8F1DE)      // Background — warm cream (no green cast)
internal val PinwoInk = Color(0xFF2B2828)        // Onsurface
internal val PinwoCharcoal = Color(0xFF514646)   // Warm grey — non-partner pin & dividers
internal val PinwoPink = Color(0xFFFFE9E9)       // primaryContainer
internal val PinwoRose = Color(0xFFF4ABAB)
internal val PinwoSage = Color(0xFFD5E0B8)       // secondaryContainer
internal val PinwoGold = Color(0xFFF7DD6D)       // tertiaryContainer
internal val PinwoApricot = Color(0xFFF4C36E)
internal val PinwoBeige = Color(0xFFE8E4DD)      // surfaceVariant
internal val PinwoStone = Color(0xFFC6C2BD)      // outline

private fun brandLightColorScheme(): ColorScheme = lightColorScheme(
    primary = PinwoRed,
    onPrimary = Color.White,
    primaryContainer = PinwoPink,
    onPrimaryContainer = PinwoWine,
    inversePrimary = PinwoRose,
    secondary = PinwoWine,
    onSecondary = Color.White,
    secondaryContainer = PinwoSage,
    onSecondaryContainer = PinwoOliveDark,
    tertiary = PinwoOlive,
    onTertiary = Color.White,
    tertiaryContainer = PinwoGold,
    onTertiaryContainer = PinwoInk,
    background = PinwoCream,
    onBackground = PinwoInk,
    surface = PinwoCream,
    onSurface = PinwoInk,
    surfaceVariant = PinwoBeige,
    onSurfaceVariant = PinwoCharcoal,
    surfaceTint = PinwoRed,
    inverseSurface = PinwoInk,
    inverseOnSurface = PinwoCream,
    outline = PinwoStone,
    outlineVariant = PinwoBeige,
    error = PinwoRed,
    onError = Color.White,
    errorContainer = PinwoPink,
    onErrorContainer = PinwoWine,
    scrim = PinwoInk,
)

private fun brandDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = PinwoWine,
    primaryContainer = PinwoWine,
    onPrimaryContainer = PinwoPink,
    secondary = Color(0xFFFFB4AB),
    onSecondary = PinwoWine,
    tertiary = PinwoGold,
    onTertiary = PinwoInk,
    background = PinwoInk,
    onBackground = PinwoCream,
    surface = Color(0xFF221C1C),
    onSurface = PinwoCream,
    surfaceVariant = Color(0xFF3A3232),
    onSurfaceVariant = PinwoBeige,
    outline = PinwoCharcoal,
    error = Color(0xFFFFB4AB),
    onError = PinwoWine,
)

// Source Sans 3 (Latin) chained with Noto Sans SC (CJK) — the brand pair from
// the V2 brandbook. Each weight has both a Latin and a CJK font; the rendering
// pipeline picks per-glyph from the chain.
@Composable
private fun pinwoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.source_sans_3_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(Res.font.source_sans_3_medium, FontWeight.Medium),
    Font(Res.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(Res.font.source_sans_3_semibold, FontWeight.SemiBold),
    Font(Res.font.noto_sans_sc_medium, FontWeight.SemiBold),
    Font(Res.font.source_sans_3_bold, FontWeight.Bold),
    Font(Res.font.noto_sans_sc_bold, FontWeight.Bold),
)
@Composable
private fun pinwoTypography(): Typography {
    val family = pinwoFontFamily()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

data class ExpressiveShapes(
    val circle: Shape = RoundedCornerShape(50),
    val pill: Shape = RoundedCornerShape(50),
    val cookie4: Shape = RoundedCornerShape(12.dp),
    val cookie6: Shape = RoundedCornerShape(12.dp),
    val clover4: Shape = RoundedCornerShape(12.dp),
    val burst: Shape = RoundedCornerShape(12.dp),
    val heart: Shape = RoundedCornerShape(12.dp),
)

val LocalExpressiveShapes = staticCompositionLocalOf { ExpressiveShapes() }

val MaterialTheme.expressiveShapes: ExpressiveShapes
    @Composable get() = LocalExpressiveShapes.current

@Composable
private fun expressiveShapes() = ExpressiveShapes(
    circle = MaterialShapes.Circle.toShape(),
    pill = MaterialShapes.Pill.toShape(),
    cookie4 = MaterialShapes.Cookie4Sided.toShape(),
    cookie6 = MaterialShapes.Cookie6Sided.toShape(),
    clover4 = MaterialShapes.Clover4Leaf.toShape(),
    burst = MaterialShapes.Burst.toShape(),
    heart = MaterialShapes.Heart.toShape(),
)

@Composable
fun AppTheme(
    darkMode: String = "system",
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDarkTheme) brandDarkColorScheme() else brandLightColorScheme()

    CompositionLocalProvider(LocalExpressiveShapes provides expressiveShapes()) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = pinwoTypography(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(32.dp),
                largeIncreased = RoundedCornerShape(28.dp),
                extraLargeIncreased = RoundedCornerShape(36.dp),
                extraExtraLarge = RoundedCornerShape(40.dp),
            ),
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
