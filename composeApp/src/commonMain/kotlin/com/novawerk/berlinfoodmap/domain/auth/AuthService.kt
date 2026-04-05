package com.novawerk.berlinfoodmap.domain.auth

interface AuthService {
    suspend fun signInAnonymously(): String
    fun getCurrentUid(): String?
}
