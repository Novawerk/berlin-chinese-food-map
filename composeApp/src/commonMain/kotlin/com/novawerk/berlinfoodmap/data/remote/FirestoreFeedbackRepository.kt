package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.domain.auth.AuthService
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackCategory
import com.novawerk.berlinfoodmap.domain.feedback.FeedbackRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import me.tatarka.inject.annotations.Inject

@Inject
class FirestoreFeedbackRepository(
    private val authService: AuthService,
) : FeedbackRepository {

    private val db = Firebase.firestore
    private val ref = db.collection("feedback")

    override suspend fun submit(
        category: FeedbackCategory,
        message: String,
        contact: String?,
        appVersion: String,
        platform: String,
        locale: String?,
    ): Result<String> = runCatching {
        // Anonymous sign-in normally completes at app start, but be defensive
        // — we need a uid for the security rule's `request.auth.uid == uid`
        // check, so claim one synchronously if it isn't there yet.
        val uid = authService.getCurrentUid() ?: authService.signInAnonymously()
        val data: Map<String, Any> = buildMap {
            put("uid", uid)
            put("category", category.firestoreValue)
            put("message", message.trim())
            contact?.trim()?.takeIf { it.isNotEmpty() }?.let { put("contact", it) }
            put("appVersion", appVersion)
            put("platform", platform)
            locale?.let { put("locale", it) }
            put("createdAt", Timestamp.now().seconds)
            put("status", "new")
        }
        val docRef = ref.add(data)
        docRef.id
    }
}
