package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavDestination(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME(
    label = "Home",
    selectedIcon = Icons.Filled.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_item_home",
  ),
  CATEGORIES(
    label = "Categories",
    selectedIcon = Icons.Filled.Category,
    unselectedIcon = Icons.Outlined.Category,
    testTag = "nav_item_categories",
  ),
  SEARCH(
    label = "Search",
    selectedIcon = Icons.Filled.Search,
    unselectedIcon = Icons.Outlined.Search,
    testTag = "nav_item_search",
  ),
  ANALYTICS(
    label = "Insights",
    selectedIcon = Icons.Filled.BarChart,
    unselectedIcon = Icons.Outlined.Analytics,
    testTag = "nav_item_analytics",
  ),
  SETTINGS(
    label = "Settings",
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_item_settings",
  ),
}
