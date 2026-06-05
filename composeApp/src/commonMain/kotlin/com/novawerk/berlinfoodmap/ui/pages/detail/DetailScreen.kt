package com.novawerk.berlinfoodmap.ui.pages.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.action_call
import berlinfoodmap.composeapp.generated.resources.action_directions
import berlinfoodmap.composeapp.generated.resources.action_favorite
import berlinfoodmap.composeapp.generated.resources.action_unfavorite
import berlinfoodmap.composeapp.generated.resources.action_website
import berlinfoodmap.composeapp.generated.resources.closed_now
import berlinfoodmap.composeapp.generated.resources.closed_today
import berlinfoodmap.composeapp.generated.resources.discount_card_body
import berlinfoodmap.composeapp.generated.resources.discount_card_title
import berlinfoodmap.composeapp.generated.resources.discount_visit_pinwo
import berlinfoodmap.composeapp.generated.resources.marker_discount
import berlinfoodmap.composeapp.generated.resources.closes_at
import berlinfoodmap.composeapp.generated.resources.closes_in_min
import berlinfoodmap.composeapp.generated.resources.hours_hide
import berlinfoodmap.composeapp.generated.resources.today_hours
import berlinfoodmap.composeapp.generated.resources.hours_unknown
import berlinfoodmap.composeapp.generated.resources.hours_view_full
import berlinfoodmap.composeapp.generated.resources.open_24h
import berlinfoodmap.composeapp.generated.resources.open_now
import berlinfoodmap.composeapp.generated.resources.opens_at
import berlinfoodmap.composeapp.generated.resources.opens_in_min
import berlinfoodmap.composeapp.generated.resources.opening_hours
import berlinfoodmap.composeapp.generated.resources.photo_count
import berlinfoodmap.composeapp.generated.resources.photos_label
import berlinfoodmap.composeapp.generated.resources.tomorrow_at
import berlinfoodmap.composeapp.generated.resources.weekday_at
import berlinfoodmap.composeapp.generated.resources.weekday_long_fri
import berlinfoodmap.composeapp.generated.resources.weekday_long_mon
import berlinfoodmap.composeapp.generated.resources.weekday_long_sat
import berlinfoodmap.composeapp.generated.resources.weekday_long_sun
import berlinfoodmap.composeapp.generated.resources.weekday_long_thu
import berlinfoodmap.composeapp.generated.resources.weekday_long_tue
import berlinfoodmap.composeapp.generated.resources.weekday_long_wed
import coil3.compose.AsyncImage
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.common.preferred
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import kotlinx.coroutines.launch
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale
import com.novawerk.berlinfoodmap.domain.restaurant.OpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.restaurant.computeOpeningStatus
import com.novawerk.berlinfoodmap.domain.restaurant.currentBerlinMinuteOfWeek
import com.novawerk.berlinfoodmap.domain.restaurant.mowToDayIndex
import com.novawerk.berlinfoodmap.domain.restaurant.mowToTimeOfDay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.novawerk.berlinfoodmap.ui.components.MIDDOT_SEP
import com.novawerk.berlinfoodmap.ui.components.tagDisplayName
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher

private val SECTION_SHAPE = RoundedCornerShape(28.dp)

/** Pinwo offers landing page, opened from the discount perks card. */
private const val PINWO_OFFER_URL = "https://pinwo.de"

/**
 * Sheet-friendly detail content. Hosted inside a [ModalBottomSheet] at the
 * app root so opening/closing it doesn't tear down the underlying screen
 * (notably the map, which is expensive to re-render).
 */
