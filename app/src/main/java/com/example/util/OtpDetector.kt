package com.example.util

/**
 * Utility to identify One-Time Passwords (OTPs) and verification codes
 * in notification titles and text.
 */
object OtpDetector {

  private val OTP_TRIGGER_WORDS = listOf(
    "otp", "code", "verification", "pin", "passcode", "password", "security code",
    "auth", "login code", "onetime", "one-time", "secret code", "valid for",
    "do not share", "do not disclose", "is your code"
  )

  // Regex matching 4 to 8 digit numbers, or hyphenated codes like 123-456
  private val OTP_REGEX = Regex("""\b(?<!\w)(\d{3,4}-\d{3,4}|\d{4,8})\b""")

  /**
   * Detects if the notification contains an OTP / verification code.
   * Returns the extracted code string or null if not an OTP.
   */
  fun extractOtp(title: String, content: String): String? {
    val fullText = "$title $content"
    val lower = fullText.lowercase()

    // Must contain at least one contextual trigger word or phrase
    val hasTrigger = OTP_TRIGGER_WORDS.any { trigger -> lower.contains(trigger) }
    if (!hasTrigger) return null

    // Find numeric candidates
    val matches = OTP_REGEX.findAll(fullText)
    for (match in matches) {
      val candidate = match.groupValues[1]
      // Exclude obvious years (e.g., 2024, 2025, 2026) unless accompanied by strong OTP words
      if (candidate.length == 4 && (candidate == "2024" || candidate == "2025" || candidate == "2026")) {
        continue
      }
      return candidate
    }

    return null
  }
}
