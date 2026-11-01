package com.luna.oneuilauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luna.oneuilauncher.model.LauncherItem
import com.luna.oneuilauncher.viewmodel.LauncherViewModel

/**
 * Home screen with:
 * - Grid layout for app icons
 * - Long-press for edit mode
 * - Notification badges
 * - Search overlay
 * - Smooth animations
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val homeItems by viewModel.homeItemsWithBadges.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    val gridRows by viewModel.gridRows.collectAsState()
    val gridCols by viewModel.gridCols.collectAsState()
    val showLabels by viewModel.showLabels.collectAsState()
    val iconSize by viewModel.iconSize.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Home grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridCols),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { viewModel.toggleEditMode() }
                        )
                    },
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(homeItems, key = { it.item.id }) { homeItem ->
                    HomeIcon(
                        item = homeItem.item,
                        showLabel = showLabels,
                        iconSize = iconSize,
                        isEditMode = isEditMode,
                        onClick = {
                            when (val item = homeItem.item) {
                                is LauncherItem.App -> viewModel.launchApp(item)
                                else -> { /* handle folder/widget */ }
                            }
                        },
                        onLongClick = { viewModel.toggleEditMode() }
                    )
                }
            }
        }

        // Search overlay
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SearchOverlay(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onDismiss = viewModel::toggleSearch,
                viewModel = viewModel
            )
        }

        // Edit mode toolbar
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EditModeToolbar(
                onDone = { viewModel.toggleEditMode() },
                onAddWidget = { /* open widget picker */ },
                onSettings = { /* open settings */ }
            )
        }
    }
}

@Composable
private fun HomeIcon(
    item: LauncherItem,
    showLabel: Boolean,
    iconSize: Float,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(4.dp)
    ) {
        Box {
            // Icon placeholder
            Surface(
                modifier = Modifier.size((48 * iconSize).dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            // Notification badge
            if (item.badgeCount > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString()
                    )
                }
            }
        }
        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(72.dp)
            )
        }
    }
}

@Composable
private fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: LauncherViewModel
) {
    val visibleItems by viewModel.visibleDrawerItems.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search apps...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleItems, key = { it.id }) { app ->
                    HomeIcon(
                        item = app,
                        showLabel = true,
                        iconSize = 1f,
                        isEditMode = false,
                        onClick = {
                            viewModel.launchApp(app)
                            onDismiss()
                        },
                        onLongClick = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun EditModeToolbar(
    onDone: () -> Unit,
    onAddWidget: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onAddWidget) { Text("Add Widget") }
            TextButton(onClick = onSettings) { Text("Settings") }
            Button(onClick = onDone) { Text("Done") }
        }
    }
}
