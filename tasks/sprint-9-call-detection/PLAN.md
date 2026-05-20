# PLAN: Sprint 9 Call Detection Foreground Service + Action Log Foundation

**Status**: Human-approved by current `/goal` request
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: High
**Generated**: 2026-05-20

## Goal Statement
Wire the existing manual-only `FollowUpNotificationHelper` to a real Android call-end event so that ending a phone call automatically posts the same Hebrew follow-up notification, while adding a minimal local Action Log that records only real user-driven outgoing actions (WhatsApp opened, share opened, copy used).

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: Sprint 9 implements Layer 4 from `context/ROADMAP.md` in its minimum viable form. It uses only `READ_PHONE_STATE`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_PHONE_CALL`. It does not add `READ_CALL_LOG`, contacts, Snooze, Room, WorkManager, backend/API, or any WhatsApp automation. The Action Log uses existing SharedPreferences patterns and stores only real outgoing user actions, matching `context/PERMISSIONS_AND_PRIVACY.md` data-retention rules and the constitution's "no false logs" principle.

## Non-Goals
- Do not add `READ_CALL_LOG`. The call number will not be auto-filled in Sprint 9. The notification opens the existing composer with empty phone, and the user types/pastes the number as today.
- Do not add `READ_CONTACTS`. Contact name resolution stays out of scope.
- Do not add Snooze, WorkManager, AlarmManager, reminders, or any background scheduling.
- Do not add Room, DataStore, migrations, or backend/API of any kind.
- Do not add the four post-call cards (`שיחה קרה נפתחה`, etc.). They are Sprint 10 work.
- Do not auto-launch any Activity from the BroadcastReceiver or service. Only post a notification with PendingIntent.
- Do not add AccessibilityService, SYSTEM_ALERT_WINDOW, QUERY_ALL_PACKAGES, USE_FULL_SCREEN_INTENT, fullScreenIntent, SMS, or any WhatsApp automation.
- Do not record anything in the Action Log that the app did not actually observe. Specifically: never record `MESSAGE_SENT`, `CALL_DETECTED`, contact data, full message text, or raw phone numbers.
- Do not change Sprint 3 My Details, Sprint 4 renderer, Sprint 5 property links, Sprint 6 template store, Sprint 7 tag insertion, or Sprint 8 notification helper behavior except by reusing them.
- Do not change Gradle versions, add dependencies, or add libraries.
- Do not mark manual smoke PASS without explicit real Android phone evidence on at least one Pixel-class device.

## Assumptions
- minSdk remains 26; targetSdk remains 34.
- `TelephonyCallback.CallStateListener` is used on API 31+. On API 26-30 the implementation falls back to `PhoneStateListener` with the same state-transition logic.
- The Foreground Service is started only after the user explicitly enables call detection in a setup toggle on the existing send screen. The service is not auto-started on boot in Sprint 9.
- The service uses foreground service type `phoneCall` on API 30+ and runs as a long-running listener with a low-importance persistent notification distinct from the follow-up notification.
- A "call ended" event for Sprint 9 is defined as the transition `OFFHOOK → IDLE`. Short calls below a configured threshold (default 5 seconds) do not trigger the follow-up notification, to avoid noise from mis-dials.
- The Action Log stores at most 100 most-recent entries (FIFO trim), in `SharedPreferences` as a single JSON-encoded string, matching the project's existing simple persistence pattern. No new dependency is required because the existing Kotlin stdlib + manual JSON or simple line-based encoding is sufficient.
- `messagePreview` is the first 80 characters of the rendered outgoing message text. No full message is stored.
- The follow-up notification opens the existing composer through the same PendingIntent extras contract used in Sprint 8 (`EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`). On a real call-end event in Sprint 9, these extras are empty because `READ_CALL_LOG` is intentionally not requested.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
- `context/POST_CALL_ENGINE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/ARCHITECTURE.md`
- `context/OEM_SETUP_WIZARD.md`
- `context/DO_NOT_BUILD.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `tasks/sprint-8-notification-followup-card/PLAN.md`
- `tasks/sprint-8-notification-followup-card/REVIEW.md`

