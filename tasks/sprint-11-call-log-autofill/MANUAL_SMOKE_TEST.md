# MANUAL SMOKE TEST: Sprint 11 CallLog Auto-Fill

Status: PASS  
Date: 2026-05-20

## Scope
Sprint 11 adds CallLog-based phone auto-fill after a detected cellular call ends, plus optional first-name auto-fill from contacts when `READ_CONTACTS` is granted. The notification remains user-tap driven and the WhatsApp send remains manual inside WhatsApp.

Manual smoke was reported by the human as PASS on a real Android phone.

## Required Device Checks
- Grant `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, and notifications, then enable automatic call detection.
- Complete an incoming answered cellular call. Confirm a follow-up notification appears after the call ends.
- Tap the notification and confirm `מה קרה בשיחה?` opens with the phone number visible.
- Select each relevant card and confirm the existing composer opens with the phone field prefilled.
- Complete an outgoing answered cellular call. Confirm the same notification tap -> decision screen -> composer path preserves the phone.
- Complete an outgoing unavailable or zero-duration call if the device records it in CallLog. Confirm recent outgoing zero-duration rows can prefill the phone.
- Deny or revoke `READ_CALL_LOG`. Confirm call detection still posts the follow-up notification and the decision/composer path stays usable with manual phone entry.
- Deny or revoke `READ_CONTACTS` while keeping `READ_CALL_LOG`. Confirm the phone is filled and the name remains manual.
- With a saved contact match and `READ_CONTACTS` granted, confirm the first name is prefilled and can still be edited.
- Trigger the Sprint 8 manual notification self-test and confirm it still opens the decision screen.
- Confirm `פתח WhatsApp` still opens WhatsApp/wa.me and does not send automatically.
- Confirm `שיתוף ידני` still opens the Android share sheet.
- Confirm `העתק הודעה` still copies the message.
- Confirm no call history, contact name, raw phone number, or full message text is added to the Follow-Up Action Log.

## Must Not Claim
- Do not claim WhatsApp messages were sent or delivered.
- Do not claim `READ_CONTACTS` is required for the core phone auto-fill flow.
- Do not claim CallLog or contact data is stored.
- Do not claim Snooze, reminders, full card forms, CRM/history UI, backend/API, or WhatsApp automation.
- Do not mark this checklist PASS without explicit real Android phone evidence from the human.

## Result
- Manual smoke: PASS, human-reported real Android phone test.
- Device/OEM QA: PARTIAL; validated on the human's test phone only.
- Notes: Human reported that the post-call flow works, including automatic phone-number fill after call and first-name fill from contacts when permission/data are available. This does not claim WhatsApp delivery, stored CallLog/contact history, Snooze, reminders, CRM/history UI, backend/API, or broad OEM coverage.
