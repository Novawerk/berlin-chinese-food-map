package com.novawerk.berlinfoodmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.close_photos
import berlinfoodmap.composeapp.generated.resources.photo_count
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource

/**
 * Edge-to-edge black photo pager. Hosted in a [Dialog] so it floats above the
 * detail bottom sheet without dismissing it. Pinch-to-zoom resets when the page
 * changes, and a double-tap toggles between fit and 2x.
 */
@Composable
fun FullscreenPhotoPager(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (photos.isEmpty()) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, photos.size - 1),
            pageCount = { photos.size },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
            ) { page ->
                ZoomableImage(
                    url = photos[page],
                    isActive = pagerState.currentPage == page,
                    activePage = pagerState.currentPage,
                )
            }

            // Counter pill (top-left)
            if (photos.size > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp),
                ) {
                    Text(
                        text = stringResource(
                            Res.string.photo_count,
                            pagerState.currentPage + 1,
                            photos.size,
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // Close button (top-right)
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
                    .size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.close_photos))
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    url: String,
    isActive: Boolean,
    activePage: Int,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset transform when the user swipes to another page so the new page
    // always opens at fit-to-screen.
    LaunchedEffect(activePage) {
        snapshotFlow { activePage }.collect {
            if (!isActive) {
                scale = 1f
                offset = Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Custom transform handler so the pager's horizontal swipe still
            // works at scale = 1. We only consume pointer events when the user
            // is actually zooming (≥2 fingers) or panning a zoomed-in image —
            // single-finger drags at fit-to-screen pass through to the pager.
            .pointerInput(url) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val multiTouch = event.changes.size >= 2
                        if (multiTouch || scale > 1f) {
                            val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale > 1f) offset + panChange else Offset.Zero
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(url) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