## Files Expected To Change
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt` (new)
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt` (new)
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionPreferences.kt` (new)
- `app/src/main/java/com/followupnadlan/log/FollowUpLogEntry.kt` (new)
- `app/src/main/java/com/followupnadlan/log/FollowUpLogStore.kt` (new)
- `app/src/main/java/com/followupnadlan/log/FollowUpActionType.kt` (new)
- `app/src/test/java/com/followupnadlan/postcall/CallStateMonitorTest.kt` (new)
- `app/src/test/java/com/followupnadlan/log/FollowUpLogStoreTest.kt` (new)
- `tasks/sprint-9-call-detection/PLAN.md`
- `tasks/sprint-9-call-detection/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-9-call-detection/EXECUTION_LOG.md`
- `tasks/sprint-9-call-detection/REVIEW.md`

## Files That Must Not Change
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `context/*` unless implementation behavior contradicts them, in which case stop and request planner re-entry.
- `app/src/main/java/com/followupnadlan/profile/*`
- `app/src/main/java/com/followupnadlan/templates/*`
- `app/src/main/java/com/followupnadlan/whatsapp/*`
- All prior sprint task folders (`tasks/sprint-1-*` through `tasks/sprint-8-*`).
- Sprint 8 notification helper public contract (`ACTION_OPEN_FOLLOW_UP`, `EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`, channel id, notification id, request code). Internal implementation may be refactored if needed, but the contract must be preserved so Sprint 8 manual smoke does not regress.

## Sprint 1: Call State Monitor Core (Pure)
**Goal**: Add a pure Kotlin call-state transition detector that can be unit-tested without Android framework.
**Demo / Validation**: `.\gradlew.bat test` proves OFFHOOK→IDLE triggers a "call ended" event with the observed duration; short-call threshold suppresses the event; rapid duplicate IDLE events do not double-trigger.
**Stop condition**: Stop if implementation drifts into using `TelephonyCallback` or `PhoneStateListener` types inside the pure core. Those belong in the platform service in Sprint 2.

### Task 1.1: Define Call State Model
- Location: `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- Description: Create a pure class with an internal enum `CallState { IDLE, RINGING, OFFHOOK }`, methods `onStateChanged(newState: CallState, nowMillis: Long)`, and a single output callback `onCallEnded(durationSeconds: Long)`. Maintain `lastState`, `offhookStartedAtMillis`, and a configurable `minCallDurationSeconds` (default 5).
- Dependencies: Kotlin stdlib only.
- Acceptance criteria: No Android imports; deterministic; emits `onCallEnded` exactly once per OFFHOOK→IDLE transition; suppresses emission when duration < threshold; ignores IDLE→IDLE and RINGING→IDLE (no active call took place).
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete `CallStateMonitor.kt`.

### Task 1.2: Unit Tests for Monitor
- Location: `app/src/test/java/com/followupnadlan/postcall/CallStateMonitorTest.kt`
- Description: Cover: IDLE→OFFHOOK→IDLE triggers with correct duration; OFFHOOK lasting 3 seconds is suppressed by default threshold; IDLE→RINGING→IDLE does not trigger (rejected/missed call without OFFHOOK); IDLE→OFFHOOK→OFFHOOK (idempotent state) does not double-start timer; two consecutive ended calls work; custom threshold parameter is honored.
- Dependencies: Existing JUnit setup.
- Acceptance criteria: All cases pass; no Android framework dependency in test.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete the test file.

## Sprint 2: Platform Call Detection Service
**Goal**: Wrap the pure monitor in a `Service` that listens to real phone state and posts the existing follow-up notification on call end.
**Demo / Validation**: On a real Android phone with permission granted and detection toggle enabled, completing a real phone call of more than 5 seconds causes the existing Hebrew follow-up notification to appear within 5 seconds of the call ending. Tapping the notification opens the existing composer with empty phone/name/template (per Non-Goals).
**Stop condition**: Stop if the implementation requires `READ_CALL_LOG`, `READ_CONTACTS`, `BIND_NOTIFICATION_LISTENER_SERVICE`, or any forbidden API; if the service requires fullScreenIntent to surface the notification; or if launching from background requires direct Activity start.

### Task 2.1: Add Required Permissions to Manifest
- Location: `app/src/main/AndroidManifest.xml`
- Description: Add exactly three new `<uses-permission>` lines: `android.permission.READ_PHONE_STATE`, `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_PHONE_CALL`. Do not add anything else. `POST_NOTIFICATIONS` from Sprint 8 stays as-is.
- Dependencies: None.
- Acceptance criteria: `git diff` on the manifest shows only these three additions plus the new service registration in Task 2.4. No other permissions appear.
- Validation command or manual check: `git diff -- app/src/main/AndroidManifest.xml`.
- Rollback: Remove the three permission lines.

### Task 2.2: Add Detection Preferences Store
- Location: `app/src/main/java/com/followupnadlan/postcall/CallDetectionPreferences.kt`
- Description: Tiny wrapper over `SharedPreferences` exposing `isEnabled(): Boolean`, `setEnabled(value: Boolean)`, `minCallDurationSeconds: Int` (default 5). Storage key prefix: `call_detection_`. Local-only.
- Dependencies: Existing SharedPreferences pattern from `MyDetailsStore`.
- Acceptance criteria: No network, no Room, no migration, no contacts read, no analytics.
- Validation command or manual check: Source review.
- Rollback: Delete the file.

### Task 2.3: Implement CallDetectionService
- Location: `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- Description: A `Service` (started, not bound) that on `onCreate` registers either `TelephonyCallback.CallStateListener` (API 31+) or `PhoneStateListener` (API 26-30) via `TelephonyManager`. Translates platform call states into the `CallStateMonitor.CallState` enum, calls `onStateChanged(state, System.currentTimeMillis())`. On the monitor's `onCallEnded` callback, calls `FollowUpNotificationHelper.showFollowUpNotification(phone = "", leadName = "", templateId = "")`. On API 30+ the service starts as foreground with type `phoneCall` and a low-importance persistent status notification on a separate channel `call_detection_status`. On `onDestroy`, unregisters the listener. Service is started by `MainActivity` only when the user enables the detection toggle. Service does not auto-restart on boot in Sprint 9 (deferred to a separate sprint).
- Dependencies: `CallStateMonitor`, `FollowUpNotificationHelper`, `CallDetectionPreferences`.
- Acceptance criteria: No direct Activity launch from the service; no Accessibility usage; no `READ_CALL_LOG` query; service runs only while detection is enabled; persistent status notification has a clear Hebrew title such as `זיהוי שיחות פעיל`; tapping the status notification opens `MainActivity` normally (no special action).
- Validation command or manual check: Real-phone smoke; source review for forbidden APIs.
- Rollback: Delete the service file and revert the manifest service registration.

### Task 2.4: Register Service in Manifest
- Location: `app/src/main/AndroidManifest.xml`
- Description: Add `<service android:name=".postcall.CallDetectionService" android:exported="false" android:foregroundServiceType="phoneCall" />` inside the `<application>` block.
- Dependencies: Task 2.3.
- Acceptance criteria: `exported="false"`; no intent filter; type `phoneCall` only.
- Validation command or manual check: `git diff -- app/src/main/AndroidManifest.xml`.
- Rollback: Remove the service tag.

### Task 2.5: Add Detection Toggle in Existing Send Screen
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Below the existing Sprint 8 `בדיקת התראת פולואפ` button, add one labeled `הפעל זיהוי שיחות אוטומטי` / `כבה זיהוי שיחות אוטומטי` (toggle text). Enabling: request `READ_PHONE_STATE` (and on Android 13+, `POST_NOTIFICATIONS` if not already granted), then `startForegroundService(Intent(this, CallDetectionService::class.java))`, then persist `enabled=true`. Disabling: `stopService(...)` and persist `enabled=false`. Show a short Hebrew status line indicating current state.
- Dependencies: Tasks 2.2 and 2.3, existing notification permission launcher pattern.
- Acceptance criteria: Toggle does not redesign My Details or template management; existing manual smoke flow remains usable; denying `READ_PHONE_STATE` leaves the app fully usable in manual mode with a clear Hebrew status.
- Validation command or manual check: Real-phone smoke; source review.
- Rollback: Remove the toggle composable and related state.

## Sprint 3: Action Log Foundation
**Goal**: Record only real user-driven outgoing actions locally.
**Demo / Validation**: Unit tests prove append + trim behavior; manual smoke confirms that tapping Open WhatsApp, Manual Share, or Copy creates exactly one new entry each, with `messagePreview` of length ≤ 80, and never records anything when the user does not press these buttons.

### Task 3.1: Define Log Entry Model and Action Type Enum
- Location: `app/src/main/java/com/followupnadlan/log/FollowUpLogEntry.kt` and `app/src/main/java/com/followupnadlan/log/FollowUpActionType.kt`
- Description: `enum class FollowUpActionType { WHATSAPP_OPENED, SHARE_OPENED, COPY_USED }`. `data class FollowUpLogEntry(val timestampMillis: Long, val actionType: FollowUpActionType, val messagePreview: String)`. `messagePreview` is the first 80 characters of the rendered message, never the full message, never any phone number.
- Dependencies: Kotlin stdlib only.
- Acceptance criteria: No fields exist for full message text, raw phone, contact name, or call duration. The enum has exactly three values in Sprint 9.
- Validation command or manual check: Source review.
- Rollback: Delete the two files.

### Task 3.2: Implement FollowUpLogStore
- Location: `app/src/main/java/com/followupnadlan/log/FollowUpLogStore.kt`
- Description: SharedPreferences-backed store with `append(entry: FollowUpLogEntry)` and `load(): List<FollowUpLogEntry>`. Serialization: one entry per line, pipe-separated fields `timestamp|actionType|previewBase64` (Base64-encoded preview to avoid newline/pipe collisions). Trim to last 100 entries on every append. Corrupted lines are skipped silently. No network. No new dependency.
- Dependencies: Kotlin stdlib only.
- Acceptance criteria: Trim works; corrupted entries do not crash load; storage key is `follow_up_action_log`; preview is truncated to 80 chars before storing.
- Validation command or manual check: `.\gradlew.bat test` via Task 3.3.
- Rollback: Delete the file.

### Task 3.3: Unit Tests for Log Store
- Location: `app/src/test/java/com/followupnadlan/log/FollowUpLogStoreTest.kt`
- Description: Cover: append + load round-trip; trim at 100 entries (101st append removes oldest); empty load returns empty list; corrupted line is skipped; preview longer than 80 chars is truncated; Hebrew/RTL preview text round-trips correctly via Base64.
- Dependencies: Existing JUnit setup. Use a fake `SharedPreferences`-like layer or extract the serialization/trim logic into a pure inner object (`FollowUpLogStoreLogic`) the same way `TemplateStoreLogic` is structured in Sprint 6.
- Acceptance criteria: All cases pass; no Android framework dependency in test.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete the test file.

### Task 3.4: Wire Log into Existing Outgoing Actions
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: In the existing private helpers `openWhatsApp`, `openShareSheet`, `copyMessageToClipboard`, append one log entry immediately after the successful side-effect (after `startActivity` returns without exception, after clipboard set). Use the `renderedMessage` value already computed in the composer; never log the raw `phone`. If `startActivity` throws `ActivityNotFoundException`, do not log (no real action happened).
- Dependencies: Task 3.2.
- Acceptance criteria: Open WhatsApp logs `WHATSAPP_OPENED` only when the intent actually starts; share logs `SHARE_OPENED` only when the chooser opens; copy logs `COPY_USED` after the clipboard set; no other code path writes to the log; no log entry is ever written when the user did not press a button.
- Validation command or manual check: Real-phone smoke + source review (grep for `FollowUpLogStore` usages must show only these three helpers).
- Rollback: Remove the three append calls.

## Sprint 4: Evidence Docs and Scope Closure
**Goal**: Update Sprint 9 evidence artifacts honestly.

### Task 4.1: Write Manual Smoke Checklist
- Location: `tasks/sprint-9-call-detection/MANUAL_SMOKE_TEST.md`
- Description: Cover: toggle enable/disable; status notification visible while enabled; real call ≥5 seconds triggers follow-up notification; real call <5 seconds does not; tapping follow-up notification opens existing composer with empty fields; manual Sprint 8 smoke trigger still works; Sprint 3/4/5/6/7 flows all still work; Open WhatsApp/Share/Copy log entries are observable via a temporary debug surface or by reading SharedPreferences via `adb`; denying `READ_PHONE_STATE` leaves the app usable in manual mode; denying `POST_NOTIFICATIONS` on Android 13+ leaves manual mode usable. Default status: NOT RUN. Real-phone PASS may only be recorded by the human after testing.
- Dependencies: Implementation complete.
- Acceptance criteria: Status starts as NOT RUN.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

### Task 4.2: Write Execution Log
- Location: `tasks/sprint-9-call-detection/EXECUTION_LOG.md`
- Description: Standard execution log format used by Sprints 1-8: pre-flight evidence, baseline `git status`, changes made (file-by-file), validation commands and results, forbidden-scope grep results, manual QA status (NOT RUN unless human ran it), deviations, blockers, next recommended action.
- Dependencies: All implementation tasks complete.
- Acceptance criteria: All commands listed with exact PASS/FAIL evidence. Manual QA recorded as NOT RUN unless the human explicitly ran a real-phone smoke before merge.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

### Task 4.3: Write Review File
- Location: `tasks/sprint-9-call-detection/REVIEW.md`
- Description: Standard reviewer format. Decision: PASS, PASS WITH NOTES, FAIL, or BLOCKED based on actual evidence. All review sections from `.agents/skills/followup-nadlan-reviewer/SKILL.md` must be filled.
- Dependencies: Tasks 4.1 and 4.2.
- Acceptance criteria: Honest verdict. PASS only if source/build/unit-test evidence supports it. Manual smoke status is recorded separately.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

## Testing Strategy
- Unit tests:
  - `CallStateMonitorTest`: pure transition logic with the cases listed in Task 1.2.
  - `FollowUpLogStoreTest`: serialization, trim, corruption, Hebrew preview, truncation.
- Build: `.\gradlew.bat test assembleDebug`. Expect Gradle wrapper lock retry pattern from earlier sprints; record both attempts in EXECUTION_LOG.md.
- Instrumented tests: Not required for Sprint 9. The platform service path is exercised by real-phone smoke instead, because emulator call simulation behavior is inconsistent across images.
- Manual QA: Required on a real Android phone before marking `MANUAL_SMOKE_TEST.md` PASS. Minimum: one Pixel-class device. Samsung/Xiaomi guidance for battery optimization is deferred to the Setup Wizard sprint and is out of Sprint 9 scope.
- Forbidden-scope grep (must be clean against `app/` and build files; matches inside `tasks/` are acceptable as historical documentation):

```powershell
rg -n "READ_CALL_LOG|READ_CONTACTS|WRITE_CALL_LOG|SEND_SMS|READ_SMS|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|WorkManager|Room|AlarmManager|setExact|setExactAndAllowWhileIdle|RECEIVE_BOOT_COMPLETED|backend|server|CRM|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties
```

- Manifest diff check: `git diff -- app/src/main/AndroidManifest.xml` must show exactly the three permission additions plus the service registration. Nothing more.
- Sprint 8 contract regression: Manual Sprint 8 notification trigger must still work end-to-end.

## Permission Impact
- Added permissions:
  - `READ_PHONE_STATE` — required to register `TelephonyCallback` / `PhoneStateListener`. Listed in `context/PERMISSIONS_AND_PRIVACY.md` as allowed.
  - `FOREGROUND_SERVICE` — required for any foreground service on API 28+.
  - `FOREGROUND_SERVICE_PHONE_CALL` — required for foreground service type `phoneCall` on API 34.
- Removed permissions: None.
- Manifest risk: Medium. New permissions are sensitive but documented and conservatively scoped. No `READ_CALL_LOG`, no contacts, no SMS, no full-screen intent.
- User disclosure required: Yes, in the existing send screen when the user enables the toggle. Short Hebrew explanation: `כדי לזהות סיום שיחה צריך הרשאת מצב טלפון. האפליקציה לא קוראת את יומן השיחות, לא שולחת הודעות אוטומטית, ולא מעלה מידע לשרת.`

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention:
  - `CallDetectionPreferences`: boolean enabled flag + integer threshold, stored in SharedPreferences.
  - `FollowUpLogStore`: at most 100 entries; each entry has timestamp + action type + 80-char preview. No raw phone, no full message, no contact data, no call duration, no call number.
- Storage mechanism: existing SharedPreferences pattern only.

## UX Impact
- Screens affected: Existing send screen gains one toggle button and a short Hebrew status line. No other screen changes.
- RTL/Hebrew checks: Toggle button label, status line, status notification (`זיהוי שיחות פעיל`), and follow-up notification (unchanged from Sprint 8) must all be readable in RTL.
- Empty/fallback states:
  - `READ_PHONE_STATE` denied → toggle stays off, status line explains, manual mode still works.
  - `POST_NOTIFICATIONS` denied on Android 13+ → toggle still enables the service, but no notifications appear; status line warns clearly.
  - Detection enabled but no calls happen → only the persistent status notification is visible; no other side-effect.
- Error states: If `startForegroundService` throws on a constrained OEM, catch and show a Hebrew error in the status line. Do not crash.

## Rollback Plan
1. Remove `app/src/main/java/com/followupnadlan/postcall/` and `app/src/main/java/com/followupnadlan/log/` directories.
2. Revert `app/src/main/AndroidManifest.xml` to remove the three new permissions and the service tag.
3. Revert the Sprint 9 hunks in `MainActivity.kt` (toggle + log calls in helpers).
4. Delete `tasks/sprint-9-call-detection/`.
5. Sprint 8 manual notification trigger continues to work because its contract was preserved.

## Review Checklist
- AndroidManifest contains only `POST_NOTIFICATIONS` (from Sprint 8) plus the three Sprint 9 permissions and the new service tag.
- No AccessibilityService, no SYSTEM_ALERT_WINDOW, no QUERY_ALL_PACKAGES, no USE_FULL_SCREEN_INTENT, no fullScreenIntent, no SMS, no READ_CALL_LOG, no READ_CONTACTS, no WRITE_CALL_LOG, no Room, no WorkManager, no AlarmManager, no RECEIVE_BOOT_COMPLETED.
- No backend/API, no network calls, no analytics.
- wa.me / ACTION_VIEW remains user-driven; user still presses Send inside WhatsApp.
- Sprint 8 manual notification contract preserved (action, extras, channel id, request code).
- Fallback works when `READ_PHONE_STATE` or `POST_NOTIFICATIONS` is denied.
- `CallStateMonitor` is pure and unit-tested.
- `FollowUpLogStore` logic is pure-testable and unit-tested.
- Log only records actions that actually happened; no `MESSAGE_SENT`, no `CALL_DETECTED`, no raw phone, no full message.
- Sprint 3/4/5/6/7 behavior preserved; no Sprint 10 work started.
- Manual smoke not claimed PASS without real-phone evidence.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sprint at a time. Sub-sprints 1→2→3→4 must be completed in order. Do not start Sub-sprint 2 until Sub-sprint 1 unit tests pass. Do not start Sub-sprint 3 until Sub-sprint 2 source review is clean.
- Expansion rule: no scope expansion without human approval. Specifically, do not add `READ_CALL_LOG`, the four post-call cards, Snooze, AlarmManager, or boot-restart in Sprint 9 — even if it feels small.

## Potential Gotchas
- Android background restrictions: On API 31+, starting a foreground service from the background requires the service type and matching permission. The toggle is user-initiated from a foreground Activity, so this should pass, but on some OEMs (Xiaomi/MIUI, Realme) the service may be killed silently. Setup Wizard will address this in a later sprint; Sprint 9 only needs Pixel-class to pass.
- Notification permission denial on Android 13+: If the user denied `POST_NOTIFICATIONS`, the service runs but no notification surfaces. The status line must warn clearly; the toggle must not look like it succeeded.
- `READ_CALL_LOG` denial fallback: Not applicable in Sprint 9 because `READ_CALL_LOG` is intentionally not requested.
- OEM battery killing FGS: Expected risk. Out of scope. Mitigation lives in the Setup Wizard sprint.
- Duplicate notifications: The follow-up notification id is shared with Sprint 8 (`8001`). On a real call end, a new notification replaces any prior one. Verify this in manual smoke.
- Invalid phone formatting for wa.me: Not applicable; Sprint 9 does not auto-fill the phone.
- RTL text and mixed Hebrew/phone-number layout: Status line may contain Hebrew + number. Verify on a real phone.
- Room migration risk: None; no Room used.
- Direct-APK update/install friction: None; no distribution changes.
- TelephonyCallback vs PhoneStateListener: On API 31+ the new API is required and `PhoneStateListener` is deprecated. Implement both paths with a version guard. Test on at least API 30 and API 33+.
- Short-call threshold: Default 5 seconds is conservative to suppress mis-dials. If too aggressive, threshold is configurable via `CallDetectionPreferences` for later tuning, but the UI for tuning is out of Sprint 9 scope.
- Base64 encoding in log: Standard `android.util.Base64` is Android-only. Use `java.util.Base64` (available on Android API 26+) so the log logic stays unit-testable on the JVM without Robolectric.
- Service auto-restart on system reboot: Deliberately not implemented in Sprint 9. The toggle starts the service; rebooting the device turns detection off until the user opens the app again. Boot-restart is a separately approved sprint because it requires `RECEIVE_BOOT_COMPLETED` and a careful disclosure.
