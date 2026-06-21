# MANUAL SMOKE TEST: Sprint 14 Lead Pipeline

**Status**: PASS WITH NOTES
**Updated**: 2026-05-25

## Scope
- Post-call notification tap opens the in-app decision card.
- Decision card actions: WhatsApp, snooze, save/track, close, edit message.
- Reminder notification restores the same saved task/card.
- Manual composer, share, copy, templates, setup wizard, and self-test still work.

## Required Real-Device Checks
- Call ends, notification appears, user taps notification, decision card opens.
- Empty-phone fallback opens and allows manual number entry through edit-message flow.
- WhatsApp action opens WhatsApp/compatible browser with prepared message; user must press Send manually.
- Snooze schedules reminder; reminder notification restores the same task/card.
- Resnooze replaces the same task reminder rather than stacking a duplicate.
- Save/track preserves phone/name locally.
- Close cancels future reminders and does not reopen the task.
- Notification-denied path is explained and does not crash.

## Current Evidence
- JVM tests: PASS.
- Debug build: PASS.
- Source review: PASS WITH NOTES.
- Real Android phone smoke: PASS WITH NOTES, human-reported on 2026-05-25: "בדקתי בטלפון ועובד תוצאות הספרינט 14".

## Notes
- PASS is limited to the human-reported Sprint 14 phone test. Codex did not observe the device session.
- No per-step evidence was provided for each checklist item, so keep this as PASS WITH NOTES rather than an unconditional PASS.
- Emulator/source validation is not a replacement for the post-call real-device flow.
