package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.local.NotificationRecord
import com.example.ui.components.AppIconImage
import com.example.ui.viewmodel.HomeViewModel
import com.example.util.NotificationAccessUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modifier: Modifier = Modifier,
  homeViewModel: HomeViewModel = viewModel(),
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val notifications by homeViewModel.notifications.collectAsStateWithLifecycle()
  val totalCount by homeViewModel.totalCount.collectAsStateWithLifecycle()
  val todayCount by homeViewModel.todayCount.collectAsStateWithLifecycle()
  val appsTrackedCount by homeViewModel.distinctAppCount.collectAsStateWithLifecycle()

  var isAccessGranted by remember {
    mutableStateOf(NotificationAccessUtils.isNotificationAccessGranted(context))
  }

  // Deletion confirmation dialog states
  var recordPendingDelete by remember { mutableStateOf<NotificationRecord?>(null) }
  var showClearAllDialog by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        isAccessGranted = NotificationAccessUtils.isNotificationAccessGranted(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("home_screen_root"),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.home_title),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
              ),
            )
            Text(
              text = stringResource(R.string.home_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          if (notifications.isNotEmpty()) {
            IconButton(
              onClick = { showClearAllDialog = true },
              modifier = Modifier.testTag("btn_home_clear_all"),
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Clear all notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.testTag("home_top_app_bar"),
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
        // Stats Overview Row with real database metrics
        StatsOverviewSection(
          totalCount = totalCount,
          todayCount = todayCount,
          appsTrackedCount = appsTrackedCount,
        )
      }

      item {
        // Notification Access status notice banner
        AccessPendingBanner(isAccessGranted = isAccessGranted)
      }

      item {
        // Section header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.recent_notifications_header),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (notifications.isNotEmpty()) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceVariant
            },
          ) {
            Text(
              text = if (notifications.isNotEmpty()) "${notifications.size} Logged" else "Empty",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = if (notifications.isNotEmpty()) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (notifications.isEmpty()) {
        item {
          // Empty state container representing the empty notification list
          EmptyNotificationsState()
        }
      } else {
        items(
          items = notifications,
          key = { it.id },
        ) { record ->
          NotificationRecordItemCard(
            record = record,
            onDelete = { recordPendingDelete = record },
            onToggleStar = { homeViewModel.toggleStar(record.id) },
            onCopyOtp = { otpCode ->
              val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
              val clip = android.content.ClipData.newPlainText("OTP Code", otpCode)
              clipboard.setPrimaryClip(clip)
              coroutineScope.launch {
                snackbarHostState.showSnackbar("Security code $otpCode copied to clipboard!")
              }
            },
          )
        }
      }

      item {
        // Privacy & Architecture Highlights
        PrivacyHighlightsSection()
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Confirmation Dialog for Individual Notification Deletion
  recordPendingDelete?.let { record ->
    AlertDialog(
      onDismissRequest = { recordPendingDelete = null },
      title = { Text(stringResource(R.string.dialog_delete_title)) },
      text = {
        Text(
          text = "Are you sure you want to delete this saved alert from \"${record.appName.ifBlank { record.packageName }}\"?\n\nThis deletes only the saved archive record in Notification Tracker; notifications in the source app are not affected.",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            homeViewModel.deleteNotification(record.id)
            recordPendingDelete = null
            coroutineScope.launch {
              snackbarHostState.showSnackbar("Notification record removed.")
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier.testTag("btn_confirm_home_delete"),
        ) {
          Text(stringResource(R.string.dialog_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { recordPendingDelete = null }) {
          Text(stringResource(R.string.dialog_cancel))
        }
      },
    )
  }

  // Confirmation Dialog for Clear All
  if (showClearAllDialog) {
    val countToDelete = notifications.size
    AlertDialog(
      onDismissRequest = { showClearAllDialog = false },
      title = { Text(stringResource(R.string.dialog_clear_all_title)) },
      text = {
        Text(
          text = "Are you sure you want to delete all $countToDelete saved notification records? This action cannot be undone.\n\nNote: Original notifications on your device will remain untouched.",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            homeViewModel.clearAll()
            showClearAllDialog = false
            coroutineScope.launch {
              snackbarHostState.showSnackbar("All $countToDelete notification records deleted.")
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier.testTag("btn_confirm_home_clear_all"),
        ) {
          Text("Delete $countToDelete Records")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearAllDialog = false }) {
          Text(stringResource(R.string.dialog_cancel))
        }
      },
    )
  }
}

@Composable
private fun StatsOverviewSection(
  totalCount: Int,
  todayCount: Int,
  appsTrackedCount: Int,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("stats_overview_row"),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    StatMetricCard(
      title = stringResource(R.string.total_notifications_label),
      count = totalCount.toString(),
      icon = Icons.Outlined.NotificationsNone,
      modifier = Modifier.weight(1f),
      testTag = "card_total_notifications",
    )
    StatMetricCard(
      title = stringResource(R.string.today_notifications_label),
      count = todayCount.toString(),
      icon = Icons.Filled.NotificationsActive,
      modifier = Modifier.weight(1f),
      testTag = "card_today_notifications",
    )
    StatMetricCard(
      title = stringResource(R.string.apps_tracked_label),
      count = appsTrackedCount.toString(),
      icon = Icons.Default.Security,
      modifier = Modifier.weight(1f),
      testTag = "card_monitored_apps",
    )
  }
}

@Composable
private fun StatMetricCard(
  title: String,
  count: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  testTag: String,
) {
  ElevatedCard(
    modifier = modifier.testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
      }
      Text(
        text = count,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
      )
    }
  }
}

@Composable
private fun AccessPendingBanner(
  isAccessGranted: Boolean,
) {
  val context = LocalContext.current
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("access_pending_banner"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isAccessGranted) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
      } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
      },
    ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
              if (isAccessGranted) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.primary,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (isAccessGranted) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = stringResource(
              if (isAccessGranted) R.string.access_banner_active_title else R.string.access_banner_title,
            ),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(22.dp),
          )
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(
              if (isAccessGranted) R.string.access_banner_active_title else R.string.access_banner_title,
            ),
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.SemiBold,
            ),
            color = if (isAccessGranted) {
              MaterialTheme.colorScheme.onSecondaryContainer
            } else {
              MaterialTheme.colorScheme.onPrimaryContainer
            },
          )
          Text(
            text = stringResource(
              if (isAccessGranted) R.string.access_banner_active_desc else R.string.access_banner_desc,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (isAccessGranted) {
              MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            } else {
              MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            },
            lineHeight = 18.sp,
          )
        }
      }

      if (!isAccessGranted) {
        Button(
          onClick = {
            NotificationAccessUtils.openNotificationListenerSettings(context)
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("btn_home_enable_access"),
          shape = RoundedCornerShape(10.dp),
        ) {
          Text("Grant Notification Access in Android Settings")
        }
      }
    }
  }
}

