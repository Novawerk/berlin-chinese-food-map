package com.novawerk.berlinfoodmap.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.PlatformContext
import com.novawerk.berlinfoodmap.data.remote.FirebaseAnalyticsService
import com.novawerk.berlinfoodmap.data.remote.FirebaseAuthService
import com.novawerk.berlinfoodmap.data.remote.FirestoreFeedbackRepository
import com.novawerk.berlinfoodmap.data.remote.FirestoreRestaurantRepository
import com.novawerk.berlinfoodmap.data.store.RestaurantStore
import com.novawerk.berlinfoodmap.domain.analytics.AnalyticsService
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.settings.SettingsRepository
import com.novawerk.berlinfoodmap.ui.pages.map.MapControlViewModel
import com.novawerk.berlinfoodmap.ui.pages.map.MapViewModel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

/**
 * Process-lifetime DI scope. Apply to a class with `@AppScope @Inject` to
 * make kotlin-inject cache a single instance per [AppComponent]. Without
 * this, `@Inject` alone produces a NEW instance on every component
 * property access — which silently starts a fresh Firestore observer per
 * `App()` recomposition and ends up melting the heap.
 */
@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class AppScope

@AppScope
@Component
abstract class AppComponent(
    @get:Provides protected val dataStore: DataStore<Preferences>,
    @get:Provides protected val platformContext: PlatformContext,
) {
    abstract val settingsRepository: SettingsRepository
    abstract val favoritesRepository: FavoritesRepository
    abstract val restaurantRepository: RestaurantRepository
    abstract val authService: AuthService
    abstract val feedbackRepository: FeedbackRepository
    abstract val analyticsService: AnalyticsService

    // App-scoped data layer for the map: holds the restaurant list,
    // favorites set, and Coil-loaded cover bitmaps. Touched at the top of
    // App() during splash so its `init` block (Firestore + DataStore
    // observers, cover prefetch) runs before MapScreen mounts.
    abstract val restaurantStore: RestaurantStore

    // App-scoped UI state holders for the map. Both also get touched
    // during splash so the user reaches a fully-warmed map.
    abstract val mapViewModel: MapViewModel
    abstract val mapControlViewModel: MapControlViewModel

    @Provides
    fun FirestoreRestaurantRepository.bind(): RestaurantRepository = this

    @Provides
    fun FirebaseAuthService.bind(): AuthService = this

    @Provides
    fun FirestoreFeedbackRepository.bind(): FeedbackRepository = this

    @Provides
    fun FirebaseAnalyticsService.bind(): AnalyticsService = this

    companion object
}

@KmpComponentCreate
expect fun AppComponent.Companion.create(
    dataStore: DataStore<Preferences>,
    platformContext: PlatformContext,
): AppComponent
