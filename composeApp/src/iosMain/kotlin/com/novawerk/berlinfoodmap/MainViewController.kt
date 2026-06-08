package com.novawerk.berlinfoodmap

import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.di.create
import okio.Path.Companion.toPath

private const val DATA_STORE_FILE_NAME = "berlinfoodmap.preferences_pb"

private var sCoilConfigured = false

fun MainViewController() = ComposeUIViewController {
    if (!sCoilConfigured) {
        // Register the singleton Coil ImageLoader factory before anything
        // in the DI graph (notably MapViewModel) touches
        // `SingletonImageLoader.get()`. See MainActivity.android.kt for
        // the same dance on Android.
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .crossfade(true)
                .build()
        }
        sCoilConfigured = true
    }
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStorePath(DATA_STORE_FILE_NAME).toPath() }
    )
    val component = AppComponent.create(dataStore, PlatformContext.INSTANCE)
    App(component)
}
