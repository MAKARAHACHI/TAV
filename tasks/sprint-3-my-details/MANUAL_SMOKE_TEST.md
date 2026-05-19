# MANUAL SMOKE TEST: Sprint 3 My Details

Status: NOT RUN

Manual smoke must not be marked PASS unless tested on a real Android phone.

## Current Evidence
- Source/build validation completed on 2026-05-19.
- Real Android phone smoke was not run in this Codex session.
- Final manual result remains NOT RUN.

## Preconditions
- Sprint 3 implementation has explicit human approval.
- The app builds successfully.
- A real Android phone is available.

## Real Phone Checklist
- [ ] App opens on a real Android phone.
- [ ] Existing manual composer is still reachable.
- [ ] My Details screen is reachable from the app.
- [ ] UI is RTL-friendly and readable.
- [ ] `agent_name` field is visible and editable.
- [ ] `office_name` field is visible and editable.
- [ ] `phone` field is visible and editable.
- [ ] `website` field is visible and editable.
- [ ] `business_card` field is visible and editable.
- [ ] `signature` field is visible and editable.
- [ ] Saving all six fields shows clear feedback.
- [ ] Leaving My Details and returning shows the saved values.
- [ ] Closing and reopening the app shows the saved values.
- [ ] Clearing a field and saving keeps it empty after reopening.
- [ ] Hebrew text, phone number text, and URL text remain readable.
- [ ] Existing `Open WhatsApp` action still requires user to press Send manually.
- [ ] Existing manual share action still opens the Android share sheet.
- [ ] Existing copy action still copies only the composed message text.
- [ ] Existing reset action still restores the selected template message and does not clear the phone field.

## Forbidden Behavior Checklist
- [ ] No Android permission prompt appears.
- [ ] No contacts permission prompt appears.
- [ ] No call-log permission prompt appears.
- [ ] No notification permission prompt appears.
- [ ] No SMS permission prompt appears.
- [ ] No WhatsApp detection or WhatsApp-number existence check appears.
- [ ] No auto-send behavior occurs.
- [ ] No CRM, backend/API, property, export, log/history, Snooze, reminder, or template-management behavior appears.

## Final Result
- Result: NOT RUN
- Tested by:
- Device:
- Android version:
- Date:
- Notes: Not tested on a real Android phone in this Codex session.
