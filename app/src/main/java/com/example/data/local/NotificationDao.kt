package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
  @Query("SELECT * FROM notifications ORDER BY post_time DESC")
  fun getAllNotifications(): Flow<List<NotificationRecord>>

  @Query("SELECT COUNT(*) FROM notifications")
  fun getTotalCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM notifications WHERE post_time >= :startOfDayTimestamp")
  fun getTodayCount(startOfDayTimestamp: Long): Flow<Int>

  @Query("SELECT COUNT(DISTINCT package_name) FROM notifications")
  fun getDistinctAppCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(record: NotificationRecord): Long

  @Query(
    """
    SELECT * FROM notifications 
    WHERE package_name = :packageName 
      AND title = :title 
      AND content = :content 
      AND post_time >= :minPostTime
      AND post_time <= :maxPostTime
    LIMIT 1
    """,
  )
  suspend fun findRecentNotification(
    packageName: String,
    title: String,
    content: String,
    minPostTime: Long,
    maxPostTime: Long,
  ): NotificationRecord?

  @Query(
    """
    SELECT * FROM notifications 
    WHERE notification_key = :notificationKey 
      AND content = :content 
    LIMIT 1
    """,
  )
  suspend fun findByKeyAndContent(
    notificationKey: String,
    content: String,
  ): NotificationRecord?

  @Query(
    """
    SELECT COUNT(*) FROM notifications 
    WHERE package_name = :packageName 
      AND title = :title 
      AND content = :content 
      AND post_time >= :minPostTime
      AND post_time <= :maxPostTime
    """,
  )
  suspend fun countRecentDuplicates(
    packageName: String,
    title: String,
    content: String,
    minPostTime: Long,
    maxPostTime: Long,
  ): Int

  @Query("SELECT * FROM notifications WHERE package_name = :packageName ORDER BY post_time DESC")
  fun getNotificationsByPackage(packageName: String): Flow<List<NotificationRecord>>

  @Query("UPDATE notifications SET is_deleted = 1, removed_time = :removedTime WHERE notification_key = :notificationKey AND is_deleted = 0")
  suspend fun markAsDeletedByKey(notificationKey: String, removedTime: Long): Int

  @Query("UPDATE notifications SET is_starred = NOT is_starred WHERE id = :id")
  suspend fun toggleStar(id: Long)

  @Query("SELECT COUNT(*) FROM notifications WHERE is_deleted = 1")
  fun getDeletedCount(): Flow<Int>

  @Query("DELETE FROM notifications WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM notifications WHERE package_name = :packageName")
  suspend fun deleteByPackage(packageName: String)

  @Query("DELETE FROM notifications")
  suspend fun clearAll()

  @Query("DELETE FROM notifications WHERE post_time < :cutoffTimestamp AND is_starred = 0")
  suspend fun deleteOlderThan(cutoffTimestamp: Long): Int
}
