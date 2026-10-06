package com.tasp1.pocketpal.data

import android.content.Context
import com.tasp1.pocketpal.BuildConfig
import com.tasp1.pocketpal.domain.ServerProfile
import com.tasp1.pocketpal.engine.EngineRouter
import com.tasp1.pocketpal.engine.RemoteOpenAIEngine
import com.tasp1.pocketpal.network.AgentShellClient

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsRepository(appContext)
    val sessions = SessionRepository(appContext)
    val attachments = AttachmentProcessor(appContext)

    private val defaultProfile = ServerProfile.kaggleBridge(
        BuildConfig.BRIDGE_URL,
        BuildConfig.BRIDGE_KEY,
    )

    val engines = EngineRouter(
        remote = RemoteOpenAIEngine(defaultProfile),
    )

    val shell = AgentShellClient(BuildConfig.BRIDGE_URL, BuildConfig.BRIDGE_KEY)

    /** Optional secondary Render agent host (gh-cli MCP etc.) */
    val renderAgent = AgentShellClient(
        BuildConfig.RENDER_AGENT_URL,
        BuildConfig.BRIDGE_KEY,
    )

    fun applyServer(url: String, key: String, useLocal: Boolean = false) {
        if (useLocal) {
            engines.useLocal = true
        } else {
            engines.useLocal = false
            engines.updateRemoteAuth(url, key)
            shell.updateCredentials(url, key)
        }
    }
}
