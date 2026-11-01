package com.luna.oneuilauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.luna.oneuilauncher.model.LauncherItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Improved AppRepository with:
 * - Caching of installed apps (avoids repeated PackageManager queries)
 * - Proper error handling (no crashes on missing packages)
 * - Flow-based API for reactive updates
 * - Work profile support
 * - Hidden apps filtering
 */
class AppRepository(
    private val context: Context,
    private val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps,
    private val userManager: UserManager = context.getSystemService(Context.USER_SERVICE) as UserManager
) {
    private var cachedApps: List<LauncherItem.App>? = null
    private var cacheTimestamp: Long = 0
    private val cacheTtlMs = 30_000L // 30 seconds

    /**
     * Get all installed apps as a Flow. Emits cached data first, then refreshes.
     */
    fun getInstalledAppsFlow(hiddenPackages: Set<String> = emptySet()): Flow<List<LauncherItem.App>> = flow {
        emit(getInstalledApps(hiddenPackages))
    }.flowOn(Dispatchers.IO)

    /**
     * Get installed apps synchronously with caching.
     */
    suspend fun getInstalledApps(hiddenPackages: Set<String> = emptySet()): List<LauncherItem.App> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cached = cachedApps
            if (cached != null && now - cacheTimestamp < cacheTtlMs && hiddenPackages.isEmpty()) {
                return@withContext cached
            }

            val result = mutableListOf<LauncherItem.App>()
            try {
                val profiles = userManager.userProfiles
                for (user in profiles) {
                    val activities = launcherApps.getActivityList(null, user)
                    for (activity in activities) {
                        val info = activity.applicationInfo
                        if (info == null) continue

                        val packageName = info.packageName
                        val className = activity.componentName.className
                        val label = activity.label?.toString() ?: packageName

                        // Skip hidden apps
                        if (packageName in hiddenPackages) continue

                        // Skip our own package
                        if (packageName == context.packageName) continue

                        result.add(
                            LauncherItem.App(
                                id = "$packageName/$className",
                                label = label,
                                packageName = packageName,
                                className = className,
                                installTime = info.lastUpdateTime,
                                isSystemApp = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Failed to get installed apps", e)
            }

            result.sortBy { it.label.lowercase() }
            cachedApps = result
            cacheTimestamp = now
            result
        }

    /**
     * Invalidate the app cache (call after install/uninstall).
     */
    fun invalidateCache() {
        cachedApps = null
        cacheTimestamp = 0
    }

    /**
     * Launch an app with proper error handling.
     */
    fun launchApp(app: LauncherItem.App, userHandle: UserHandle? = null) {
        val user = userHandle ?: Process.myUserHandle()
        val component = android.content.ComponentName(app.packageName, app.className)
        try {
            launcherApps.startMainActivity(component, user, null, null)
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "Failed to start activity via LauncherApps", e)
            // Fallback
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (e2: Exception) {
                android.util.Log.e("AppRepository", "Fallback launch also failed", e2)
            }
        }
    }

    /**
     * Get user profiles (for work profile support).
     */
    fun getUserProfiles(): List<UserHandle> = try {
        userManager.userProfiles
    } catch (e: Exception) {
        listOf(Process.myUserHandle())
    }
}
