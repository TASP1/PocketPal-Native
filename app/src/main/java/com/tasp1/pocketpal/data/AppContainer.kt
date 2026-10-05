package com.tasp1.pocketpal.data

import android.content.Context
import com.tasp1.pocketpal.BuildConfig
import com.tasp1.pocketpal.network.BridgeClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsRepository(appContext)
    val bridge = BridgeClient(BuildConfig.BRIDGE_URL, BuildConfig.BRIDGE_KEY)
    val attachments = AttachmentProcessor(appContext)
}
