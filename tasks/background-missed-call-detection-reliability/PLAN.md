# PLAN: Background Missed-Call Detection Reliability

**Status**: Done
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: High
**Generated**: 2026-07-02

## Goal Statement
Make missed incoming cellular calls reliably detected while the app is backgrounded or closed, then route confirmed missed calls through the existing missed-call decision engine without changing UI design or adding new product scope.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: No, inherited branch already contains an AccessibilityService; this sprint must not expand it and must preserve manual-mode safety.
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: The release blocker is in the core post-call detection layer. The smallest safe fix is to repair call-state monitoring, permission truth, diagnostics, and conservative CallLog backfill while reusing the existing `MissedCallAutoResponseHandler`.

## Non-Goals
- No UI redesign or Home visual polish beyond truthful readiness/status text.
- No AI, recording, transcription, CRM, caller ID scraping, backend, analytics, or new sending channels.
- No new WhatsApp automation behavior.
- No AccessibilityService expansion.
- No SMS behavior expansion beyond existing branch behavior and existing decision engine.
- No broad contact permissions at onboarding; `READ_CONTACTS` remains only for contact verification/contact picking.
- No old CallLog replay from hours or days ago.

## Assumptions
- Direct APK distribution remains in scope, so `READ_PHONE_STATE`, `READ_CALL_LOG`, foreground service permissions, and optionally `RECEIVE_BOOT_COMPLETED` are acceptable when disclosed.
- "Bridge enabled" should map to both the existing missed-call response setting and the call-detection foreground service setting.
- Manual mode means `MissedCallWhatsAppMode.PREPARED_MANUAL` and must produce prompt/notification only.
- The inherited dirty worktree is user/previous-sprint work and must be preserved.
- The initial implementation should support the current codebase's SharedPreferences stores rather than adding Room schema changes.

