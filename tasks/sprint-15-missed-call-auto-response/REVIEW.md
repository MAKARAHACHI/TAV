# REVIEW: Sprint 15 Missed Call Auto Response

**Decision**: PASS WITH NOTES
**Reviewed at**: 2026-06-21

## Scope Review
- Matches PLAN.md: Yes
- Scope expanded: Minor manifest deviation for `MANAGE_OWN_CALLS` lint compliance
- Files changed match expected list: Mostly
- Unexpected files: Existing Sprint 14 dirty files remain in the worktree and were not part of this Sprint 15 review.

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes
- Local-first MVP preserved: Yes
- No backend/API introduced: Yes
- Hebrew RTL preserved where applicable: Yes
- Snooze remains Core: Yes

## Permission and Manifest Review
- No AccessibilityService: Yes
- No auto-send WhatsApp automation: Yes
- No QUERY_ALL_PACKAGES: Yes
- SMS permissions: `SEND_SMS` added as explicit Sprint 15 opt-in exception
- No READ_SMS/RECEIVE_SMS: Yes
- No WRITE_CALL_LOG: Yes
- READ_CALL_LOG only if planned and disclosed: Yes, pre-existing
- POST_NOTIFICATIONS handled: Yes, pre-existing
- FOREGROUND_SERVICE_PHONE_CALL handled: Yes
- `MANAGE_OWN_CALLS`: Added to satisfy target SDK 34 lint for existing `foregroundServiceType="phoneCall"`

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Existing manual flow preserved
- User must press Send in WhatsApp: Yes
- Handles invalid/missing number: Existing flow preserved
- Handles WhatsApp not installed if relevant: Existing fallback preserved

## Post-Call Review
- No direct Activity launch from background receiver: Yes
- Notification opens Activity via user action: Yes
- Fallback without number works: Existing manual/card flow preserved; auto SMS skips no-number
- Setup/self-test not broken: Tests/build pass; self-test adds read-only Sprint 15 checks

## Snooze Review
- Prepared card state persists: Existing Sprint 14 path preserved
- Reminder notification restores card: Existing path preserved
- Duplicate reminders avoided: Existing scheduler path preserved
- Time options match MVP: Existing options preserved

## Template Review
- Blocks can compose a message: Existing template editor preserved
- Agent profile placeholders resolve: Yes
- Message is editable before WhatsApp: Existing manual flow preserved
- Hebrew RTL and phone-number mixed layout acceptable: Source-level only; visual QA not run

## Tests and Validation
- Unit tests: `.\gradlew.bat test` PASS
- Build: `.\gradlew.bat assembleDebug` PASS
- Lint: `.\gradlew.bat lintDebug` PASS
- Manual QA: NOT RUN
- Device/OEM QA: NOT RUN
- Evidence missing: Real SMS send, real missed-call detection, duplicate cooldown on device, denied-permission fallback on device, Samsung/OEM behavior

## Blockers
- None for code/build.

## Required fixes before release
- Run real-device missed-call SMS QA with SIM/SMS capability.
- Verify denied `SEND_SMS` fallback notification opens prepared SMS composer.
- Verify duplicate suppression within 6 hours on the same normalized phone number.
- Verify Samsung/OEM background behavior.
- Complete release signing and release-channel QA before commercial readiness claims.
