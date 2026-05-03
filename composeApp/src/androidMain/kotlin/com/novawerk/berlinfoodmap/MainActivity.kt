package com.novawerk.berlinfoodmap

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.android.gms.maps.MapsInitializer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.di.create
import com.novawerk.berlinfoodmap.ui.pages.map.locationPermissionAppContext
import okio.Path.Companion.toPath

private const val DATA_STORE_FILE_NAME = "berlinfoodmap.preferences_pb"

// DataStore + AppComponent must be process-singletons. Configuration changes
// (rotation, dark-mode toggle, language switch) recreate MainActivity, so
// instantiating either in onCreate produces the
//
//   IllegalStateException: There are multiple DataStores active for the
//   same file: berlinfoodmap.preferences_pb
//
// crash on the second activity instance.
@Volatile private var sDataStore: DataStore<Preferences>? = null
@Volatile private var sComponent: AppComponent? = null
@Volatile private var sFirestoreConfigured = false
private val sLock = Any()

// Configure Firestore's persistent cache before anything else touches the
// FirebaseFirestore instance. This must happen before kotlin-inject
// constructs FirestoreRestaurantRepository (which calls Firebase.firestore
// for the first time and effectively "starts" the instance — settings
// changes after that throw IllegalStateException).
private fun ensureFirestoreConfigured() {
    if (sFirestoreConfigured) return
    synchronized(sLock) {
        if (sFirestoreConfigured) return
        runCatching {
            FirebaseFirestore.getInstance().firestoreSettings =
                FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
        }
        sFirestoreConfigured = true
    }
}

private fun appDataStore(context: Context): DataStore<Preferences> =
    sDataStore ?: synchronized(sLock) {
        sDataStore ?: PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                context.applicationContext.filesDir
                    .resolve(DATA_STORE_FILE_NAME)
                    .absolutePath
                    .toPath()
            },
        ).also { sDataStore = it }
    }

private fun appComponent(context: Context): AppComponent =
    sComponent ?: synchronized(sLock) {
        sComponent ?: AppComponent.create(appDataStore(context))
            .also { sComponent = it }
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Kick off the Google Maps SDK warm-up before composition starts.
        // Without this the SDK only begins loading when the first MapView is
        // attached, adding ~200-500ms to first map paint. The empty callback
        // is required by the API; we don't need the renderer result.
        MapsInitializer.initialize(
            applicationContext,
            MapsInitializer.Renderer.LATEST,
        ) {}

        ensureFirestoreConfigured()

        // Capture app context for hasLocationPermission() — used by the
        // map's startup-time silent prefetch to skip the request when
        // permission isn't granted yet, so we don't auto-prompt.
        locationPermissionAppContext = applicationContext

        val component = appComponent(applicationContext)

        setContent {
            App(component)
        }
    }
}