## Audit Findings
- Current call-state receiver: `CallDetectionService` registers `TelephonyCallback.CallStateListener` on Android 12+ and `PhoneStateListener.LISTEN_CALL_STATE` on older APIs.
- Manifest receiver: none found for `PHONE_STATE`; `AndroidManifest.xml` declares only `.postcall.CallDetectionService` for call detection.
- States received when service is alive: `RINGING`, `OFFHOOK`, and `IDLE` are mapped in `CallDetectionService.handlePlatformState`.
- Incoming number: Android 12+ callback path does not expose it; older `PhoneStateListener` receives `phoneNumber` but the current implementation ignores it.
- Background/killed behavior: not reliable. No manifest `PHONE_STATE` receiver is registered, no boot receiver is present, and current Home bridge enabling updates only `MissedCallAutoResponseSettings`, not `CallDetectionPreferences` or foreground service lifecycle.
- Pending state: `CallStateMonitor` keeps `incomingRang` and `answeredDuringCurrentCall` only in memory; process death between `RINGING` and `IDLE` loses the missed-call candidate.
- Current missed-call handler: `MissedCallAutoResponseHandler.handleMissedIncomingCandidate()` rereads the latest CallLog row after IDLE and routes to the existing decision engine.
- Current Home status truth: `HomeServiceStatus` claims `גישור פעיל` from the bridge toggle alone and does not check `READ_PHONE_STATE`, `READ_CALL_LOG`, or active service/listener status.
- Current permissions in manifest: `POST_NOTIFICATIONS`, `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, `SEND_SMS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`, and `MANAGE_OWN_CALLS`.
- Current requested runtime permissions seen in accessibility UI: `SEND_SMS` only. Existing self-test reads phone/call-log/contact/notification states but the bridge enable flow does not request or require the call-detection permissions.

## Files To Read First
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionPreferences.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallCandidate.kt`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/accessibility/HomeServiceStatus.kt`
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- `app/src/main/java/com/followupnadlan/setup/SetupReadinessLogic.kt`
- Existing tests under `app/src/test/java/com/followupnadlan/postcall`, `missedcall`, `setup`, and `accessibility`.

## Files Expected To Change
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionPreferences.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt`
- New `app/src/main/java/com/followupnadlan/postcall/PhoneStateReceiver.kt`
- New `app/src/main/java/com/followupnadlan/postcall/CallDetectionDiagnostics.kt`
- New `app/src/main/java/com/followupnadlan/postcall/PendingIncomingCallStore.kt`
- New `app/src/main/java/com/followupnadlan/postcall/MissedCallBackfill.kt`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/accessibility/HomeServiceStatus.kt`
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- `app/src/main/java/com/followupnadlan/setup/SetupReadinessLogic.kt`
- Focused unit tests in `app/src/test/java/com/followupnadlan/postcall`, `missedcall`, `setup`, and `accessibility`.

## Files That Must Not Change
- No Gradle dependency expansion unless compilation requires an AndroidX/core API already expected by the app.
- No Room schema/migrations.
- No template content redesign.
- No ActivityFeed/Home design changes unrelated to truthful status/diagnostics.
- No WhatsApp sender, SMS sender, or AccessibilityService behavior changes except tests proving manual-mode safety through the existing decision path.

## Sprint 1: Truthful Detector Lifecycle and Permissions
**Goal**: Make bridge enablement start/stop the foreground detector and stop claiming readiness when required permissions are missing.
**Demo / Validation**: Enabling bridge with permissions starts the foreground service; disabling bridge stops it; missing `READ_PHONE_STATE` or required `READ_CALL_LOG` shows warning status and does not claim `גישור פעיל`.
**Stop condition**: Stop if implementing this requires a new dangerous permission not already approved by the constitution or task.

### Task 1.1: Connect bridge setting to detector setting and service lifecycle
- Location: `AccessibilityApp.kt`, `CallDetectionPreferences.kt`, `CallDetectionService.kt`
- Description: When the user enables the bridge, persist call detection as enabled and start `CallDetectionService` as a foreground service. When disabled, persist disabled and stop the service. Keep service detection-only.
- Dependencies: Existing bridge toggle and `MissedCallAutoResponseSettings`.
- Acceptance criteria: Bridge enabled starts foreground service; bridge disabled stops it; service does not send or open WhatsApp directly.
- Validation command or manual check: Unit-test lifecycle helper; manual `adb shell dumpsys activity services | findstr CallDetectionService`.
- Rollback: Revert lifecycle helper and restore previous toggle-only behavior.

### Task 1.2: Add permission readiness model
- Location: `HomeServiceStatus.kt`, `SelfTestChecker.kt`, `SetupReadinessLogic.kt`
- Description: Treat `READ_PHONE_STATE` as required for detector operation and `READ_CALL_LOG` as required when the app needs the incoming number for missed-call routing. Surface `זיהוי שיחות לא פעיל — חסרה הרשאה` or `חסרה הרשאה לזיהוי שיחות`.
- Dependencies: Existing setup/self-test logic.
- Acceptance criteria: Home does not show `גישור פעיל` if detection is not actually ready; Settings/self-test shows exact missing permissions.
- Validation command or manual check: Unit tests for missing `READ_PHONE_STATE` and missing `READ_CALL_LOG`.
- Rollback: Revert status model changes.

### Task 1.3: Keep runtime permission requests scoped
- Location: `AccessibilityApp.kt`
- Description: Request `READ_PHONE_STATE` and `READ_CALL_LOG` only from bridge setup/readiness flow. Keep `READ_CONTACTS` only for contact picker/verification flows and `SEND_SMS` only for existing SMS fallback.
- Dependencies: Android runtime permission launcher pattern.
- Acceptance criteria: No startup contact request; no silent bridge enable without required call-detection permissions.
- Validation command or manual check: Source review and manual fresh-install permission flow.
- Rollback: Revert permission launch changes.

## Sprint 2: Reliable State Machine, Receiver, and Diagnostics
**Goal**: Persist missed-call state across process death and add QA diagnostics for call-state events.
**Demo / Validation**: `RINGING -> IDLE` without `OFFHOOK` confirms a missed call after process/state recreation; diagnostics show last state, last number status, last detected missed-call time, service/receiver status, and permission status.
**Stop condition**: Stop if Android API limitations prevent a manifest receiver from receiving useful call-state events on target devices; document fallback and continue with FGS/backfill only after approval.

### Task 2.1: Add manifest phone-state receiver
- Location: `AndroidManifest.xml`, new `PhoneStateReceiver.kt`
- Description: Add a manifest receiver for phone-state broadcasts that logs `CALL_RECEIVER_REGISTERED`, receives state changes when available, and delegates to shared detector logic. It should only detect/store/pass events, not send.
- Dependencies: `READ_PHONE_STATE`; optional number extra availability varies by API/OEM and should not be assumed.
- Acceptance criteria: Receiver exists in manifest and logs `PHONE_STATE_RECEIVED_*`; no UI launch from receiver.
- Validation command or manual check: Source tests for intent handling; manual `adb shell am broadcast` where possible for debug intent if supported.
- Rollback: Remove receiver and manifest entry.

### Task 2.2: Persist pending incoming call state
- Location: new `PendingIncomingCallStore.kt`, `CallStateMonitor.kt`, `CallDetectionService.kt`, `PhoneStateReceiver.kt`
- Description: On `RINGING`, persist `phoneNumber`, `timestamp`, `sawOffhook=false`. On `OFFHOOK`, set `sawOffhook=true`. On `IDLE`, if pending exists, recent, and not offhook, confirm missed call; otherwise ignore answered/old events.
- Dependencies: SharedPreferences.
- Acceptance criteria: Pending call survives recreation; old pending call ignored; answered call ignored.
- Validation command or manual check: Unit tests for state transitions and persistence.
- Rollback: Remove store and revert to in-memory `CallStateMonitor`.

### Task 2.3: Feed confirmed missed calls into existing decision engine
- Location: `CallDetectionService.kt`, `PhoneStateReceiver.kt`, `MissedCallAutoResponseHandler.kt` only if overload needed.
- Description: Pass a `MissedCallCandidate` with the detected/CallLog phone number, `INCOMING`, `wasAnswered=false`, and existing source. If number is missing/private, log and skip through existing decision handling.
- Dependencies: Existing `MissedCallCandidate` and decision engine.
- Acceptance criteria: No parallel send path; automatic/manual behavior remains decided only by `MissedCallAutoResponseDecision`.
- Validation command or manual check: Unit tests verifying handler call for confirmed missed call and no call for answered/unknown.
- Rollback: Revert handoff helper.

### Task 2.4: Add local diagnostic telemetry
- Location: new `CallDetectionDiagnostics.kt`, Settings or hidden developer diagnostic section.
- Description: Store local diagnostic events: `CALL_RECEIVER_REGISTERED`, `PHONE_STATE_RECEIVED_RINGING`, `PHONE_STATE_RECEIVED_OFFHOOK`, `PHONE_STATE_RECEIVED_IDLE`, `INCOMING_NUMBER_PRESENT`, `INCOMING_NUMBER_MISSING`, `MISSED_CALL_CANDIDATE_STORED`, `MISSED_CALL_CONFIRMED`, `MISSED_CALL_IGNORED_ANSWERED`, `MISSED_CALL_HANDLER_STARTED`, `MISSED_CALL_DECISION_RESULT`, `BACKGROUND_SERVICE_STARTED`, `BACKGROUND_SERVICE_STOPPED`, `PERMISSION_MISSING_READ_PHONE_STATE`, `PERMISSION_MISSING_READ_CALL_LOG`.
- Dependencies: SharedPreferences or log store; keep separate from user activity feed if needed.
- Acceptance criteria: QA screen labeled `בדיקת זיהוי שיחות` shows last phone state event, last incoming number seen, last missed-call detected time, receiver/service active status, and permission status.
- Validation command or manual check: Unit test diagnostics formatting; manual Settings check.
- Rollback: Remove diagnostics store/screen.

## Sprint 3: Conservative CallLog Backfill
**Goal**: Recover recent missed calls when a broadcast/listener event was missed while the process was killed.
**Demo / Validation**: Starting the app/service with `READ_CALL_LOG` granted processes only recent unhandled missed incoming calls within 2-5 minutes.
**Stop condition**: Stop if CallLog query cannot reliably distinguish missed incoming calls on target API/OEM without overprocessing.

### Task 3.1: Add recent missed-call backfill
- Location: `CallLogReader.kt`, new `MissedCallBackfill.kt`, `CallDetectionService.kt`
- Description: Query recent missed calls only when `READ_CALL_LOG` is granted. Process only `MISSED_TYPE`, recent rows, usable numbers, and rows not already handled.
- Dependencies: Existing `CallLogReaderLogic`; cooldown/history helpers.
- Acceptance criteria: Old calls ignored; already handled calls ignored; number/private skips logged.
- Validation command or manual check: Unit tests for recent, old, duplicate, and missing-number rows.
- Rollback: Remove backfill call and helper.

### Task 3.2: Invoke backfill on app/service start
- Location: `CallDetectionService.kt`, optionally `MainActivity.kt` startup if service start is gated by bridge enabled.
- Description: Run lightweight backfill when the foreground detector starts and optionally when app starts while bridge is enabled.
- Dependencies: Bridge/detector readiness from Sprint 1.
- Acceptance criteria: Backfill does not run when bridge disabled; no old-call replay.
- Validation command or manual check: Unit tests and manual logs.
- Rollback: Remove startup invocation.

## Sprint 4: Regression Coverage and Release Validation
**Goal**: Lock down the exact state machine, permissions, readiness, backfill, service lifecycle, and manual-mode safety.
**Demo / Validation**: Requested Gradle/test/lint/diff/status checks complete with evidence.
**Stop condition**: Stop if inherited dirty changes cause unrelated failures; report exact failing test/file and do not clean user work without approval.

### Task 4.1: Add requested regression tests
- Location: unit tests under `app/src/test/java/com/followupnadlan/postcall`, `missedcall`, `accessibility`, and `setup`.
- Description: Add or update tests for all 15 requested scenarios.
- Dependencies: Sprint 1-3 implementation.
- Acceptance criteria: Tests cover missed/answered/missing/private/persisted/manual/automatic/disabled/missing-permission/backfill/service start-stop cases.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Remove new tests with implementation rollback.

### Task 4.2: Run final validation
- Location: repo root.
- Description: Run `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, `.\gradlew.bat lintDebug`, `git diff --check`, and `git status`.
- Dependencies: All implementation tasks.
- Acceptance criteria: Passing or exact blocker reported.
- Validation command or manual check: Commands above.
- Rollback: Use git diff to revert only sprint-owned files after approval.

