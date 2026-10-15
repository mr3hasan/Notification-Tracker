package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Utility helper specifically tailored for Xiaomi, Redmi, and Poco devices running MIUI or HyperOS.
 * Provides direct deep-links to Autostart, Battery Saver "No Restrictions", and background lock settings
 * to prevent MIUI aggressive task killers from terminating NotificationTrackerListenerService.
 */
object XiaomiOptimizationHelper {

  private const val TAG = "XiaomiOptimization"

  /**
   * Checks whether the current device is manufactured by Xiaomi (including Redmi and Poco).
   */
  fun isXiaomiDevice(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    return manufacturer.contains("xiaomi") ||
      brand.contains("xiaomi") ||
      brand.contains("redmi") ||
      brand.contains("poco")
  }

  /**
   * Attempts to launch MIUI / HyperOS Autostart management screen.
   */
  fun openAutostartSettings(context: Context): Boolean {
    val intents = listOf(
      // Strategy 1: Standard MIUI Autostart activity
      Intent().setComponent(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
      ),
      // Strategy 2: MIUI Autostart action
      Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT),
      // Strategy 3: Older MIUI PermCenter
      Intent().setComponent(
        ComponentName("com.miui.permcenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
      ),
      // Strategy 4: App permissions editor
      Intent("miui.intent.action.APP_PERM_EDITOR").apply {
        setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
        putExtra("extra_pkgname", context.packageName)
      }
    )

    for (intent in intents) {
      try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
          context.startActivity(intent)
          Log.d(TAG, "Successfully opened Xiaomi Autostart with intent: $intent")
          return true
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed intent $intent: ${e.message}")
      }
    }

    // Fallback: App info details
    return openAppDetails(context)
  }

  /**
   * Attempts to open MIUI Battery Saver configuration to set background restriction to "No Restrictions".
   */
  fun openNoRestrictionsBatterySettings(context: Context): Boolean {
    val intents = listOf(
      // Strategy 1: MIUI Powerkeeper HiddenAppsConfigActivity
      Intent().setComponent(
        ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
      ).apply {
        putExtra("package_name", context.packageName)
        putExtra("package_label", context.applicationInfo.loadLabel(context.packageManager))
      },
      // Strategy 2: System battery optimization ignore request
      Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
      },
      // Strategy 3: General ignore battery optimizations screen
      Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    )

    for (intent in intents) {
      try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) != null) {
          context.startActivity(intent)
          Log.d(TAG, "Successfully opened battery settings with intent: $intent")
          return true
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed battery intent $intent: ${e.message}")
      }
    }

    return openAppDetails(context)
  }

  private fun openAppDetails(context: Context): Boolean {
    return try {
      val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to open app details fallback", e)
      false
    }
  }
}
