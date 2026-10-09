package com.tasp1.pocketpal.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global crash / ANR-adjacent safety:
 * - Logs uncaught exceptions to file
 * - Shows a short toast on main-thread failures when possible
 * - Does not swallow native crashes; focuses on Java/Kotlin path
 */
object CrashGuard {
    private const val TAG = "CrashGuard"
    private val installed = AtomicBoolean(false)
    @Volatile private var appCtx: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun install(context: Context) {
        if (!installed.compareAndSet(false, true)) return
        appCtx = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persist(throwable, thread.name) }
            Log.e(TAG, "Uncaught on ${thread.name}", throwable)
            // Prefer previous (system) so process still terminates cleanly after log
            previous?.uncaughtException(thread, throwable)
                ?: run {
                    // Last resort
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                }
        }
        // Soft main-thread trap for posted failures
        mainHandler.post {
            Log.i(TAG, "CrashGuard active")
        }
    }

    fun softFail(where: String, t: Throwable, userMessage: String? = null) {
        Log.e(TAG, "softFail@$where: ${t.message}", t)
        runCatching { persist(t, where) }
        val msg = userMessage ?: "Something went wrong — try again"
        val ctx = appCtx ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
        } else {
            mainHandler.post {
                runCatching { Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun runCatchingSoft(where: String, userMessage: String? = null, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            softFail(where, t, userMessage)
        }
    }

    suspend fun <T> suspendCatch(
        where: String,
        fallback: T,
        userMessage: String? = null,
        block: suspend () -> T,
    ): T {
        return try {
            block()
        } catch (t: Throwable) {
            softFail(where, t, userMessage)
            fallback
        }
    }

    private fun persist(t: Throwable, thread: String) {
        val ctx = appCtx ?: return
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        val text = "thread=$thread\n${sw}\n----\n"
        val dir = File(ctx.filesDir, "crash_logs").apply { mkdirs() }
        File(dir, "crash_${System.currentTimeMillis()}.txt").writeText(text)
        // keep last 20
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(20)?.forEach { it.delete() }
    }

    fun lastCrashSnippet(context: Context): String? {
        val dir = File(context.filesDir, "crash_logs")
        val f = dir.listFiles()?.maxByOrNull { it.lastModified() } ?: return null
        return f.readText().take(2000)
    }
}
