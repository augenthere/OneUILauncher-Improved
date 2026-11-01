package com.luna.oneuilauncher.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler that:
 * 1. Catches uncaught exceptions
 * 2. Writes a structured crash report to app-private storage
 * 3. Logs to logcat with a consistent tag
 * 4. Provides a CoroutineExceptionHandler for ViewModel scopes
 *
 * This is a major stability improvement over the original which had no crash handling.
 */
object CrashHandler {

    private const val TAG = "OneUILauncher"
    private const val CRASH_DIR = "crash_reports"
    private const val MAX_REPORTS = 10

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashReport(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash report", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, CRASH_DIR).apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val file = File(dir, "crash_$timestamp.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        file.writeText(buildString {
            appendLine("=== OneUILauncher Crash Report ===")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("App version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}")
            appendLine()
            appendLine("=== Stack Trace ===")
            appendLine(sw.toString())
            appendLine()
            appendLine("=== Thread State ===")
            appendLine("Thread state: ${thread.state}")
        })

        // Prune old reports
        dir.listFiles()?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_REPORTS)
            ?.forEach { it.delete() }

        Log.e(TAG, "Crash report written to ${file.absolutePath}")
    }

    /**
     * CoroutineExceptionHandler for ViewModel scopes.
     * Logs the exception and writes a crash report instead of crashing the app.
     */
    fun coroutineExceptionHandler(context: Context): CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Coroutine exception", throwable)
            try {
                writeCrashReport(context, Thread.currentThread(), throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write crash report from coroutine", e)
            }
        }

    /**
     * Supervisor scope for background work that shouldn't crash the app.
     */
    fun createSupervisorScope(context: Context): kotlinx.coroutines.CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler(context))
}
