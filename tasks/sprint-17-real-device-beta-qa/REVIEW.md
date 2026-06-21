# REVIEW: Sprint 17 Real Device QA Hardening and Beta Release Candidate

**Decision**: PASS WITH NOTES
**Reviewed at**: 2026-06-21

## Scope Review
- Matches PLAN.md: Yes
- Scope expanded: No new product feature added
- Files changed match expected list: Yes
- Unexpected files: Existing Sprint 14/15/16 dirty files remain and are documented in `WORKTREE_REVIEW.md`.

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes
- Local-first MVP preserved: Yes
- No backend/API introduced: Yes
- Hebrew RTL preserved where applicable: Yes
- Snooze remains Core: Yes

## Permission and Manifest Review
- No new AccessibilityService: Yes
- No new auto-send WhatsApp automation: Yes
- No QUERY_ALL_PACKAGES: Yes
- No new SMS permissions: Yes
- No WRITE_CALL_LOG: Yes
- READ_CALL_LOG only if planned and disclosed: Existing
- POST_NOTIFICATIONS handled: Existing
- FOREGROUND_SERVICE_PHONE_CALL handled: Existing

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Existing Sprint 16 path preserved
- User must press Send in WhatsApp: Manual mode yes; existing optional Accessibility mode remains explicit Sprint 16 exception
- Handles invalid/missing number: Covered by decision tests
- Handles WhatsApp not installed if relevant: Covered by fallback decision tests

## Post-Call Review
- No direct Activity launch from background receiver: Existing path preserved
- Notification opens Activity via user action: Existing path preserved
- Fallback without number works: Decision skips no-number
- Setup/self-test not broken: Unit tests/build/lint pass; real-device self-test not run

## Snooze Review
- Prepared card state persists: Not changed
- Reminder notification restores card: Not changed
- Duplicate reminders avoided: Not changed
- Time options match MVP: Not changed

## Template Review
- Blocks can compose a message: Not changed
- Agent profile placeholders resolve: Not changed
- Message is editable before WhatsApp: Existing manual path preserved
- Hebrew RTL and phone-number mixed layout acceptable: Requires device visual QA

## Tests and Validation
- Unit tests: `.\gradlew.bat test` PASS
- Build: `.\gradlew.bat assembleDebug` PASS
- Lint: `.\gradlew.bat lintDebug` PASS
- Manual QA: NOT RUN
- Device/OEM QA: NOT RUN
- Evidence missing: Real-device WhatsApp, Accessibility, SMS, duplicate cooldown, private number, and OEM/background checks.

## Blockers
- None for local code/build/lint.

## Required fixes before merge
- Complete manual QA before any beta confidence claim.
- Keep distribution limited to controlled private APK until real-device evidence is recorded.
