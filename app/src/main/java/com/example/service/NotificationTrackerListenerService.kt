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
    isConnected = true
    Log.d(TAG, "Notification listener connected.")
    // Instantly capture any active notifications currently sitting in the status bar
    try {
      val activeSbns = activeNotifications
      if (!activeSbns.isNullOrEmpty()) {
        for (sbn in activeSbns) {
          onNotificationPosted(sbn)
        }
      }
      // Periodically prune expired notifications once on connect
      serviceScope.launch {
        try {
          val retention = settingsRepository.getRetentionPeriod()
          repository.purgeExpired(retention)
        } catch (e: Exception) {
          Log.e(TAG, "Error purging expired notifications on connect", e)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error collecting initial active notifications", e)
    }
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    isConnected = false
    Log.d(TAG, "Notification listener disconnected. Requesting auto-rebind...")
    try {
      requestRebind(android.content.ComponentName(this, NotificationTrackerListenerService::class.java))
    } catch (e: Exception) {
      Log.w(TAG, "Auto requestRebind failed on disconnect", e)
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return

    val sourcePackage = sbn.packageName ?: return

    // 1. Ignore notifications posted by Notification Tracker itself to prevent loops
    if (sourcePackage == packageName) {
      return
    }

    // 2. Check if collection is currently paused by user in Settings
    if (settingsRepository.isCollectionPaused()) {
      return
    }

    // 3. Check if the source app is in the user's excluded apps list
    if (settingsRepository.isAppExcluded(sourcePackage)) {
      return
    }

    // 4. Extract notification content safely with complete Android fallbacks
    val notification = sbn.notification ?: return
    val extras = notification.extras

    // Extract title (supports standard title, conversation title, big title, android extras)
    val titleCharSequence = extras?.getCharSequence(Notification.EXTRA_TITLE)
      ?: extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
      ?: extras?.getCharSequence(Notification.EXTRA_TITLE_BIG)
      ?: extras?.getCharSequence("android.title")
      ?: extras?.getCharSequence("android.conversationTitle")

    var textCharSequence = extras?.getCharSequence(Notification.EXTRA_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)
      ?: extras?.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
      ?: extras?.getCharSequence("android.text")
      ?: extras?.getCharSequence("android.bigText")

    val textStr = textCharSequence?.toString()?.trim()
    val isSummary = isSummaryText(textStr)

    // For modern chat apps (WhatsApp, Facebook Messenger, Telegram, Signal, Google Messages) using Notification.MessagingStyle
    // For modern chat apps (WhatsApp, Facebook Messenger, Telegram, Signal, Google Messages) using Notification.MessagingStyle
    // When multiple messages arrive, WhatsApp sets EXTRA_TEXT to e.g. "2 new messages", while the actual latest message is in EXTRA_MESSAGES!
    if ((textCharSequence.isNullOrBlank() || isSummary) && extras != null) {
      try {
        val messagesRaw = extras.get(Notification.EXTRA_MESSAGES)
        val messageBundles: List<android.os.Bundle> = when (messagesRaw) {
          is Array<*> -> messagesRaw.filterIsInstance<android.os.Bundle>()
          is List<*> -> messagesRaw.filterIsInstance<android.os.Bundle>()
          else -> emptyList()
        }
        if (messageBundles.isNotEmpty()) {
          val lastBundle = messageBundles.lastOrNull()
          val msgText = lastBundle?.getCharSequence("text") ?: lastBundle?.getCharSequence("android.text")
          var sender = lastBundle?.getCharSequence("sender")?.toString() ?: lastBundle?.getCharSequence("android.sender")?.toString()
          if (sender.isNullOrBlank() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val person = lastBundle?.getParcelable<android.app.Person>("sender_person")
            sender = person?.name?.toString()
          }
          if (!msgText.isNullOrBlank()) {
            textCharSequence = if (!sender.isNullOrBlank() && titleCharSequence?.contains(sender, ignoreCase = true) == false) {
              "$sender: $msgText"
            } else {
              msgText
            }
          }
        }
      } catch (ignored: Exception) {
        // Fallback safely if parcelable casting varies by OS
      }
    }

    // For messaging apps like WhatsApp that bundle multiple messages as EXTRA_TEXT_LINES (InboxStyle)
    if ((textCharSequence.isNullOrBlank() || isSummaryText(textCharSequence?.toString())) && extras != null) {
      val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
      if (!textLines.isNullOrEmpty()) {
        val lastLine = textLines.lastOrNull { !it.isNullOrBlank() }
        if (!lastLine.isNullOrBlank()) {
          textCharSequence = lastLine
        }
      }
    }

    // Fallback to tickerText if text is still empty
    if (textCharSequence.isNullOrBlank()) {
      textCharSequence = notification.tickerText
    }

    // Extract display name of source app using PackageManager with AppNamingUtils fallback
    val rawLabel = try {
      val appInfo = packageManager.getApplicationInfo(sourcePackage, 0)
      packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
      null
    }
    val appDisplayName = com.example.util.AppNamingUtils.getCleanAppName(sourcePackage, rawLabel)

    var title = titleCharSequence?.toString()?.trim() ?: ""
    var text = textCharSequence?.toString()?.trim() ?: ""

    // Never drop any notification: if both are blank, use app name and alert placeholder
    if (title.isEmpty() && text.isEmpty()) {
      title = appDisplayName
      text = "Notification alert received"
    } else if (title.isEmpty()) {
      title = appDisplayName
    } else if (text.isEmpty()) {
      // For media/image/voice alerts where title is contact name but text body was empty
      text = "Media or notification alert received"
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

    // Safely persist record to local database
    serviceScope.launch {
      try {
        repository.insertNotification(record)
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

    // Avoid false "Recalled" status on system, utility, or ongoing notifications
    if (sourcePackage == "android" ||
      sourcePackage.startsWith("com.android.systemui") ||
      sourcePackage.startsWith("com.miui.system") ||
      sourcePackage.contains("inputmethod") ||
      sourcePackage.contains("keyboard") ||
      sbn.isOngoing
    ) {
      return
    }

    // Mark notification in DB as deleted/recalled by sender
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

    @Volatile
    var isConnected: Boolean = false
      private set

    private val SUMMARY_REGEX = Regex("""^\d+\s+(new\s+)?(messages?|alerts?|chats?|unread)""", RegexOption.IGNORE_CASE)

    fun isSummaryText(text: String?): Boolean {
      if (text.isNullOrBlank()) return false
      val trimmed = text.trim()
      return SUMMARY_REGEX.containsMatchIn(trimmed) ||
        trimmed.contains("messages from", ignoreCase = true) ||
        trimmed.equals("new message", ignoreCase = true) ||
        trimmed.equals("new messages", ignoreCase = true)
    }
  }
}
