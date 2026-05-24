# EXECUTION LOG: Sprint 13 Setup Wizard + OEM Battery Guidance + Self-Test

## Sub-sprint 1: Pure Setup/Self-Test Status Model + Readiness Logic
Status: Completed
Started: 2026-05-24
Completed: 2026-05-24

### Changes made
- `tasks/sprint-13-setup-wizard-selftest/PLAN.md`: created the Human-approved Sprint 13 plan artifact.
- `app/src/main/java/com/followupnadlan/setup/SetupStatus.kt`: added pure readiness status enums and result models.
- `app/src/main/java/com/followupnadlan/setup/SetupReadinessLogic.kt`: added pure readiness evaluation from permission/status inputs.
- `app/src/test/java/com/followupnadlan/setup/SetupReadinessLogicTest.kt`: added JVM unit coverage for required, optional, and unknown readiness combinations.

### Validation run
- Command: `rg -n "^import android|^import androidx" app/src/main/java/com/followupnadlan/setup app/src/test/java/com/followupnadlan/setup`
- Result: PASS
- Evidence: no matches; Sub-sprint 1 setup logic has no Android/AndroidX imports.

- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: command exited 0.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 4s`; 37 actionable tasks: 4 executed, 33 up-to-date.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence:

```txt
```

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence:

```txt
```

- Command: `rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|backend|server|Firestore|Supabase|retrofit|okhttp|version\.json|activation|license|trial|Billing|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS
- Evidence: no matches; command exited 1 because ripgrep found no forbidden-scope hits.

- Command: `Select-String ... -Pattern <mojibake sentinel pattern>`
- Result: PASS
- Evidence: no matches in current command output.

### Manual QA
- Check: Sprint 8-12 contract regression.
- Result: NOT RUN
- Notes: Sub-sprint 1 is pure logic only; no app UI/service/notification/snooze/WhatsApp code changed. Manual real-phone regression remains required in later gates.

### Deviations from plan
- None for Sub-sprint 1 implementation scope.

### Blockers
- None.

### Next recommended action
- Stop at Gate 1. Continue to Sub-sprint 2 only after human approval to proceed.

## Sub-sprint 2: OEM Detection + Guidance + Safe Battery Intent
Status: Completed
Started: 2026-05-24
Completed: 2026-05-24

### Changes made
- `app/src/main/java/com/followupnadlan/setup/OemGuidance.kt`: added pure manufacturer/brand guidance mapping for Samsung, Xiaomi, Redmi, Realme, OnePlus, and generic fallback.
- `app/src/main/java/com/followupnadlan/setup/BatteryOptimizationIntents.kt`: added Android-dependent app-settings intent helper using `ACTION_APPLICATION_DETAILS_SETTINGS`, package URI, resolvability check, and exception-safe `null` fallback.
- `app/src/test/java/com/followupnadlan/setup/OemGuidanceTest.kt`: added pure JVM tests for OEM mapping and Hebrew guidance presence/distinctness.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 6s`; 51 actionable tasks: 14 executed, 37 up-to-date.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 1s`; 37 actionable tasks: 3 executed, 34 up-to-date.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence:

```txt
```

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence:

```txt
```

- Command: `rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|backend|server|Firestore|Supabase|retrofit|okhttp|version\.json|activation|license|trial|Billing|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS
- Evidence: no matches; command exited 1 because ripgrep found no forbidden-scope hits.

- Command: `Select-String ... -Pattern <mojibake sentinel pattern>`
- Result: PASS
- Evidence: no matches in current command output.

- Command: `rg -n "^import android|^import androidx" app/src/main/java/com/followupnadlan/setup/OemGuidance.kt`
- Result: PASS
- Evidence: no matches; `OemGuidance.kt` has no Android/AndroidX imports.

- Command: `rg -n "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS|REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" app/src/main/java/com/followupnadlan/setup/BatteryOptimizationIntents.kt`
- Result: PASS
- Evidence: no matches; helper does not use the permission-requiring request action.

### Manual QA
- Check: Battery settings helper behavior on a real device.
- Result: NOT RUN
- Notes: Source-level check confirms the helper resolves `ACTION_APPLICATION_DETAILS_SETTINGS` or returns `null` inside a try/catch. Real-device deep-link behavior remains for later UI/manual gates.

### Deviations from plan
- None for Sub-sprint 2 implementation scope.

### Blockers
- None.

### Next recommended action
- Stop at Gate 2. Continue to Sub-sprint 3 only after human approval to proceed.

## Sub-sprint 3: Setup Wizard UI
Status: Completed
Started: 2026-05-24
Completed: 2026-05-24

### Changes made
- `app/src/main/java/com/followupnadlan/setup/SetupPreferences.kt`: added SharedPreferences wrapper with `setup_wizard_completed`.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: added `SetupWizard` and placeholder `SelfTest` screen states, first-run wizard routing, re-run `×”×’×“×¨×” ×•×‘×“×™×§×”` entry button, permission step, agent-profile step using the existing `MyDetailsStore`, OEM/battery guidance step, and self-test handoff placeholder.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: first run failed on a local syntax error from the Sub-sprint 3 edit at `MainActivity.kt:131`; fixed by removing the extra brace. Rerun passed with `BUILD SUCCESSFUL in 11s`; 51 actionable tasks: 14 executed, 37 up-to-date.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 3s`; 37 actionable tasks: 3 executed, 34 up-to-date.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence:

