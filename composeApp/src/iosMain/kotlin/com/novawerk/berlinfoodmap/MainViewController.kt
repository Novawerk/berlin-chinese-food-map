package com.novawerk.berlinfoodmap

import androidx.compose.ui.window.ComposeUIViewController
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.novawerk.berlinfoodmap.di.AppComponent
import com.novawerk.berlinfoodmap.di.create
import okio.Path.Companion.toPath

private const val DATA_STORE_FILE_NAME = "berlinfoodmap.preferences_pb"

fun MainViewController() = ComposeUIViewController {
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = { dataStorePath(DATA_STORE_FILE_NAME).toPath() }
    )
    val component = AppComponent.create(dataStore)
    App(component)
}
