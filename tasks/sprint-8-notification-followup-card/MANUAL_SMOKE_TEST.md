# MANUAL SMOKE TEST: Sprint 8 Notification Follow-Up Pop-Out Card

Status: PASS
Date: 2026-05-20

## Scope
Verify the manual notification smoke path only. Real call detection is deferred.

## Test Checklist
- [x] Fresh install / first open.
- [x] On Android 13+, grant notification permission and confirm the app does not crash.
- [x] On Android 13+, deny notification permission and confirm the app shows a clear status and the manual send screen remains usable.
- [x] Enter phone and name in the existing send screen.
- [x] Select a template.
- [x] Tap `×‘×“×™×§×ª ×”×ª×¨××ª ×¤×•×œ×•××¤`.
- [x] Verify the notification appears in Hebrew.
- [x] Tap the notification.
- [x] Verify the existing follow-up/send screen opens.
- [x] Verify phone/name/template are preserved where available.
- [x] Clear phone/name, trigger the notification again, tap it, and verify the screen opens cleanly with empty fields.
- [x] Verify the WhatsApp open path still requires the user to press Send inside WhatsApp.
- [x] Verify manual share still works.
- [x] Verify copy message still works.
- [x] Verify My Details still opens, saves, and returns to the send flow.
- [x] Verify no CRM, scheduling, contact-management, backend, analytics, or new lead-management screens appear.

## Expected Result
PASS only after a real Android device or emulator confirms the notification appears and opens the existing composer.

## Current Result
PASS. The human performed the manual notification smoke test on a real Android phone and reported that the feature works.

This PASS is limited to the Sprint 8 manual notification smoke path described above. It does not claim real call detection, Snooze, log/history, export, contacts, call-log reading, backend, AI, analytics, CRM, or any other new scope.
