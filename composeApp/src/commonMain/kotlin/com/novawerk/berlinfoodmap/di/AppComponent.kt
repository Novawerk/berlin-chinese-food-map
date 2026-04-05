package com.novawerk.berlinfoodmap.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.novawerk.berlinfoodmap.domain.settings.SettingsRepository
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides

@Component
abstract class AppComponent(
    @get:Provides protected val dataStore: DataStore<Preferences>
) {
    abstract val settingsRepository: SettingsRepository

    companion object
}

@KmpComponentCreate
expect fun AppComponent.Companion.create(
    dataStore: DataStore<Preferences>
): AppComponent
