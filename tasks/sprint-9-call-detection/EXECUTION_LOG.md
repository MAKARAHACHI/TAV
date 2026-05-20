# EXECUTION LOG: Sprint 9 Call Detection + Action Log

## Pre-flight
Status: Completed
Started: 2026-05-20
Completed: 2026-05-20

### Evidence
- Mandatory product/context/skill files read.
- Existing `tasks/sprint-9-call-detection/PLAN.md` found and treated as human-approved by the current `/goal` request.
- Baseline git state: `## main...origin/main [ahead 1]` with untracked `tasks/sprint-9-call-detection/`.
- Existing Action Log code was present before this sprint execution and was narrowed to match the stricter Sprint 9 privacy rules.

## Sub-sprint 1: Pure CallStateMonitor + Unit Tests
Status: Completed
Started: 2026-05-20
Completed: 2026-05-20

### Changes made
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`: Added pure transition detector for `IDLE`, `RINGING`, and `OFFHOOK`.
- `app/src/test/java/com/followupnadlan/postcall/CallStateMonitorTest.kt`: Added deterministic JVM tests for call-end transitions, short-call suppression, duplicate states, consecutive calls, and custom threshold.
- `tasks/sprint-9-call-detection/PLAN.md`: Updated status to human-approved by the current `/goal` request.

### Validation run
- Command: `.\gradlew.bat test`
- First result: FAIL due to known Gradle wrapper cache lock: `C:\Users\danie\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9-bin.zip.lck (Access is denied)`.
- Retry command: `.\gradlew.bat test` with elevated Gradle cache access.
- Result: PASS.

### Deviations from plan
- None.

### Blockers
- None.

### Next recommended action
- Continue.

## Sub-sprint 2: Platform Service + Manifest Permissions + UI Toggle
Status: Completed
Started: 2026-05-20
Completed: 2026-05-20

### Changes made
- `app/src/main/AndroidManifest.xml`: Added `READ_PHONE_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`, and registered `.postcall.CallDetectionService` with `foregroundServiceType="phoneCall"`.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionPreferences.kt`: Added SharedPreferences-backed enabled flag and default 5-second threshold.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`: Added started foreground service using `TelephonyCallback.CallStateListener` on API 31+ and `PhoneStateListener` fallback on API 26-30.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: Added user-controlled call detection toggle, runtime permission request, service start/stop, and status messaging.

### Validation run
- Command: `.\gradlew.bat test assembleDebug`
- Result: PASS.
- Evidence: `BUILD SUCCESSFUL in 14s`.
- Notes: Kotlin compile emitted expected deprecation warnings for planned API 26-30 `PhoneStateListener` fallback.

### Manifest / Gradle diff
```diff
diff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml
index 0a6341d..d9ac764 100644
--- a/app/src/main/AndroidManifest.xml
+++ b/app/src/main/AndroidManifest.xml
@@ -1,5 +1,8 @@
 <manifest xmlns:android="http://schemas.android.com/apk/res/android">
     <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
+    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
+    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
+    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
 
     <application
         android:allowBackup="true"
@@ -17,5 +20,9 @@
                 <category android:name="android.intent.category.LAUNCHER" />
             </intent-filter>
         </activity>
+        <service
+            android:name=".postcall.CallDetectionService"
+            android:exported="false"
+            android:foregroundServiceType="phoneCall" />
     </application>
 </manifest>
```

### Forbidden-scope grep
- Command: `rg -n "READ_CALL_LOG|READ_CONTACTS|WRITE_CALL_LOG|SEND_SMS|READ_SMS|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|WorkManager|Room|AlarmManager|setExact|setExactAndAllowWhileIdle|RECEIVE_BOOT_COMPLETED|backend|server|CRM|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS.
- Output: no matches.

### Sprint 8 contract regression
- `FollowUpNotificationHelper.kt` has no diff.
- Confirmed unchanged: `ACTION_OPEN_FOLLOW_UP`, `EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`, channel id `follow_up_cards`, notification id `8001`, request code `8001`.

### Deviations from plan
- None.

### Blockers
- None.

### Next recommended action
- Continue.

## Sub-sprint 3: Action Log Foundation + Unit Tests + Outgoing Helper Wiring
Status: Completed
Started: 2026-05-20
Completed: 2026-05-20

### Changes made
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`: Narrowed entries to `actionType`, `timestampEpochMs`, and `messagePreview` only.
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`: Set retention to 100 entries and preview cap to 80 characters.
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`: Updated tests for 100-entry trim, 80-character preview, malformed rows, empty load, truthful action labels, and Hebrew preview round-trip.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: Preserved logging only after the three explicit successful outgoing actions: WhatsApp open, share sheet open, and clipboard copy.

### Validation run
- Command: `.\gradlew.bat test assembleDebug`
- Result: PASS.
- Evidence: `BUILD SUCCESSFUL in 8s`.

### Privacy review
- No `MESSAGE_SENT` or `CALL_DETECTED` enum exists.
- No raw phone number is stored in `FollowUpLogEntry`.
- No full message text is stored; preview is capped at 80 characters.

### Deviations from plan
- Used the existing `com.followupnadlan.followuplog` package from prior work instead of creating a duplicate `com.followupnadlan.log` package.
- Rationale: This avoids two competing local log implementations and brings the existing one into compliance with the current stricter Sprint 9 rules.

### Blockers
- None.

### Next recommended action
- Continue.

## Sub-sprint 4: Evidence Docs and Closure
Status: Completed
Started: 2026-05-20
Completed: 2026-05-20

### Changes made
- `tasks/sprint-9-call-detection/MANUAL_SMOKE_TEST.md`: Added real-phone checklist with status `NOT RUN`.
- `tasks/sprint-9-call-detection/EXECUTION_LOG.md`: Added execution evidence.
- `tasks/sprint-9-call-detection/REVIEW.md`: Added source review result.

### Validation run
- Command: docs/source review.
- Result: PASS.

### Manual QA
- Check: Real Android phone smoke.
- Result: NOT RUN.
- Notes: Must remain NOT RUN until the human reports real-phone evidence.

### Deviations from plan
- None beyond the Action Log package reuse documented above.

### Blockers
- None.

### Next recommended action
- Review, then human real-phone smoke.

## Post-review correction: Hebrew encoding
Status: Completed
Completed: 2026-05-20

### Issue found
- Source review found Sprint 9 Hebrew call-detection status strings committed as mojibake in `MainActivity.kt` and `CallDetectionService.kt`.
- Impact: source/build behavior was valid, but the new Hebrew UI/status notification text would display incorrectly on device.

### Changes made
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: Replaced mojibake call-detection toggle/status/error strings with valid Hebrew.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`: Replaced mojibake foreground-service notification channel/title/body strings with valid Hebrew.

### Validation run
- Command: `Select-String -Path app\src\main\java\com\followupnadlan\MainActivity.kt,app\src\main\java\com\followupnadlan\postcall\CallDetectionService.kt -Pattern "×|Ã"`
- Result: PASS; no output.
- Command: `git diff --check`
- Result: PASS; only CRLF warnings.
- Command: forbidden-scope grep from PLAN.md against `app build.gradle.kts settings.gradle.kts gradle.properties`
- Result: PASS; no output.
- Command: `.\gradlew.bat test assembleDebug`
- Result: PASS; `BUILD SUCCESSFUL in 7s`.

### Manual QA
- Real Android phone smoke: NOT RUN.
- Notes: The encoding fix improves what manual smoke should verify, but does not replace real-phone evidence.
