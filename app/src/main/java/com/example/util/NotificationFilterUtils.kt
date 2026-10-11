package com.example.util

import com.example.data.local.NotificationRecord

object NotificationFilterUtils {

  /**
   * Filters a list of NotificationRecords by search query and optional date boundaries.
   *
   * @param records The original notification records list
   * @param query The text search query (matches title, content, app name, or package name case-insensitively)
   * @param startDateMillis Optional inclusive start timestamp in milliseconds
   * @param endDateMillis Optional inclusive end timestamp in milliseconds
   */
  fun filter(
    records: List<NotificationRecord>,
    query: String = "",
    startDateMillis: Long? = null,
    endDateMillis: Long? = null,
  ): List<NotificationRecord> {
    val q = query.trim()
    val isDateRangeInvalid = startDateMillis != null && endDateMillis != null && endDateMillis < startDateMillis

    return records.filter { item ->
      val matchesSearch = if (q.isBlank()) {
        true
      } else {
        item.title.contains(q, ignoreCase = true) ||
          item.content.contains(q, ignoreCase = true) ||
          item.appName.contains(q, ignoreCase = true) ||
          item.packageName.contains(q, ignoreCase = true)
      }

      val matchesDate = when {
        isDateRangeInvalid -> true // Avoid blanking results if date range selection is invalid
        startDateMillis != null && endDateMillis != null -> item.postTime in startDateMillis..endDateMillis
        startDateMillis != null -> item.postTime >= startDateMillis
        endDateMillis != null -> item.postTime <= endDateMillis
        else -> true
      }

      matchesSearch && matchesDate
    }
  }

  /**
   * Validates if start and end date range is valid.
   */
  fun isDateRangeValid(startDateMillis: Long?, endDateMillis: Long?): Boolean {
    return if (startDateMillis != null && endDateMillis != null) {
      endDateMillis >= startDateMillis
    } else {
      true
    }
  }
}
