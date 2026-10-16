package com.example.util

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log

/**
 * Handles biometric (fingerprint/face) and device credential (PIN/pattern/password)
 * authentication for app security lock with robust fallback on all Android versions and OEM skins (e.g. Xiaomi / MIUI).
 */
object AppLockManager {

  private const val TAG = "AppLockManager"

  fun isDeviceSecure(context: Context): Boolean {
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return keyguardManager?.isDeviceSecure ?: false
  }

  fun canAuthenticateBiometric(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        val biometricManager = context.getSystemService(BiometricManager::class.java) ?: return false
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        } else {
          0
        }
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          biometricManager.canAuthenticate(authenticators)
        } else {
          @Suppress("DEPRECATION")
          biometricManager.canAuthenticate()
        }
        return status == BiometricManager.BIOMETRIC_SUCCESS
      } catch (e: Exception) {
        Log.w(TAG, "Biometric check exception", e)
      }
    }
    return false
  }

  /**
   * Prompts user for Fingerprint/Face unlock, with fallback to Device PIN / Pattern.
   */
  fun authenticate(
    activity: Activity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onFallbackToDeviceCredential: () -> Unit
  ) {
    val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    if (keyguardManager == null || !keyguardManager.isDeviceSecure) {
      // If user hasn't set any lock on their phone, allow access
      onSuccess()
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      try {
        val cancellationSignal = CancellationSignal()
        val builder = BiometricPrompt.Builder(activity)
          .setTitle("Notification Tracker")
          .setSubtitle("Touch the fingerprint sensor")
          .setDescription("Verify your identity to unlock notification history")

        var useDeviceCredentialInPrompt = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          try {
            builder.setAllowedAuthenticators(
              BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            useDeviceCredentialInPrompt = true
          } catch (e: Exception) {
            Log.w(TAG, "DEVICE_CREDENTIAL authenticator not accepted by builder, using negative button", e)
          }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
          try {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
            useDeviceCredentialInPrompt = true
          } catch (e: Exception) {
            Log.w(TAG, "setDeviceCredentialAllowed not supported", e)
          }
        }

        // If prompt doesn't natively combine device credential, supply negative button
        if (!useDeviceCredentialInPrompt) {
          builder.setNegativeButton(
            "Use PIN / Pattern",
            activity.mainExecutor
          ) { _, _ ->
            onFallbackToDeviceCredential()
          }
        }

        val prompt = builder.build()

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
              if (errorCode == 13 || errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
                onFallbackToDeviceCredential()
              } else if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT ||
                         errorCode == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT ||
                         errorCode == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS ||
                         errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT ||
                         errorCode == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                // Biometrics locked out or unavailable - redirect directly to PIN/Pattern
                onFallbackToDeviceCredential()
              } else {
                onError(errString?.toString() ?: "Authentication cancelled or failed")
              }
            }

            override fun onAuthenticationFailed() {
              super.onAuthenticationFailed()
              Log.w(TAG, "Biometric verification failed - finger not recognized")
              onError("Fingerprint not recognized. Please try again or use PIN.")
            }
          }
        )
        return
      } catch (e: Exception) {
        Log.e(TAG, "Failed to launch BiometricPrompt, falling back to device credential", e)
        onFallbackToDeviceCredential()
        return
      }
    }

    // Fallback for API < 28
    onFallbackToDeviceCredential()
  }
}