@Composable
private fun NotificationRecordItemCard(
  record: NotificationRecord,
  onDelete: () -> Unit,
  onToggleStar: () -> Unit,
  onCopyOtp: (String) -> Unit,
) {
  val context = LocalContext.current
  val detectedOtp = remember(record.title, record.content) {
    com.example.util.OtpDetector.extractOtp(record.title, record.content)
  }

  OutlinedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("notification_item_${record.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = if (record.isDeleted) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
      } else {
        MaterialTheme.colorScheme.surface
      },
    ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Header: App icon, App name, anti-delete status badge & actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          AppIconImage(
            packageName = record.packageName,
            appName = record.appName,
            size = 32.dp,
          )

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = record.appName.ifBlank { record.packageName },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (record.isDeleted) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.errorContainer,
                ) {
                  Text(
                    text = "RECALLED / DELETED",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp,
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  )
                }
              }
            }
            Text(
              text = record.packageName,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = formatRelativeTimestamp(record.postTime),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          IconButton(
            onClick = onToggleStar,
            modifier = Modifier.size(28.dp).testTag("btn_star_home_${record.id}"),
          ) {
            Icon(
              imageVector = if (record.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
              contentDescription = if (record.isStarred) "Starred" else "Star",
              tint = if (record.isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(16.dp),
            )
          }
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp).testTag("btn_delete_home_${record.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete record",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      // Notification Title (when available)
      if (record.title.isNotBlank()) {
        Text(
          text = record.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Notification Body / Content (when available)
      if (record.content.isNotBlank()) {
        Text(
          text = record.content,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 4,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp,
        )
      }

      // Smart OTP / Verification Code Banner with 1-Tap Copy
      if (detectedOtp != null) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          ),
          modifier = Modifier.fillMaxWidth().testTag("otp_card_${record.id}"),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Column {
                Text(
                  text = "Security Code Detected",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                  text = detectedOtp,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                  ),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }

            Button(
              onClick = { onCopyOtp(detectedOtp) },
              shape = RoundedCornerShape(8.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.testTag("btn_copy_otp_${record.id}"),
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.size(4.dp))
              Text("Copy Code", fontSize = 12.sp)
            }
          }
        }
      }
    }
  }
}

private fun formatRelativeTimestamp(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestamp
  return when {
    diff < 60_000L -> "Just now"
    diff < 3600_000L -> "${diff / 60_000L}m ago"
    diff < 86400_000L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
  }
}

@Composable
private fun EmptyNotificationsState() {
  OutlinedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("empty_notifications_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.NotificationsNone,
          contentDescription = stringResource(R.string.empty_notifications_title),
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(36.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.empty_notifications_title),
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.empty_notifications_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
      )
    }
  }
}

@Composable
private fun PrivacyHighlightsSection() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("privacy_highlights_card"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Architecture Highlights",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Zero Cloud Sync: Stored strictly on device",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.BatterySaver,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Lightweight Listener: Event-driven architecture",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
