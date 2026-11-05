# LifeVault

LifeVault is a native Android personal organiser for documents, reminders, expenses, subscriptions, calendar events and home maintenance.

## Active Android implementation
- Kotlin + Jetpack Compose + Material 3
- Room database for on-device records and offline-first use
- Native Android navigation and edge-to-edge layout
- Android SDK 36, minimum Android 8.0 (API 26)
- Package: com.lifevault.app

The native Android rebuild is being developed on `feature/lifevault-android-native`. The earlier Next.js/Supabase frontend remains in the repository as a legacy prototype; it is not the Android application and is not loaded through a WebView.

## Build an APK
The GitHub Actions workflow `.github/workflows/android-apk.yml` builds a debug APK on pushes to the native branch. Open the workflow run and download the `LifeVault-debug-apk` artifact. A local build requires JDK 17, Android SDK 36 and Gradle 8.13:

```bash
gradle --no-daemon :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Current native milestone
- Home dashboard with local summaries
- Room-backed reminders: create and complete
- Room-backed income and expense records
- Native tabs for Home, Vault, Reminders, Money and Home care

The Vault and Home care tabs are currently screen placeholders. Document picking, scheduled notifications, calendar, budgets, subscriptions, appliance records, settings and backup are planned follow-up milestones.

## Backend
The Android app does not currently require Supabase credentials. Data added in this milestone stays on the device. Supabase sync/authentication will be added only when backend integration is requested.

## Legacy web prototype
The previous Next.js implementation and its Supabase schema/migrations remain available in the repository for reference. They are separate from the native Android build.
