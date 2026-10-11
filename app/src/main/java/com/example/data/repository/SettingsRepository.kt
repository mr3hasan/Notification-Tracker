package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Retention period options for notification history.
 */
enum class RetentionPeriod(val days: Int, val label: String) {
  DAYS_7(7, "7 days"),
  DAYS_30(30, "30 days"),
  DAYS_90(90, "90 days"),
  FOREVER(-1, "Keep forever");

  companion object {
    fun fromDays(days: Int): RetentionPeriod =
      entries.find { it.days == days } ?: FOREVER
  }
}

/**
 * Repository to persist and expose user configuration for notification collection,
 * app exclusions, and history retention using local SharedPreferences.
 */
class SettingsRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _isCollectionPaused = MutableStateFlow(
    prefs.getBoolean(KEY_COLLECTION_PAUSED, false)
  )
  val isCollectionPaused: StateFlow<Boolean> = _isCollectionPaused.asStateFlow()

  private val _excludedPackages = MutableStateFlow(
    prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet())?.toSet() ?: emptySet()
  )
  val excludedPackages: StateFlow<Set<String>> = _excludedPackages.asStateFlow()

  private val _retentionPeriod = MutableStateFlow(
    RetentionPeriod.fromDays(prefs.getInt(KEY_RETENTION_DAYS, RetentionPeriod.FOREVER.days))
  )
  val retentionPeriod: StateFlow<RetentionPeriod> = _retentionPeriod.asStateFlow()

  /**
   * Synchronous check used by NotificationTrackerListenerService before saving an alert.
   */
  fun isCollectionPaused(): Boolean = prefs.getBoolean(KEY_COLLECTION_PAUSED, false)

  fun setCollectionPaused(paused: Boolean) {
    prefs.edit().putBoolean(KEY_COLLECTION_PAUSED, paused).apply()
    _isCollectionPaused.value = paused
  }

  /**
   * Checks whether an app by package name is in the excluded list.
   */
  fun isAppExcluded(packageName: String): Boolean {
    val set = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
    return set.contains(packageName)
  }

  fun getExcludedPackages(): Set<String> {
    return prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet())?.toSet() ?: emptySet()
  }

  fun addExcludedApp(packageName: String) {
    val updated = (getExcludedPackages() + packageName).toSet()
    prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, updated).apply()
    _excludedPackages.value = updated
  }

  fun removeExcludedApp(packageName: String) {
    val updated = (getExcludedPackages() - packageName).toSet()
    prefs.edit().putStringSet(KEY_EXCLUDED_PACKAGES, updated).apply()
    _excludedPackages.value = updated
  }

  fun toggleExcludedApp(packageName: String) {
    if (isAppExcluded(packageName)) {
      removeExcludedApp(packageName)
    } else {
      addExcludedApp(packageName)
    }
  }

  fun getRetentionPeriod(): RetentionPeriod {
    val days = prefs.getInt(KEY_RETENTION_DAYS, RetentionPeriod.FOREVER.days)
    return RetentionPeriod.fromDays(days)
  }

  fun setRetentionPeriod(period: RetentionPeriod) {
    prefs.edit().putInt(KEY_RETENTION_DAYS, period.days).apply()
    _retentionPeriod.value = period
  }

  companion object {
    const val PREFS_NAME = "notification_tracker_settings"
    const val KEY_COLLECTION_PAUSED = "key_collection_paused"
    const val KEY_EXCLUDED_PACKAGES = "key_excluded_packages"
    const val KEY_RETENTION_DAYS = "key_retention_days"

    @Volatile
    private var INSTANCE: SettingsRepository? = null

    fun getInstance(context: Context): SettingsRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
      }
    }

    /**
     * Calculates the cutoff timestamp before which records should be pruned.
     * Returns null if retention is FOREVER.
     */
    fun calculateCutoffTimestamp(
      period: RetentionPeriod,
      currentTime: Long = System.currentTimeMillis(),
    ): Long? {
      if (period == RetentionPeriod.FOREVER) return null
      val retentionMillis = period.days.toLong() * 24L * 60L * 60L * 1000L
      return currentTime - retentionMillis
    }
  }
}
