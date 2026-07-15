# REVIEW: Background Missed-Call Detection Reliability

**Decision**: PASS
**Reviewed at**: 2026-07-02

## Scope Review
- Matches PLAN.md: Yes
- Scope expanded: No
- Files changed match expected list: Yes
- Unexpected files: Existing dirty files from prior accessibility/Home work remain present in the worktree; this sprint preserved them.

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes
- Local-first MVP preserved: Yes
- No backend/API introduced: Yes
- Hebrew RTL preserved where applicable: Yes
- Snooze remains Core: Yes, not touched

## Permission and Manifest Review
- No AccessibilityService: No, inherited `WhatsAppAccessibilityService` remains in this branch. This sprint did not add or expand it.
- No auto-send WhatsApp automation: PASS WITH NOTES, inherited auto-send code remains; this sprint did not expand it and manual-mode tests still prove prompt-only behavior.
- No QUERY_ALL_PACKAGES: Yes
- No SMS permissions: No, inherited `SEND_SMS` remains. This sprint did not add it.
- No WRITE_CALL_LOG: Yes
- READ_CALL_LOG only if planned and disclosed: Yes
- POST_NOTIFICATIONS handled: Yes
- FOREGROUND_SERVICE_PHONE_CALL handled: Yes
- Added permission: `RECEIVE_BOOT_COMPLETED`, planned for detector restart.

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Yes, existing paths preserved
- User must press Send in WhatsApp: Yes for manual mode; inherited auto mode remains separate existing branch behavior
- Handles invalid/missing number: Yes, decision engine skips no-number/private/unknown values
- Handles WhatsApp not installed if relevant: Yes, existing decision/fallback path preserved

## Post-Call Review
- No direct Activity launch from background receiver: Yes
- Notification opens Activity via user action: Yes, existing notification path preserved
- Fallback without number works: Yes, no-number path skips sending and manual app remains usable
- Setup/self-test not broken: Yes, unit tests pass and `READ_CALL_LOG` status is now truthful
- Foreground service starts when bridge enabled and permissions are present: Yes
- Foreground service stops when bridge disabled or required detection permissions are absent: Yes by lifecycle helper
- Pending missed-call state survives process recreation: Yes by SharedPreferences store and unit test
- Recent CallLog backfill is conservative: Yes by recency/type/dedup tests

## Snooze Review
- Prepared card state persists: Not applicable, not touched
- Reminder notification restores card: Not applicable, not touched
- Duplicate reminders avoided: Not applicable, not touched
- Time options match MVP: Not applicable, not touched

## Template Review
- Blocks can compose a message: Not applicable, not touched by this sprint
- Agent profile placeholders resolve: Not applicable, not touched by this sprint
- Message is editable before WhatsApp: Not applicable, existing behavior preserved
- Hebrew RTL and phone-number mixed layout acceptable: PASS WITH NOTES, diagnostics use compact Settings rows; no visual screenshot was taken

## Tests and Validation
- Unit tests: `.\gradlew.bat test` PASS
- Build: `.\gradlew.bat assembleDebug` PASS
- Lint: `.\gradlew.bat lintDebug` PASS
- Diff check: `git diff --check` PASS with CRLF warnings only
- Device QA: debug APK installed on API 28 Samsung device `35373951464c3098`; app launch started foreground `CallDetectionService`; logcat showed `BACKGROUND_SERVICE_STARTED` and `CALL_RECEIVER_REGISTERED`; `dumpsys` showed `isForeground=true`, `foregroundId=9001`.
- Manual QA: Real incoming missed call produced `PHONE_STATE_RECEIVED_RINGING`, `INCOMING_NUMBER_PRESENT`, `MISSED_CALL_CANDIDATE_STORED`, `PHONE_STATE_RECEIVED_IDLE`, `MISSED_CALL_CONFIRMED`, `MISSED_CALL_HANDLER_STARTED`, and `MISSED_CALL_DECISION_RESULT:SHOW_MANUAL_REPLY_PROMPT`.
- Answered-call QA: A later answered incoming call produced `PHONE_STATE_RECEIVED_OFFHOOK` and then `MISSED_CALL_IGNORED_ANSWERED`.
- Device/OEM QA: Samsung foreground-service path is proven on API 28. Battery optimization kill behavior remains a release-note risk for longer idle periods.
- Evidence missing: long-duration idle/battery-optimization soak test on Samsung/Xiaomi/Redmi.

## Blockers
- None.

## Required fixes before merge
- None from source/build/device review.
- Recommended before release signoff: longer Samsung/Xiaomi/Redmi battery-optimization soak test.
