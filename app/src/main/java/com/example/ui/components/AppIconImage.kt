package com.example.ui.components

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Loads the native system app icon for a given package name with fallback.
 */
@Composable
fun AppIconImage(
  packageName: String,
  appName: String = "",
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  val context = LocalContext.current
  val drawable: Drawable? = remember(packageName) {
    try {
      context.packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
      null
    }
  }

  if (drawable != null) {
    AndroidView(
      factory = { ctx ->
        ImageView(ctx).apply {
          scaleType = ImageView.ScaleType.FIT_CENTER
        }
      },
      update = { imageView ->
        imageView.setImageDrawable(drawable)
      },
      modifier = modifier
        .size(size)
        .clip(CircleShape),
    )
  } else {
    // Safe fallback icon
    Box(
      modifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      val initial = appName.firstOrNull { it.isLetterOrDigit() }?.uppercase()
      if (initial != null) {
        Text(
          text = initial,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
        )
      } else {
        Icon(
          imageVector = Icons.Default.Apps,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(size / 2),
        )
      }
    }
  }
}
