package com.novawerk.berlinfoodmap.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberUrlLauncher(): UrlLauncher {
    val context = LocalContext.current
    return remember(context) {
        UrlLauncher { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // No handler available — ignore.
            }
        }
    }
}

private inline fun UrlLauncher(crossinline body: (String) -> Unit): UrlLauncher =
    object : UrlLauncher {
        override fun open(url: String) = body(url)
    }
