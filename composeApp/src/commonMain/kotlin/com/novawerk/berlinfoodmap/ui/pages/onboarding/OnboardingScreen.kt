package com.novawerk.berlinfoodmap.ui.pages.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.onboarding_get_started
import berlinfoodmap.composeapp.generated.resources.onboarding_p1_body
import berlinfoodmap.composeapp.generated.resources.onboarding_p1_title
import berlinfoodmap.composeapp.generated.resources.onboarding_p2_body
import berlinfoodmap.composeapp.generated.resources.onboarding_p2_title
import berlinfoodmap.composeapp.generated.resources.onboarding_p3_body
import berlinfoodmap.composeapp.generated.resources.onboarding_p3_title
import berlinfoodmap.composeapp.generated.resources.onboarding_p4_body
import berlinfoodmap.composeapp.generated.resources.onboarding_p4_title
import berlinfoodmap.composeapp.generated.resources.onboarding_page1
import berlinfoodmap.composeapp.generated.resources.onboarding_page2
import berlinfoodmap.composeapp.generated.resources.onboarding_page3
import berlinfoodmap.composeapp.generated.resources.onboarding_page4
import berlinfoodmap.composeapp.generated.resources.onboarding_skip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private data class OnboardingPage(
    val image: DrawableResource,
    val title: StringResource,
    val body: StringResource,
)

private val Pages = listOf(
    OnboardingPage(
        image = Res.drawable.onboarding_page1,
        title = Res.string.onboarding_p1_title,
        body = Res.string.onboarding_p1_body,
    ),
    OnboardingPage(
        image = Res.drawable.onboarding_page2,
        title = Res.string.onboarding_p2_title,
        body = Res.string.onboarding_p2_body,
    ),
    OnboardingPage(
        image = Res.drawable.onboarding_page3,
        title = Res.string.onboarding_p3_title,
        body = Res.string.onboarding_p3_body,
    ),
    OnboardingPage(
        image = Res.drawable.onboarding_page4,
        title = Res.string.onboarding_p4_title,
        body = Res.string.onboarding_p4_body,
    ),
)

// Onboarding fixes the Pinwo brand red regardless of light/dark theme — the
// hero PNGs are baked at this colour, so swapping to a theme token would
// produce a seam between the artwork and the surrounding chrome.
private val BrandRed = Color(0xFFB51F1F)
private val BrandWine = Color(0xFF780A09)
private val OnBrand = Color(0xFFFFFCF8)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Pages.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    // Shared "advance" closure used by both the AdvanceButton and the
    // tap-anywhere gesture on the page body. Pre-final → next page;
    // final → onComplete. Per the May-24 PM note, users want to be able
    // to advance by tapping anywhere on the screen (not just the FAB).
    val advance = {
        if (currentPage == Pages.lastIndex) {
            onComplete()
        } else {
            scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
            Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandRed),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            OnboardingPageView(
                page = Pages[index],
                onTap = { advance() },
            )
        }

        // Skip control, top-right. Flips straight to Main from any page.
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            TextButton(onClick = onComplete) {
                Text(
                    stringResource(Res.string.onboarding_skip),
                    color = OnBrand,
                )
            }
        }

        // Bottom action row: page dots + advance/finish FAB.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PageIndicator(
                pageCount = Pages.size,
                current = currentPage,
            )
            Spacer(Modifier.width(16.dp).fillMaxWidth().weight(1f))
            AdvanceButton(
                isLast = currentPage == Pages.lastIndex,
                onClick = { advance() },
            )
        }
    }
}

@Composable
private fun OnboardingPageView(
    page: OnboardingPage,
    onTap: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            // Tap-anywhere advance (May-24 redesign). Doesn't fight the
            // pager's swipe gesture — HorizontalPager consumes drag
            // events before this clickable layer sees them.
            .clickable(onClick = onTap),
    ) {
        Image(
            painter = painterResource(page.image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Vertical brand-red gradient over the lower half so headline +
        // body text stay legible on top of the artwork (cards / map).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1.0f to BrandRed.copy(alpha = 0.92f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .systemBarsPadding()
                // Bottom-padded enough to clear the page-indicator + FAB row.
                .padding(start = 28.dp, end = 28.dp, bottom = 120.dp),
        ) {
            Text(
                text = stringResource(page.title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = OnBrand,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(page.body),
                style = MaterialTheme.typography.bodyLarge,
                color = OnBrand.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, current: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (active) 18.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) OnBrand else OnBrand.copy(alpha = 0.4f),
                    ),
            )
        }
    }
}

@Composable
private fun AdvanceButton(isLast: Boolean, onClick: () -> Unit) {
    val description = stringResource(
        if (isLast) Res.string.onboarding_get_started else Res.string.onboarding_skip,
    )
    Box(
        modifier = Modifier
            .size(if (isLast) 64.dp else 56.dp)
            .clip(CircleShape)
            .background(BrandWine)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = description,
            tint = OnBrand,
        )
    }
}
