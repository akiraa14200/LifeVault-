# LifeVault native Android app

The native Android implementation lives in the app module and is built with Kotlin, Jetpack Compose, Material 3 and Room. It is independent of the former Next.js frontend.

## Build
- JDK 17
- Android SDK 36
- Gradle 8.13
- Run: `gradle :app:assembleDebug`
- APK output: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions builds a debug APK on pushes to `feature/lifevault-android-native` and on manual workflow dispatch. Download the APK from the workflow Artifacts section.

## Current native functionality
- Native Compose navigation across Home, Vault, Reminders, Money and Home care.
- Offline Room persistence for reminders, transactions, budgets and document metadata.
- Calendar date and clock-time selection for each reminder.
- Reminder alerts scheduled 24 hours before, then hourly during the final five hours, plus an alert at the due time. Past alert stages are skipped when a reminder is created less than 24 hours or five hours ahead.
- Daily, weekly and 30-day recurring reminders advance to the next due date after the due-time alert.
- Pending reminder schedules are rebuilt when the app opens; completing a reminder cancels its pending alerts.
- Notification preferences and WorkManager scheduling. Android battery controls can still delay background work, and notification permission must be allowed.
- Manual income/expense records, categories and monthly budgets with a monthly income/expense/net overview, top spending category, previous-month comparison and remaining/over-budget summaries.
- Vault import through Android's document picker, user-entered folders, moving files between folders, tags, in-vault search, filtering and name/date sorting.
- Local thumbnail previews for image files and the first page of PDFs; other file types use a type icon and open through a compatible Android app.
- Local JSON backup/restore. Document bytes are not embedded; the backup contains their metadata and URI references.
- Global search across reminders, money records and vault metadata.
- CSV export for transactions and vault index; PDF money summary export.
- Optional biometric lock at app launch using AndroidX BiometricPrompt.
- Dashboard includes upcoming-reminder and vault-file counts, monthly income/expense totals, spending summary and quick navigation actions. Dashboard cards can be shown or hidden in Settings.
- Android home-screen widget showing the next open reminder and recorded expenses.

## Privacy and storage
- The native app does not connect to Supabase or any remote backend.
- Data is stored in the local Room database and app preferences.
- Export destinations are selected by the user through Android's Storage Access Framework.
- Vault imports retain references to the original document provider; exporting the vault index does not copy the original files.
- The optional lock uses AndroidX BiometricPrompt and the device's enrolled biometric or screen-lock credential.

## Notes
- The PDF report is a compact summary and lists up to 38 recent entries. CSV export contains the full transaction list.
- Home-screen widget refreshes after in-app reminder and transaction changes and also receives Android's periodic widget refresh.
- Keep independent copies of critical documents and backups.
- The Android app is developed only on `feature/lifevault-android-native`; do not merge the legacy web PR or modify the production branch as part of native work.
