package com.example.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.service.NotificationTrackerListenerService

object NotificationAccessUtils {
  private const val TAG = "NotificationAccessUtils"

  /**
   * Checks whether this application currently has permission to listen to notifications.
   */
  fun isNotificationAccessGranted(context: Context): Boolean {
    return try {
      val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
      if (enabledPackages.contains(context.packageName)) {
        return true
      }

      // Supplementary check using the secure settings string
      val flatListeners = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners",
      )
      if (!flatListeners.isNullOrEmpty()) {
        val targetComponent = ComponentName(context, NotificationTrackerListenerService::class.java)
        val names = flatListeners.split(":")
        for (name in names) {
          val cn = ComponentName.unflattenFromString(name)
          if (cn != null && (cn == targetComponent || cn.packageName == context.packageName)) {
            return true
          }
        }
      }
      false
    } catch (e: Exception) {
      Log.e(TAG, "Error checking notification access status", e)
      false
    }
  }

  /**
   * Rebinds the NotificationListenerService safely.
   * If the service is already connected and active, it does nothing so that active listening is NOT interrupted.
   * If not connected, it uses Android 7+'s official NotificationListenerService.requestRebind() API.
   */
  fun triggerServiceRebind(context: Context) {
    try {
      if (!isNotificationAccessGranted(context)) return
      val component = ComponentName(context, NotificationTrackerListenerService::class.java)
      try {
        android.service.notification.NotificationListenerService.requestRebind(component)
        Log.d(TAG, "Executed NotificationListenerService.requestRebind.")
      } catch (e: Exception) {
        Log.w(TAG, "requestRebind failed", e)
      }
      if (!NotificationTrackerListenerService.isConnected) {
        try {
          val pm = context.packageManager
          pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
          )
          pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
          )
          Log.d(TAG, "Revived NotificationListenerService component.")
        } catch (ignored: Exception) {}
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to rebind NotificationListenerService", e)
    }
  }

  /**
   * Attempts to launch the system Settings screen where the user can grant Notification Access.
   *
   * @return true if an intent was successfully launched; false if no matching activity was found.
   */
  fun openNotificationListenerSettings(context: Context): Boolean {
    val serviceComponent = ComponentName(context, NotificationTrackerListenerService::class.java)

    // Strategy 1: On Android 11+ (API 30+), try the app-specific detail settings screen
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
          putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            serviceComponent.flattenToString(),
          )
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (detailIntent.resolveActivity(context.packageManager) != null) {
          context.startActivity(detailIntent)
          return true
        }
      } catch (e: Exception) {
        Log.w(TAG, "Detail settings intent failed, falling back to general listener settings", e)
      }
    }

    // Strategy 2: General notification listener settings screen
    try {
      val generalIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (generalIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(generalIntent)
        return true
      }
    } catch (e: Exception) {
      Log.w(TAG, "General listener settings intent failed, trying standard settings fallback", e)
    }

    // Strategy 3: Standard system settings fallback
    return try {
      val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(fallbackIntent)
      true
    } catch (e: ActivityNotFoundException) {
      Log.e(TAG, "No Settings activity could be resolved", e)
      false
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch settings", e)
      false
    }
  }

  /**
   * Checks whether the app is whitelisted from battery optimizations.
   */
  fun isBatteryOptimizationIgnored(context: Context): Boolean {
    return try {
      val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
      powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Opens the system battery optimization settings screen so the user can ensure background collection never gets killed by OEM task killers.
   */
  fun openBatteryOptimizationSettings(context: Context): Boolean {
    // Strategy 1: Direct request intent
    try {
      val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        return true
      }
    } catch (ignored: Exception) {}

    // Strategy 2: General battery saver settings
    try {
      val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        return true
      }
    } catch (ignored: Exception) {}

    // Strategy 3: Application details settings
    return try {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      false
    }
  }
}