```txt
```

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence:

```txt
```

- Command: `rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|backend|server|Firestore|Supabase|retrofit|okhttp|version\.json|activation|license|trial|Billing|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS
- Evidence: no matches; command exited 1 because ripgrep found no forbidden-scope hits.

- Command: `Select-String ... -Pattern <mojibake sentinel pattern>`
- Result: PASS
- Evidence: no matches in current command output.

- Command: `rg -n "Room|DataStore|AppDatabase|SharedPreferences|getSharedPreferences" app/src/main/java/com/followupnadlan/setup/SetupPreferences.kt`
- Result: PASS
- Evidence: only `getSharedPreferences` matched; no Room/DataStore/AppDatabase usage.

- Command: `rg -n "SetupAgentProfileStep|store: MyDetailsStore|MyDetailsStore\(|store\.save|myDetailsStore = remember" app/src/main/java/com/followupnadlan/MainActivity.kt`
- Result: PASS
- Evidence: top-level app creates one `MyDetailsStore` with `remember`; wizard profile step receives `store: MyDetailsStore` and saves through `store.save`; no second store is created by the wizard step.

### Manual QA
- Check: Wizard reachable first-run and from Settings/top entry; self-test placeholder renders.
- Result: NOT RUN
- Notes: Build-level validation passed. Real-device/manual UI smoke remains required by Gate 3 before human approval if the human wants device evidence now.

### Deviations from plan
- None for Sub-sprint 3 scope. The self-test screen is intentionally a placeholder for Sub-sprint 4.

### Blockers
- None.

### Next recommended action
- Stop at Gate 3. Continue to Sub-sprint 4 only after human approval to proceed.

## Sub-sprint 4: Real Self-Test Screen
Status: Completed
Started: 2026-05-24
Completed: 2026-05-24

### Changes made
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`: added read-only on-device readiness checker for notification permission, notification channel, phone-state permission, optional call-log/contacts permissions, detection-enabled preference, and battery optimization state with exception-safe UNKNOWN fallback.
- `app/src/main/java/com/followupnadlan/setup/SetupPreferences.kt`: added `isSelfTestPassed()` and `setSelfTestPassed(Boolean)` backed by local SharedPreferences key `self_test_passed`.
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`: exposed existing `CHANNEL_ID` so self-test reads the notification channel without hardcoding the id.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: replaced the self-test placeholder route with a real Hebrew RTL self-test screen, check rows, actionable settings/permission buttons, manual test-call guidance, user-observation buttons, and re-check refresh.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 1s`; 51 actionable tasks up-to-date on final rerun.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 8s`; 37 actionable tasks: 5 executed, 32 up-to-date.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence:

```txt
```

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence:

```txt
```

- Command: `rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|backend|server|Firestore|Supabase|retrofit|okhttp|version\.json|activation|license|trial|Billing|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS
- Evidence: no matches; command exited 1 because ripgrep found no forbidden-scope hits.

- Command: `Select-String ... -Pattern <mojibake sentinel pattern>`
- Result: PASS
- Evidence: no matches in current command output.

- Command: `rg -n "set[A-Z]|putBoolean|edit\(|apply\(|startService|startForegroundService|stopService|setEnabled|showFollowUpNotification|notify\(|launch\(|startActivity|CALL_DETECTED|FollowUpLog" app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- Result: PASS
- Evidence: no matches; `SelfTestChecker` reads only and does not write preferences, start/stop services, launch activities, send notifications, or log actions.

- Command: `rg -n "CHANNEL_ID|follow_up_cards|getNotificationChannel" app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- Result: PASS
- Evidence: `SelfTestChecker.kt` calls `getNotificationChannel(FollowUpNotificationHelper.CHANNEL_ID)`; `follow_up_cards` appears only in the helper constant declaration.

- Command: `rg -n "setupPreferences\.setSelfTestPassed|followUpLogStore\.record|CALL_DETECTED" app/src/main/java/com/followupnadlan/MainActivity.kt`
- Result: PASS
- Evidence: self-test observation buttons call only `setupPreferences.setSelfTestPassed(true/false)`; no `CALL_DETECTED` match and no `followUpLogStore.record` in the self-test path.

