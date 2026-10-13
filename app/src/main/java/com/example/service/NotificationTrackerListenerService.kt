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
    // Instantly capture any active notifications currently sitting in the status bar
    try {
      val activeSbns = activeNotifications
      if (!activeSbns.isNullOrEmpty()) {
        for (sbn in activeSbns) {
          onNotificationPosted(sbn)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error collecting initial active notifications", e)
    }
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

    // Extract title (supports standard title, conversation title, big title)
    val titleCharSequence = extras?.getCharSequence(Notification.EXTRA_TITLE)
      ?: extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
      ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)

    var textCharSequence = extras?.getCharSequence(Notification.EXTRA_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)

    // For messaging apps like WhatsApp that bundle multiple messages as EXTRA_TEXT_LINES
    if (textCharSequence.isNullOrBlank() && extras != null) {
      val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
      if (!textLines.isNullOrEmpty()) {
        textCharSequence = textLines.joinToString("\n")
      }
    }

    // For modern chat apps (Facebook Messenger, Instagram, Signal, Google Messages) using Notification.MessagingStyle
    if (textCharSequence.isNullOrBlank() && extras != null) {
      try {
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messages.isNullOrEmpty()) {
          val lastMessageBundle = messages.lastOrNull() as? android.os.Bundle
          val msgText = lastMessageBundle?.getCharSequence("text")
          val sender = lastMessageBundle?.getCharSequence("sender")
          if (!msgText.isNullOrBlank()) {
            textCharSequence = if (!sender.isNullOrBlank()) "$sender: $msgText" else msgText
          }
        }
      } catch (ignored: Exception) {
        // Fallback safely if parcelable casting varies by OS
      }
    }

    // Fallback to tickerText if text is still empty
    if (textCharSequence.isNullOrBlank()) {
      textCharSequence = notification.tickerText
    }

    // Extract display name of source app when available
    val appDisplayName = try {
      val appInfo = packageManager.getApplicationInfo(sourcePackage, 0)
      packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
      sourcePackage
    } catch (e: Exception) {
      sourcePackage
    }

    var title = titleCharSequence?.toString()?.trim() ?: ""
    var text = textCharSequence?.toString()?.trim() ?: ""

    // Never drop any notification: if both are blank, use app name and alert placeholder
    if (title.isEmpty() && text.isEmpty()) {
      title = appDisplayName
      text = "Notification alert received"
    } else if (title.isEmpty()) {
      title = appDisplayName
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
