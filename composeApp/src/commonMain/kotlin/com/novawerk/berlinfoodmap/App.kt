package com.novawerk.berlinfoodmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.domain.analytics.AnalyticsService
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale
import com.novawerk.berlinfoodmap.domain.restaurant.family
import com.novawerk.berlinfoodmap.ui.pages.detail.DetailScreen
import com.novawerk.berlinfoodmap.ui.pages.favorites.FavoritesSheet
import com.novawerk.berlinfoodmap.ui.pages.map.MapCommands
import com.novawerk.berlinfoodmap.ui.pages.map.MapControlViewModel
import com.novawerk.berlinfoodmap.ui.pages.map.MapScreen
import com.novawerk.berlinfoodmap.ui.pages.map.MapViewModel
import com.novawerk.berlinfoodmap.ui.pages.onboarding.OnboardingScreen
import com.novawerk.berlinfoodmap.ui.pages.search.SearchAndFilterSheet
import com.novawerk.berlinfoodmap.ui.pages.settings.SettingsScreen
import com.novawerk.berlinfoodmap.ui.pages.splash.SplashScreen
import com.novawerk.berlinfoodmap.ui.theme.AppTheme
import eu.buney.maps.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_favorites
import berlinfoodmap.composeapp.generated.resources.nav_locate
import berlinfoodmap.composeapp.generated.resources.nav_search
import berlinfoodmap.composeapp.generated.resources.settings

private enum class AppPhase { Splash, Onboarding, Main }

