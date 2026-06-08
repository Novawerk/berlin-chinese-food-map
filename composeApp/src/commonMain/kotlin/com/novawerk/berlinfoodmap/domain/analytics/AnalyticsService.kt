package com.novawerk.berlinfoodmap.domain.analytics

interface AnalyticsService {
    fun logScreenView(screenName: String)
    fun logEvent(name: String, params: Map<String, Any> = emptyMap())
    fun setUserId(uid: String?)
    fun setUserProperty(name: String, value: String?)
    fun recordException(throwable: Throwable, message: String? = null)
}
