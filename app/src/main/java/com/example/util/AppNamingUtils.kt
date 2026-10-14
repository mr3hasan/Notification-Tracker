package com.example.util

object AppNamingUtils {

  private val KNOWN_APP_NAMES = mapOf(
    "com.whatsapp" to "WhatsApp",
    "com.whatsapp.w4b" to "WhatsApp Business",
    "com.facebook.orca" to "Messenger",
    "com.facebook.katana" to "Facebook",
    "com.facebook.lite" to "Facebook Lite",
    "com.google.android.youtube" to "YouTube",
    "com.google.android.apps.youtube.music" to "YouTube Music",
    "com.google.android.gm" to "Gmail",
    "com.google.android.apps.messaging" to "Messages",
    "com.google.android.apps.photos" to "Google Photos",
    "com.google.android.googlequicksearchbox" to "Google",
    "com.google.android.dialer" to "Phone",
    "com.android.chrome" to "Chrome",
    "com.android.vending" to "Google Play Store",
    "org.telegram.messenger" to "Telegram",
    "org.telegram.plus" to "Telegram Plus",
    "com.imo.android.imoim" to "IMO",
    "com.instagram.android" to "Instagram",
    "com.viber.voip" to "Viber",
    "com.snapchat.android" to "Snapchat",
    "com.tiktok.android" to "TikTok",
    "com.zhiliaoapp.musically" to "TikTok",
    "com.bKash.customerapp" to "bKash",
    "com.konasl.nagad" to "Nagad",
    "com.grameenphone" to "MyGP",
    "com.banglalink.mytariff" to "MyBL",
    "com.robi.myrobi" to "MyRobi",
    "com.pathao.user" to "Pathao",
    "com.ubercab" to "Uber",
    "com.foodpanda.android" to "Foodpanda",
    "com.daraz.android" to "Daraz",
    "com.spotify.music" to "Spotify",
    "com.xiaomi.discover" to "System apps updater",
    "com.miui.misound" to "Earphones",
    "com.miui.securitycenter" to "Security",
    "com.miui.gallery" to "Gallery",
    "com.miui.weather2" to "Weather",
    "com.miui.notes" to "Notes",
    "com.miui.calculator" to "Calculator",
    "android" to "Android System",
    "com.android.systemui" to "System UI",
    "com.borno.inputmethod" to "Borno Keyboard",
    "com.ridmik.keyboard" to "Ridmik Keyboard",
  )

  /**
   * Returns a clean, human-readable display name for any application.
   * Eliminates raw "com.xxxxx" package formats and ensures proper naming like "WhatsApp", "YouTube", "Messenger".
   */
  fun getCleanAppName(packageName: String, existingAppName: String? = null): String {
    val trimmedPkg = packageName.trim()
    val trimmedName = existingAppName?.trim().orEmpty()

    // 1. If existingAppName is already a clean name (not a package identifier)
    if (trimmedName.isNotEmpty() && !isRawPackageName(trimmedName)) {
      return trimmedName
    }

    // 2. Direct lookup in known app mapping
    KNOWN_APP_NAMES[trimmedPkg]?.let { return it }
    KNOWN_APP_NAMES[trimmedName]?.let { return it }

    // 3. Fallback: Parse package name intelligently to "XXXX"
    return formatPackageToName(trimmedPkg)
  }

  fun isRawPackageName(name: String): Boolean {
    val lower = name.lowercase()
    return lower.startsWith("com.") ||
      lower.startsWith("org.") ||
      lower.startsWith("net.") ||
      lower.startsWith("io.") ||
      lower.startsWith("android.") ||
      lower == "android" ||
      lower.contains(".")
  }

  private fun formatPackageToName(pkg: String): String {
    if (pkg.isBlank()) return "Application"
    val parts = pkg.split(".").filter { it.isNotEmpty() }
    if (parts.isEmpty()) return pkg

    // Choose relevant segment
    val candidate = if (parts.size > 2 && (parts.last() == "android" || parts.last() == "app")) {
      parts[parts.size - 2]
    } else {
      parts.last()
    }

    // Convert camelCase or snake_case to Title Case
    val words = candidate
      .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
      .replace("_", " ")
      .replace("-", " ")
      .split(" ")
      .filter { it.isNotEmpty() }

    return if (words.isNotEmpty()) {
      words.joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      }
    } else {
      candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
  }
}
