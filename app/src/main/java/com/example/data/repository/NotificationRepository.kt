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
    // 1. Direct rapid burst duplicate check: exact same package, title, and content within 2.5 seconds
    val recent = notificationDao.findRecentNotification(
      packageName = record.packageName,
      title = record.title,
      content = record.content,
      minPostTime = record.postTime - 2500L,
      maxPostTime = record.postTime + 2500L,
    )
    if (recent != null) {
      return false
    }

    // 2. Active notification re-post check:
    // If the same notificationKey already exists in the database with the exact same content,
    // do not create duplicate entries when the OS simply refreshes or unbundles the status bar item.
    if (record.notificationKey.isNotBlank()) {
      val existingWithKey = notificationDao.findByKeyAndContent(record.notificationKey, record.content)
      if (existingWithKey != null) {
        return false
      }
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
