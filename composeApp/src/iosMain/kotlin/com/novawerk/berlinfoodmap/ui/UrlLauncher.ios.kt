package com.novawerk.berlinfoodmap.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUrlLauncher(): UrlLauncher = remember {
    object : UrlLauncher {
        override fun open(url: String) {
            val nsUrl = NSURL.URLWithString(url) ?: return
            val app = UIApplication.sharedApplication
            if (app.canOpenURL(nsUrl)) {
                app.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
            }
        }
    }
}