## Testing Strategy
- Unit tests: State machine, persistence, diagnostics, permission readiness, Home status truth, handler routing, manual-mode prompt-only, backfill recency/dedup.
- Instrumented tests: Not required for first pass unless service lifecycle cannot be covered by JVM tests.
- Manual QA: Real cellular call tests with app foreground, backgrounded, swiped away, and after service restart.
- Device/OEM checks: Pixel/stock Android first, then Samsung with battery optimization enabled/disabled.

## Permission Impact
- Added permissions: likely `RECEIVE_BOOT_COMPLETED` if boot restart is implemented; otherwise none beyond existing manifest permissions.
- Removed permissions: none in this sprint because inherited `SEND_SMS` and Accessibility pieces are outside this requested fix and dirty branch scope.
- Manifest risk: High; adding a manifest receiver and optional boot receiver must be tightly scoped and disclosed.
- User disclosure required: `READ_PHONE_STATE` for call state; `READ_CALL_LOG` for incoming number and backfill; `READ_CONTACTS` only for contact verification/picking; `SEND_SMS` only for inherited SMS fallback mode.

## Data/Schema Impact
- Room entities: none.
- migrations: none.
- local data retention: SharedPreferences for pending call and diagnostics only; do not store broad call history.

## UX Impact
- Screens affected: Home status text/action and Settings/hidden diagnostic section only.
- RTL/Hebrew checks: Ensure `זיהוי שיחות לא פעיל — חסרה הרשאה`, `חסרה הרשאה לזיהוי שיחות`, `השלם הרשאות`, and `בדיקת זיהוי שיחות` render correctly.
- Empty/fallback states: Missing/private number logs a friendly skip and does not send.
- Error states: Bridge enabled but detection not ready must show warning, not active.

