package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.NotificationAccessUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Dedicated Foreground Service to keep the application process alive 24/7 on Xiaomi MIUI / HyperOS.
 * This service runs without BIND_NOTIFICATION_LISTENER_SERVICE, allowing the app to start it freely.
 * Its persistent notification prevents Android and OEM battery managers from terminating the listener.
 */
class NotificationMonitorForegroundService : Service() {

  private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    startForegroundSafely()
    startListenerWatchdog()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForegroundSafely()
    return START_STICKY
  }

  private fun startForegroundSafely() {
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
        .setContentText("Monitoring device notifications 24/7 in background")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(pendingIntent)
        .build()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          FOREGROUND_SERVICE_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
      } else {
        startForeground(FOREGROUND_SERVICE_ID, notification)
      }
      Log.d(TAG, "NotificationMonitorForegroundService successfully started in foreground.")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start foreground notification", e)
    }
  }

  private fun startListenerWatchdog() {
    serviceScope.launch {
      while (isActive) {
        try {
          if (!NotificationTrackerListenerService.isConnected) {
            Log.d(TAG, "Watchdog detected listener disconnected, triggering rebind...")
            NotificationAccessUtils.triggerServiceRebind(applicationContext)
          }
        } catch (ignored: Exception) {}
        delay(15_000L) // Verify connection every 15 seconds
      }
    }
  }

  companion object {
    private const val TAG = "NotifMonitorService"
    const val FOREGROUND_SERVICE_ID = 8802

    fun start(context: Context) {
      try {
        val intent = Intent(context, NotificationMonitorForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start NotificationMonitorForegroundService", e)
      }
    }
  }
}
