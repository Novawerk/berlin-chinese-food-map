package com.novawerk.berlinfoodmap.data.remote

import com.novawerk.berlinfoodmap.domain.analytics.AnalyticsService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import me.tatarka.inject.annotations.Inject

// Thin gitlive wrapper. Firebase auto-initializes on both platforms
// (google-services.json on Android, FirebaseApp.configure() on iOS), so
// constructing this service is enough — no extra init call needed.
@Inject
class FirebaseAnalyticsService : AnalyticsService {

    private val analytics = Firebase.analytics
    private val crashlytics = Firebase.crashlytics

    override fun logScreenView(screenName: String) {
        // Use Firebase's reserved screen_view event so the data lands in
        // the Analytics dashboard's screen-tracking reports rather than as
        // a generic custom event.
        analytics.logEvent(
            name = "screen_view",
            parameters = mapOf("screen_name" to screenName),
        )
    }

    override fun logEvent(name: String, params: Map<String, Any>) {
        analytics.logEvent(name = name, parameters = params.ifEmpty { null })
    }

    override fun setUserId(uid: String?) {
        analytics.setUserId(uid)
        // Crashlytics doesn't accept null — the only way to "clear" the
        // identifier is to overwrite it with a sentinel.
        crashlytics.setUserId(uid ?: "")
    }

    override fun setUserProperty(name: String, value: String?) {
        // gitlive's setUserProperty signature is non-null; coerce to empty
        // so callers can pass null when they want to clear a property.
        analytics.setUserProperty(name, value ?: "")
    }

    override fun recordException(throwable: Throwable, message: String?) {
        if (message != null) crashlytics.log(message)
        crashlytics.recordException(throwable)
    }
}
