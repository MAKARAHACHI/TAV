# REVIEW: Sprint 8 Notification Follow-Up Pop-Out Card

**Decision**: PASS
**Reviewed at**: 2026-05-20

## Scope Review
- Matches PLAN.md: Yes.
- Scope expanded: No.
- Real call detection deferred: Yes.
- Manual/dev smoke trigger added: Yes, on the existing send screen.
- Existing Sprint 6/7 uncommitted files preserved: Yes.

## Product Constitution Review
- Reduces follow-up friction: Yes.
- User remains in control of WhatsApp Send: Yes.
- Local-first MVP preserved: Yes.
- No backend/API introduced: Yes.
- No CRM, scheduling, analytics, or contact-management implementation: Yes.
- Hebrew/RTL flow preserved: Yes.

## Permission and Manifest Review
- `POST_NOTIFICATIONS`: Added because targetSdk is 34 and Android 13+ requires runtime notification permission.
- `SYSTEM_ALERT_WINDOW`: Not added.
- `USE_FULL_SCREEN_INTENT`: Not added.
- Full-screen notification intent: Not used.
- AccessibilityService: Not added.
- Manifest/Gradle diff: Manifest-only, one permission line. No Gradle diff.

## Notification Review
- Channel created for Android 8+: Yes.
- Hebrew title/body: Yes.
- Stable request code: Yes, `8001`.
- Safe PendingIntent flags: Yes, `FLAG_UPDATE_CURRENT` and `FLAG_IMMUTABLE`.
- Opens existing send/follow-up screen on tap: Yes.
- Does not start Activity directly from background: Yes, only PendingIntent on user tap.
- If permission denied: UI shows status and manual send flow remains usable.

## State Review
- Phone/name preservation: Yes where available from current composer state via PendingIntent extras.
- Selected template preservation: Yes where available by template id.
- Missing phone/name fallback: Opens composer with empty values and no crash path.
- My Details reused for existing profile/template rendering: Yes.
- My Details screen rebuilt or redesigned: No.

## WhatsApp Flow Review
- Existing `wa.me` / `ACTION_VIEW` path preserved: Yes.
- User must press Send inside WhatsApp: Yes.
- Share/copy fallback preserved: Yes.
- No automatic WhatsApp sending: Yes.

## Tests and Validation
- Unit tests: PASS via `.\gradlew.bat test`.
- Build: PASS via `.\gradlew.bat assembleDebug`.
- Manifest/Gradle diff: PASS, only `POST_NOTIFICATIONS` in `app/src/main/AndroidManifest.xml`.
- Forbidden app-code grep: PASS, no matches in `app`.
- Broad `app tasks` grep: PASS WITH NOTES, matches are task-doc references from previous sprints.
- Manual notification smoke: PASS by human-reported real Android phone test on 2026-05-20.

## Required Follow-Up
- Keep real call detection deferred for a separately approved sprint with explicit permission/service design.

## Final Decision
PASS for Sprint 8 notification follow-up card closeout.

The human reported that the real Android phone manual notification smoke test was performed and passed. This review does not claim anything beyond that report and the existing source/build evidence. Real call detection, Snooze, log/history, export, contacts, call-log reading, backend, AI, analytics, CRM, and any other new scope remain outside Sprint 8.
