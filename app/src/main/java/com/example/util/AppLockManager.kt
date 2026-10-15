package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log

/**
 * Handles biometric (fingerprint/face) and device credential (PIN/pattern/password)
 * authentication for app security lock.
 */
object AppLockManager {

  private const val TAG = "AppLockManager"

  fun isDeviceSecure(context: Context): Boolean {
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return keyguardManager?.isDeviceSecure ?: false
  }

  /**
   * Prompts user for Fingerprint/Face or PIN/Pattern unlock.
   */
  fun authenticate(
    activity: Activity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
      // If user hasn't set any lock on their phone, allow bypass
      onSuccess()
      return
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val cancellationSignal = CancellationSignal()

        val prompt = BiometricPrompt.Builder(activity)
          .setTitle("Unlock Notification Tracker")
          .setSubtitle("Use your fingerprint, face, or device PIN")
          .setDescription("Verify your identity to view your notification history")
          .setDeviceCredentialAllowed(true)
          .build()

        prompt.authenticate(
          cancellationSignal,
          activity.mainExecutor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
              super.onAuthenticationSucceeded(result)
              Log.d(TAG, "Biometric authentication succeeded")
              onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
              super.onAuthenticationError(errorCode, errString)
              Log.w(TAG, "Biometric error: $errString (code: $errorCode)")
              onError(errString?.toString() ?: "Authentication failed")
            }

            override fun onAuthenticationFailed() {
              super.onAuthenticationFailed()
              Log.w(TAG, "Biometric verification failed")
            }
          }
        )
      } else {
        // Fallback for older APIs
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(
          "Unlock Notification Tracker",
          "Enter your device PIN/pattern"
        )
        if (intent != null) {
          activity.startActivityForResult(intent, 9999)
        } else {
          onSuccess()
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch authentication prompt", e)
      // Fallback
      val intent = keyguardManager.createConfirmDeviceCredentialIntent(
        "Unlock Notification Tracker",
        "Enter your device PIN/pattern"
      )
      if (intent != null) {
        activity.startActivity(intent)
      } else {
        onSuccess()
      }
    }
  }
}
