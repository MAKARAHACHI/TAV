# MANUAL SMOKE TEST: Sprint 10 Post-Call Decision Screen

Status: PASS  
Date: 2026-05-20

## Scope
Sprint 10 adds the first real post-call decision screen. A follow-up notification tap should open `מה קרה בשיחה?`, show the four MVP cards, and let each selected card continue into the existing manual WhatsApp composer.

Manual smoke was reported by the human as PASS on a real Android phone.

## Required Device Checks
- Open the app from the launcher and confirm the manual composer is still usable.
- Tap the in-app `מה קרה?` entry and confirm the screen title is `מה קרה בשיחה?`.
- Confirm exactly four cards appear, in this order:
  1. `שיחה קרה נפתחה` / `בעל נכס הסכים להמשך`
  2. `שיחה קרה נסגרה בנימוס` / `להשאיר דלת פתוחה`
  3. `קונה מתעניין` / `לשלוח פרטי נכס`
  4. `צריך לחזור אליו` / `רק תזכורת — בלי הודעה`
- With no phone/name context, confirm the fallback line says the user can continue manually.
- Trigger the Sprint 8 manual notification self-test, tap the notification, and confirm it opens the decision screen.
- Select each of the four cards and confirm it routes to the existing manual composer with an editable seeded message.
- Confirm phone/name context from the notification path is preserved if entered before triggering the test notification.
- Confirm Open WhatsApp still opens a user-driven WhatsApp/wa.me flow and does not send automatically.
- Confirm `שיתוף ידני` still opens the Android share sheet.
- Confirm `העתק הודעה` still copies the message.
- Confirm the Sprint 9 call-detection toggle/status still displays valid Hebrew and remains usable.
- If a real Sprint 9 call-detection notification is available, tap it and confirm it opens the decision screen.
- Deny notification permission if testing on Android 13+ and confirm manual composer entry still works.

## Must Not Claim
- Do not claim WhatsApp messages were sent or delivered.
- Do not claim CallLog number detection or contact lookup.
- Do not claim Snooze, reminders, card-specific forms, or CRM/history behavior.
- Do not mark this checklist PASS without explicit real Android phone evidence from the human.

## Result
- Manual smoke: PASS, human-reported real Android phone test.
- Device/OEM QA: PARTIAL; validated on the human's test phone only.
- Notes: Human reported the post-call notification and decision-card flow working. This does not claim WhatsApp delivery, CallLog auto-fill for Sprint 10, Snooze, reminders, CRM/history behavior, or broader OEM coverage.
