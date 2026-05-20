# MANUAL SMOKE TEST: Sprint 7 Manual Smoke Fixes

**Status**: PASS
**Last updated**: 2026-05-19

## Scope
Verify only the Sprint 7 usability repairs from the Sprint 6 manual smoke: Hebrew tag labels, cursor-aware tag insertion, and preserved WhatsApp send-screen client name/phone after screen navigation.

## Real Phone Checklist
- [x] Open the app on a real Android phone.
- [x] Open template management.
- [x] Confirm supported tag buttons show Hebrew labels, not raw English placeholder names.
- [x] Confirm tapping "שם הסוכן" inserts `{agent_name}` into the template text.
- [x] Confirm tapping "טלפון הסוכן" inserts the internal agent-phone tag into the template text.
- [x] Confirm tapping "שם הלקוח" inserts the internal client-name tag into the template text.
- [x] Confirm tapping "שם הנכס" inserts `{property_name}` into the template text.
- [x] Confirm tapping "קישור לנכס" inserts `{property_link}` into the template text.
- [x] Put the cursor in the middle of template text, insert a tag, and confirm it is inserted at the cursor.
- [x] Select existing template text, insert a tag, and confirm the selected text is replaced.
- [x] Confirm the cursor moves immediately after the inserted tag.
- [x] In the WhatsApp send screen, enter client phone and name.
- [x] Navigate to another local screen and return to the WhatsApp send screen.
- [x] Confirm the entered client phone and name are still visible.
- [ ] Confirm Open WhatsApp still uses `wa.me` and still requires the user to press Send manually inside WhatsApp.
- [ ] Confirm no new permission prompt appears.

## Automated Evidence
- `TemplateTagsTest`: covers supported keys, Hebrew labels, insertion at cursor, selected text replacement, and cursor position.
- Existing `TemplateTagRendererTest`: renderer behavior remains unchanged.

## User-Reported Evidence
Manual phone smoke PASS was reported by the user on 2026-05-19 for the Sprint 7 fixes:
- Template tag labels are shown in Hebrew.
- Adding a tag inserts it at the current cursor position in the template text.
- If text is selected, inserting a tag replaces the selected text.
- After entering phone number and name on the WhatsApp send screen, navigating forward/back preserves the entered phone/name state.

## Notes
The Open WhatsApp/no-new-permission checks were not explicitly included in the user-reported Sprint 7 PASS evidence and remain unchecked here.
