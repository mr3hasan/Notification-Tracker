package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.repository.RetentionPeriod
import com.example.ui.components.AppIconImage
import com.example.ui.viewmodel.HomeViewModel
import com.example.util.NotificationAccessUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  homeViewModel: HomeViewModel = viewModel(),
) {
  BackHandler(onBack = onNavigateBack)

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Dynamic system notification access check
  var isAccessGranted by remember {
    mutableStateOf(NotificationAccessUtils.isNotificationAccessGranted(context))
  }
  var isBatteryIgnored by remember {
    mutableStateOf(NotificationAccessUtils.isBatteryOptimizationIgnored(context))
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        isAccessGranted = NotificationAccessUtils.isNotificationAccessGranted(context)
        isBatteryIgnored = NotificationAccessUtils.isBatteryOptimizationIgnored(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Settings StateFlows
  val isPaused by homeViewModel.isCollectionPaused.collectAsStateWithLifecycle()
  val excludedPackages by homeViewModel.excludedPackages.collectAsStateWithLifecycle()
  val retentionPeriod by homeViewModel.retentionPeriod.collectAsStateWithLifecycle()
  val totalNotificationCount by homeViewModel.totalCount.collectAsStateWithLifecycle()
  val appCategories by homeViewModel.appCategories.collectAsStateWithLifecycle()

  // Dialog & Sheet States
  var showClearAllDialog by remember { mutableStateOf(false) }
  var showExclusionSheet by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("settings_screen_root"),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
            ),
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.testTag("settings_top_app_bar"),
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
      // 1. Android System Access Section
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_general))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_access_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(
                    if (isAccessGranted) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer,
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (isAccessGranted) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = if (isAccessGranted) MaterialTheme.colorScheme.onSecondaryContainer
                  else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp),
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.settings_service_status),
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = if (isAccessGranted) {
                    stringResource(R.string.status_access_granted_sub)
                  } else {
                    stringResource(R.string.status_access_not_granted_sub)
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 16.sp,
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isAccessGranted) {
                  MaterialTheme.colorScheme.secondaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
              ) {
                Text(
                  text = if (isAccessGranted) {
                    stringResource(R.string.status_access_granted)
                  } else {
                    stringResource(R.string.status_access_not_granted)
                  },
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isAccessGranted) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            Button(
              onClick = {
                val opened = NotificationAccessUtils.openNotificationListenerSettings(context)
                if (!opened) {
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                      message = context.getString(R.string.toast_settings_launch_failed),
                    )
                  }
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("enable_notification_access_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isAccessGranted) {
                  MaterialTheme.colorScheme.secondary
                } else {
                  MaterialTheme.colorScheme.primary
                },
              ),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.size(8.dp))
              Text(
                text = if (isAccessGranted) {
                  stringResource(R.string.btn_manage_notification_access)
                } else {
                  stringResource(R.string.btn_enable_notification_access)
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              )
            }
          }
        }
      }

      // 1b. Lock Screen & Background Protection Section
      item {
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = if (isBatteryIgnored) {
              MaterialTheme.colorScheme.surface
            } else {
              MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_battery_protection_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .background(
                    if (isBatteryIgnored) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    CircleShape,
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = if (isBatteryIgnored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(22.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Lock Screen & Background Keep-Alive",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = if (isBatteryIgnored) {
                    "Active: Android will not kill the notification listener when screen is locked or idle."
                  } else {
                    "Recommended: Disable battery restrictions so notifications are never missed while phone is locked."
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            if (!isBatteryIgnored) {
              Button(
                onClick = {
                  NotificationAccessUtils.openBatteryOptimizationSettings(context)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("btn_disable_battery_restrictions"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary,
                ),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Launch,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                  text = "Disable Battery Restrictions",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
              }
            }
          }
        }
      }

      // 2. Collection Pause / Resume Section
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_collection))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_collection_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                      if (!isPaused) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.errorContainer,
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = if (!isPaused) Icons.Default.NotificationsActive else Icons.Default.PauseCircle,
                    contentDescription = null,
                    tint = if (!isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                  )
                }

                Column {
                  Text(
                    text = stringResource(R.string.settings_collection_save_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = if (!isPaused) {
                      stringResource(R.string.settings_collection_enabled_sub)
                    } else {
                      stringResource(R.string.settings_collection_paused_sub)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                  )
                }
              }

              Switch(
                checked = !isPaused,
                onCheckedChange = { isEnabled ->
                  homeViewModel.setCollectionPaused(!isEnabled)
                  coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                      if (isEnabled) "Notification collection resumed."
                      else "Notification collection paused. Existing records are preserved.",
                    )
                  }
                },
                modifier = Modifier.testTag("switch_collection_pause"),
                colors = SwitchDefaults.colors(
                  checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                  checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
              )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Explanatory note regarding separate Android system permission
            Row(
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp).padding(top = 2.dp),
              )
              Text(
                text = stringResource(R.string.settings_collection_permission_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
              )
            }
          }
        }
      }

      // 3. Excluded Apps Management Section
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_exclusions))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_exclusions_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }

                Column {
                  Text(
                    text = stringResource(R.string.settings_excluded_apps_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = if (excludedPackages.isEmpty()) {
                      stringResource(R.string.settings_excluded_apps_empty)
                    } else {
                      "${excludedPackages.size} app(s) currently excluded"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (excludedPackages.isNotEmpty()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = "${excludedPackages.size}",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (excludedPackages.isNotEmpty()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
              }
            }

            Text(
              text = stringResource(R.string.settings_excluded_apps_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp,
            )

            Button(
              onClick = { showExclusionSheet = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_manage_excluded_apps"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(text = "Manage Excluded Apps (${excludedPackages.size})")
            }
          }
        }
      }

      // 4. Notification History Retention Period Section
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_retention))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_retention_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = stringResource(R.string.settings_retention_title),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Text(
              text = stringResource(R.string.settings_retention_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp,
            )

            // Selectable Retention Period Options
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              RetentionPeriod.entries.forEach { option ->
                val isSelected = retentionPeriod == option
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    homeViewModel.setRetentionPeriod(option)
                    coroutineScope.launch {
                      snackbarHostState.showSnackbar("History retention set to ${option.label}.")
                    }
                  },
                  label = {
                    Text(
                      text = option.label,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                  },
                  modifier = Modifier.testTag("retention_chip_${option.days}"),
                )
              }
            }
          }
        }
      }

      // 5. Data Management & Clear All Records Section
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_storage))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_storage_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            SettingsRowItem(
              icon = Icons.Default.Lock,
              title = stringResource(R.string.settings_storage_local),
              subtitle = stringResource(R.string.settings_storage_local_sub),
              badgeText = "Local",
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.btn_clear_all_records),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Total stored records: $totalNotificationCount",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              OutlinedButton(
                onClick = { showClearAllDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.testTag("btn_clear_all_settings"),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All")
              }
            }
          }
        }
      }

      // 6. Comprehensive Privacy Information Section
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_privacy))
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_privacy_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            PrivacyItemRow(
              icon = Icons.Default.Shield,
              title = stringResource(R.string.privacy_local_storage_title),
              description = stringResource(R.string.privacy_local_storage_desc),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PrivacyItemRow(
              icon = Icons.Default.Security,
              title = stringResource(R.string.privacy_listener_title),
              description = stringResource(R.string.privacy_listener_desc),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PrivacyItemRow(
              icon = Icons.Outlined.WarningAmber,
              title = stringResource(R.string.privacy_sensitive_title),
              description = stringResource(R.string.privacy_sensitive_desc),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PrivacyItemRow(
              icon = Icons.Default.NotificationsOff,
              title = stringResource(R.string.privacy_shade_title),
              description = stringResource(R.string.privacy_shade_desc),
            )
          }
        }
      }

      // 7. About & Project Tech Specs
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
        Spacer(modifier = Modifier.height(6.dp))
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_about_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ProjectDetailRow(label = "Application ID", value = "com.hasan.notificationtracker")
            ProjectDetailRow(label = "Listener Service", value = "NotificationTrackerListenerService")
            ProjectDetailRow(label = "Minimum SDK", value = "Android 10 (API 29)")
            ProjectDetailRow(label = "Target SDK", value = "Android 16 (API 36)")
            ProjectDetailRow(label = "Database", value = "SQLite / Room (Local Only)")
            ProjectDetailRow(label = "Cloud & Analytics", value = "None (0% Remote Traffic)")
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  // Clear All Confirmation Dialog
  if (showClearAllDialog) {
    val countToDelete = totalNotificationCount
    AlertDialog(
      onDismissRequest = { showClearAllDialog = false },
      title = { Text(stringResource(R.string.dialog_clear_all_title)) },
      text = {
        Text(
          text = "Are you sure you want to permanently delete all $countToDelete saved notification records?\n\nThis deletes only Notification Tracker's local database history; notifications on your system shade will remain untouched.",
        )
      },
      confirmButton = {
        Button(
          onClick = {
            homeViewModel.clearAll()
            showClearAllDialog = false
            coroutineScope.launch {
              snackbarHostState.showSnackbar("All $countToDelete saved notification records deleted.")
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
          ),
          modifier = Modifier.testTag("btn_confirm_clear_all_settings"),
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

  // Excluded Apps Management Modal Bottom Sheet
  if (showExclusionSheet) {
    ExcludedAppsSheet(
      context = context,
      excludedPackages = excludedPackages,
      recordedApps = appCategories.map { it.packageName to it.appName },
      onTogglePackage = { pkg ->
        homeViewModel.toggleAppExclusion(pkg)
      },
      onDismiss = { showExclusionSheet = false },
    )
  }
}

data class AppItem(
  val packageName: String,
  val appName: String,
  val isInstalled: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcludedAppsSheet(
  context: Context,
  excludedPackages: Set<String>,
  recordedApps: List<Pair<String, String>>,
  onTogglePackage: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var searchQuery by remember { mutableStateOf("") }
  var showOnlyExcluded by remember { mutableStateOf(false) }

  // Query installed launchable apps without QUERY_ALL_PACKAGES permission
  val allAppsList = remember(context, recordedApps, excludedPackages) {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = try {
      pm.queryIntentActivities(launcherIntent, 0)
    } catch (e: Exception) {
      emptyList()
    }

    val installedMap = mutableMapOf<String, String>()
    for (info in resolveInfos) {
      val pkg = info.activityInfo.packageName
      if (pkg != context.packageName) {
        val label = try {
          info.loadLabel(pm).toString()
        } catch (e: Exception) {
          pkg
        }
        installedMap[pkg] = label
      }
    }

    // Combine launcher apps, apps previously recorded in DB, and excluded packages
    val combinedPackages = (installedMap.keys + recordedApps.map { it.first } + excludedPackages).distinct()
    combinedPackages.map { pkg ->
      val recordedName = recordedApps.find { it.first == pkg }?.second
      val name = installedMap[pkg] ?: recordedName ?: pkg
      AppItem(
        packageName = pkg,
        appName = if (name.isBlank()) pkg else name,
        isInstalled = installedMap.containsKey(pkg),
      )
    }.sortedBy { it.appName.lowercase() }
  }

  val filteredApps = remember(allAppsList, searchQuery, showOnlyExcluded, excludedPackages) {
    allAppsList.filter { app ->
      val matchesSearch = if (searchQuery.isBlank()) {
        true
      } else {
        val q = searchQuery.trim()
        app.appName.contains(q, ignoreCase = true) || app.packageName.contains(q, ignoreCase = true)
      }
      val matchesFilter = if (showOnlyExcluded) {
        excludedPackages.contains(app.packageName)
      } else {
        true
      }
      matchesSearch && matchesFilter
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("excluded_apps_bottom_sheet"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = stringResource(R.string.dialog_excluded_apps_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          )
          Text(
            text = "${excludedPackages.size} app(s) excluded from recording",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Default.Clear, contentDescription = "Close")
        }
      }

      // Search field
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text(stringResource(R.string.search_apps_hint)) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("search_excluded_apps_input"),
      )

      // Filter chips: All Apps vs Excluded Only
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = !showOnlyExcluded,
          onClick = { showOnlyExcluded = false },
          label = { Text("All (${allAppsList.size})") },
        )
        FilterChip(
          selected = showOnlyExcluded,
          onClick = { showOnlyExcluded = true },
          label = { Text("Excluded (${excludedPackages.size})") },
          modifier = Modifier.testTag("chip_filter_excluded_only"),
        )
      }

      // List of apps
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (filteredApps.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = if (showOnlyExcluded) "No excluded apps found." else "No matching apps found.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          items(
            items = filteredApps,
            key = { it.packageName },
          ) { app ->
            val isExcluded = excludedPackages.contains(app.packageName)
            OutlinedCard(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.outlinedCardColors(
                containerColor = if (isExcluded) {
                  MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                } else {
                  MaterialTheme.colorScheme.surface
                },
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onTogglePackage(app.packageName) }
                .testTag("app_exclusion_item_${app.packageName}"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f),
                ) {
                  AppIconImage(
                    packageName = app.packageName,
                    appName = app.appName,
                    size = 36.dp,
                  )

                  Column {
                    Text(
                      text = app.appName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = if (app.isInstalled) app.packageName else "${app.packageName} (Uninstalled/System)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }

                Switch(
                  checked = isExcluded,
                  onCheckedChange = { onTogglePackage(app.packageName) },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onError,
                    checkedTrackColor = MaterialTheme.colorScheme.error,
                  ),
                  modifier = Modifier.testTag("switch_exclude_${app.packageName}"),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrivacyItemRow(
  icon: ImageVector,
  title: String,
  description: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp,
      )
    }
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
    color = MaterialTheme.colorScheme.primary,
  )
}

@Composable
private fun SettingsRowItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  badgeText: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 16.sp,
      )
    }

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
      Text(
        text = badgeText,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun ProjectDetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
