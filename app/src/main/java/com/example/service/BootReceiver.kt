package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.NotificationAccessUtils

/**
 * Receiver to restore NotificationListenerService connection automatically on device boot or app update.
 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent == null) return
    val action = intent.action
    Log.d(TAG, "BootReceiver received action: $action")
    if (action == Intent.ACTION_BOOT_COMPLETED ||
      action == Intent.ACTION_MY_PACKAGE_REPLACED ||
      action == "android.intent.action.QUICKBOOT_POWERON"
    ) {
      NotificationAccessUtils.triggerServiceRebind(context)
    }
  }

  companion object {
    private const val TAG = "BootReceiver"
  }
}
