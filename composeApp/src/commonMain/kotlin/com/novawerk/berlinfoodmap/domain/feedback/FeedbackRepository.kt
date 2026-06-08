package com.novawerk.berlinfoodmap.domain.feedback

interface FeedbackRepository {
    suspend fun submit(
        category: FeedbackCategory,
        message: String,
        contact: String?,
        appVersion: String,
        platform: String,
        locale: String?,
    ): Result<String>
}
