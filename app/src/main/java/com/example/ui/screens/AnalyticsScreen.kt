package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppIconImage
import com.example.ui.viewmodel.HomeViewModel
import java.util.Calendar

data class HourlyBucket(
  val hour: Int,
  val count: Int,
  val label: String,
)

data class AppInsightItem(
  val packageName: String,
  val appName: String,
  val count: Int,
  val percentage: Float,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  homeViewModel: HomeViewModel = viewModel(),
) {
  BackHandler(onBack = onNavigateBack)

  val allNotifications by homeViewModel.notifications.collectAsStateWithLifecycle()
  val totalCount by homeViewModel.totalCount.collectAsStateWithLifecycle()
  val todayCount by homeViewModel.todayCount.collectAsStateWithLifecycle()
  val deletedCount by homeViewModel.deletedCount.collectAsStateWithLifecycle()

  // Calculate starred count
  val starredCount = remember(allNotifications) {
    allNotifications.count { it.isStarred }
  }

  // Calculate 24-hour distribution buckets (grouped into 6 4-hour windows for clear mobile display)
  val timeWindows = remember(allNotifications) {
    val windows = Array(6) { 0 }
    val cal = Calendar.getInstance()
    allNotifications.forEach { record ->
      cal.timeInMillis = record.postTime
      val hour = cal.get(Calendar.HOUR_OF_DAY)
      val windowIdx = (hour / 4).coerceIn(0, 5)
      windows[windowIdx]++
    }
    listOf(
      "12am - 4am" to windows[0],
      "4am - 8am" to windows[1],
      "8am - 12pm" to windows[2],
      "12pm - 4pm" to windows[3],
      "4pm - 8pm" to windows[4],
      "8pm - 12am" to windows[5],
    )
  }

  // Calculate top active apps & volume share
  val topApps = remember(allNotifications, totalCount) {
    if (allNotifications.isEmpty()) emptyList()
    else {
      allNotifications.groupBy { it.packageName }
        .map { (pkg, records) ->
          val appName = records.firstOrNull()?.appName?.ifBlank { pkg } ?: pkg
          val count = records.size
          val pct = if (totalCount > 0) (count.toFloat() / totalCount.toFloat()) else 0f
          AppInsightItem(packageName = pkg, appName = appName, count = count, percentage = pct)
        }
        .sortedByDescending { it.count }
        .take(6)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("analytics_screen_root"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Insights & Analytics",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "100% on-device notification intelligence",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.testTag("analytics_top_bar"),
      )
    },
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        // 4 KPI Summary Cards
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AnalyticsMetricCard(
            title = "Total Captured",
            value = totalCount.toString(),
            icon = Icons.Default.BarChart,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
          )
          AnalyticsMetricCard(
            title = "Received Today",
            value = todayCount.toString(),
            icon = Icons.Default.NotificationsActive,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AnalyticsMetricCard(
            title = "Recalled / Deleted",
            value = deletedCount.toString(),
            icon = Icons.Default.DeleteSweep,
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
          )
          AnalyticsMetricCard(
            title = "Starred / Pinned",
            value = starredCount.toString(),
            icon = Icons.Default.Star,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Time of Day Activity Distribution (Bar Chart representation)
      item {
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().testTag("chart_time_distribution"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.QueryBuilder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Time of Day Activity Peak",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
            }

            val maxWindow = timeWindows.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              timeWindows.forEach { (windowLabel, count) ->
                val ratio = (count.toFloat() / maxWindow.toFloat()).coerceIn(0.04f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = windowLabel,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      text = "$count alerts",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(8.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant),
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth(ratio)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Most Active Applications Ranking
      item {
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth().testTag("chart_top_apps"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Top Active Apps (Volume Share)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
            }

            if (topApps.isEmpty()) {
              Text(
                text = "No notification traffic recorded yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                topApps.forEach { item ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    AppIconImage(
                      packageName = item.packageName,
                      appName = item.appName,
                      size = 32.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Text(
                          text = item.appName,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          text = "${(item.percentage * 100).toInt()}% (${item.count})",
                          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(6.dp)
                          .clip(RoundedCornerShape(3.dp))
                          .background(MaterialTheme.colorScheme.surfaceVariant),
                      ) {
                        Box(
                          modifier = Modifier
                            .fillMaxWidth(item.percentage.coerceIn(0.04f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun AnalyticsMetricCard(
  title: String,
  value: String,
  icon: ImageVector,
  containerColor: androidx.compose.ui.graphics.Color,
  contentColor: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(20.dp),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = contentColor,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor.copy(alpha = 0.85f),
      )
    }
  }
}