@Composable
fun DetailScreen(
    restaurantId: String,
    repository: RestaurantRepository,
    favoritesRepository: FavoritesRepository,
    authService: AuthService,
) {
    var restaurant by remember(restaurantId) { mutableStateOf<Restaurant?>(null) }
    var loading by remember(restaurantId) { mutableStateOf(true) }
    val urlLauncher = rememberUrlLauncher()
    val scope = rememberCoroutineScope()

    val favorites by favoritesRepository.observe()
        .collectAsState(initial = emptySet())
    val isFavorite = restaurantId in favorites

    LaunchedEffect(restaurantId) {
        restaurant = repository.getById(restaurantId)
        loading = false

        // Track view (best-effort — fails silently if anonymous auth hasn't
        // landed yet on first launch).
        val uid = authService.getCurrentUid()
        if (uid != null) {
            repository.incrementViewCount(restaurantId, uid)
        }
    }

    if (loading) {
        Box(
            Modifier.fillMaxWidth().padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val r = restaurant ?: return
    val photos = remember(r.galleries, r.googleData?.photoUrls) {
        buildList {
            addAll(r.galleries)
            r.googleData?.photoUrls?.let { addAll(it) }
        }.distinct()
    }
    var fullscreenIndex by remember(restaurantId) { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            HeroCover(
                photos = photos,
                isFavorite = isFavorite,
                onFavoriteClick = {
                    scope.launch { favoritesRepository.toggle(restaurantId) }
                },
                onTap = { fullscreenIndex = 0 },
            )

            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                TitleBlock(restaurant = r)

                ChipsRow(restaurant = r)

                if (r.hasDiscount) {
                    DiscountCard(
                        offer = r.discountInfo?.preferred(LocalAppLocale.current)
                            ?.takeIf { it.isNotBlank() },
                        onVisitPinwo = { urlLauncher.open(PINWO_OFFER_URL) },
                    )
                }

                HoursCard(
                    periods = r.googleData?.periods.orEmpty(),
                    weekdayText = r.googleData?.weekdayText.orEmpty(),
                )

                AddressCard(
                    restaurant = r,
                    onNavigate = {
                        // Google Maps URL spec: directions to a destination,
                        // pinned by place_id when we have one (more accurate
                        // than coordinates which can land mid-block).
                        // Spec: developers.google.com/maps/documentation/urls
                        val url = buildString {
                            append("https://www.google.com/maps/dir/?api=1")
                            append("&destination=").append(r.latitude).append(',').append(r.longitude)
                            r.googleData?.placeId?.let {
                                append("&destination_place_id=").append(it)
                            }
                        }
                        urlLauncher.open(url)
                    },
                )

                r.description?.let { desc ->
                    val text = desc.preferred(LocalAppLocale.current)
                    if (text.isNotBlank()) DescriptionCard(text = text)
                }
            }

            // Photo carousel breaks out of the padded info column so it can
            // use the full screen width — Material 3's MultiBrowseCarousel
            // computes peek widths off its outer bounds, and a 40dp-shrunk
            // container collapses the items on narrow iPhones.
            // Internal padding is applied to the title row + carousel
            // contentPadding instead.
            if (photos.size > 1) {
                Spacer(Modifier.height(24.dp))
                PhotoCarousel(
                    photos = photos,
                    horizontalPadding = 20.dp,
                    onTap = { fullscreenIndex = it },
                )
            }

            // Reserve space for the sticky bottom action bar so the last
            // visible section is fully scrollable above it.
            Spacer(Modifier.height(120.dp))
        }

        // Sticky action bar — pinned to the bottom of the sheet, fades in over
        // the scrolling content via a soft top scrim so the buttons always read.
        StickyActionBar(
            phone = r.phone ?: r.googleData?.formattedPhoneNumber,
            website = r.googleData?.website,
            latitude = r.latitude,
            longitude = r.longitude,
            placeId = r.googleData?.placeId,
            onOpenUrl = urlLauncher::open,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    fullscreenIndex?.let { idx ->
        com.novawerk.berlinfoodmap.ui.components.FullscreenPhotoPager(
            photos = photos,
            initialIndex = idx,
            onDismiss = { fullscreenIndex = null },
        )
    }
}

// region Hero --------------------------------------------------------------

@Composable
private fun HeroCover(
    photos: List<String>,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (photos.isNotEmpty()) Modifier.clickable { onTap() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (photos.isNotEmpty()) {
            AsyncImage(
                model = photos.first(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp),
            )
        }

        FavoriteToggle(
            isFavorite = isFavorite,
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
        )
    }
}

@Composable
private fun FavoriteToggle(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pill button on a translucent dark scrim — readable against any cover
    // photo without competing with brand surfaces. The heart fills with
    // brand red when saved; outline icon when not, matching the standard
    // save-pattern users expect.
    Surface(
        color = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) Res.string.action_unfavorite else Res.string.action_favorite,
                ),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (isFavorite) Res.string.action_unfavorite else Res.string.action_favorite,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

/**
 * Material 3 multi-browse carousel for photo browsing — anchors a large
 * focused item with smaller peeking neighbours, letting users scrub through
 * the gallery without leaving the detail screen. Tap any tile to enter the
 * fullscreen pager at that index.
 *
 * Spec: https://m3.material.io/components/carousel/overview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoCarousel(
    photos: List<String>,
    horizontalPadding: Dp,
    onTap: (Int) -> Unit,
) {
    val state = rememberCarouselState { photos.size }
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = horizontalPadding + 4.dp, bottom = 12.dp),
        ) {
            Text(
                text = stringResource(Res.string.photos_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${photos.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalMultiBrowseCarousel(
            state = state,
            preferredItemWidth = 240.dp,
            itemSpacing = 8.dp,
            // Inner padding so first / last items breathe the same as the
            // sections above; the carousel itself takes the full screen
            // width to give M3's MultiBrowse algorithm enough room to
            // compute peek widths correctly (especially on narrow iPhones).
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) { index ->
            AsyncImage(
                model = photos[index],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onTap(index) },
            )
        }
    }
}

// endregion

// region Title + chips ----------------------------------------------------

@Composable
private fun TitleBlock(restaurant: Restaurant) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = restaurant.name.zh,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (restaurant.featured) {
                Spacer(Modifier.width(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp),
                    )
                }
            }
        }
        if (restaurant.name.en.isNotBlank() && restaurant.name.en != restaurant.name.zh) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = restaurant.name.en,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChipsRow(restaurant: Restaurant) {
    val rating = restaurant.googleData?.rating
    val count = restaurant.googleData?.userRatingsTotal
    if (rating == null && restaurant.tags.isEmpty() && restaurant.priceRange == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (rating != null) {
            RatingChip(rating = rating, count = count)
        }
        ViewCountChip(count = restaurant.viewCount)
        restaurant.tags.forEach { tag ->
            FilledChip(label = tagDisplayName(tag))
        }
        restaurant.priceRange?.let { price ->
            FilledChip(label = price, emphasised = true)
        }
    }
}

@Composable
private fun ViewCountChip(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                Icons.Filled.Visibility,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RatingChip(rating: Double, count: Int?) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = rating.toFormattedRating(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (count != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun FilledChip(label: String, emphasised: Boolean = false) {
    val container = if (emphasised) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (emphasised) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(color = container, shape = RoundedCornerShape(50)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (emphasised) FontWeight.SemiBold else FontWeight.Medium,
            color = content,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

// endregion

// region Quick actions ----------------------------------------------------

@Composable
private fun StickyActionBar(
    phone: String?,
    website: String?,
    latitude: Double,
    longitude: Double,
    placeId: String?,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Documented Maps URL scheme for *viewing* a place — opens the business
    // card in Google Maps so the user can read it, then choose for themselves
    // whether to start navigation. Going straight into directions felt too
    // aggressive (users sometimes just want to verify the spot first).
    // Spec: https://developers.google.com/maps/documentation/urls/get-started#search-action
    val mapsUrl = remember(placeId, latitude, longitude) {
        buildString {
            append("https://www.google.com/maps/search/?api=1")
            append("&query=").append(latitude).append(',').append(longitude)
            if (placeId != null) {
                append("&query_place_id=").append(placeId)
            }
        }
    }
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier = modifier.fillMaxWidth()) {
        // Soft top-fade so the bar reads cleanly over busy content while
        // scrolling — no hard divider line.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            1f to surface,
                        ),
                    ),
                ),
        )
        Surface(color = surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            ) {
                SatelliteAction(
                    icon = Icons.Filled.Phone,
                    labelRes = Res.string.action_call,
                    enabled = phone != null,
                    onClick = { phone?.let { onOpenUrl("tel:$it") } },
                )
                PrimaryAction(
                    icon = Icons.Filled.Map,
                    labelRes = Res.string.action_directions,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpenUrl(mapsUrl) },
                )
                SatelliteAction(
                    icon = Icons.Filled.Language,
                    labelRes = Res.string.action_website,
                    enabled = website != null,
                    onClick = { website?.let(onOpenUrl) },
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(
    icon: ImageVector,
    labelRes: StringResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // `secondary` (PinwoWine, deep red) instead of `primary` (PinwoRed) so
    // the action bar reads as a different surface from the brand-red POI
    // pins on the map. Same colour family, ~one stop darker — visible
    // distinction without breaking brand consistency.
    Surface(
        color = MaterialTheme.colorScheme.secondary,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(labelRes),
                color = MaterialTheme.colorScheme.onSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SatelliteAction(
    icon: ImageVector,
    labelRes: StringResource,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val container = if (enabled) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Surface(
        color = container,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .size(width = 72.dp, height = 72.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(labelRes),
                color = content,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// endregion

// endregion

// region Hours card -------------------------------------------------------

@Composable
private fun HoursCard(periods: List<String>, weekdayText: List<String>) {
    if (periods.isEmpty() && weekdayText.isEmpty()) return
    val status = remember(periods) { computeOpeningStatus(periods) }
    val nowMow = remember(periods) { currentBerlinMinuteOfWeek() }
    var expanded by remember { mutableStateOf(false) }

    // Open is the happy path — keep the card neutral so it doesn't compete
    // with the closed/error state. Only "Closed" gets the alarming tint.
    // The status headline still picks up an accent colour to read at a glance.
    val (container, onContainer, headlineColor) = when (status) {
        is OpeningStatus.Open, OpeningStatus.AlwaysOpen -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface,
            // Bold + headlineSmall already carries the emphasis; no need
            // for a tinted colour that would clash with the neutral card.
            MaterialTheme.colorScheme.onSurface,
        )
        is OpeningStatus.Closed -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        OpeningStatus.Unknown -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Surface(
        color = container,
        shape = SECTION_SHAPE,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = statusHeadline(status),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = headlineColor,
                )
                Spacer(Modifier.weight(1f))
                if (weekdayText.isNotEmpty()) {
                    Surface(
                        color = onContainer.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable { expanded = !expanded },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (expanded) Res.string.hours_hide else Res.string.hours_view_full,
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = onContainer,
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = onContainer,
                                modifier = Modifier.size(18.dp).padding(start = 2.dp),
                            )
                        }
                    }
                }
            }
            // Primary subline: today's hours (always shown when we have weekday data),
            // so users see "Today 12:00–22:00" even when the venue is currently open.
            val today = remember(weekdayText, nowMow) {
                todayHoursLine(weekdayText, mowToDayIndex(nowMow))
            }
            todaySubline(status, today)?.let { line ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = onContainer.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                )
            }
            // Secondary subline: countdown ("Closes in 25 min" / "Opens 12:00").
            countdownSubline(status, nowMow)?.let { line ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onContainer.copy(alpha = 0.75f),
                )
            }

            if (expanded && weekdayText.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = onContainer.copy(alpha = 0.18f))
                Spacer(Modifier.height(12.dp))
                WeekHoursList(
                    weekdayText = weekdayText,
                    todayIndex = mowToDayIndex(nowMow),
                    color = onContainer,
                )
            }
        }
    }
}

@Composable
private fun WeekHoursList(weekdayText: List<String>, todayIndex: Int, color: Color) {
    val mondayBasedToday = (todayIndex + 6) % 7
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        weekdayText.forEachIndexed { index, line ->
            val isToday = index == mondayBasedToday
            val parts = line.split(":", limit = 2).map { it.trim() }
            val label = parts.getOrNull(0) ?: ""
            val hours = parts.getOrNull(1) ?: ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) color else color.copy(alpha = 0.75f),
                )
                Text(
                    text = hours,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) color else color.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun statusHeadline(status: OpeningStatus): String = when (status) {
    OpeningStatus.AlwaysOpen -> stringResource(Res.string.open_24h)
    is OpeningStatus.Open -> stringResource(Res.string.open_now)
    is OpeningStatus.Closed -> stringResource(Res.string.closed_now)
    OpeningStatus.Unknown -> stringResource(Res.string.opening_hours)
}

/**
 * "Today 12:00–22:00" / "Closed today" / null — the always-visible hours line.
 * Returns null when we genuinely don't know (no weekdayText) or when the venue
 * is 24/7 (the headline already says "Open 24 hours").
 */
@Composable
private fun todaySubline(status: OpeningStatus, todayHours: String?): String? {
    if (status == OpeningStatus.AlwaysOpen) return null
    return when {
        todayHours == null -> if (status == OpeningStatus.Unknown) {
            stringResource(Res.string.hours_unknown)
        } else null
        todayHours.isClosedHours() -> stringResource(Res.string.closed_today)
        else -> stringResource(Res.string.today_hours, todayHours)
    }
}

/** "Closes 22:00" / "Opens in 25 min" / null — the secondary countdown. */
@Composable
private fun countdownSubline(status: OpeningStatus, nowMow: Int): String? = when (status) {
    OpeningStatus.AlwaysOpen, OpeningStatus.Unknown -> null
    is OpeningStatus.Open -> if (status.minutesUntilClose <= 60) {
        stringResource(Res.string.closes_in_min, status.minutesUntilClose)
    } else {
        stringResource(Res.string.closes_at, formatTimeFromNow(status.closeMow, status.minutesUntilClose, nowMow))
    }
    is OpeningStatus.Closed -> if (status.minutesUntilOpen <= 60) {
        stringResource(Res.string.opens_in_min, status.minutesUntilOpen)
    } else {
        stringResource(Res.string.opens_at, formatTimeFromNow(status.nextOpenMow, status.minutesUntilOpen, nowMow))
    }
}

/**
 * Pull just the time portion of today's `weekdayText` line — drops the leading
 * "Monday: " / "Friday: " label that would otherwise duplicate the date context.
 */
private fun todayHoursLine(weekdayText: List<String>, sundayBasedTodayIndex: Int): String? {
    if (weekdayText.isEmpty()) return null
    val mondayBased = (sundayBasedTodayIndex + 6) % 7
    val raw = weekdayText.getOrNull(mondayBased) ?: return null
    return raw.split(":", limit = 2).getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
}

/** Heuristic: Google's `weekdayText` uses "Closed" (or localised equivalent) for off-days. */
private fun String.isClosedHours(): Boolean {
    val s = lowercase()
    return s == "closed" || s == "geschlossen" || contains("休息") || contains("打烊")
}

@Composable
private fun formatTimeFromNow(targetMow: Int, deltaMinutes: Int, nowMow: Int): String {
    val time = mowToTimeOfDay(targetMow)
    val timeStr = formatTime(time.hour, time.minute)
    val minutesLeftToday = 1440 - (nowMow % 1440)
    return when {
        deltaMinutes < minutesLeftToday -> timeStr
        deltaMinutes < minutesLeftToday + 1440 ->
            stringResource(Res.string.tomorrow_at, timeStr)
        else -> {
            val dayName = stringResource(weekdayLongRes(mowToDayIndex(targetMow)))
            stringResource(Res.string.weekday_at, dayName, timeStr)
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$h:$m"
}

private fun weekdayLongRes(sundayBasedIndex: Int): StringResource = when (sundayBasedIndex) {
    0 -> Res.string.weekday_long_sun
    1 -> Res.string.weekday_long_mon
    2 -> Res.string.weekday_long_tue
    3 -> Res.string.weekday_long_wed
    4 -> Res.string.weekday_long_thu
    5 -> Res.string.weekday_long_fri
    else -> Res.string.weekday_long_sat
}

// endregion

// region Address + description + photos + footer --------------------------

@Composable
private fun AddressCard(
    restaurant: Restaurant,
    onNavigate: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = SECTION_SHAPE,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        restaurant.address.addressLine1,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    restaurant.address.addressLine2?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${restaurant.address.postalCode} ${restaurant.address.city} · ${restaurant.address.district}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Trailing nav button — opens Google Maps directions to this
                // place. The bottom action bar has "View on map" (place
                // card); this is the deeper action that starts navigation.
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable(onClick = onNavigate),
                ) {
                    Icon(
                        Icons.Filled.Directions,
                        contentDescription = stringResource(Res.string.action_directions),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(10.dp).size(20.dp),
                    )
                }
            }
            restaurant.chain?.let { chain ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(8.dp).size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = buildString {
                            append(chain.brand)
                            chain.branch?.let { append(MIDDOT_SEP).append(it) }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DescriptionCard(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = SECTION_SHAPE,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp),
        )
    }
}

/**
 * Pinwo partner perks card, shown when [Restaurant.hasDiscount]. Leads with the
 * discount marker symbol so it reads as the same identity users see on the map,
 * states the offer ([offer], falling back to generic copy), and links out to
 * pinwo.de for the full details.
 */
@Composable
private fun DiscountCard(offer: String?, onVisitPinwo: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = SECTION_SHAPE,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(Res.drawable.marker_discount),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = stringResource(Res.string.discount_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = offer ?: stringResource(Res.string.discount_card_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = onVisitPinwo,
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.discount_visit_pinwo),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// endregion

private fun Double.toFormattedRating(): String {
    val rounded = (this * 10).toInt() / 10.0
    val whole = rounded.toInt()
    val tenth = ((rounded - whole) * 10 + 0.5).toInt()
    return if (tenth == 0) "$whole.0" else "$whole.$tenth"
}
