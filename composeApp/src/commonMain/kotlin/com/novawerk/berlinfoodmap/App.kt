package com.novawerk.berlinfoodmap

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
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
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.ui.locale.LocalAppLocale
import com.novawerk.berlinfoodmap.ui.navigation.*
import com.novawerk.berlinfoodmap.ui.pages.detail.DetailScreen
import com.novawerk.berlinfoodmap.ui.pages.list.ListScreen
import com.novawerk.berlinfoodmap.ui.pages.map.MapScreen
import com.novawerk.berlinfoodmap.ui.pages.settings.SettingsScreen
import com.novawerk.berlinfoodmap.ui.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import berlinfoodmap.composeapp.generated.resources.Res
import berlinfoodmap.composeapp.generated.resources.nav_map
import berlinfoodmap.composeapp.generated.resources.nav_list
import berlinfoodmap.composeapp.generated.resources.settings

@Composable
fun App(component: AppComponent) {
    val settings = component.settingsRepository
    val authService = component.authService
    val restaurantRepository = component.restaurantRepository
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

    CompositionLocalProvider(
        LocalAppLocale provides language,
    ) {
        key(language) {
            AppTheme(darkMode = darkMode) {
                val navController = rememberNavController()
                val currentEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentEntry?.destination?.route

                val showBottomBar = currentRoute?.let {
                    it.contains("MapRoute") || it.contains("ListRoute")
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
                                    selected = currentRoute?.contains("ListRoute") == true,
                                    onClick = {
                                        navController.navigate(ListRoute) {
                                            popUpTo(MapRoute)
                                        }
                                    },
                                    icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null) },
                                    label = { Text(stringResource(Res.string.nav_list)) },
                                )
                                NavigationBarItem(
                                    selected = currentRoute?.contains("SettingsRoute") == true,
                                    onClick = { navController.navigate(SettingsRoute) },
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
                                onNavigateSettings = { navController.navigate(SettingsRoute) },
                                onNavigateList = {
                                    navController.navigate(ListRoute) {
                                        popUpTo(MapRoute)
                                    }
                                },
                                onNavigateDetail = { id -> navController.navigate(DetailRoute(id)) },
                            )
                        }

                        composable<ListRoute> {
                            ListScreen(
                                repository = restaurantRepository,
                                onNavigateSettings = { navController.navigate(SettingsRoute) },
                                onNavigateMap = {
                                    navController.navigate(MapRoute) {
                                        popUpTo(MapRoute) { inclusive = true }
                                    }
                                },
                                onNavigateDetail = { id -> navController.navigate(DetailRoute(id)) },
                            )
                        }

                        composable<DetailRoute> { backStackEntry ->
                            val route = backStackEntry.toRoute<DetailRoute>()
                            DetailScreen(
                                restaurantId = route.restaurantId,
                                repository = restaurantRepository,
                                authService = authService,
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
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
