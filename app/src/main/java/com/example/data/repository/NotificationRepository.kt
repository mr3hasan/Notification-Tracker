package com.example.data.repository

import com.example.data.local.NotificationDao
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class NotificationRepository(private val notificationDao: NotificationDao) {

  val allNotifications: Flow<List<NotificationRecord>> = notificationDao.getAllNotifications()

  val totalCount: Flow<Int> = notificationDao.getTotalCount()

  val distinctAppCount: Flow<Int> = notificationDao.getDistinctAppCount()

  fun getTodayCount(): Flow<Int> {
    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return notificationDao.getTodayCount(calendar.timeInMillis)
  }

  suspend fun insertNotification(record: NotificationRecord): Boolean {
    // Exact match deduplication:
    // Drop if exact same package, title, and content arrived within 60 seconds (60000ms)
    // This eliminates redundant OS/app status-bar re-posts of the same message (e.g. 3x at 09:08).
    val recent = notificationDao.findRecentNotification(
      packageName = record.packageName,
      title = record.title,
      content = record.content,
      minPostTime = record.postTime - 60000L,
    )
    if (recent != null) {
      return false
    }

    notificationDao.insert(record)
    return true
  }

  fun getNotificationsByPackage(packageName: String): Flow<List<NotificationRecord>> =
    notificationDao.getNotificationsByPackage(packageName)

  val deletedCount: Flow<Int> = notificationDao.getDeletedCount()

  suspend fun markAsDeletedByKey(key: String, removedTime: Long = System.currentTimeMillis()): Int =
    notificationDao.markAsDeletedByKey(key, removedTime)

  suspend fun toggleStar(id: Long) = notificationDao.toggleStar(id)

  suspend fun deleteById(id: Long) = notificationDao.deleteById(id)

  suspend fun deleteByPackage(packageName: String) = notificationDao.deleteByPackage(packageName)

  suspend fun clearAll() = notificationDao.clearAll()

  suspend fun deleteOlderThan(cutoffTimestamp: Long): Int = notificationDao.deleteOlderThan(cutoffTimestamp)

  suspend fun purgeExpired(
    retentionPeriod: RetentionPeriod,
    currentTime: Long = System.currentTimeMillis(),
  ): Int {
    val cutoff = SettingsRepository.calculateCutoffTimestamp(retentionPeriod, currentTime) ?: return 0
    return notificationDao.deleteOlderThan(cutoff)
  }
}