## Rollback Plan
Revert only files touched by this task folder's implementation: manifest receiver/service lifecycle changes, new postcall helpers, readiness/status additions, diagnostics section, and tests. Leave inherited dirty accessibility/Home/template/missed-call changes intact unless explicitly approved.

## Review Checklist
- AndroidManifest is clean.
- No new AccessibilityService.
- No auto-send WhatsApp behavior added.
- wa.me / ACTION_VIEW remains user-driven unless inherited branch already has separate opt-in behavior.
- Manual mode never auto-sends WhatsApp, SMS, or Accessibility auto-click.
- Snooze restores prepared card.
- Fallback works without `READ_CALL_LOG`.
- Setup/self-test status is not broken.
- `גישור פעיל` appears only when detection is actually ready.
- Service/receiver only detect and pass candidates into existing decision engine.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas
- Android background restrictions may limit manifest phone-state broadcasts on newer devices; foreground service plus CallLog backfill is the reliability layer.
- Notification permission denial may hide the persistent service notification or user prompt; readiness must report this.
- `READ_CALL_LOG` denial means the app may observe state but not know the number; it must not send.
- OEM battery managers, especially Samsung, Xiaomi/Redmi, Realme, and OnePlus, may kill the service unless the user allows background activity.
- Duplicate reminders or repeated CallLog backfill can happen if handled-call keys are weak.
- Invalid phone formatting for wa.me can turn a real call into a skip; keep normalization tests.
- RTL text with phone numbers needs LTR handling in diagnostic/status rows.
- No Room migration should be introduced; SharedPreferences avoids migration risk.
- Direct-APK install/update friction remains outside this sprint.
