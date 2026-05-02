package com.novawerk.berlinfoodmap.ui

import androidx.compose.runtime.Composable

interface UrlLauncher {
    fun open(url: String)
}

@Composable
expect fun rememberUrlLauncher(): UrlLauncher
