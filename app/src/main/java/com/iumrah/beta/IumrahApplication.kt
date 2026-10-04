package com.iumrah.beta

import android.app.Application
import com.iumrah.beta.core.di.IumrahAppContainer
import com.iumrah.beta.core.push.IumrahFirebaseMessagingService

class IumrahApplication : Application() {
    lateinit var container: IumrahAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = IumrahAppContainer(this)
        IumrahFirebaseMessagingService.createNotificationChannel(this)
        container.pushManager.initialize()
    }
}
