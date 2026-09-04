package com.iumrah.beta.core.di

import android.content.Context
import com.iumrah.beta.core.navigation.AppChromeStore
import com.iumrah.beta.core.network.APIClient
import com.iumrah.beta.core.security.IumrahAccountDeviceIdentity
import com.iumrah.beta.core.security.SecureJsonStore
import com.iumrah.beta.core.settings.AppSettingsStore
import com.iumrah.beta.data.account.IumrahAccountService
import com.iumrah.beta.data.account.IumrahAccountStore

class IumrahAppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val secureStore = SecureJsonStore(appContext)

    val apiClient = APIClient()
    val deviceIdentity = IumrahAccountDeviceIdentity(appContext, secureStore)
    val accountService = IumrahAccountService(apiClient, deviceIdentity)
    val accountStore = IumrahAccountStore(accountService, secureStore)
    val settingsStore = AppSettingsStore(appContext)
    val chromeStore = AppChromeStore()
}
