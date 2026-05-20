# REVIEW: Sprint 9 Call Detection + Action Log

**Decision**: PASS WITH NOTES
**Reviewed at**: 2026-05-20

## Scope Review
- Matches PLAN.md: Yes.
- Scope expanded: No.
- Files changed match expected list: Yes, with one package reuse note.
- Unexpected files: None.
- Package reuse note: Existing `com.followupnadlan.followuplog` was reused and corrected instead of creating duplicate `com.followupnadlan.log`.
- Post-review correction: Hebrew call-detection status strings were fixed after review found mojibake in the new Sprint 9 UI/status-notification text.

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes.
- Local-first MVP preserved: Yes.
- No backend/API introduced: Yes.
- Hebrew RTL preserved where applicable: Yes.
- Snooze remains Core: Not applicable; Snooze was not implemented or removed.

## Permission and Manifest Review
- No AccessibilityService: Yes.
- No auto-send WhatsApp automation: Yes.
- No QUERY_ALL_PACKAGES: Yes.
- No SMS permissions: Yes.
- No WRITE_CALL_LOG: Yes.
- READ_CALL_LOG only if planned and disclosed: Not applicable; not added.
- READ_PHONE_STATE added: Yes, planned and user-toggle gated.
- POST_NOTIFICATIONS handled: Yes, existing Sprint 8 runtime path preserved and reused for service enable flow.
- FOREGROUND_SERVICE handled: Yes.
- FOREGROUND_SERVICE_PHONE_CALL handled: Yes.

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Yes.
- User must press Send in WhatsApp: Yes.
- Handles invalid/missing number: Yes, existing validation preserved.
- Handles WhatsApp not installed if relevant: Yes, existing `ActivityNotFoundException` fallback preserved.

## Post-Call Review
- No direct Activity launch from background receiver: Yes; no receiver was added.
- No direct Activity launch from service: Yes; service posts notification only.
- Notification opens Activity via user action: Yes, Sprint 8 `PendingIntent` helper is reused.
- Fallback without number works: Yes; service posts notification with empty extras.
- Setup/self-test not broken: Not applicable; setup wizard/self-test not implemented in this sprint.

## Snooze Review
- Prepared card state persists: Not applicable.
- Reminder notification restores card: Not applicable.
- Duplicate reminders avoided: Not applicable.
- Time options match MVP: Not applicable.

## Template Review
- Blocks can compose a message: Not changed.
- Agent profile placeholders resolve: Not changed.
- Message is editable before WhatsApp: Yes, existing composer preserved.
- Hebrew RTL and phone-number mixed layout acceptable: Source-level Hebrew strings are now valid; manual phone smoke not run.

## Action Log Review
- Logs only explicit user actions: Yes.
- Allowed actions only: `WHATSAPP_OPENED`, `SHARE_OPENED`, `COPY_USED`.
- No `MESSAGE_SENT`: Yes.
- No `CALL_DETECTED`: Yes.
- No raw phone numbers: Yes.
- No full message text: Yes.
- Preview cap: Yes, 80 characters.
- Storage: SharedPreferences only.
- Retention: Latest 100 entries.

## Tests and Validation
- Unit tests: PASS via `.\gradlew.bat test assembleDebug`.
- Build: PASS via `.\gradlew.bat test assembleDebug`.
- Manifest/Gradle diff: PASS, manifest-only with three planned permissions plus service tag; Gradle/settings files unchanged.
- Forbidden-scope grep: PASS, no matches.
- Hebrew encoding check: PASS, no remaining `×`/`Ã` mojibake markers in Sprint 9 call-detection strings.
- Sprint 8 contract: PASS, `FollowUpNotificationHelper.kt` unchanged and constants preserved.
- Manual QA: NOT RUN.
- Device/OEM QA: NOT RUN.
- Evidence missing: Real-phone call-detection smoke is pending.

## Blockers
- None for source/build review.

## Required fixes before merge
- None.

## Notes Before Human Smoke
- Android 14 foreground-service phone-call policy can be stricter on real devices; source/build passes, but real-phone testing must confirm service startup behavior.
- Manual smoke must remain `NOT RUN` until the human tests on a real phone.
