package com.novawerk.berlinfoodmap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

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
    val colorScheme = when {
        isDarkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    CompositionLocalProvider(LocalExpressiveShapes provides expressiveShapes()) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
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
