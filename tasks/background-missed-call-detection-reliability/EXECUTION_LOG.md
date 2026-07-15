# EXECUTION LOG: Background Missed-Call Detection Reliability

## Sprint 1: Truthful Detector Lifecycle and Permissions
Status: Completed
Started: 2026-07-02
Completed: 2026-07-02

### Changes made
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`: bridge setup now requests only `READ_PHONE_STATE` and `READ_CALL_LOG`, syncs `CallDetectionPreferences`, starts/stops `CallDetectionService`, shows a warning action when permissions are missing, and adds the Settings QA section `בדיקת זיהוי שיחות`.
- `app/src/main/java/com/followupnadlan/accessibility/HomeServiceStatus.kt`: Home status now distinguishes bridge off, detection ready, missing permission, and detector repair-needed states.
- `app/src/main/java/com/followupnadlan/setup/SetupReadinessLogic.kt`: `READ_CALL_LOG` is now required for reliable numbered missed-call detection readiness; `READ_CONTACTS` remains optional.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: app startup syncs an already-enabled bridge with the foreground detector lifecycle.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionServiceLifecycle.kt`: pure lifecycle decision helper for start/stop behavior.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 14s`; debug and release unit tests passed.

### Manual QA
- Check: Device foreground service and permission prompt flow.
- Result: PASS WITH NOTES
- Notes: Installed `app-debug.apk` on attached API 28 device `35373951464c3098`, granted `READ_PHONE_STATE` and `READ_CALL_LOG`, launched `MainActivity`, and verified `CallDetectionService` foreground state through `dumpsys activity services`. Logcat showed `BACKGROUND_SERVICE_STARTED` and `CALL_RECEIVER_REGISTERED`.

### Deviations from plan
- PLAN.md was still `Draft`, but the active-goal continuation explicitly instructed implementation toward the full objective. Execution proceeded and this deviation is recorded here.

### Blockers
- None.

### Next recommended action
- Continue.

## Sprint 2: Reliable State Machine, Receiver, and Diagnostics
Status: Completed
Started: 2026-07-02
Completed: 2026-07-02

### Changes made
- `app/src/main/AndroidManifest.xml`: added `RECEIVE_BOOT_COMPLETED` and manifest receiver `.postcall.PhoneStateReceiver` for `PHONE_STATE` and boot restart.
- `app/src/main/java/com/followupnadlan/postcall/PhoneStateReceiver.kt`: receives phone-state broadcasts, gates on bridge/permissions, starts the foreground detector, delays IDLE processing briefly, and delegates to the shared processor.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`: now runs as `START_STICKY`, records service diagnostics, registers phone callbacks, delegates missed-call state to the shared processor, and runs recent backfill on start.
- `app/src/main/java/com/followupnadlan/postcall/PendingIncomingCallStore.kt`: persists pending incoming call state with `phoneNumber`, `timestampMillis`, and `sawOffhook`.
- `app/src/main/java/com/followupnadlan/postcall/PhoneStateEventProcessor.kt`: shared state-machine processor for service and receiver.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionDiagnostics.kt`: local QA diagnostics store for the required call-detection events and status snapshot.
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`: returns the existing decision result and adds `handleConfirmedMissedIncomingCandidate(phoneNumber)` while preserving existing decision-engine routing.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `PendingIncomingCallLogicTest`, `HomeServiceStatusTest`, `SetupReadinessLogicTest`, and existing missed-call decision tests passed inside full unit suite.

### Manual QA
- Check: Real cellular `RINGING/OFFHOOK/IDLE` flow.
- Result: PASS
- Notes: On attached API 28 Samsung device `35373951464c3098`, logcat captured real incoming call events. A missed incoming call produced `PHONE_STATE_RECEIVED_RINGING`, `INCOMING_NUMBER_PRESENT`, `MISSED_CALL_CANDIDATE_STORED`, `PHONE_STATE_RECEIVED_IDLE`, `MISSED_CALL_CONFIRMED`, `MISSED_CALL_HANDLER_STARTED`, and `MISSED_CALL_DECISION_RESULT:SHOW_MANUAL_REPLY_PROMPT`. A later answered incoming call produced `PHONE_STATE_RECEIVED_OFFHOOK` followed by `MISSED_CALL_IGNORED_ANSWERED`.

### Deviations from plan
- The receiver uses `runCatching` around foreground-service start to avoid crashing if Android/OEM background-start rules reject the start. The event still routes through the receiver processor when permissions and bridge state allow it.

