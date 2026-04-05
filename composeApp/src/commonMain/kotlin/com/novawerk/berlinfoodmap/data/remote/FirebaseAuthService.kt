package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.domain.auth.AuthService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import me.tatarka.inject.annotations.Inject

@Inject
class FirebaseAuthService : AuthService {

    private val auth = Firebase.auth

    override suspend fun signInAnonymously(): String {
        val result = auth.signInAnonymously()
        return result.user!!.uid
    }

    override fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }
}
