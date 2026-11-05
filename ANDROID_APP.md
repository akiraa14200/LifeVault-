# LifeVault native Android app

The native Android implementation lives in the app module and is built with Kotlin, Jetpack Compose, Material 3 and Room. It is independent of the former Next.js frontend.

## Build
- JDK 17
- Android SDK 36
- Gradle 8.13
- Run: gradle :app:assembleDebug
- APK output: app/build/outputs/apk/debug/app-debug.apk

GitHub Actions builds a debug APK on pushes to feature/lifevault-android-native and on manual workflow dispatch. Download the APK from the workflow Artifacts section.

## Current native functionality
- Native Compose navigation across Home, Vault, Reminders, Money and Home care.
- Local Room persistence for reminders and income/expense records.
- Add and complete reminders.
- Add income and expense records.
- Dashboard summary backed by local database flows.

## Next milestones
1. Android document picker, metadata storage and secure local document access.
2. Reminder date/time selection, notification channels and WorkManager scheduling.
3. Calendar, budgets, subscriptions and home maintenance records.
4. Settings, backup/export and accessibility polish.
5. Optional Supabase sync and authentication, only after backend integration is requested.

No Supabase credentials are required by the Android app. Data remains on the device in this milestone.