private const val SPLASH_DURATION_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(component: AppComponent) {
    val settings = component.settingsRepository
    val favoritesRepository = component.favoritesRepository
    val authService = component.authService
    val restaurantRepository = component.restaurantRepository
    val feedbackRepository = component.feedbackRepository
    val analytics = component.analyticsService
    val scope = rememberCoroutineScope()

    // Process-lifetime singletons via DI. Touching them at the top of App()
    // triggers their `init` blocks during the splash hold — MapViewModel's
    // dependency on RestaurantStore transitively kicks off the Firestore +
    // DataStore observers and Coil cover prefetch; MapControlViewModel
    // fires a best-effort first location fetch. By the time the user
    // reaches the map, data is already in memory.
    val mapViewModel = component.mapViewModel
    val mapControlViewModel = component.mapControlViewModel

    var darkMode by remember { mutableStateOf("system") }
    var language by remember { mutableStateOf<String?>(null) }
    var phase by remember { mutableStateOf(AppPhase.Splash) }

    // Hoisted detail-sheet state — opening a restaurant updates this id and
    // shows a ModalBottomSheet over the current screen instead of navigating
    // to a new destination. This keeps the underlying map mounted so
    // dismissing the sheet doesn't trigger a re-render.
    var detailRestaurantId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        darkMode = settings.getDarkMode()
        language = settings.getLanguage()
        val onboardingShown = settings.getOnboardingShown()
        // Hold the splash for a beat so the brand reads cleanly even on a
        // fast warm start. The MapViewModel singleton accessed above is
        // already running its Firestore + favorites observers in parallel
        // with this delay, so the user reaches a populated map.
        delay(SPLASH_DURATION_MS)
        phase = if (onboardingShown) AppPhase.Main else AppPhase.Onboarding
        // Anonymous sign-in is needed for view/visit tracking but NOT for the
        // map or restaurant list to render — Firestore's persistent cache can
        // serve those reads while auth completes. Run it fire-and-forget so a
        // first-launch network round-trip doesn't gate first paint.
        if (authService.getCurrentUid() == null) {
            runCatching { authService.signInAnonymously() }
        }
        // Tag the Analytics user / Crashlytics session with the anon uid so
        // crash reports group per-install and we can join Analytics events
        // back to the same id. Cheap, fire-and-forget.
        analytics.setUserId(authService.getCurrentUid())
    }

    LaunchedEffect(phase) {
        when (phase) {
            AppPhase.Splash -> analytics.logScreenView("Splash")
            AppPhase.Onboarding -> analytics.logScreenView("Onboarding")
            AppPhase.Main -> Unit // MainShell logs per-surface
        }
    }

    LaunchedEffect(detailRestaurantId) {
        detailRestaurantId?.let { id ->
            analytics.logEvent(
                name = "restaurant_open",
                params = mapOf("restaurant_id" to id),
            )
            analytics.logScreenView("Detail")
        }
    }

    CompositionLocalProvider(
        LocalAppLocale provides language,
    ) {
        key(language) {
            AppTheme(darkMode = darkMode) {
                // MainShell renders during BOTH Splash and Main phases so the
                // GoogleMap composable mounts immediately at app start —
                // MapView creation, tile network fetch, and onMapLoaded all
                // happen in parallel with the splash hold instead of starting
                // only after splash dismisses. The SplashScreen sits on top
                // as an opaque overlay and fades out when phase transitions
                // to Main, by which point the map is typically already loaded.
                Box(modifier = Modifier.fillMaxSize()) {
                    if (phase != AppPhase.Onboarding) {
                        MainShell(
                            mapViewModel = mapViewModel,
                            mapControlViewModel = mapControlViewModel,
                            darkMode = darkMode,
                            language = language,
                            restaurantRepository = restaurantRepository,
                            favoritesRepository = favoritesRepository,
                            feedbackRepository = feedbackRepository,
                            authService = authService,
                            analytics = analytics,
                            isActive = phase == AppPhase.Main,
                            detailRestaurantId = detailRestaurantId,
                            onDetailIdChange = { detailRestaurantId = it },
                            onDarkModeChange = { newMode ->
                                darkMode = newMode
                                scope.launch { settings.setDarkMode(newMode) }
                                analytics.logEvent(
                                    name = "dark_mode_change",
                                    params = mapOf("mode" to newMode),
                                )
                            },
                            onLanguageChange = { newLang ->
                                language = newLang
                                scope.launch { settings.setLanguage(newLang) }
                                analytics.logEvent(
                                    name = "language_change",
                                    params = mapOf("language" to (newLang ?: "system")),
                                )
                            },
                        )
                    }
                    if (phase == AppPhase.Onboarding) {
                        OnboardingScreen(
                            onComplete = {
                                scope.launch { settings.setOnboardingShown(true) }
                                phase = AppPhase.Main
                            },
                        )
                    }
                    AnimatedVisibility(
                        visible = phase == AppPhase.Splash,
                        enter = androidx.compose.animation.EnterTransition.None,
                        exit = fadeOut(),
                    ) {
                        SplashScreen()
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)
@Composable
private fun MainShell(
    mapViewModel: MapViewModel,
    mapControlViewModel: MapControlViewModel,
    darkMode: String,
    language: String?,
    restaurantRepository: com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository,
    favoritesRepository: com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository,
    feedbackRepository: com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository,
    authService: com.novawerk.berlinfoodmap.domain.auth.AuthService,
    analytics: AnalyticsService,
    /**
     * False while MainShell is mounted underneath the splash overlay (so the
     * map can warm up in the background). Gates user-facing side-effects
     * like analytics screen-view logging that would otherwise double-fire
     * before the user actually sees the screen.
     */
    isActive: Boolean,
    detailRestaurantId: String?,
    onDetailIdChange: (String?) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String?) -> Unit,
) {
    // Map stays full-screen with a 3-icon bottom bar overlaid on top.
    // Settings · Search · Favorites are all ModalBottomSheets; Locate is a
    // one-shot action triggered by the top-right LocateFab on MapScreen
    // (also toggles walking-radius rings). None of the bottom-nav buttons
    // is a "tab" in the navigational sense — there's no selected state.
    val mapCommands = remember { MapCommands() }
    var searchSheetOpen by rememberSaveable { mutableStateOf(false) }
    var settingsSheetOpen by rememberSaveable { mutableStateOf(false) }
    var favoritesSheetOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(searchSheetOpen, settingsSheetOpen, favoritesSheetOpen, isActive) {
        // Suppress while we're mounted invisibly behind the splash overlay.
        if (!isActive) return@LaunchedEffect
        val screen = when {
            settingsSheetOpen -> "Settings"
            searchSheetOpen -> "Search"
            favoritesSheetOpen -> "Favorites"
            else -> "Map"
        }
        analytics.logScreenView(screen)
    }

    Scaffold(
        bottomBar = {
            MainBottomBar(
                onSettingsClick = { settingsSheetOpen = true },
                onSearchClick = { searchSheetOpen = true },
                onFavoritesClick = { favoritesSheetOpen = true },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Always rendered. MapScreen never leaves composition for the
            // lifetime of the Main phase, so its camera state, marker bitmap
            // caches, and the GoogleMap composable's underlying MapView all
            // persist regardless of which overlay is foregrounded.
            MapScreen(
                viewModel = mapViewModel,
                controlVm = mapControlViewModel,
                commands = mapCommands,
                snackbarHostState = snackbarHostState,
                onNavigateDetail = { id -> onDetailIdChange(id) },
            )

        }
    }

    if (settingsSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { settingsSheetOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SettingsScreen(
                currentDarkMode = darkMode,
                currentLanguage = language,
                feedbackRepository = feedbackRepository,
                onDarkModeChange = onDarkModeChange,
                onLanguageChange = onLanguageChange,
            )
        }
    }

    if (searchSheetOpen) {
        SearchAndFilterSheet(
            allRestaurants = mapViewModel.allRestaurants,
            favorites = mapViewModel.favorites,
            onPickTag = { tag ->
                // Apply as sole filter for its family; clear any prior selection
                // so single-tap really means "show me this and only this".
                mapViewModel.resetFilters()
                when (tag.family()) {
                    com.novawerk.berlinfoodmap.domain.restaurant.TagFamily.REGIONAL ->
                        mapViewModel.setCuisines(setOf(tag))
                    com.novawerk.berlinfoodmap.domain.restaurant.TagFamily.FORMAT ->
                        mapViewModel.setFormats(setOf(tag))
                }
            },
            onShowOnMap = { mapViewModel.resetFilters() },
            onShowFavoritesOnMap = {
                mapViewModel.resetFilters()
                mapViewModel.toggleFavoritesOnly(true)
            },
            onOpenFavorites = {
                searchSheetOpen = false
                favoritesSheetOpen = true
            },
            onRestaurantClick = { restaurant ->
                searchSheetOpen = false
                mapCommands.requestPan(
                    target = LatLng(restaurant.latitude, restaurant.longitude),
                    zoom = 16f,
                )
                onDetailIdChange(restaurant.id)
            },
            onDismiss = { searchSheetOpen = false },
        )
    }

    if (favoritesSheetOpen) {
        val favoriteRestaurants = remember(mapViewModel.allRestaurants, mapViewModel.favorites) {
            mapViewModel.allRestaurants.filter { it.id in mapViewModel.favorites }
        }
        FavoritesSheet(
            favorites = favoriteRestaurants,
            onRestaurantClick = { id ->
                favoritesSheetOpen = false
                val r = mapViewModel.allRestaurants.firstOrNull { it.id == id }
                if (r != null) {
                    mapCommands.requestPan(
                        target = LatLng(r.latitude, r.longitude),
                        zoom = 16f,
                    )
                }
                onDetailIdChange(id)
            },
            onBrowseMap = { favoritesSheetOpen = false },
            onDismiss = { favoritesSheetOpen = false },
        )
    }

    detailRestaurantId?.let { id ->
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
        ModalBottomSheet(
            onDismissRequest = { onDetailIdChange(null) },
            sheetState = sheetState,
        ) {
            DetailScreen(
                restaurantId = id,
                repository = restaurantRepository,
                favoritesRepository = favoritesRepository,
                authService = authService,
            )
        }
    }
}

@Composable
private fun MainBottomBar(
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
) {
    // Custom 3-icon bar — Settings · Search · Favorites (per May-24 design).
    // Locate moved to a top-right FAB on the map itself.
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomBarIcon(
                icon = Icons.Filled.Settings,
                contentDescription = stringResource(Res.string.settings),
                onClick = onSettingsClick,
            )
            BottomBarIcon(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(Res.string.nav_search),
                onClick = onSearchClick,
            )
            BottomBarIcon(
                icon = Icons.Filled.Favorite,
                contentDescription = stringResource(Res.string.nav_favorites),
                onClick = onFavoritesClick,
            )
        }
    }
}

@Composable
private fun BottomBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
