package com.luna.oneuilauncher.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.luna.oneuilauncher.data.AppRepository
import com.luna.oneuilauncher.model.HomeItem
import com.luna.oneuilauncher.model.LauncherItem
import com.luna.oneuilauncher.util.CrashHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Improved LauncherViewModel with:
 * - Single source of truth via StateFlow
 * - Proper error handling (no crashes on data load failures)
 * - Memory-efficient icon loading (lazy, cached)
 * - Hidden apps support
 * - Notification badge counts
 * - Gesture state management
 * - Crash-safe coroutine scopes
 */
class LauncherViewModel(
    private val application: Application
) : ViewModel() {

    private val context: Context get() = application
    private val appRepository = AppRepository(context)
    private val crashHandler = CrashHandler.coroutineExceptionHandler(context)

    // ===== Core State =====
    private val _homeItems = MutableStateFlow<List<HomeItem>>(emptyList())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    private val _dockItems = MutableStateFlow<List<LauncherItem>>(emptyList())
    val dockItems: StateFlow<List<LauncherItem>> = _dockItems.asStateFlow()

    private val _drawerItems = MutableStateFlow<List<LauncherItem.App>>(emptyList())
    val drawerItems: StateFlow<List<LauncherItem.App>> = _drawerItems.asStateFlow()

    private val _hiddenApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps.asStateFlow()

    private val _notificationCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val notificationCounts: StateFlow<Map<String, Int>> = _notificationCounts.asStateFlow()

    // ===== UI State =====
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _pageCount = MutableStateFlow(1)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    // ===== Settings =====
    private val _gridRows = MutableStateFlow(5)
    val gridRows: StateFlow<Int> = _gridRows.asStateFlow()

    private val _gridCols = MutableStateFlow(4)
    val gridCols: StateFlow<Int> = _gridCols.asStateFlow()

    private val _showLabels = MutableStateFlow(true)
    val showLabels: StateFlow<Boolean> = _showLabels.asStateFlow()

    private val _iconSize = MutableStateFlow(1.0f)
    val iconSize: StateFlow<Float> = _iconSize.asStateFlow()

    private val _blurEnabled = MutableStateFlow(true)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    // ===== Gesture State =====
    private val _gestureState = MutableStateFlow(GestureState.IDLE)
    val gestureState: StateFlow<GestureState> = _gestureState.asStateFlow()

    enum class GestureState { IDLE, DRAGGING, RESIZING, SWIPING }

    // ===== Derived State =====
    val visibleDrawerItems: StateFlow<List<LauncherItem.App>> = combine(
        _drawerItems, _searchQuery, _hiddenApps
    ) { items, query, hidden ->
        items.filter { app ->
            app.id !in hidden && (
                query.isBlank() ||
                app.label.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeItemsWithBadges: StateFlow<List<HomeItem>> = combine(
        _homeItems, _notificationCounts
    ) { items, counts ->
        items.map { homeItem ->
            val item = homeItem.item
            val badge = when (item) {
                is LauncherItem.App -> counts[item.packageName] ?: 0
                else -> 0
            }
            if (badge > 0 && item is LauncherItem.App) {
                homeItem.copy(item = item.copy(badgeCount = badge))
            } else homeItem
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch(crashHandler) {
            try {
                _isLoading.value = true
                refreshApps()
            } catch (e: Exception) {
                android.util.Log.e("LauncherViewModel", "Failed to load initial data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch(crashHandler) {
            try {
                val apps = appRepository.getInstalledApps(_hiddenApps.value)
                _drawerItems.value = apps
            } catch (e: Exception) {
                android.util.Log.e("LauncherViewModel", "Failed to refresh apps", e)
            }
        }
    }

    fun hideApp(packageName: String) {
        _hiddenApps.value = _hiddenApps.value + packageName
        appRepository.invalidateCache()
        refreshApps()
    }

    fun unhideApp(packageName: String) {
        _hiddenApps.value = _hiddenApps.value - packageName
        appRepository.invalidateCache()
        refreshApps()
    }

    fun updateNotificationCounts(counts: Map<String, Int>) {
        _notificationCounts.value = counts
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchVisible.value = !_isSearchVisible.value
        if (!_isSearchVisible.value) _searchQuery.value = ""
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page.coerceIn(0, _pageCount.value - 1)
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun launchApp(app: LauncherItem.App) {
        viewModelScope.launch(crashHandler) {
            try {
                appRepository.launchApp(app)
            } catch (e: Exception) {
                android.util.Log.e("LauncherViewModel", "Failed to launch app: ${app.packageName}", e)
            }
        }
    }

    fun addItemToHome(item: LauncherItem, screen: Int = _currentPage.value, cellX: Int = 0, cellY: Int = 0) {
        val homeItem = HomeItem(item, screen, cellX, cellY)
        _homeItems.value = _homeItems.value + homeItem
    }

    fun removeItemFromHome(itemId: String) {
        _homeItems.value = _homeItems.value.filter { it.item.id != itemId }
    }

    fun moveItem(itemId: String, newScreen: Int, newCellX: Int, newCellY: Int) {
        _homeItems.value = _homeItems.value.map { hi ->
            if (hi.item.id == itemId) hi.copy(screen = newScreen, cellX = newCellX, cellY = newCellY)
            else hi
        }
    }

    fun updateGridSize(rows: Int, cols: Int) {
        _gridRows.value = rows.coerceIn(3, 8)
        _gridCols.value = cols.coerceIn(3, 6)
    }

    fun setIconSize(size: Float) {
        _iconSize.value = size.coerceIn(0.7f, 1.5f)
    }

    fun setBlurEnabled(enabled: Boolean) {
        _blurEnabled.value = enabled
    }

    fun setShowLabels(show: Boolean) {
        _showLabels.value = show
    }

    // ===== Factory =====
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LauncherViewModel::class.java)) {
                return LauncherViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
