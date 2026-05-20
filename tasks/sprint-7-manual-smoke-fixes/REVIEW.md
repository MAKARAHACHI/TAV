# REVIEW: Sprint 7 Manual Smoke Fixes

**Decision**: PASS
**Reviewed at**: 2026-05-19

## Scope Review
- Matches PLAN.md: Yes
- Scope expanded: No
- Files changed match expected list: Yes
- Unexpected files: None for Sprint 7. Repo already had uncommitted Sprint 6 files before this sprint.

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes
- Local-first MVP preserved: Yes
- No backend/API introduced: Yes
- Hebrew RTL preserved where applicable: Yes
- Snooze remains Core: Yes, not touched

## Permission and Manifest Review
- No AccessibilityService: Yes
- No auto-send WhatsApp automation: Yes
- No QUERY_ALL_PACKAGES: Yes
- No SMS permissions: Yes
- No WRITE_CALL_LOG: Yes
- READ_CALL_LOG only if planned and disclosed: Not applicable
- POST_NOTIFICATIONS handled: Not applicable
- FOREGROUND_SERVICE_PHONE_CALL handled: Not applicable
- Manifest diff: Empty
- Gradle diff: Empty

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Yes, existing path preserved
- User must press Send in WhatsApp: Yes
- Handles invalid/missing number: Yes, existing validation preserved
- Handles WhatsApp not installed if relevant: Yes, existing `ActivityNotFoundException` handling preserved

## Post-Call Review
- No direct Activity launch from background receiver: Not applicable
- Notification opens Activity via user action: Not applicable
- Fallback without number works: Yes, manual composer remains usable
- Setup/self-test not broken: Not applicable

## Snooze Review
- Prepared card state persists: Not applicable
- Reminder notification restores card: Not applicable
- Duplicate reminders avoided: Not applicable
- Time options match MVP: Not applicable

## Template Review
- Blocks can compose a message: Yes
- Agent profile placeholders resolve: Yes, renderer path unchanged
- Message is editable before WhatsApp: Yes
- Hebrew RTL and phone-number mixed layout acceptable: Yes by user-reported real-phone smoke
- Visible tag labels are Hebrew: Yes by source and unit tests
- Internal tag keys unchanged: Yes by unit tests
- Cursor-aware insertion: Yes by source and unit tests

## Tests and Validation
- Unit tests: PASS via `.\gradlew.bat test`
- Build: PASS via `.\gradlew.bat test assembleDebug`
- Manual QA: PASS by user-reported real-phone smoke on 2026-05-19
- Device/OEM QA: PASS for one real Android phone smoke; broader OEM matrix not run
- Manifest/Gradle diff: PASS, empty
- Forbidden-scope grep: PASS, no forbidden added lines
- Evidence missing: Broader OEM matrix not run

## Blockers
- None for source/build completion.

## Required fixes before merge
- None from source review.
- None from user-reported manual smoke.

## User-Reported Manual Smoke Evidence
- Template tag labels are now shown in Hebrew.
- Adding a tag inserts it at the current cursor position in the template text.
- If text is selected, inserting a tag replaces the selected text.
- After entering phone number and name on the WhatsApp send screen, navigating forward/back preserves the entered phone/name state.
