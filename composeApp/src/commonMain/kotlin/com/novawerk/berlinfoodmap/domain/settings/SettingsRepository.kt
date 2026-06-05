package com.novawerk.berlinfoodmap.domain.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

@Inject
open class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    }

    suspend fun getDarkMode(): String =
        dataStore.data.map { it[DARK_MODE] ?: "system" }.first()

    suspend fun setDarkMode(value: String) {
        dataStore.edit { it[DARK_MODE] = value }
    }

    suspend fun getLanguage(): String? =
        dataStore.data.map { it[LANGUAGE] }.first()

    suspend fun setLanguage(value: String?) {
        dataStore.edit {
            if (value == null) it.remove(LANGUAGE)
            else it[LANGUAGE] = value
        }
    }

    suspend fun getOnboardingShown(): Boolean =
        dataStore.data.map { it[ONBOARDING_SHOWN] ?: false }.first()

    suspend fun setOnboardingShown(value: Boolean) {
        dataStore.edit { it[ONBOARDING_SHOWN] = value }
    }
}
