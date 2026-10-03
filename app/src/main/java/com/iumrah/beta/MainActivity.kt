package com.iumrah.beta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private val appContainer get() = (application as IumrahApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLink(intent?.data)
        setContent { IumrahApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent.data)
    }

    private fun handleDeepLink(uri: Uri?) {
        uri ?: return
        val chrome = appContainer.chromeStore
        val parts = uri.pathSegments.filter { it.isNotBlank() }
        when {
            uri.scheme == "https" && uri.host == "iumrah.app" && parts.size >= 3 && parts[0] == "flights" && parts[1] == "package" ->
                chrome.openFlightPackage(parts[2])
            uri.scheme == "https" && uri.host == "iumrah.app" && parts.size >= 2 && (parts[0] == "hotel" || parts[0] == "h") ->
                chrome.openHotel(parts[1])
            uri.scheme == "iumrahapp" && uri.host == "hotel" && parts.isNotEmpty() ->
                chrome.openHotel(parts[0])
        }
    }
}
