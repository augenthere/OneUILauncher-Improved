package com.luna.oneuilauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Improved NotificationService with:
 * - Counts notifications per package (never reads content)
 * - Proper lifecycle handling
 * - Thread-safe state updates
 * - Crash-safe error handling
 */
class NotificationService : NotificationListenerService() {

    companion object {
        private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
        val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    }

    private var isConnectedFlag = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnectedFlag = true
        _isConnected.value = true
        updateCounts()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnectedFlag = false
        _isConnected.value = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateCounts()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateCounts()
    }

    private fun updateCounts() {
        try {
            val counts = mutableMapOf<String, Int>()
            activeNotifications?.forEach { sbn ->
                val pkg = sbn.packageName
                counts[pkg] = (counts[pkg] ?: 0) + 1
            }
            _notificationCounts.value = counts
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Failed to update counts", e)
        }
    }
}
