# MANUAL SMOKE TEST: Sprint 9 Call Detection + Action Log

Status: NOT RUN
Date: 2026-05-20

## Scope
Sprint 9 adds a user-enabled call-detection foreground service, posts the existing Sprint 8 follow-up notification after a qualifying cellular call ends, and records only explicit outgoing actions in the local Action Log.

Manual smoke must be run by a human on a real Android phone before this file can be changed to PASS.

## Required Device Checks
- Toggle call detection on from the existing manual composer.
- Confirm the persistent call-detection status notification is visible while enabled.
- Place or receive a real cellular call lasting at least 5 seconds.
- Confirm a follow-up notification appears after the call ends.
- Tap the follow-up notification and confirm the existing composer opens with empty phone/name fields.
- Place or receive a real cellular call shorter than 5 seconds and confirm no follow-up notification is created.
- Toggle call detection off and confirm the persistent status notification is removed.
- Confirm the Sprint 8 manual notification smoke button still posts a notification and routes to the composer.
- Confirm Open WhatsApp, Manual Share, and Copy still work as user-driven actions.
- Confirm Action Log entries are observable by reading SharedPreferences via adb; there is no user-visible history screen in Sprint 9.
- Deny READ_PHONE_STATE and confirm the manual composer remains usable with a clear status message.
- Deny POST_NOTIFICATIONS on Android 13+ and confirm manual mode remains usable.

## Must Not Claim
- Do not mark WhatsApp messages as sent or delivered.
- Do not claim CallLog number detection.
- Do not claim contact-name resolution.
- Do not claim Snooze or post-call cards.
- Do not mark this checklist PASS without explicit real-phone evidence from the human.

## Result
- Manual smoke: NOT RUN.
- Device/OEM QA: NOT RUN.
- Notes: Source, unit-test, and debug-build validation passed. Real-phone validation remains pending.
