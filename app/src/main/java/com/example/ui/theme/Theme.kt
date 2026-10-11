package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = TrackerBlueNight,
    onPrimary = Color(0xFF002B66),
    primaryContainer = TrackerBlueContainerNight,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = TrackerTealNight,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = TrackerTealLight,
    tertiary = TrackerPurpleNight,
    background = SurfaceNight,
    onBackground = OnSurfaceNight,
    surface = SurfaceNight,
    onSurface = OnSurfaceNight,
    surfaceVariant = SurfaceContainerNight,
    onSurfaceVariant = OnSurfaceVariantNight,
    outline = OutlineNight,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TrackerBlue,
    onPrimary = Color.White,
    primaryContainer = TrackerBlueLight,
    onPrimaryContainer = TrackerBlueDark,
    secondary = TrackerTeal,
    onSecondary = Color.White,
    secondaryContainer = TrackerTealLight,
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = TrackerPurple,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
  )

@Composable
fun NotificationTrackerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  NotificationTrackerTheme(
    darkTheme = darkTheme,
    dynamicColor = dynamicColor,
    content = content,
  )
}
