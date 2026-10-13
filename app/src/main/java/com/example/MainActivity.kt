package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.NotificationTrackerApp
import com.example.ui.theme.NotificationTrackerTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      NotificationTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          NotificationTrackerApp()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Rebind listener service if access is granted (critical for Xiaomi / MIUI / HyperOS)
    com.example.util.NotificationAccessUtils.triggerServiceRebind(this)
  }
}

@Preview(showBackground = true)
@Composable
fun NotificationTrackerPreview() {
  NotificationTrackerTheme {
    NotificationTrackerApp()
  }
}
