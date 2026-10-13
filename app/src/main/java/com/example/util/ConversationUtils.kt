package com.example.util

/**
 * Utility to extract clean conversation/chat folder names from notification titles.
 * Handles patterns from WhatsApp, Messenger, Telegram, Gmail, etc.
 *
 * Examples:
 * - "Digital Sample Group : Rafin Bhai Designer" -> "Digital Sample Group"
 * - "Digital Sample Group: Rafin Bhai Designer" -> "Digital Sample Group"
 * - "Arif Mama" -> "Arif Mama"
 * - "Office Chat (3 messages)" -> "Office Chat"
 * - "5 new messages" -> fallback / previous
 */
object ConversationUtils {

  fun extractConversationName(title: String, appName: String): String {
    val trimmed = title.trim()
    if (trimmed.isBlank()) {
      return appName.ifBlank { "General" }
    }

    // Strip unread counters like " (5 messages)" or " [3]" or " (2)"
    var cleaned = trimmed
      .replace(Regex("""\s*\(\d+\s*(new\s*)?messages?\)\s*""", RegexOption.IGNORE_CASE), "")
      .replace(Regex("""\s*\(\d+\)\s*$"""), "")
      .replace(Regex("""\s*\[\d+\]\s*$"""), "")
      .trim()

    // If the title contains group separator like "Digital Sample Group : Rafin Bhai Designer"
    // or "Project Team: John Doe"
    if (cleaned.contains(" : ")) {
      val parts = cleaned.split(" : ")
      if (parts[0].isNotBlank()) {
        cleaned = parts[0].trim()
      }
    } else if (cleaned.contains(": ") && !cleaned.startsWith("http")) {
      val firstColon = cleaned.indexOf(": ")
      // Only split if before colon is meaningful (e.g., group title)
      if (firstColon > 0) {
        val candidateGroup = cleaned.substring(0, firstColon).trim()
        // If candidateGroup has letters and not just "Re" or "Fwd"
        if (candidateGroup.length > 2 && !candidateGroup.equals("Re", ignoreCase = true) && !candidateGroup.equals("Fwd", ignoreCase = true)) {
          cleaned = candidateGroup
        }
      }
    }

    return cleaned.ifBlank { appName.ifBlank { "General" } }
  }
}
