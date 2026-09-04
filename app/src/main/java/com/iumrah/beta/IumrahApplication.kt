package com.iumrah.beta

import android.app.Application
import com.iumrah.beta.core.di.IumrahAppContainer

class IumrahApplication : Application() {
    lateinit var container: IumrahAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = IumrahAppContainer(this)
    }
}
