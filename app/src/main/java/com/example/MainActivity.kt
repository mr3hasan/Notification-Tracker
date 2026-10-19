package com.example

import android.content.Intent
import android.os.Build
import android.util.Log

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SettingsRepository
import com.example.ui.NotificationTrackerApp
import com.example.ui.theme.NotificationTrackerTheme
import com.example.util.AppLockManager
import com.example.util.NotificationAccessUtils
import com.example.service.NotificationMonitorForegroundService

class MainActivity : ComponentActivity() {

  private var isUnlocked by mutableStateOf(false)
  private var authErrorMessage by mutableStateOf<String?>(null)

  private val deviceCredentialLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      isUnlocked = true
      authErrorMessage = null
    } else {
      authErrorMessage = "Security verification was cancelled"
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    startTrackingServiceSafely()

    val settingsRepo = SettingsRepository.getInstance(this)
    val appLockEnabled = settingsRepo.isAppLockEnabled()

    // If app lock is disabled, start unlocked immediately
    if (!appLockEnabled) {
      isUnlocked = true
    }

    setContent {
      NotificationTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          if (isUnlocked) {
            NotificationTrackerApp()
          } else {
            // Once the UI is attached, trigger biometric prompt automatically
            LaunchedEffect(Unit) {
              triggerBiometricAuth()
            }

            AppLockScreen(
              errorMessage = authErrorMessage,
              onFingerprintClick = { triggerBiometricAuth() },
              onPinClick = { launchDeviceCredential() }
            )
          }
        }
      }
    }
  }

  private fun triggerBiometricAuth() {
    authErrorMessage = null
    AppLockManager.authenticate(
      activity = this,
      onSuccess = {
        isUnlocked = true
        authErrorMessage = null
      },
      onError = { message ->
        authErrorMessage = message
      },
      onFallbackToDeviceCredential = {
        launchDeviceCredential()
      }
    )
  }

  private fun launchDeviceCredential() {
    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
      "Unlock Notification Tracker",
      "Enter your device PIN, pattern or password"
    )
    if (intent != null) {
      deviceCredentialLauncher.launch(intent)
    } else {
      // Device has no credential configured
      isUnlocked = true
    }
  }

  override fun onResume() {
    super.onResume()
    startTrackingServiceSafely()
    NotificationAccessUtils.triggerServiceRebind(this)
  }

  private fun startTrackingServiceSafely() {
    NotificationMonitorForegroundService.start(this)
  } else {
        startService(serviceIntent)
      }
    } catch (e: Exception) {
      Log.w("MainActivity", "Failed to start tracking service", e)
    }
  }
}

@Composable
fun AppLockScreen(
  errorMessage: String?,
  onFingerprintClick: () -> Unit,
  onPinClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth(0.9f)
    ) {
      Box(
        modifier = Modifier
          .size(86.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "App Locked",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(44.dp)
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Notification Tracker Locked",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "ফিঙ্গারপ্রিন্ট বা সিকিউরিটি লক দিয়ে আনলক করুন",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }

      if (errorMessage != null) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = onFingerprintClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("ফিঙ্গারপ্রিন্ট দিয়ে আনলক করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onPinClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 13.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Key,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("পিন বা পাসওয়ার্ড ব্যবহার করুন", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}
