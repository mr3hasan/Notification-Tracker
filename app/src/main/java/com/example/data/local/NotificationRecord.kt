package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a locally captured notification event.
 */
@Entity(
  tableName = "notifications",
  indices = [
    Index(value = ["package_name"]),
    Index(value = ["post_time"]),
  ],
)
data class NotificationRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,

  @ColumnInfo(name = "package_name")
  val packageName: String,

  @ColumnInfo(name = "app_name")
  val appName: String,

  @ColumnInfo(name = "title")
  val title: String,

  @ColumnInfo(name = "content")
  val content: String,

  @ColumnInfo(name = "post_time")
  val postTime: Long,

  @ColumnInfo(name = "notification_key")
  val notificationKey: String = "",

  @ColumnInfo(name = "is_deleted")
  val isDeleted: Boolean = false,

  @ColumnInfo(name = "removed_time")
  val removedTime: Long? = null,

  @ColumnInfo(name = "is_starred")
  val isStarred: Boolean = false,
)
