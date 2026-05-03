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
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale
import com.novawerk.berlinfoodmap.ui.navigation.*
import com.novawerk.berlinfoodmap.ui.pages.detail.DetailScreen
import com.novawerk.berlinfoodmap.ui.pages.map.MapScreen
import com.novawerk.berlinfoodmap.ui.pages.search.SearchScreen
import com.novawerk.berlinfoodmap.ui.pages.settings.SettingsScreen
import com.novawerk.berlinfoodmap.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_map
import berlinfoodmap.composeapp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(component: AppComponent) {
    val settings = component.settingsRepository
    val authService = component.authService
    val restaurantRepository = component.restaurantRepository
    val scope = rememberCoroutineScope()

    var darkMode by remember { mutableStateOf("system") }
    var language by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }

    // Hoisted detail-sheet state — opening a restaurant updates this id and
    // shows a ModalBottomSheet over the current screen instead of navigating
    // to a new destination. This keeps the underlying screen (notably the
    // map) mounted, so dismissing the sheet doesn't trigger a re-render.
    var detailRestaurantId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        darkMode = settings.getDarkMode()
        language = settings.getLanguage()
        ready = true
        // Anonymous sign-in is needed for view/visit tracking but NOT for the
        // map or restaurant list to render — Firestore's persistent cache can
        // serve those reads while auth completes. Run it fire-and-forget so a
        // first-launch network round-trip doesn't gate first paint.
        if (authService.getCurrentUid() == null) {
            runCatching { authService.signInAnonymously() }
        }
    }

    if (!ready) return

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    CompositionLocalProvider(
        LocalAppLocale provides language,
    ) {
        key(language) {
            AppTheme(darkMode = darkMode) {
                val navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentEntry?.destination?.route

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
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = MapRoute,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable<MapRoute> {
                            MapScreen(
                                repository = restaurantRepository,
                                onNavigateDetail = { id -> detailRestaurantId = id },
                                onNavigateSearch = { navController.navigate(SearchRoute()) },
                            )
                        }

                        composable<SearchRoute> { backStackEntry ->
                            val route = backStackEntry.toRoute<SearchRoute>()
                            SearchScreen(
                                repository = restaurantRepository,
                                initialCuisine = route.initialCuisine,
                                initialDistrict = route.initialDistrict,
                                onNavigateDetail = { id -> detailRestaurantId = id },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                currentDarkMode = darkMode,
                                currentLanguage = language,
                                onDarkModeChange = { newMode ->
                                    darkMode = newMode
                                    scope.launch { settings.setDarkMode(newMode) }
                                },
                                onLanguageChange = { newLang ->
                                    language = newLang
                                    scope.launch { settings.setLanguage(newLang) }
                                },
                            )
                        }
                    }
                }

                detailRestaurantId?.let { id ->
                    val sheetState = rememberModalBottomSheetState(
                        skipPartiallyExpanded = true,
                    )
                    ModalBottomSheet(
                        onDismissRequest = { detailRestaurantId = null },
                        sheetState = sheetState,
                    ) {
                        DetailScreen(
                            restaurantId = id,
                            repository = restaurantRepository,
                            authService = authService,
                        )
                    }
                }
            }
        }
    }
}
