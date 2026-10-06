package com.tasp1.pocketpal.data

import android.content.Context
import com.tasp1.pocketpal.BuildConfig
import com.tasp1.pocketpal.domain.ServerProfile
import com.tasp1.pocketpal.engine.EngineRouter
import com.tasp1.pocketpal.engine.RemoteOpenAIEngine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsRepository(appContext)
    val attachments = AttachmentProcessor(appContext)

    private val defaultProfile = ServerProfile.kaggleBridge(
        BuildConfig.BRIDGE_URL,
        BuildConfig.BRIDGE_KEY,
    )

    val engines = EngineRouter(
        remote = RemoteOpenAIEngine(defaultProfile),
    )

    fun applyServer(url: String, key: String, useLocal: Boolean = false) {
        if (useLocal) {
            engines.useLocal = true
        } else {
            engines.useLocal = false
            engines.updateRemoteAuth(url, key)
        }
    }
}
