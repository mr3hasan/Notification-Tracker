# Notification Tracker

A native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** for tracking, organizing, and monitoring notifications with complete privacy and zero battery overhead.

---

## 📱 Project Overview

- **Application ID:** `com.hasan.notificationtracker`
- **Minimum SDK:** Android 10 (API 29)
- **Target SDK:** Android 16 (API 36)
- **UI Toolkit:** Jetpack Compose with Material 3 Design
- **Architecture:** MVVM + Clean separation of UI destinations
- **Storage & Privacy:** 100% on-device (No accounts, external APIs, or cloud servers required)

---

## 🚀 Initial Version Features

1. **Home Screen ("Notification Tracker"):**
   - Live metric cards for Total Notifications, Today's Alerts, and Monitored Apps (initialized at 0).
   - Prominent notification access status banner explaining that the listener service will be hooked up in the next development step.
   - Clean, friendly empty-state illustration informing the user that notification history will populate automatically once collection starts.
   - Architecture highlights explaining zero cloud transmission and event-driven battery efficiency.

2. **Bottom Navigation (4 Tabs):**
   - **Home:** Overview counters, access notice banner, and alert history feed.
   - **Categories:** Auto-classification buckets (Messages, Social, Finance, Shopping, System).
   - **Search:** Instant search bar with filter chips (All Alerts, Today, Priority, Messages).
   - **Settings:** Technical specifications, local-only storage guarantee, and build info.

3. **Custom Adaptive Icon:**
   - Custom modern notification bell with an activity pulse monitor badge.
   - Pinned solid background `#1E293B` and matching round/square density mipmaps.

4. **CI/CD Ready:**
   - Pre-configured Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`).
   - GitHub Actions workflow (`.github/workflows/build.yml`) that builds the debug APK on every commit and uploads it as an artifact.

---

## 🛠️ GitHub Sync & Export Guide

To sync or push this project to your GitHub repository named `Notification-Tracker`:

### Option A: Direct Push via Git (Recommended)

1. Open your terminal or Git bash in your local workspace.
2. If Git is not initialized yet, run:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: Notification Tracker project scaffold"
   ```
3. Set your default branch to `main`:
   ```bash
   git branch -M main
   ```
4. Add your GitHub repository remote:
   ```bash
   git remote add origin https://github.com/<YOUR_GITHUB_USERNAME>/Notification-Tracker.git
   ```
5. Push to GitHub:
   ```bash
   git push -u origin main
   ```

### Option B: Export as ZIP from Google AI Studio
1. In Google AI Studio, open the **Project Settings** / **Export** menu in the top bar.
2. Select **Export as ZIP**.
3. Extract the ZIP archive onto your computer.
4. Push the extracted folder to `https://github.com/<YOUR_GITHUB_USERNAME>/Notification-Tracker`.

---

## 🤖 GitHub Actions Automated APK Builds

Every time you push changes to your GitHub repository:
1. GitHub Actions runs `.github/workflows/build.yml`.
2. It sets up Java 17 and runs `./gradlew assembleDebug`.
3. The generated `app-debug.apk` is saved under **Actions > Artifacts** in your GitHub repository, ready for download and installation on any Android phone running Android 10 or newer.

---

## 🗺️ Next Steps (Future Updates)
- Implement `NotificationListenerService` in Android.
- Request user permission via system `ACTION_NOTIFICATION_LISTENER_SETTINGS`.
- Local Room database persistence for notification history.
