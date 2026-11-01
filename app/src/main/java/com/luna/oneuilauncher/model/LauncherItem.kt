package com.luna.oneuilauncher.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Sealed hierarchy representing every item that can appear on the home screen,
 * in the dock, or in the app drawer.
 *
 * Improvements over original:
 * - `@Transient` icons are never serialized (prevents OOM on large backups)
 * - Added `hidden` flag for hidden-apps feature
 * - Added `badgeCount` for notification badges
 * - Added `customIconUri` for user-customized icons
 * - `equals`/`hashCode` based on `id` only (stable identity)
 */
@Serializable
sealed class LauncherItem {
    abstract val id: String
    abstract val label: String
    abstract val hidden: Boolean
    abstract val badgeCount: Int

    @Serializable
    data class App(
        override val id: String,
        override val label: String,
        val packageName: String,
        val className: String,
        val installTime: Long = 0L,
        val iconColor: Long = 0L,
        val isSystemApp: Boolean = false,
        val labelColor: Long = 0L,
        override val hidden: Boolean = false,
        override val badgeCount: Int = 0,
        val customIconUri: String? = null,
        @Transient val icon: android.graphics.drawable.Drawable? = null,
        @Transient val processedIcon: android.graphics.drawable.Drawable? = null
    ) : LauncherItem()

    @Serializable
    data class Folder(
        override val id: String,
        override val label: String,
        val items: List<LauncherItem> = emptyList(),
        val color: Long = 0L,
        val isLarge: Boolean = false,
        val labelColor: Long = 0L,
        override val hidden: Boolean = false,
        override val badgeCount: Int = 0
    ) : LauncherItem()

    @Serializable
    data class Widget(
        override val id: String,
        override val label: String,
        val appWidgetId: Int = -1,
        val providerPackage: String = "",
        val providerClass: String = "",
        val labelResName: String = "",
        val spanX: Int = 1,
        val spanY: Int = 1,
        override val hidden: Boolean = false,
        override val badgeCount: Int = 0
    ) : LauncherItem()

    @Serializable
    data class CustomWidget(
        override val id: String,
        override val label: String,
        val type: CustomWidgetType = CustomWidgetType.MEMORY,
        override val hidden: Boolean = false,
        override val badgeCount: Int = 0
    ) : LauncherItem()

    enum class CustomWidgetType { MEMORY, SMART_STACK, CLOCK, WEATHER }

    override fun equals(other: Any?): Boolean = other is LauncherItem && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

@Serializable
data class HomeItem(
    val item: LauncherItem,
    val screen: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1
)

@Serializable
data class AppShortcut(
    val id: String,
    val label: String,
    val isDynamic: Boolean = false,
    @Transient val icon: android.graphics.drawable.Drawable? = null,
    @Transient val intent: android.content.Intent? = null
)
