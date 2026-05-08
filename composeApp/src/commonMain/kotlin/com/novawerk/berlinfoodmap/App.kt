package com.novawerk.berlinfoodmap

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.domain.analytics.AnalyticsService
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale
import com.novawerk.berlinfoodmap.ui.navigation.*
import com.novawerk.berlinfoodmap.ui.pages.detail.DetailScreen
import com.novawerk.berlinfoodmap.ui.pages.map.MapScreen
import com.novawerk.berlinfoodmap.ui.pages.onboarding.OnboardingScreen
import com.novawerk.berlinfoodmap.ui.pages.settings.SettingsScreen
import com.novawerk.berlinfoodmap.ui.pages.splash.SplashScreen
import com.novawerk.berlinfoodmap.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_map
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

    var darkMode by remember { mutableStateOf("system") }
    var language by remember { mutableStateOf<String?>(null) }
    var phase by remember { mutableStateOf(AppPhase.Splash) }

    // Hoisted detail-sheet state — opening a restaurant updates this id and
    // shows a ModalBottomSheet over the current screen instead of navigating
    // to a new destination. This keeps the underlying screen (notably the
    // map) mounted, so dismissing the sheet doesn't trigger a re-render.
    var detailRestaurantId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        darkMode = settings.getDarkMode()
        language = settings.getLanguage()
        val onboardingShown = settings.getOnboardingShown()
        // Hold the splash for a beat so the brand reads cleanly even on a
        // fast warm start. The first composition is already on screen as the
        // splash; this delay gates the transition to the next phase.
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

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    LaunchedEffect(phase) {
        when (phase) {
            AppPhase.Splash -> analytics.logScreenView("Splash")
            AppPhase.Onboarding -> analytics.logScreenView("Onboarding")
            AppPhase.Main -> Unit // MainShell logs per-route inside the NavHost
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
                when (phase) {
                    AppPhase.Splash -> SplashScreen()
                    AppPhase.Onboarding -> OnboardingScreen(
                        onComplete = {
                            scope.launch { settings.setOnboardingShown(true) }
                            phase = AppPhase.Main
                        },
                    )
                    AppPhase.Main -> MainShell(
                        darkMode = darkMode,
                        language = language,
                        restaurantRepository = restaurantRepository,
                        favoritesRepository = favoritesRepository,
                        feedbackRepository = feedbackRepository,
                        authService = authService,
                        analytics = analytics,
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    darkMode: String,
    language: String?,
    restaurantRepository: com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository,
    favoritesRepository: com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository,
    feedbackRepository: com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository,
    authService: com.novawerk.berlinfoodmap.domain.auth.AuthService,
    analytics: AnalyticsService,
    detailRestaurantId: String?,
    onDetailIdChange: (String?) -> Unit,
    onDarkModeChange: (String) -> Unit,
    onLanguageChange: (String?) -> Unit,
) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        // Map nav routes to short, stable screen names. We don't pass the
        // raw kotlinx-serialization route string because it includes the
        // package prefix and reads as noise in the Analytics dashboard.
        val screen = when {
            currentRoute == null -> null
            currentRoute.contains("MapRoute") -> "Map"
            currentRoute.contains("SettingsRoute") -> "Settings"
            else -> null
        }
        screen?.let { analytics.logScreenView(it) }
    }

    val showBottomBar = currentRoute?.let {
        it.contains("MapRoute") || it.contains("SettingsRoute")
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                // Override the default NavigationBar palette so the
                // selected pill uses the brand red instead of the
                // sage/olive secondary container — those default
                // tones read as a muddy green/purple here.
                val navItemColors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    NavigationBarItem(
                        selected = currentRoute?.contains("MapRoute") == true,
                        onClick = {
                            navController.navigate(MapRoute) {
                                popUpTo(MapRoute) { inclusive = true }
                            }
                        },
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text(stringResource(Res.string.nav_map)) },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = currentRoute?.contains("SettingsRoute") == true,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(MapRoute)
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(Res.string.settings)) },
                        colors = navItemColors,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MapRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<MapRoute> {
                MapScreen(
                    repository = restaurantRepository,
                    favoritesRepository = favoritesRepository,
                    onNavigateDetail = { id -> onDetailIdChange(id) },
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    currentDarkMode = darkMode,
                    currentLanguage = language,
                    feedbackRepository = feedbackRepository,
                    onDarkModeChange = onDarkModeChange,
                    onLanguageChange = onLanguageChange,
                )
            }
        }
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
