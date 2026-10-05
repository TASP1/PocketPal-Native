package com.tasp1.pocketpal

import android.app.Application
import com.tasp1.pocketpal.data.AppContainer

class PocketPalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
