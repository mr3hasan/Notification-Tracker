package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationRecord
import com.example.data.repository.NotificationRepository
import com.example.data.repository.RetentionPeriod
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppCategoryGroup(
  val packageName: String,
  val appName: String,
  val notificationCount: Int,
  val latestPostTime: Long,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: NotificationRepository
  private val settingsRepository: SettingsRepository

  init {
    val db = AppDatabase.getInstance(application)
    repository = NotificationRepository(db.notificationDao())
    settingsRepository = SettingsRepository.getInstance(application)

    // Automatically purge expired notifications on startup based on configured retention period
    viewModelScope.launch {
      repository.purgeExpired(settingsRepository.getRetentionPeriod())
    }
  }

  val isCollectionPaused: StateFlow<Boolean> = settingsRepository.isCollectionPaused
  val excludedPackages: StateFlow<Set<String>> = settingsRepository.excludedPackages
  val retentionPeriod: StateFlow<RetentionPeriod> = settingsRepository.retentionPeriod

  val notifications: StateFlow<List<NotificationRecord>> = repository.allNotifications
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  val totalCount: StateFlow<Int> = repository.totalCount
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0,
    )

  val todayCount: StateFlow<Int> = repository.getTodayCount()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0,
    )

  val distinctAppCount: StateFlow<Int> = repository.distinctAppCount
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0,
    )

  val deletedCount: StateFlow<Int> = repository.deletedCount
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 0,
    )

  // Reactive list of apps grouped by package name
  val appCategories: StateFlow<List<AppCategoryGroup>> = repository.allNotifications
    .map { list ->
      list.groupBy { it.packageName }
        .map { (pkg, records) ->
          val rawName = records.firstOrNull { it.appName.isNotBlank() }?.appName
          val appName = com.example.util.AppNamingUtils.getCleanAppName(pkg, rawName)
          val count = records.size
          val latestTime = records.maxOfOrNull { it.postTime } ?: 0L
          AppCategoryGroup(
            packageName = pkg,
            appName = appName,
            notificationCount = count,
            latestPostTime = latestTime,
          )
        }
        .sortedWith(
          compareByDescending<AppCategoryGroup> { it.notificationCount }
            .thenByDescending { it.latestPostTime },
        )
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  fun getNotificationsForPackage(packageName: String): Flow<List<NotificationRecord>> =
    repository.getNotificationsByPackage(packageName)

  fun toggleStar(id: Long) {
    viewModelScope.launch {
      repository.toggleStar(id)
    }
  }

  fun deleteNotification(id: Long) {
    viewModelScope.launch {
      repository.deleteById(id)
    }
  }

  fun deleteNotificationsForPackage(packageName: String) {
    viewModelScope.launch {
      repository.deleteByPackage(packageName)
    }
  }

  fun clearAll() {
    viewModelScope.launch {
      repository.clearAll()
    }
  }

  fun setCollectionPaused(paused: Boolean) {
    settingsRepository.setCollectionPaused(paused)
  }

  fun toggleAppExclusion(packageName: String) {
    settingsRepository.toggleExcludedApp(packageName)
  }

  fun setRetentionPeriod(period: RetentionPeriod) {
    settingsRepository.setRetentionPeriod(period)
    viewModelScope.launch {
      repository.purgeExpired(period)
    }
  }
}
