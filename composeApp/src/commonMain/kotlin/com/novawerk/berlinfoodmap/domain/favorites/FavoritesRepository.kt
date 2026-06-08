package com.novawerk.berlinfoodmap.domain.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Inject

/**
 * Local-only favorites store. Backed by DataStore preferences as a
 * `Set<String>` of restaurant ids — privacy-first, no cloud sync.
 *
 * The list is small enough (typically < 100 entries) that a single-set key
 * is fine; we don't need a per-id document model.
 */
@Inject
open class FavoritesRepository(private val dataStore: DataStore<Preferences>) {

    private companion object {
        val FAVORITES = stringSetPreferencesKey("favorites")
    }

    fun observe(): Flow<Set<String>> =
        dataStore.data.map { it[FAVORITES] ?: emptySet() }

    suspend fun setFavorite(restaurantId: String, favorite: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (favorite) current + restaurantId else current - restaurantId
        }
    }

    suspend fun toggle(restaurantId: String) {
        dataStore.edit { prefs ->
            val current = prefs[FAVORITES] ?: emptySet()
            prefs[FAVORITES] = if (restaurantId in current) {
                current - restaurantId
            } else {
                current + restaurantId
            }
        }
    }
}
