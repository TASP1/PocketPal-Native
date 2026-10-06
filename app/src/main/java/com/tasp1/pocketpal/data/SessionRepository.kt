package com.tasp1.pocketpal.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tasp1.pocketpal.domain.ChatSession
import com.tasp1.pocketpal.domain.ChatTurn
import com.tasp1.pocketpal.domain.SessionPayload
import com.tasp1.pocketpal.domain.StoredTurn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.sessionStore by preferencesDataStore("pocketpal_sessions")

class SessionRepository(private val context: Context) {
    private val KEY_INDEX = stringPreferencesKey("session_index")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val sessions: Flow<List<ChatSession>> = context.sessionStore.data.map { prefs ->
        runCatching { json.decodeFromString<List<ChatSession>>(prefs[KEY_INDEX] ?: "[]") }
            .getOrDefault(emptyList())
            .sortedByDescending { it.updatedAt }
    }

    suspend fun load(sessionId: String): SessionPayload? {
        val key = stringPreferencesKey("sess_$sessionId")
        val raw = context.sessionStore.data.first()[key] ?: return null
        return runCatching { json.decodeFromString<SessionPayload>(raw) }.getOrNull()
    }

    suspend fun save(session: ChatSession, turns: List<ChatTurn>) {
        val title = session.title.ifBlank {
            turns.firstOrNull { it.role == ChatTurn.Role.User }?.content?.take(40) ?: "New chat"
        }
        val preview = turns.lastOrNull { it.role == ChatTurn.Role.User }?.content?.take(80).orEmpty()
        val payload = SessionPayload(
            session = session.copy(
                title = title,
                preview = preview,
                updatedAt = System.currentTimeMillis(),
            ),
            turns = turns.map {
                StoredTurn(
                    id = it.id,
                    role = if (it.role == ChatTurn.Role.User) "user" else "assistant",
                    content = it.content,
                    reasoning = it.reasoning,
                    sourcesJson = it.sourcesJson,
                )
            },
        )
        val key = stringPreferencesKey("sess_${session.id}")
        context.sessionStore.edit { prefs ->
            prefs[key] = json.encodeToString(payload)
            val index = runCatching {
                json.decodeFromString<List<ChatSession>>(prefs[KEY_INDEX] ?: "[]")
            }.getOrDefault(emptyList()).toMutableList()
            index.removeAll { it.id == session.id }
            index.add(0, payload.session)
            prefs[KEY_INDEX] = json.encodeToString(index.take(50))
        }
    }

    suspend fun delete(sessionId: String) {
        val key = stringPreferencesKey("sess_$sessionId")
        context.sessionStore.edit { prefs ->
            prefs.remove(key)
            val index = runCatching {
                json.decodeFromString<List<ChatSession>>(prefs[KEY_INDEX] ?: "[]")
            }.getOrDefault(emptyList()).filterNot { it.id == sessionId }
            prefs[KEY_INDEX] = json.encodeToString(index)
        }
    }

    fun newSessionId(): String = UUID.randomUUID().toString()
}
