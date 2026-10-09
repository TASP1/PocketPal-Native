package com.tasp1.pocketpal

import android.app.Application
import com.tasp1.pocketpal.core.CrashGuard
import com.tasp1.pocketpal.data.AppContainer

class PocketPalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
        try {
            container = AppContainer(this)
        } catch (t: Throwable) {
            CrashGuard.softFail("AppContainer", t, "Failed to start app data layer")
            // Minimal container retry once
            container = AppContainer(this)
        }
    }
}
