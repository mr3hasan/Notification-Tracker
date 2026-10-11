package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationRecord
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: NotificationRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = NotificationRepository(database.notificationDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Notification Tracker", appName)
    val homeTitle = context.getString(R.string.home_title)
    assertEquals("Notification Tracker", homeTitle)
    val totalCount = context.getString(R.string.total_notifications_count)
    assertEquals("0", totalCount)
    val btnLabel = context.getString(R.string.btn_enable_notification_access)
    assertEquals("Enable Notification Access", btnLabel)
    val serviceLabel = context.getString(R.string.notification_service_label)
    assertEquals("Notification Tracker Listener", serviceLabel)

    // Verify NotificationAccessUtils executes safely
    val accessGranted = com.example.util.NotificationAccessUtils.isNotificationAccessGranted(context)
    assertNotNull(accessGranted)
  }

  @Test
  fun `test room persistence and deduplication`() = runBlocking {
    val record1 = NotificationRecord(
      packageName = "com.whatsapp",
      appName = "WhatsApp",
      title = "New Message",
      content = "Hello there",
      postTime = 1000000L,
    )

    // Insert first notification
    val inserted = repository.insertNotification(record1)
    assertTrue(inserted)

    val count = repository.totalCount.first()
    assertEquals(1, count)

    val list = repository.allNotifications.first()
    assertEquals(1, list.size)
    assertEquals("WhatsApp", list[0].appName)
    assertEquals("New Message", list[0].title)

    // Duplicate within 2 seconds should be rejected
    val duplicateRecord = NotificationRecord(
      packageName = "com.whatsapp",
      appName = "WhatsApp",
      title = "New Message",
      content = "Hello there",
      postTime = 1000500L, // 500ms later
    )
    val secondInsert = repository.insertNotification(duplicateRecord)
    assertFalse(secondInsert)

    val countAfterDuplicate = repository.totalCount.first()
    assertEquals(1, countAfterDuplicate)
  }

  @Test
  fun `test app-based grouping and package queries`() = runBlocking {
    val waRecord1 = NotificationRecord(
      packageName = "com.whatsapp",
      appName = "WhatsApp",
      title = "Message 1",
      content = "Hi",
      postTime = 1000L,
    )
    val waRecord2 = NotificationRecord(
      packageName = "com.whatsapp",
      appName = "WhatsApp",
      title = "Message 2",
      content = "How are you?",
      postTime = 20000L,
    )
    val tgRecord = NotificationRecord(
      packageName = "org.telegram.messenger",
      appName = "Telegram",
      title = "Telegram alert",
      content = "Photo received",
      postTime = 30000L,
    )

    repository.insertNotification(waRecord1)
    repository.insertNotification(waRecord2)
    repository.insertNotification(tgRecord)

    val waList = repository.getNotificationsByPackage("com.whatsapp").first()
    assertEquals(2, waList.size)
    assertEquals("Message 2", waList[0].title)

    val tgList = repository.getNotificationsByPackage("org.telegram.messenger").first()
    assertEquals(1, tgList.size)
    assertEquals("Telegram alert", tgList[0].title)

    repository.deleteByPackage("com.whatsapp")
    val waListAfter = repository.getNotificationsByPackage("com.whatsapp").first()
    assertEquals(0, waListAfter.size)

    val remainingTotal = repository.totalCount.first()
    assertEquals(1, remainingTotal)
  }

  @Test
  fun `test search notifications by title content and app name case-insensitively`() = runBlocking {
    val rec1 = NotificationRecord(
      id = 1L,
      packageName = "com.google.android.gm",
      appName = "Gmail",
      title = "Important Security Alert",
      content = "Someone signed in to your account",
      postTime = 10000L,
    )
    val rec2 = NotificationRecord(
      id = 2L,
      packageName = "com.slack",
      appName = "Slack",
      title = "Project Update",
      content = "Meeting rescheduled to 3 PM",
      postTime = 20000L,
    )
    val rec3 = NotificationRecord(
      id = 3L,
      packageName = "org.telegram.messenger",
      appName = "Telegram",
      title = "Alice",
      content = "Check out this document",
      postTime = 30000L,
    )

    val allList = listOf(rec1, rec2, rec3)

    // Match by title (case-insensitive "SECURITY")
    val searchByTitle = com.example.util.NotificationFilterUtils.filter(allList, query = "security")
    assertEquals(1, searchByTitle.size)
    assertEquals("Gmail", searchByTitle[0].appName)

    // Match by content (case-insensitive "rescheduled")
    val searchByContent = com.example.util.NotificationFilterUtils.filter(allList, query = "RESCHEDULED")
    assertEquals(1, searchByContent.size)
    assertEquals("Slack", searchByContent[0].appName)

    // Match by app name (case-insensitive "telegram")
    val searchByAppName = com.example.util.NotificationFilterUtils.filter(allList, query = "telegram")
    assertEquals(1, searchByAppName.size)
    assertEquals("Alice", searchByAppName[0].title)

    // Match by package name
    val searchByPkg = com.example.util.NotificationFilterUtils.filter(allList, query = "google.android")
    assertEquals(1, searchByPkg.size)

    // Empty search query returns all records
    val searchEmpty = com.example.util.NotificationFilterUtils.filter(allList, query = "")
    assertEquals(3, searchEmpty.size)

    // Non-matching query returns empty list
    val searchNoMatch = com.example.util.NotificationFilterUtils.filter(allList, query = "NonExistentWordXYZ")
    assertEquals(0, searchNoMatch.size)
  }

  @Test
  fun `test date range filter boundaries and validity`() {
    val rec1 = NotificationRecord(id = 1L, packageName = "app.a", appName = "A", title = "T1", content = "C1", postTime = 1000L)
    val rec2 = NotificationRecord(id = 2L, packageName = "app.b", appName = "B", title = "T2", content = "C2", postTime = 2000L)
    val rec3 = NotificationRecord(id = 3L, packageName = "app.c", appName = "C", title = "T3", content = "C3", postTime = 3000L)
    val allList = listOf(rec1, rec2, rec3)

    // Filter with both start and end inclusive
    val rangeExact = com.example.util.NotificationFilterUtils.filter(
      records = allList,
      query = "",
      startDateMillis = 1000L,
      endDateMillis = 2000L,
    )
    assertEquals(2, rangeExact.size)
    assertTrue(rangeExact.any { it.id == 1L })
    assertTrue(rangeExact.any { it.id == 2L })

    // Filter with only start date
    val startOnly = com.example.util.NotificationFilterUtils.filter(
      records = allList,
      query = "",
      startDateMillis = 2000L,
      endDateMillis = null,
    )
    assertEquals(2, startOnly.size)
    assertTrue(startOnly.none { it.id == 1L })

    // Filter with only end date
    val endOnly = com.example.util.NotificationFilterUtils.filter(
      records = allList,
      query = "",
      startDateMillis = null,
      endDateMillis = 1500L,
    )
    assertEquals(1, endOnly.size)
    assertEquals(1L, endOnly[0].id)

    // Date range validity checks
    assertTrue(com.example.util.NotificationFilterUtils.isDateRangeValid(1000L, 2000L))
    assertTrue(com.example.util.NotificationFilterUtils.isDateRangeValid(1000L, 1000L))
    assertFalse(com.example.util.NotificationFilterUtils.isDateRangeValid(2000L, 1000L))
  }

  @Test
  fun `test combined search and date filtering`() {
    val rec1 = NotificationRecord(id = 1L, packageName = "app.mail", appName = "Mail", title = "Receipt", content = "Order #123", postTime = 1000L)
    val rec2 = NotificationRecord(id = 2L, packageName = "app.chat", appName = "Chat", title = "Receipt discussion", content = "Sent receipt", postTime = 2000L)
    val rec3 = NotificationRecord(id = 3L, packageName = "app.mail", appName = "Mail", title = "Receipt", content = "Order #456", postTime = 5000L)
    val allList = listOf(rec1, rec2, rec3)

    // Search "Receipt" within time range 1500L..6000L
    val combined = com.example.util.NotificationFilterUtils.filter(
      records = allList,
      query = "Receipt",
      startDateMillis = 1500L,
      endDateMillis = 6000L,
    )
    assertEquals(2, combined.size)
    assertTrue(combined.any { it.id == 2L })
    assertTrue(combined.any { it.id == 3L })
    assertFalse(combined.any { it.id == 1L })
  }

  @Test
  fun `test individual notification deletion and clear all in repository`() = runBlocking {
    val rec1 = NotificationRecord(packageName = "app.a", appName = "A", title = "T1", content = "C1", postTime = 1000L)
    val rec2 = NotificationRecord(packageName = "app.b", appName = "B", title = "T2", content = "C2", postTime = 2000L)
    val rec3 = NotificationRecord(packageName = "app.c", appName = "C", title = "T3", content = "C3", postTime = 3000L)

    repository.insertNotification(rec1)
    repository.insertNotification(rec2)
    repository.insertNotification(rec3)

    val currentList = repository.allNotifications.first()
    assertEquals(3, currentList.size)
    assertEquals(3, repository.totalCount.first())

    // Delete single notification by ID
    val itemToDelete = currentList[0]
    repository.deleteById(itemToDelete.id)

    val listAfterSingleDelete = repository.allNotifications.first()
    assertEquals(2, listAfterSingleDelete.size)
    assertEquals(2, repository.totalCount.first())
    assertTrue(listAfterSingleDelete.none { it.id == itemToDelete.id })

    // Clear all records
    repository.clearAll()
    val listAfterClearAll = repository.allNotifications.first()
    assertEquals(0, listAfterClearAll.size)
    assertEquals(0, repository.totalCount.first())
  }

  @Test
  fun `test settings persistence across repository instances`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val settingsRepo1 = com.example.data.repository.SettingsRepository(context)
    // Initially not paused, empty exclusions, FOREVER retention
    assertFalse(settingsRepo1.isCollectionPaused())
    assertTrue(settingsRepo1.getExcludedPackages().isEmpty())
    assertEquals(com.example.data.repository.RetentionPeriod.FOREVER, settingsRepo1.getRetentionPeriod())

    // Modify settings
    settingsRepo1.setCollectionPaused(true)
    settingsRepo1.addExcludedApp("com.whatsapp")
    settingsRepo1.addExcludedApp("com.slack")
    settingsRepo1.setRetentionPeriod(com.example.data.repository.RetentionPeriod.DAYS_30)

    // Create a new instance representing app restart
    val settingsRepo2 = com.example.data.repository.SettingsRepository(context)
    assertTrue(settingsRepo2.isCollectionPaused())
    assertTrue(settingsRepo2.isAppExcluded("com.whatsapp"))
    assertTrue(settingsRepo2.isAppExcluded("com.slack"))
    assertFalse(settingsRepo2.isAppExcluded("com.telegram"))
    assertEquals(2, settingsRepo2.getExcludedPackages().size)
    assertEquals(com.example.data.repository.RetentionPeriod.DAYS_30, settingsRepo2.getRetentionPeriod())

    // Resume collection and toggle exclusions
    settingsRepo2.setCollectionPaused(false)
    settingsRepo2.toggleExcludedApp("com.whatsapp") // removes whatsapp
    settingsRepo2.toggleExcludedApp("com.telegram") // adds telegram

    val settingsRepo3 = com.example.data.repository.SettingsRepository(context)
    assertFalse(settingsRepo3.isCollectionPaused())
    assertFalse(settingsRepo3.isAppExcluded("com.whatsapp"))
    assertTrue(settingsRepo3.isAppExcluded("com.slack"))
    assertTrue(settingsRepo3.isAppExcluded("com.telegram"))
  }

  @Test
  fun `test retention expiration cutoff and purging`() = runBlocking {
    val now = 1000000000000L // arbitrary current time
    val dayMillis = 24L * 60L * 60L * 1000L

    // Verify cutoff calculation
    val cutoff7 = com.example.data.repository.SettingsRepository.calculateCutoffTimestamp(
      com.example.data.repository.RetentionPeriod.DAYS_7,
      now,
    )
    assertEquals(now - (7 * dayMillis), cutoff7)

    val cutoffForever = com.example.data.repository.SettingsRepository.calculateCutoffTimestamp(
      com.example.data.repository.RetentionPeriod.FOREVER,
      now,
    )
    assertEquals(null, cutoffForever)

    // Insert records with varying postTimes
    val recentRecord = NotificationRecord(
      packageName = "app.recent",
      appName = "Recent App",
      title = "New alert",
      content = "Today",
      postTime = now - (2 * dayMillis), // 2 days old
    )
    val oldRecord = NotificationRecord(
      packageName = "app.old",
      appName = "Old App",
      title = "Old alert",
      content = "10 days ago",
      postTime = now - (10 * dayMillis), // 10 days old
    )
    val ancientRecord = NotificationRecord(
      packageName = "app.ancient",
      appName = "Ancient App",
      title = "Ancient alert",
      content = "40 days ago",
      postTime = now - (40 * dayMillis), // 40 days old
    )

    repository.insertNotification(recentRecord)
    repository.insertNotification(oldRecord)
    repository.insertNotification(ancientRecord)
    assertEquals(3, repository.totalCount.first())

    // Retention FOREVER purges nothing
    val deletedForever = repository.purgeExpired(com.example.data.repository.RetentionPeriod.FOREVER, now)
    assertEquals(0, deletedForever)
    assertEquals(3, repository.totalCount.first())

    // Purge with 30-day retention -> deletes 40-day old record
    val deleted30 = repository.purgeExpired(com.example.data.repository.RetentionPeriod.DAYS_30, now)
    assertEquals(1, deleted30)
    assertEquals(2, repository.totalCount.first())

    // Purge with 7-day retention -> deletes 10-day old record
    val deleted7 = repository.purgeExpired(com.example.data.repository.RetentionPeriod.DAYS_7, now)
    assertEquals(1, deleted7)
    val remaining = repository.allNotifications.first()
    assertEquals(1, remaining.size)
    assertEquals("Recent App", remaining[0].appName)
  }

  @Test
  fun `test otp detector extracts verification codes accurately`() {
    val otp1 = com.example.util.OtpDetector.extractOtp(
      title = "Bank Verification",
      content = "Your OTP is 482910. Do not share this with anyone.",
    )
    assertEquals("482910", otp1)

    val otp2 = com.example.util.OtpDetector.extractOtp(
      title = "WhatsApp",
      content = "Your WhatsApp code is 839-102 or tap link to verify.",
    )
    assertNotNull(otp2)

    val nonOtp = com.example.util.OtpDetector.extractOtp(
      title = "Casual Chat",
      content = "Hey, are you free tomorrow at 5pm? Call me at 12345.",
    )
    assertEquals(null, nonOtp)
  }

  @Test
  fun `test anti delete detection marks recalled notification`() = runBlocking {
    val record = NotificationRecord(
      packageName = "com.whatsapp",
      appName = "WhatsApp",
      title = "Alex",
      content = "Secret meeting details",
      postTime = System.currentTimeMillis(),
      notificationKey = "key_wa_12345",
    )
    repository.insertNotification(record)

    val initialDeleted = repository.deletedCount.first()
    assertEquals(0, initialDeleted)

    // Simulate notification removal / recall event
    val updatedCount = repository.markAsDeletedByKey("key_wa_12345")
    assertEquals(1, updatedCount)

    val deletedAfter = repository.deletedCount.first()
    assertEquals(1, deletedAfter)

    val savedList = repository.allNotifications.first()
    val saved = savedList.first { it.notificationKey == "key_wa_12345" }
    assertTrue(saved.isDeleted)
    assertNotNull(saved.removedTime)
  }

  @Test
  fun `test star toggling and protection from deletion`() = runBlocking {
    val record = NotificationRecord(
      packageName = "com.important",
      appName = "Important App",
      title = "Special Notice",
      content = "Do not lose this",
      postTime = 1000L,
    )
    repository.insertNotification(record)
    val inserted = repository.allNotifications.first().first { it.packageName == "com.important" }
    assertFalse(inserted.isStarred)

    // Toggle star on
    repository.toggleStar(inserted.id)
    val starred = repository.allNotifications.first().first { it.packageName == "com.important" }
    assertTrue(starred.isStarred)

    // Verify retention does not delete starred record even if ancient
    val deleted = repository.purgeExpired(com.example.data.repository.RetentionPeriod.DAYS_7, System.currentTimeMillis())
    assertEquals(0, deleted)
    val stillPresent = repository.allNotifications.first().any { it.id == inserted.id }
    assertTrue(stillPresent)
  }
}
