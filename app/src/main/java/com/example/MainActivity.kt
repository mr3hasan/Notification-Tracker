package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SettingsRepository
import com.example.ui.NotificationTrackerApp
import com.example.ui.theme.NotificationTrackerTheme
import com.example.util.AppLockManager

class MainActivity : ComponentActivity() {

  private var isUnlocked by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val settingsRepo = SettingsRepository.getInstance(this)
    val appLockEnabled = settingsRepo.isAppLockEnabled()

    // If app lock is disabled, start unlocked immediately
    if (!appLockEnabled) {
      isUnlocked = true
    } else {
      triggerBiometricAuth()
    }

    setContent {
      NotificationTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          if (isUnlocked) {
            NotificationTrackerApp()
          } else {
            AppLockScreen(
              onUnlockClick = { triggerBiometricAuth() }
            )
          }
        }
      }
    }
  }

  private fun triggerBiometricAuth() {
    AppLockManager.authenticate(
      activity = this,
      onSuccess = { isUnlocked = true },
      onError = { /* Keep locked */ }
    )
  }

  override fun onResume() {
    super.onResume()
    // Rebind listener service if access is granted (critical for Xiaomi / MIUI / HyperOS)
    com.example.util.NotificationAccessUtils.triggerServiceRebind(this)
  }
}

@Composable
fun AppLockScreen(onUnlockClick: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "App Locked",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(40.dp)
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
          text = "ফিঙ্গারপ্রিন্ট বা সিকিউরিটি পিন দিয়ে আনলক করুন",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Button(
        onClick = onUnlockClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(0.85f),
        contentPadding = PaddingValues(vertical = 14.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("আনলক করুন (Unlock)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
