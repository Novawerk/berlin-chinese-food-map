package com.novawerk.berlinfoodmap.domain.favorites

import com.novawerk.berlinfoodmap.domain.auth.AuthService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.tatarka.inject.annotations.Inject

@Inject
class FavoritesRepository(private val authService: AuthService) {

    private val db = Firebase.firestore

    private fun userFavoritesRef() =
        db.collection("users").document(authService.getCurrentUid() ?: "anonymous")
            .collection("favorites")

    val favoriteIds: Flow<Set<String>> = flow {
        val ids = getFavoriteIds()
        emit(ids)
    }

    suspend fun getFavoriteIds(): Set<String> {
        val uid = authService.getCurrentUid() ?: return emptySet()
        val snapshot = db.collection("users").document(uid)
            .collection("favorites").get()
        return snapshot.documents.map { it.id }.toSet()
    }

    suspend fun isFavorite(restaurantId: String): Boolean =
        getFavoriteIds().contains(restaurantId)

    suspend fun toggleFavorite(restaurantId: String) {
        val uid = authService.getCurrentUid() ?: return
        val ref = db.collection("users").document(uid)
            .collection("favorites").document(restaurantId)
        val existing = ref.get()
        if (existing.exists) {
            ref.delete()
        } else {
            val data: Map<String, Any> = mapOf(
                "addedAt" to dev.gitlive.firebase.firestore.Timestamp.now().seconds,
            )
            ref.set(data)
        }
    }

    suspend fun clearAll() {
        val uid = authService.getCurrentUid() ?: return
        val snapshot = db.collection("users").document(uid)
            .collection("favorites").get()
        snapshot.documents.forEach { it.reference.delete() }
    }
}
