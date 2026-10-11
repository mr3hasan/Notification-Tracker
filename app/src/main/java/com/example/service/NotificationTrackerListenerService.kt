package com.example.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationRecord
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import com.example.util.NotificationAccessUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service to intercept and locally record incoming notification events.
 */
class NotificationTrackerListenerService : NotificationListenerService() {

  private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private lateinit var repository: NotificationRepository
  private lateinit var settingsRepository: SettingsRepository

  override fun onCreate() {
    super.onCreate()
    val db = AppDatabase.getInstance(applicationContext)
    repository = NotificationRepository(db.notificationDao())
    settingsRepository = SettingsRepository.getInstance(applicationContext)
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }

  override fun onListenerConnected() {
    super.onListenerConnected()
    Log.d(TAG, "Notification listener connected.")
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    Log.d(TAG, "Notification listener disconnected.")
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return

    val sourcePackage = sbn.packageName ?: return

    // 1. Ignore notifications posted by Notification Tracker itself to prevent loops
    if (sourcePackage == packageName) {
      return
    }

    // 2. Capture new posted notifications only after notification access has been granted
    if (!NotificationAccessUtils.isNotificationAccessGranted(this)) {
      return
    }

    // 3. Check if collection is currently paused by user in Settings
    if (settingsRepository.isCollectionPaused()) {
      return
    }

    // 4. Check if the source app is in the user's excluded apps list
    if (settingsRepository.isAppExcluded(sourcePackage)) {
      return
    }

    // 3. Extract notification content safely
    val notification = sbn.notification ?: return
    val extras = notification.extras

    val titleCharSequence = extras?.getCharSequence(Notification.EXTRA_TITLE)
    val textCharSequence = extras?.getCharSequence(Notification.EXTRA_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)

    val title = titleCharSequence?.toString()?.trim() ?: ""
    val text = textCharSequence?.toString()?.trim() ?: ""

    // Ignore completely empty payloads
    if (title.isEmpty() && text.isEmpty()) {
      return
    }

    // 4. Extract display name of source app when available
    val appDisplayName = try {
      val appInfo = packageManager.getApplicationInfo(sourcePackage, 0)
      packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
      sourcePackage
    } catch (e: Exception) {
      sourcePackage
    }

    val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
    val notificationKey = sbn.key ?: ""

    val record = NotificationRecord(
      packageName = sourcePackage,
      appName = appDisplayName,
      title = title,
      content = text,
      postTime = postTime,
      notificationKey = notificationKey,
    )

    // 7. Safely persist record to local database without logging sensitive text
    serviceScope.launch {
      try {
        val inserted = repository.insertNotification(record)
        if (inserted) {
          val retention = settingsRepository.getRetentionPeriod()
          repository.purgeExpired(retention)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save notification record", e)
      }
    }
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val key = sbn.key ?: return
    val sourcePackage = sbn.packageName ?: return

    // Do not process internal notifications
    if (sourcePackage == packageName) return

    // Mark notification in DB as deleted/recalled by sender or dismissed
    serviceScope.launch {
      try {
        repository.markAsDeletedByKey(key, System.currentTimeMillis())
      } catch (e: Exception) {
        Log.e(TAG, "Failed to mark notification as deleted for key: $key", e)
      }
    }
  }

  companion object {
    private const val TAG = "NotifTrackerListener"
  }
}