### Blockers
- None.

### Next recommended action
- Continue.

## Sprint 3: Conservative CallLog Backfill
Status: Completed
Started: 2026-07-02
Completed: 2026-07-02

### Changes made
- `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt`: added recent missed-call query.
- `app/src/main/java/com/followupnadlan/postcall/MissedCallBackfill.kt`: processes only recent missed CallLog rows, skips already handled keys, and routes candidates through the existing handler.
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`: invokes backfill when the foreground detector starts.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `MissedCallBackfillLogicTest` covers recent, old, already handled, answered incoming, and missing-number rows.

### Manual QA
- Check: Kill app, miss cellular call, start app/service within 2-5 minutes.
- Result: NOT RUN
- Notes: Requires physical Android device and CallLog permission.

### Deviations from plan
- None.

### Blockers
- None.

### Next recommended action
- Continue.

## Sprint 4: Regression Coverage and Release Validation
Status: Completed
Started: 2026-07-02
Completed: 2026-07-02

### Changes made
- `app/src/test/java/com/followupnadlan/postcall/PendingIncomingCallLogicTest.kt`: covers `RINGING -> IDLE`, `RINGING -> OFFHOOK -> IDLE`, persisted/recreated pending state, and stale pending state.
- `app/src/test/java/com/followupnadlan/postcall/MissedCallBackfillLogicTest.kt`: covers recent/old/duplicate/missing-number CallLog backfill.
- `app/src/test/java/com/followupnadlan/postcall/CallDetectionServiceLifecycleTest.kt`: covers foreground-service start/stop decisions for enabled, disabled, and missing-permission states.
- `app/src/test/java/com/followupnadlan/accessibility/HomeServiceStatusTest.kt`: covers truthful active/missing-permission/repair-needed Home status.
- `app/src/test/java/com/followupnadlan/setup/SetupReadinessLogicTest.kt`: updated `READ_CALL_LOG` readiness expectation.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 14s`.
- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 5s`.
- Command: `.\gradlew.bat lintDebug`
- Result: PASS
- Evidence: command exited successfully with code 0.
- Command: `adb install -r app\build\outputs\apk\debug\app-debug.apk`
- Result: PASS
- Evidence: installed successfully on API 28 device `35373951464c3098`.
- Command: `adb shell am start -n com.followupnadlan/.MainActivity` then `adb logcat -d CallDetection:D *:S`
- Result: PASS
- Evidence: logcat showed `BACKGROUND_SERVICE_STARTED`, `CALL_RECEIVER_REGISTERED`, real `PHONE_STATE_RECEIVED_RINGING`, missed-call confirmation, manual-mode decision `SHOW_MANUAL_REPLY_PROMPT`, and answered-call skip `MISSED_CALL_IGNORED_ANSWERED`.
- Command: `adb shell dumpsys activity services com.followupnadlan`
- Result: PASS
- Evidence: `CallDetectionService` had `isForeground=true`, `foregroundId=9001`, and `startRequested=true`.
- Command: `git diff --check`
- Result: PASS
- Evidence: command exited successfully with code 0; Git reported CRLF normalization warnings only.
- Command: `git status --short --branch`
- Result: PASS WITH NOTES
- Evidence: branch is `feature/accessibility-design-compose-final`; worktree remains dirty with inherited accessibility/Home/missed-call changes plus this task's files.

### Manual QA
- Check: End-to-end real-device call detection.
- Result: PASS
- Notes: Real incoming missed-call and answered-call events were observed on API 28 Samsung device `35373951464c3098`. Shell spoofing of protected `PHONE_STATE` remains blocked, so the evidence comes from real telephony logs, not fake broadcasts.

### Deviations from plan
- None.

### Blockers
- None.

### Next recommended action
- Review.

## Manifest and Scope Review
- Added permission: `RECEIVE_BOOT_COMPLETED`.
- Existing sensitive permissions preserved: `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, `SEND_SMS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`, `MANAGE_OWN_CALLS`.
- Existing AccessibilityService remains inherited branch state; this sprint did not add or expand it.
- No AI, recording, transcription, CRM, backend, analytics, caller ID scraping, or new sending channel was added.
- Manual mode remains routed through `SHOW_MANUAL_REPLY_PROMPT`; it does not auto-send WhatsApp, SMS, or accessibility clicks.
