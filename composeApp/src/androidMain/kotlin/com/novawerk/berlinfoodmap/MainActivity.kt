package com.novawerk.berlinfoodmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.android.gms.maps.MapsInitializer
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.di.create
import okio.Path.Companion.toPath

private const val DATA_STORE_FILE_NAME = "berlinfoodmap.preferences_pb"

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

        val dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath() }
        )
        val component = AppComponent.create(dataStore)

        setContent {
            App(component)
        }
    }
}
