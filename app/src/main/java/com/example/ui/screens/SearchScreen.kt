package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.local.NotificationRecord
import com.example.ui.components.AppIconImage
import com.example.ui.viewmodel.HomeViewModel
import com.example.util.NotificationFilterUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier,
  homeViewModel: HomeViewModel = viewModel(),
) {
  BackHandler(onBack = onNavigateBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  val allNotifications by homeViewModel.notifications.collectAsStateWithLifecycle()

  var searchQuery by remember { mutableStateOf("") }
  var startDateMillis by remember { mutableStateOf<Long?>(null) }
  var endDateMillis by remember { mutableStateOf<Long?>(null) }

  // Quick preset filter index: 0: All, 1: Today, 2: Last 7 Days, 3: This Month, 4: Custom
  var selectedPresetIndex by remember { mutableStateOf(0) }

  // Dialog states for deletion
  var recordPendingDelete by remember { mutableStateOf<NotificationRecord?>(null) }
  var showClearAllDialog by remember { mutableStateOf(false) }

  // Date formatters
  val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

  // Validate date range
  val isDateRangeInvalid = remember(startDateMillis, endDateMillis) {
    !NotificationFilterUtils.isDateRangeValid(startDateMillis, endDateMillis)
  }

  var filterDeletedOnly by remember { mutableStateOf(false) }
  var filterStarredOnly by remember { mutableStateOf(false) }

  // Combined filtering: text search + date filter + smart flags
  val filteredNotifications = remember(
    allNotifications,
    searchQuery,
    startDateMillis,
    endDateMillis,
    filterDeletedOnly,
    filterStarredOnly,
  ) {
    val baseFiltered = NotificationFilterUtils.filter(
      records = allNotifications,
      query = searchQuery,
      startDateMillis = startDateMillis,
      endDateMillis = endDateMillis,
    )
    baseFiltered.filter { record ->
      val matchesDeleted = if (filterDeletedOnly) record.isDeleted else true
      val matchesStarred = if (filterStarredOnly) record.isStarred else true
      matchesDeleted && matchesStarred
    }
  }

  // Date picker dialog launcher
  fun showDatePicker(isStartDate: Boolean) {
    val initialTime = if (isStartDate) {
      startDateMillis ?: System.currentTimeMillis()
    } else {
      endDateMillis ?: System.currentTimeMillis()
    }
    val cal = Calendar.getInstance().apply { timeInMillis = initialTime }

    DatePickerDialog(
      context,
      { _, year, month, dayOfMonth ->
        val selectedCal = Calendar.getInstance().apply {
          set(year, month, dayOfMonth)
          if (isStartDate) {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          } else {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
          }
        }
        if (isStartDate) {
          startDateMillis = selectedCal.timeInMillis
        } else {
          endDateMillis = selectedCal.timeInMillis
        }
        selectedPresetIndex = 4 // Set to custom preset
      },
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
    ).show()
  }

  // Preset setter helper
  fun applyPreset(index: Int) {
    selectedPresetIndex = index
    val now = Calendar.getInstance()
    when (index) {
      0 -> { // All Dates
        startDateMillis = null
        endDateMillis = null
      }
      1 -> { // Today
        val start = (now.clone() as Calendar).apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val end = (now.clone() as Calendar).apply {
          set(Calendar.HOUR_OF_DAY, 23)
          set(Calendar.MINUTE, 59)
          set(Calendar.SECOND, 59)
          set(Calendar.MILLISECOND, 999)
        }
        startDateMillis = start.timeInMillis
        endDateMillis = end.timeInMillis
      }
      2 -> { // Last 7 Days
        val start = (now.clone() as Calendar).apply {
          add(Calendar.DAY_OF_YEAR, -7)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val end = (now.clone() as Calendar).apply {
          set(Calendar.HOUR_OF_DAY, 23)
          set(Calendar.MINUTE, 59)
          set(Calendar.SECOND, 59)
          set(Calendar.MILLISECOND, 999)
        }
        startDateMillis = start.timeInMillis
        endDateMillis = end.timeInMillis
      }
      3 -> { // This Month
        val start = (now.clone() as Calendar).apply {
          set(Calendar.DAY_OF_MONTH, 1)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        val end = (now.clone() as Calendar).apply {
          set(Calendar.HOUR_OF_DAY, 23)
          set(Calendar.MINUTE, 59)
          set(Calendar.SECOND, 59)
          set(Calendar.MILLISECOND, 999)
        }
        startDateMillis = start.timeInMillis
        endDateMillis = end.timeInMillis
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize().testTag("search_screen_root"),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = stringResource(R.string.search_title),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
              ),
            )
            Text(
              text = if (filteredNotifications.isNotEmpty()) {
                "${filteredNotifications.size} results found"
              } else {
                "Search & filter archive"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          if (allNotifications.isNotEmpty()) {
            IconButton(
              onClick = { showClearAllDialog = true },
              modifier = Modifier.testTag("btn_clear_all_notifications"),
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
        modifier = Modifier.testTag("search_top_app_bar"),
      )
    },
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // 1. Search Query Input Field
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text(stringResource(R.string.search_hint)) },
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
                  contentDescription = "Clear search",
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_input_field"),
        )
      }

      // 2. Date Filter Controls Card
      item {
        OutlinedCard(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          modifier = Modifier.fillMaxWidth().testTag("date_filter_card"),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarMonth,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Date Range Filter",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              if (startDateMillis != null || endDateMillis != null) {
                TextButton(
                  onClick = {
                    startDateMillis = null
                    endDateMillis = null
                    selectedPresetIndex = 0
                  },
                  modifier = Modifier.testTag("btn_clear_date_filter"),
                ) {
                  Text(
                    text = stringResource(R.string.date_filter_clear),
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }

            // Quick Preset Chips
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              val presets = listOf("All Dates", "Today", "Last 7 Days", "This Month")
              presets.forEachIndexed { index, name ->
                FilterChip(
                  selected = selectedPresetIndex == index,
                  onClick = { applyPreset(index) },
                  label = { Text(name, fontSize = 12.sp) },
                  modifier = Modifier.testTag("preset_chip_$index"),
                )
              }
            }

            // Smart Status Filter Chips: Recalled only / Starred only
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = filterDeletedOnly,
                onClick = { filterDeletedOnly = !filterDeletedOnly },
                label = { Text("Recalled / Deleted Only", fontSize = 11.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                  )
                },
                modifier = Modifier.testTag("chip_filter_recalled"),
              )
              FilterChip(
                selected = filterStarredOnly,
                onClick = { filterStarredOnly = !filterStarredOnly },
                label = { Text("Starred Only", fontSize = 11.sp) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                  )
                },
                modifier = Modifier.testTag("chip_filter_starred"),
              )
            }

            // Start Date and End Date Pickers Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = { showDatePicker(isStartDate = true) },
                modifier = Modifier.weight(1f).testTag("btn_select_start_date"),
                shape = RoundedCornerShape(10.dp),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = stringResource(R.string.date_filter_start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = if (startDateMillis != null) {
                      dateFormatter.format(Date(startDateMillis!!))
                    } else {
                      "Select"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }
              }

              OutlinedButton(
                onClick = { showDatePicker(isStartDate = false) },
                modifier = Modifier.weight(1f).testTag("btn_select_end_date"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = if (isDateRangeInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                    text = stringResource(R.string.date_filter_end),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDateRangeInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = if (endDateMillis != null) {
                      dateFormatter.format(Date(endDateMillis!!))
                    } else {
                      "Select"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDateRangeInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                  )
                }
              }
            }

            // Date range validation message
            if (isDateRangeInvalid) {
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = stringResource(R.string.date_filter_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }
          }
        }
      }

      // 3. Results Section
      if (filteredNotifications.isEmpty()) {
        item {
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.outlinedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_empty_state_card"),
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
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp),
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                text = if (allNotifications.isEmpty()) {
                  stringResource(R.string.search_empty_title)
                } else {
                  "No Matching Notifications"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                text = if (allNotifications.isEmpty()) {
                  stringResource(R.string.search_empty_desc)
                } else {
                  val activeFilters = buildList {
                    if (searchQuery.isNotBlank()) add("query: \"$searchQuery\"")
                    if (startDateMillis != null) add("after ${dateFormatter.format(Date(startDateMillis!!))}")
                    if (endDateMillis != null) add("before ${dateFormatter.format(Date(endDateMillis!!))}")
                  }
                  "No saved notification matched your criteria (${activeFilters.joinToString(", ")}). Try adjusting or resetting your filters."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
              )

              if (searchQuery.isNotBlank() || startDateMillis != null || endDateMillis != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                  onClick = {
                    searchQuery = ""
                    startDateMillis = null
                    endDateMillis = null
                    selectedPresetIndex = 0
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("btn_reset_filters"),
                ) {
                  Text("Reset All Filters")
                }
              }
            }
          }
        }
      } else {
        items(
          items = filteredNotifications,
          key = { it.id },
        ) { record ->
          SearchNotificationCard(
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
          modifier = Modifier.testTag("btn_confirm_delete_record"),
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

  // Confirmation Dialog for Clear All Records
  if (showClearAllDialog) {
    val countToDelete = allNotifications.size
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
          modifier = Modifier.testTag("btn_confirm_clear_all"),
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
private fun SearchNotificationCard(
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
      .testTag("search_item_${record.id}"),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          AppIconImage(
            packageName = record.packageName,
            appName = record.appName,
            size = 32.dp,
          )

          Column(modifier = Modifier.weight(1f, fill = false)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = com.example.util.AppNamingUtils.getCleanAppName(record.packageName, record.appName),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
              )
              if (record.isDeleted) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                ) {
                  Text(
                    text = "Recalled",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 10.sp,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    softWrap = false,
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
          IconButton(
            onClick = onToggleStar,
            modifier = Modifier.size(28.dp).testTag("btn_star_search_${record.id}"),
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
            modifier = Modifier.size(28.dp).testTag("btn_delete_${record.id}"),
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
          modifier = Modifier.fillMaxWidth().testTag("search_otp_card_${record.id}"),
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
              modifier = Modifier.testTag("btn_copy_otp_search_${record.id}"),
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
