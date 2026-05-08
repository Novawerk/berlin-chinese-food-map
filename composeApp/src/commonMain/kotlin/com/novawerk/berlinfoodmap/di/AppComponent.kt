package com.novawerk.berlinfoodmap.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.novawerk.berlinfoodmap.data.remote.FirebaseAuthService
import com.novawerk.berlinfoodmap.data.remote.FirestoreRestaurantRepository
import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.favorites.FavoritesRepository
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

    @Provides
    fun FirestoreRestaurantRepository.bind(): RestaurantRepository = this

    @Provides
    fun FirebaseAuthService.bind(): AuthService = this

    companion object
}

@KmpComponentCreate
expect fun AppComponent.Companion.create(
    dataStore: DataStore<Preferences>
): AppComponent
