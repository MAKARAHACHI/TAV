# MANUAL SMOKE TEST: Sprint 5 Property Links / Active Property

**Status**: NOT RUN
**Device**: Not tested
**Tester**: Not tested
**Date**: Not tested

## Scope
Verify that "הפרטים שלי" supports up to 3 local property names and links, one active property can be selected and saved, and existing message rendering can use `{property_name}` and `{property_link}` from the active property.

## Preconditions
- Sprint 5 implementation has been completed after human approval.
- App is installed on a real Android phone.
- Existing manual composer and My Details screens are reachable.
- No new permissions, Manifest changes, Gradle changes, backend/API, contacts, WhatsApp automation, CRM, analytics, property database, image upload, or lead-management features were added.

## Checks
- [ ] App opens without crash.
- [ ] "הפרטים שלי" is reachable.
- [ ] Three property name fields are visible.
- [ ] Three property link fields are visible.
- [ ] Active property selection is visible and obvious.
- [ ] Property 1 name and link can be edited.
- [ ] Property 2 name and link can be edited.
- [ ] Property 3 name and link can be edited.
- [ ] Active property can be changed to property 1.
- [ ] Active property can be changed to property 2.
- [ ] Active property can be changed to property 3.
- [ ] Save persists all property fields.
- [ ] Save persists `active_property_index`.
- [ ] Values remain after leaving and reopening "הפרטים שלי".
- [ ] Values remain after app restart.
- [ ] A message containing `{property_name}` renders the active property name.
- [ ] A message containing `{property_link}` renders the active property link.
- [ ] Changing the active property changes rendered `{property_name}` and `{property_link}`.
- [ ] Empty property fields do not crash the app.
- [ ] No real-estate link metadata is fetched.
- [ ] No property search UI appears.
- [ ] No property database UI appears.
- [ ] No contact-reading prompt appears.
- [ ] No new permission prompt appears.
- [ ] Open WhatsApp uses the rendered active-property message.
- [ ] WhatsApp still requires the user to press Send manually.
- [ ] Manual share opens the Android chooser with the same rendered message.
- [ ] Copy message places the same rendered message on the clipboard.
- [ ] Hebrew RTL layout remains readable with property names and LTR links.

## Source And Build Evidence
- `.\gradlew.bat test assembleDebug`: PASS after rerun with access to the Gradle wrapper lock.
- Unit test for active property tag rendering: PASS as part of `.\gradlew.bat test assembleDebug`.
- Manifest and Gradle diff check: PASS; no output, unchanged.
- Forbidden-scope grep: PASS; no matches in app/build files.
- Manual phone smoke: NOT RUN.

## Result
NOT RUN.

Do not change this to PASS unless a real Android phone smoke test was actually performed and the exact result is recorded.
