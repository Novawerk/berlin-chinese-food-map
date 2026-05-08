package com.novawerk.berlinfoodmap.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.novawerk.berlinfoodmap.data.remote.FirebaseAnalyticsService
import com.novawerk.berlinfoodmap.data.remote.FirebaseAuthService
import com.novawerk.berlinfoodmap.data.remote.FirestoreFeedbackRepository
import com.novawerk.berlinfoodmap.data.remote.FirestoreRestaurantRepository
import com.novawerk.berlinfoodmap.domain.analytics.AnalyticsService
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository
import com.novawerk.berlinfoodmap.domain.restaurant.RestaurantRepository
import com.novawerk.berlinfoodmap.domain.settings.SettingsRepository
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

@Component
abstract class AppComponent(
    @get:Provides protected val dataStore: DataStore<Preferences>
) {
    abstract val settingsRepository: SettingsRepository
    abstract val favoritesRepository: FavoritesRepository
    abstract val restaurantRepository: RestaurantRepository
    abstract val authService: AuthService
    abstract val feedbackRepository: FeedbackRepository
    abstract val analyticsService: AnalyticsService

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
    dataStore: DataStore<Preferences>
): AppComponent
