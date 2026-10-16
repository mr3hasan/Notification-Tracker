package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.navigation.NavDestination
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen

@Composable
fun NotificationTrackerApp(
  modifier: Modifier = Modifier,
) {
  var currentDestination by rememberSaveable { mutableStateOf(NavDestination.HOME) }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.navigationBars,
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
      ) {
        NavDestination.entries.forEach { destination ->
          val isSelected = destination == currentDestination
          NavigationBarItem(
            selected = isSelected,
            onClick = { currentDestination = destination },
            icon = {
              Icon(
                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = destination.label,
              )
            },
            label = {
              Text(
                text = destination.label,
                maxLines = 1,
                softWrap = false,
                fontSize = 11.sp,
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.testTag(destination.testTag),
          )
        }
      }
    },
  ) { innerPadding ->
    Crossfade(
      targetState = currentDestination,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      label = "screen_crossfade",
    ) { destination ->
      when (destination) {
        NavDestination.HOME -> HomeScreen()
        NavDestination.CATEGORIES -> CategoriesScreen(
          onNavigateBack = { currentDestination = NavDestination.HOME },
        )
        NavDestination.SEARCH -> SearchScreen(
          onNavigateBack = { currentDestination = NavDestination.HOME },
        )
        NavDestination.ANALYTICS -> com.example.ui.screens.AnalyticsScreen(
          onNavigateBack = { currentDestination = NavDestination.HOME },
        )
        NavDestination.SETTINGS -> SettingsScreen(
          onNavigateBack = { currentDestination = NavDestination.HOME },
        )
      }
    }
  }
}
