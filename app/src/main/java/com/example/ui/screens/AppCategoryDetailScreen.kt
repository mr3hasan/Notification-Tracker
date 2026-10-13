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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord
import com.example.ui.components.AppIconImage
import com.example.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
data class ConversationFolderInfo(
  val name: String,
  val count: Int,
  val latestTime: Long,
  val isGroup: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategoryDetailScreen(
  packageName: String,
  appName: String,
  homeViewModel: HomeViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Selected conversation folder filter (null means viewing the list of folders)
  var selectedConversationFolder by remember { mutableStateOf<String?>(null) }

  // If inside a folder, back press returns to all folders; otherwise back returns to CategoriesScreen
  BackHandler {
    if (selectedConversationFolder != null) {
      selectedConversationFolder = null
    } else {
      onNavigateBack()
    }
  }

  val recordsFlow = remember(packageName) {
    homeViewModel.getNotificationsForPackage(packageName)
  }
  val allRecords by recordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

  val context = androidx.compose.ui.platform.LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilterIndex by remember { mutableIntStateOf(0) }
  val filterOptions = listOf("All", "Today", "Has Content")

  // Export menu & deletion dialog states
  var showExportMenu by remember { mutableStateOf(false) }
  var recordPendingDelete by remember { mutableStateOf<NotificationRecord?>(null) }
  var showClearAppDialog by remember { mutableStateOf(false) }

  val startOfToday = remember {
    Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  // Extract all distinct chat / sender / group folders with their count and latest timestamp
  val conversationFolders = remember(allRecords) {
    allRecords
      .groupBy { record ->
        com.example.util.ConversationUtils.extractConversationName(record.title, appName)
      }
      .map { (convName, records) ->
        val latestTime = records.maxOfOrNull { it.postTime } ?: 0L
        val isGroup = records.any { it.title.contains(":") }
        ConversationFolderInfo(
          name = convName,
          count = records.size,
          latestTime = latestTime,
          isGroup = isGroup,
        )
      }
      .sortedByDescending { it.latestTime }
  }

  val displayedRecords = remember(allRecords, searchQuery, selectedFilterIndex, selectedConversationFolder) {
    allRecords.filter { record ->
      val matchesFolder = if (selectedConversationFolder == null) {
        true
      } else {
        com.example.util.ConversationUtils.extractConversationName(record.title, appName) == selectedConversationFolder
      }

      val matchesSearch = if (searchQuery.isBlank()) {
        true
      } else {
        record.title.contains(searchQuery, ignoreCase = true) ||
          record.content.contains(searchQuery, ignoreCase = true)
      }

      val matchesFilter = when (selectedFilterIndex) {
        1 -> record.postTime >= startOfToday
        2 -> record.content.isNotBlank()
        else -> true
      }

      matchesFolder && matchesSearch && matchesFilter
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("app_category_detail_screen"),
  ) {
    TopAppBar(
      navigationIcon = {
        IconButton(
          onClick = {
            if (selectedConversationFolder != null) {
              selectedConversationFolder = null
            } else {
              onNavigateBack()
            }
          },
          modifier = Modifier.testTag("btn_back_to_categories"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = if (selectedConversationFolder != null) "Back to folders" else "Back to categories",
          )
        }
      },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (selectedConversationFolder != null) {
            val currentFolder = conversationFolders.find { it.name == selectedConversationFolder }
            val isGrp = currentFolder?.isGroup == true
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(36.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isGrp) Icons.Default.Group else Icons.Default.AccountCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
            Column {
              Text(
                text = selectedConversationFolder!!,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${displayedRecords.size} alerts in $appName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            AppIconImage(
              packageName = packageName,
              appName = appName,
              size = 32.dp,
            )
            Column {
              Text(
                text = appName.ifBlank { packageName },
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${allRecords.size} alerts • ${conversationFolders.size} folders",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      },
      actions = {
        if (allRecords.isNotEmpty()) {
          Box {
            IconButton(
              onClick = { showExportMenu = true },
              modifier = Modifier.testTag("btn_export_history"),
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Export History",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            DropdownMenu(
              expanded = showExportMenu,
              onDismissRequest = { showExportMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text("Export as PDF document") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  showExportMenu = false
                  com.example.util.HistoryExportUtils.exportAsPdf(
                    context = context,
                    appName = appName.ifBlank { packageName },
                    packageName = packageName,
                    records = allRecords,
                  )
                },
              )
              DropdownMenuItem(
                text = { Text("Export as TXT transcript") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                  )
                },
                onClick = {
                  showExportMenu = false
                  com.example.util.HistoryExportUtils.exportAsTxt(
                    context = context,
                    appName = appName.ifBlank { packageName },
                    packageName = packageName,
                    records = allRecords,
                  )
                },
              )
            }
          }

          IconButton(
            onClick = { showClearAppDialog = true },
            modifier = Modifier.testTag("btn_clear_app_records"),
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Clear all notifications for $appName",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
      ),
      modifier = Modifier.testTag("app_category_top_bar"),
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (allRecords.isNotEmpty()) {
        item {
          // Search box within this app's notifications
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter alerts in $appName…") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear filter",
                  )
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("app_filter_input"),
          )
        }

        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            filterOptions.forEachIndexed { index, option ->
              FilterChip(
                selected = selectedFilterIndex == index,
                onClick = { selectedFilterIndex = index },
                label = { Text(option) },
                modifier = Modifier.testTag("app_filter_chip_$index"),
              )
            }
          }
        }
      }

      if (selectedConversationFolder == null) {
        // --- LEVEL 1: LIST OF FOLDERS (VERTICAL CARDS, EXACTLY LIKE CATEGORIES SCREEN) ---
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
              Column {
                Text(
                  text = "Chat & Contact Folders (${conversationFolders.size})",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "Tap any person or group folder to view its message history",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        if (conversationFolders.isEmpty()) {
          item {
            EmptyStateCard(
              title = if (searchQuery.isNotBlank()) "No Matching Folders" else "No Notifications Yet",
              subtitle = if (searchQuery.isNotBlank()) "No folder matched '$searchQuery'." else "No alerts recorded for this app yet.",
            )
          }
        } else {
          val filteredFolders = if (searchQuery.isBlank()) {
            conversationFolders
          } else {
            conversationFolders.filter { it.name.contains(searchQuery, ignoreCase = true) }
          }

          items(
            items = filteredFolders,
            key = { it.name },
          ) { folder ->
            ConversationFolderCard(
              folder = folder,
              onClick = { selectedConversationFolder = folder.name },
            )
          }
        }
      } else {
        // --- LEVEL 2: INSIDE A SELECTED FOLDER (MESSAGES TIMELINE) ---
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Folder,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Folder: ${selectedConversationFolder!!}",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }

              TextButton(
                onClick = { selectedConversationFolder = null },
                modifier = Modifier.testTag("btn_back_to_folder_list"),
              ) {
                Text("View All Folders", fontSize = 12.sp)
              }
            }
          }
        }

        if (displayedRecords.isEmpty()) {
          item {
            EmptyStateCard(
              title = if (searchQuery.isNotBlank()) "No Matching Messages" else "No Messages",
              subtitle = if (searchQuery.isNotBlank()) "No message in this folder matched '$searchQuery'." else "This folder has no alerts.",
            )
          }
        } else {
          items(
            items = displayedRecords,
            key = { it.id },
          ) { record ->
            AppNotificationRecordItem(
              record = record,
              onDelete = { homeViewModel.deleteNotification(record.id) },
              onToggleStar = { homeViewModel.toggleStar(record.id) },
              onCopyOtp = { otpCode ->
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("OTP Code", otpCode)
                clipboard.setPrimaryClip(clip)
              },
            )
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
private fun AppNotificationRecordItem(
  record: NotificationRecord,
  onDelete: () -> Unit,
  onToggleStar: () -> Unit,
  onCopyOtp: (String) -> Unit,
) {
  val detectedOtp = remember(record.title, record.content) {
    com.example.util.OtpDetector.extractOtp(record.title, record.content)
  }

  OutlinedCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("app_record_item_${record.id}"),
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Text(
              text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(record.postTime)),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }

          if (record.isDeleted) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.errorContainer,
            ) {
              Text(
                text = "RECALLED",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp,
                ),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          IconButton(
            onClick = onToggleStar,
            modifier = Modifier.size(28.dp),
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
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete alert",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      if (record.title.isNotBlank()) {
        Text(
          text = record.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

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
          modifier = Modifier.fillMaxWidth().testTag("app_otp_card_${record.id}"),
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
              modifier = Modifier.testTag("btn_copy_otp_app_${record.id}"),
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

@Composable
private fun ConversationFolderCard(
  folder: ConversationFolderInfo,
  onClick: () -> Unit,
) {
  OutlinedCard(
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("conversation_folder_card_${folder.name.hashCode()}")
      .clickable(onClick = onClick),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = if (folder.isGroup) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (folder.isGroup) Icons.Default.Group else Icons.Default.AccountCircle,
            contentDescription = null,
            tint = if (folder.isGroup) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = folder.name,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = if (folder.isGroup) "Group Chat" else "Direct Chat",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = "${folder.count} alerts",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
private fun EmptyStateCard(
  title: String,
  subtitle: String,
) {
  Spacer(modifier = Modifier.height(24.dp))
  OutlinedCard(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("app_folder_empty_card"),
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
          .size(64.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.NotificationsNone,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(32.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp,
      )
    }
  }
}
