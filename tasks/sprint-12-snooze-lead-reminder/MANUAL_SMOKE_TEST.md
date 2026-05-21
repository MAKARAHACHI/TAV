# MANUAL SMOKE TEST: Sprint 12 Snooze + Lead Reminder

**Status**: PARTIAL PASS BY HUMAN REPORT
**Last updated**: 2026-05-21

## Human-Reported PASS
- Full snooze loop through Sub-sprint 4: user reported the app works after testing.
- Covered by report: snooze entry point, reminder scheduling, reminder notification, and returning to the prepared card flow.

## Still NOT RUN After Sub-sprint 5
- Tap `שמור כליד`.
- Confirm Hebrew success message.
- Close and reopen the app.
- Confirm the saved lead persists locally after restart.
- Confirm save lead does not block WhatsApp, share, copy, reset, or snooze.

## Regression Checklist
- App opens normally.
- Manual composer still works.
- Post-call notification still opens the card flow.
- Phone/name auto-fill from Sprint 11 still works.
- Four post-call cards still route to the composer.
- `הזכר לי אחר כך` still schedules a reminder.
- Reminder tap restores the prepared task.
- No WhatsApp auto-send; user still presses Send manually.

## Notes
- No backend/API, analytics, CRM screen, lead list, or lead search was added.
- Manual lead persistence remains pending for full Gate 5 PASS.
