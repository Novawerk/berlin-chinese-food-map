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
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.di.create
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
private val sLock = Any()

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

        val component = appComponent(applicationContext)

        setContent {
            App(component)
        }
    }
}
