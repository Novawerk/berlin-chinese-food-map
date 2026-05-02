package com.novawerk.berlinfoodmap

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import com.novawerk.berlinfoodmap.ui.pages.favorites.FavoritesScreen
import com.novawerk.berlinfoodmap.ui.pages.map.MapScreen
import com.novawerk.berlinfoodmap.ui.pages.search.SearchScreen
import com.novawerk.berlinfoodmap.ui.pages.settings.SettingsScreen
import com.novawerk.berlinfoodmap.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_map
import berlinfoodmap.composeapp.generated.resources.nav_favorites
import berlinfoodmap.composeapp.generated.resources.settings

@Composable
fun App(component: AppComponent) {
    val settings = component.settingsRepository
    val authService = component.authService
    val restaurantRepository = component.restaurantRepository
    val favoritesRepository = component.favoritesRepository
    val scope = rememberCoroutineScope()

    var darkMode by remember { mutableStateOf("system") }
    var language by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        darkMode = settings.getDarkMode()
        language = settings.getLanguage()
        // Anonymous sign-in
        if (authService.getCurrentUid() == null) {
            authService.signInAnonymously()
        }
        ready = true
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
                    it.contains("MapRoute") ||
                        it.contains("FavoritesRoute") ||
                        it.contains("SettingsRoute")
                } ?: true

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentRoute?.contains("MapRoute") == true,
                                    onClick = {
                                        navController.navigate(MapRoute) {
                                            popUpTo(MapRoute) { inclusive = true }
                                        }
                                    },
                                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                                    label = { Text(stringResource(Res.string.nav_map)) },
                                )
                                NavigationBarItem(
                                    selected = currentRoute?.contains("FavoritesRoute") == true,
                                    onClick = {
                                        navController.navigate(FavoritesRoute) {
                                            popUpTo(MapRoute)
                                        }
                                    },
                                    icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                                    label = { Text(stringResource(Res.string.nav_favorites)) },
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
                                onNavigateDetail = { id -> navController.navigate(DetailRoute(id)) },
                                onNavigateSearch = { navController.navigate(SearchRoute()) },
                            )
                        }

                        composable<FavoritesRoute> {
                            FavoritesScreen(
                                repository = restaurantRepository,
                                favoritesRepository = favoritesRepository,
                                onNavigateDetail = { id -> navController.navigate(DetailRoute(id)) },
                            )
                        }

                        composable<DetailRoute> { backStackEntry ->
                            val route = backStackEntry.toRoute<DetailRoute>()
                            DetailScreen(
                                restaurantId = route.restaurantId,
                                repository = restaurantRepository,
                                authService = authService,
                                favoritesRepository = favoritesRepository,
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable<SearchRoute> { backStackEntry ->
                            val route = backStackEntry.toRoute<SearchRoute>()
                            SearchScreen(
                                repository = restaurantRepository,
                                initialCuisine = route.initialCuisine,
                                initialDistrict = route.initialDistrict,
                                onNavigateDetail = { id -> navController.navigate(DetailRoute(id)) },
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
                                favoritesRepository = favoritesRepository,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
