package com.novawerk.berlinfoodmap.ui.pages.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.app_name
import berlinfoodmap.composeapp.generated.resources.nova_star
import berlinfoodmap.composeapp.generated.resources.pinwo_wordmark
import berlinfoodmap.composeapp.generated.resources.splash_brand_lockup
import berlinfoodmap.composeapp.generated.resources.splash_powered_by
import berlinfoodmap.composeapp.generated.resources.splash_tagline_line1
import berlinfoodmap.composeapp.generated.resources.splash_tagline_line2
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// Splash chrome fixes brand colours regardless of light/dark theme — the
// wordmark is cream on deep red, and theme tokens would shift in dark mode.
private val SplashBackground = Color(0xFF780A09) // PinwoWine
private val SplashForeground = Color(0xFFFFFCF8) // PinwoCream

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashForeground),
    ) {
        // Wine area, clipped with a downward-curving bottom edge so the
        // cream strip below it reads as a "card pulling away" — matches
        // the May-24 splash design.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(WineCurveShape)
                .background(SplashBackground),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.fillMaxHeight(0.36f))

                Image(
                    painter = painterResource(Res.drawable.pinwo_wordmark),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier.height(64.dp),
                    colorFilter = ColorFilter.tint(SplashForeground),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.splash_tagline_line1),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 26.sp,
                    ),
                    color = SplashForeground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.splash_tagline_line2),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 26.sp,
                    ),
                    color = SplashForeground,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // "Powered by Novawerk × Pinwo" sits in the cream strip below the
        // wine curve, in PinwoWine for legibility.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.nova_star),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.splash_powered_by),
                style = MaterialTheme.typography.labelSmall,
                color = SplashBackground.copy(alpha = 0.70f),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(Res.string.splash_brand_lockup),
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.sp,
                ),
                color = SplashBackground,
            )
        }
    }
}

/**
 * Downward-curving shape used to clip the splash's wine area. The bottom
 * edge dips below the canvas like a concave arc, leaving the cream strip
 * below visible at the corners and dipping deeper in the middle.
 */
private val WineCurveShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        // Curve depth: ~6% of height, biting down from the bottom edge.
        val dip = size.height * 0.06f
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - dip)
            // Quadratic curve from the right edge down through the centre
            // bottom and back up to the left edge. Control point sits below
            // the canvas to create the concave dip.
            quadraticBezierTo(
                size.width / 2f,
                size.height + dip,
                0f,
                size.height - dip,
            )
            close()
        }
        return Outline.Generic(path)
    }
}
