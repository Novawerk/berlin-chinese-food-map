package com.novawerk.berlinfoodmap.ui.pages.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.restaurant.GooglePlaceData
import com.novawerk.berlinfoodmap.domain.restaurant.Restaurant
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.ui.components.cuisineDisplayName
import com.novawerk.berlinfoodmap.ui.components.cuisineIconResource
import com.novawerk.berlinfoodmap.ui.rememberUrlLauncher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.call_phone
import berlinfoodmap.composeapp.generated.resources.open_in_maps
import berlinfoodmap.composeapp.generated.resources.open_website
import berlinfoodmap.composeapp.generated.resources.opening_hours
import berlinfoodmap.composeapp.generated.resources.rating_count

/**
 * Sheet-friendly detail content. Hosted inside a [ModalBottomSheet] at the
 * app root so opening/closing it doesn't tear down the underlying screen
 * (notably the map, which is expensive to re-render).
 */
@Composable
fun DetailScreen(
    restaurantId: String,
    repository: RestaurantRepository,
    authService: AuthService,
) {
    var restaurant by remember(restaurantId) { mutableStateOf<Restaurant?>(null) }
    var loading by remember(restaurantId) { mutableStateOf(true) }
    val urlLauncher = rememberUrlLauncher()

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
            // Photos — admin-uploaded gallery first, then Google Places photos.
            // The cover image is the first one in this combined list.
            val photos = remember(r.galleries, r.googleData?.photoUrls) {
                buildList {
                    addAll(r.galleries)
                    r.googleData?.photoUrls?.let { addAll(it) }
                }.distinct()
            }
            if (photos.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { photos.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                ) { page ->
                    AsyncImage(
                        model = photos[page],
                        contentDescription = r.name.zh,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (photos.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(photos.size) { i ->
                            val active = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (active) 8.dp else 6.dp)
                                    .background(
                                        color = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                // Cuisine-iconed placeholder when no photo is available.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(cuisineIconResource(r.cuisineType)),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(96.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Restaurant name
                Text(r.name.zh, style = MaterialTheme.typography.headlineMedium)
                Text(
                    r.name.en,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Cuisine badge + price range
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(cuisineDisplayName(r.cuisineType)) },
                    )
                    r.priceRange?.let { price ->
                        Text(
                            text = price,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider()

                // Address section
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(r.address.addressLine1, style = MaterialTheme.typography.bodyMedium)
                        r.address.addressLine2?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "${r.address.postalCode} ${r.address.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            r.address.district,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Phone — fall back to the Google-fetched number if YAML is empty.
                val phoneNumber = r.phone ?: r.googleData?.formattedPhoneNumber
                phoneNumber?.let { phone ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = stringResource(Res.string.call_phone),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(phone, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Google Places enrichment — rating + hours + website
                r.googleData?.let { gd ->
                    HorizontalDivider()
                    GoogleDataSection(gd = gd, onOpenUrl = urlLauncher::open)
                }

                // Description
                r.description?.let { desc ->
                    HorizontalDivider()
                    Text(desc.zh, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        desc.en,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()

                // View count — keeps the existing analytics signal without
                // surfacing visit/favorite affordances.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${r.viewCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Open in Google Maps — placeId is preferred because it
                // resolves to the exact establishment card. The pre-fetched
                // `googleMapsUrl` is a fallback, and lat/lng search is the
                // last resort for restaurants without any Google data.
                val mapsUrl = r.googleData?.placeId?.let { id ->
                    "https://www.google.com/maps/place/?q=place_id:$id"
                }
                    ?: r.googleData?.googleMapsUrl
                    ?: "https://www.google.com/maps/search/?api=1&query=${r.latitude},${r.longitude}"
                OutlinedButton(
                    onClick = { urlLauncher.open(mapsUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.open_in_maps))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
}

@Composable
private fun GoogleDataSection(
    gd: GooglePlaceData,
    onOpenUrl: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Rating row
        gd.rating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = rating.toFormattedRating(),
                    style = MaterialTheme.typography.titleMedium,
                )
                gd.userRatingsTotal?.let { count ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.rating_count, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Weekly hours
        if (gd.weekdayText.isNotEmpty()) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.opening_hours),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    gd.weekdayText.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Website link
        gd.website?.let { website ->
            TextButton(
                onClick = { onOpenUrl(website) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.open_website),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

private fun Double.toFormattedRating(): String {
    val rounded = (this * 10).toInt() / 10.0
    val whole = rounded.toInt()
    val tenth = ((rounded - whole) * 10 + 0.5).toInt()
    return if (tenth == 0) "$whole.0" else "$whole.$tenth"
}
