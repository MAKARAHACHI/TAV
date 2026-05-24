# REVIEW: Sprint 13 Setup Wizard + OEM Battery Guidance + Self-Test

**Decision**: PASS WITH NOTES
**Reviewed at**: 2026-05-24

## Scope Review
- Matches PLAN.md: Yes
- Scope expanded: No
- Files changed match expected list: Yes, with one narrow helper exposure noted below
- Unexpected files: `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt` changed only to expose the existing `CHANNEL_ID` constant so `SelfTestChecker` does not hardcode the notification channel id.
- Sprint 14 started: No
- Licensing/trial/activation added: No

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes
- Local-first MVP preserved: Yes
- No backend/API introduced: Yes
- Hebrew RTL preserved where applicable: Yes
- Snooze remains Core: Yes
- User-controlled WhatsApp sending preserved: Yes
- Accessibility and unauthorized automation avoided: Yes
- Fallback mode preserved if permissions fail: Yes

## Permission and Manifest Review
- No AccessibilityService: Yes
- No auto-send WhatsApp automation: Yes
- No QUERY_ALL_PACKAGES: Yes
- No SMS permissions: Yes
- No WRITE_CALL_LOG: Yes
- READ_CALL_LOG only if planned and disclosed: Yes
- POST_NOTIFICATIONS handled: Yes
- FOREGROUND_SERVICE_PHONE_CALL handled: Not changed
- New permissions added: No
- Manifest diff empty: Yes
- Gradle dependency diff empty: Yes

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Yes
- User must press Send in WhatsApp: Yes
- Handles invalid/missing number: Yes, existing manual composer behavior preserved
- Handles WhatsApp not installed if relevant: Yes, existing fallback/share/copy behavior preserved
- WhatsApp open/share/copy changed by Sprint 13: No

## Post-Call Review
- No direct Activity launch from background receiver: Yes
- Notification opens Activity via user action: Yes
- Fallback without number works: Yes, existing post-call/manual fallback remains present
- Setup/self-test not broken: Yes by source/build review
- Wizard wraps detection setup but does not change call detection logic: Yes
- Self-test places no call: Yes

## Snooze Review
- Prepared card state persists: Yes, existing Room-backed snooze path remains present
- Reminder notification restores card: Yes, existing reminder open path remains present
- Duplicate reminders avoided: Not changed by Sprint 13
- Time options match MVP: Yes, existing snooze options remain present
- Snooze code changed by Sprint 13: No

## Template Review
- Blocks can compose a message: Yes, existing template flow remains present
- Agent profile placeholders resolve: Yes, existing renderer/profile flow remains present
- Message is editable before WhatsApp: Yes
- Hebrew RTL and phone-number mixed layout acceptable: Source review only; real-device UI check NOT RUN
- Template code changed by Sprint 13: No

## Setup Wizard Review
- First-run routing present: Yes, setup wizard is selected when setup-completed flag is unset
- Re-run entry present: Yes, `הגדרה ובדיקה` entry is available in the main app shell
- Steps present: Yes, welcome, permissions, agent profile, OEM/battery guidance, and self-test handoff
- Profile reuse: Yes, wizard profile step uses existing `MyDetailsStore`
- Duplicate profile store introduced: No
- Existing profile values reset: No source evidence of reset; real-device smoke NOT RUN
- OEM guidance safe: Yes, pure mapping with generic fallback
- Battery intent safe: Yes, app-settings intent helper returns nullable result and UI catches runtime failures
- Manual mode blocked by denied permission: No

## Self-Test Review
- Reads real conditions only: Yes
- Writes detection state: No
- Starts/stops call detection service: No
- Places phone calls: No
- Sends notifications: No
- Channel ID sourced correctly: Yes, `SelfTestChecker` uses `FollowUpNotificationHelper.CHANNEL_ID`
- Notification channel id hardcoded in checker: No
- Battery optimization check exception-safe: Yes, returns UNKNOWN through nullable state on runtime/security exceptions
- User-observation pattern correct: Yes, `זה עבד ✓` and `זה לא עבד ✗` record only `self_test_passed`
- Writes to FollowUp Action Log: No
- Logs or claims `CALL_DETECTED`: No
- `בדוק שוב` refresh path present: Yes

## Tests and Validation
- Unit tests: PASS, `.\gradlew.bat test` returned `BUILD SUCCESSFUL in 1s`; 51 actionable tasks up-to-date
- Build: PASS, `.\gradlew.bat assembleDebug` returned `BUILD SUCCESSFUL in 1s`; 37 actionable tasks up-to-date
- Gradle diff: PASS, `git diff -- app/build.gradle.kts build.gradle.kts` empty
- Manifest diff: PASS, `git diff -- app/src/main/AndroidManifest.xml` empty
- Forbidden-scope grep: PASS, no matches
- Hebrew mojibake check: PASS, no matches in changed Kotlin files checked
- Sprint 8-12 source contract regression: PASS by source review for post-call notification, four cards, snooze, manual composer, WhatsApp open, share, copy, My Details, and templates
- Manual QA: NOT RUN
- Device/OEM QA: NOT RUN
- Evidence missing: Real-phone setup wizard and self-test smoke evidence

## Blockers
- None for source/build/unit review.

## Required fixes before merge
- None expected from source review.

## Notes
- Decision is `PASS WITH NOTES`, not `PASS`, because real-phone manual smoke remains `NOT RUN`.
- Do not mark Sprint 13 as fully device-validated until `MANUAL_SMOKE_TEST.md` has explicit real-phone evidence.