- Command: source review for Sprint 8-12 contracts using `rg -n "openWhatsApp|openShareSheet|copyMessageToClipboard|PostCallCards|snooze|Snooze|showFollowUpNotification|ACTION_OPEN_FOLLOW_UP|ManualWhatsAppScreen|PostCallScreen" ...`
- Result: PASS
- Evidence: existing post-call notification handoff, four-card `PostCallCards` flow, manual composer, snooze controls, WhatsApp open, share, and copy functions remain present and were not refactored by Sub-sprint 4.

### Manual QA
- Check: Real-phone self-test denied-vs-granted behavior and manual test-call observation.
- Result: NOT RUN
- Notes: Gate 4 requested source/build validation only for real-phone regression; actual device smoke remains unclaimed.

### Deviations from plan
- `FollowUpNotificationHelper.kt` was changed narrowly to expose the existing channel id constant. This avoids hardcoding the channel string in `SelfTestChecker` and preserves notification behavior.

### Blockers
- None.

### Next recommended action
- Stop at Gate 4. Wait for human Gate 4 approval before Sub-sprint 5.

## Sub-sprint 5: Evidence Docs + Review
Status: Completed
Started: 2026-05-24
Completed: 2026-05-24

### Files changed across full sprint
- `F:\followup\app\src\main\java\com\followupnadlan\MainActivity.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\notifications\FollowUpNotificationHelper.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\setup\BatteryOptimizationIntents.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\setup\OemGuidance.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\setup\SelfTestChecker.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\setup\SetupPreferences.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\setup\SetupReadinessLogic.kt`
- `F:\followup\app\src\main\java\com\followupnadlan\setup\SetupStatus.kt`
- `F:\followup\app\src\test\java\com\followupnadlan\setup\OemGuidanceTest.kt`
- `F:\followup\app\src\test\java\com\followupnadlan\setup\SetupReadinessLogicTest.kt`
- `F:\followup\tasks\sprint-13-setup-wizard-selftest\PLAN.md`
- `F:\followup\tasks\sprint-13-setup-wizard-selftest\EXECUTION_LOG.md`
- `F:\followup\tasks\sprint-13-setup-wizard-selftest\MANUAL_SMOKE_TEST.md`
- `F:\followup\tasks\sprint-13-setup-wizard-selftest\REVIEW.md`

### Changes made
- `tasks/sprint-13-setup-wizard-selftest/EXECUTION_LOG.md`: added final Sub-sprint 5 closure evidence.
- `tasks/sprint-13-setup-wizard-selftest/MANUAL_SMOKE_TEST.md`: added truthful real-phone checklist with status `NOT RUN`.
- `tasks/sprint-13-setup-wizard-selftest/REVIEW.md`: added reviewer-format Sprint 13 review with `PASS WITH NOTES` because source/build/unit evidence is clean but real-phone smoke is not run.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 1s`; 51 actionable tasks: 51 up-to-date.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 1s`; 37 actionable tasks: 37 up-to-date.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence:

```txt
```

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence:

```txt
```

- Command: `rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|backend|server|Firestore|Supabase|retrofit|okhttp|version\.json|activation|license|trial|Billing|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS
- Evidence: no matches; command exited 1 because ripgrep found no forbidden-scope hits.

- Command: `Select-String ... -Pattern <mojibake sentinel pattern>`
- Result: PASS
- Evidence: no matches in current command output.

- Command: source review for Sprint 8-12 contracts using `rg -n "openWhatsApp|openShareSheet|copyMessageToClipboard|PostCallCards|snooze|Snooze|showFollowUpNotification|ACTION_OPEN_FOLLOW_UP|ManualWhatsAppScreen|PostCallScreen|MyDetailsScreen|TemplateManagementScreen" ...`
- Result: PASS
- Evidence: existing post-call notification handoff, four-card `PostCallCards` flow, manual composer, snooze controls, WhatsApp open, share, copy, My Details, and templates remain present and were not refactored by Sprint 13 beyond the additive setup/self-test entry.

### Manual QA
- Check: Full Sprint 13 real-phone smoke checklist.
- Result: NOT RUN
- Notes: `MANUAL_SMOKE_TEST.md` is explicitly marked `NOT RUN`. No PASS is claimed without real-phone evidence.

### Deviations from plan
- None for Sub-sprint 5. Documentation only; no code, Manifest, or Gradle changes.

### Blockers
- None.

### Next recommended action
- Ready for `followup-nadlan-reviewer` / human Gate 5 review. Do not start Sprint 14 until explicitly approved.

