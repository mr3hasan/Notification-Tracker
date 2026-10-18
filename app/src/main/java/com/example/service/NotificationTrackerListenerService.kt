package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationRecord
import com.example.data.repository.NotificationRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to intercept and locally record incoming notification events.
 * Runs with Foreground Service protection to ensure 24/7 background uptime on Xiaomi / MIUI / HyperOS.
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

    // Ensure persistent foreground service is active immediately
    startForegroundServiceSafely()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    startForegroundServiceSafely()
    return START_STICKY
  }

  private fun startForegroundServiceSafely() {
    try {
      val channelId = "notification_tracker_persistent_channel"
      val notificationManager = getSystemService(NotificationManager::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          channelId,
          "Background Monitoring Service",
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = "Maintains 24/7 notification monitoring in background"
          setShowBadge(false)
          enableLights(false)
          enableVibration(false)
          setSound(null, null)
        }
        notificationManager?.createNotificationChannel(channel)
      }

      val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0,
      )

      val notification = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Notification Tracker Active")
        .setContentText("Monitoring device notifications in background")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(pendingIntent)
        .build()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          PERSISTENT_NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
      } else {
        startForeground(PERSISTENT_NOTIFICATION_ID, notification)
      }
      Log.d(TAG, "Foreground service running to prevent OEM task killer termination.")
    } catch (e: Exception) {
      Log.w(TAG, "startForegroundServiceSafely exception", e)
    }
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
    try {
      processNotificationPosted(sbn)
    } catch (t: Throwable) {
      Log.e(TAG, "Unhandled error in onNotificationPosted", t)
    }
  }

  private fun processNotificationPosted(sbn: StatusBarNotification?) {
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
          if (sender.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

    var rawTitle = titleCharSequence?.toString()?.trim() ?: ""
    var text = textCharSequence?.toString()?.trim() ?: ""

    // Never drop any notification: if both are blank, use app name and alert placeholder
    if (rawTitle.isEmpty() && text.isEmpty()) {
      rawTitle = appDisplayName
      text = "Notification alert received"
    } else if (rawTitle.isEmpty()) {
      rawTitle = appDisplayName
    } else if (text.isEmpty()) {
      // For media/image/voice alerts where title is contact name but text body was empty
      text = "Media or notification alert received"
    }

    // Clean dynamic counters like (8 messages) from group titles
    val title = cleanNotificationTitle(rawTitle)

    // Check if the incoming alert is explicitly flagged as a deleted/recalled message
    val isDeletedMessage = isMessageRecalledText(text) || isMessageRecalledText(title)

    val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
    val notificationKey = sbn.key ?: ""

    // In-memory rapid deduplication check (safely iterate without ConcurrentHashMap.removeIf)
    val now = System.currentTimeMillis()
    try {
      val iterator = recentNotificationTimestamps.entries.iterator()
      while (iterator.hasNext()) {
        val entry = iterator.next()
        if (now - entry.value > 60_000L) {
          iterator.remove()
        }
      }
    } catch (t: Throwable) {
      Log.w(TAG, "Error pruning timestamp map", t)
    }

    val dedupKey = "$sourcePackage|$title|$text"
    val lastSeen = recentNotificationTimestamps[dedupKey]
    if (lastSeen != null && (now - lastSeen) < 1500L) {
      Log.d(TAG, "Deduplication: Skipped rapid burst duplicate within 1.5s from $sourcePackage: $title")
      return
    }
    recentNotificationTimestamps[dedupKey] = now

    val record = NotificationRecord(
      packageName = sourcePackage,
      appName = appDisplayName,
      title = title,
      content = text,
      postTime = postTime,
      notificationKey = notificationKey,
      isDeleted = isDeletedMessage,
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

  override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
    // No-op: Do not mark notifications as recalled on removal because WhatsApp cancels
    // notifications when user opens chat or posts group updates.
    // Real retracted messages are detected directly via isMessageRecalledText.
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    super.onNotificationRemoved(sbn)
  }

  companion object {
    private const val TAG = "NotifTrackerListener"
    private const val PERSISTENT_NOTIFICATION_ID = 8801
    private val recentNotificationTimestamps = ConcurrentHashMap<String, Long>()

    @Volatile
    var isConnected: Boolean = false
      private set

    fun isMessageRecalledText(text: String?): Boolean {
      if (text.isNullOrBlank()) return false
      val lower = text.lowercase(java.util.Locale.ROOT)
      return lower.contains("this message was deleted") ||
        lower.contains("message was deleted") ||
        lower.contains("ডিলিট করা হয়েছে") ||
        lower.contains("মুছে ফেলা হয়েছে") ||
        lower.contains("message was retracted") ||
        lower.contains("this message was removed")
    }

    fun cleanNotificationTitle(rawTitle: String): String {
      if (rawTitle.isBlank()) return rawTitle
      return rawTitle
        .replace(Regex("""\s*\(\d+\s+(?:new\s+)?messages?\):?""", RegexOption.IGNORE_CASE), ":")
        .replace(Regex("""\s*:\s*:""", RegexOption.IGNORE_CASE), ":")
        .replace(Regex("""\s+""", RegexOption.IGNORE_CASE), " ")
        .trim()
    }

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
