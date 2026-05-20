# MANUAL SMOKE TEST: Sprint 6 Message Templates Management

**Status**: NOT RUN
**Device**: Not tested
**Tester**: Not tested
**Date**: Not tested

## Scope
Verify that local-only editable message templates can be viewed, edited, saved, tagged, previewed, and used through the existing copy/share/Open WhatsApp flows without changing My Details, active property links, permissions, Manifest, Gradle, backend/API, auth, CRM, automation, scheduled sending, or contact database scope.

## Preconditions
- Sprint 6 implementation has been completed after human approval.
- App is installed on a real Android phone.
- Existing manual composer, My Details, and Sprint 5 active property behavior are available.
- No new permissions, Manifest changes, Gradle changes, backend/API, auth, CRM, contacts/contact database, scheduled sending, or WhatsApp automation features were added.

## Checks
- [ ] App opens without crash.
- [ ] Manual composer is still reachable.
- [ ] My Details is still reachable.
- [ ] Template management screen is reachable.
- [ ] Existing built-in templates are visible.
- [ ] A built-in template can be selected for editing.
- [ ] Template text can be edited.
- [ ] Supported tag controls are visible.
- [ ] `{lead_name}` can be inserted.
- [ ] `{agent_name}` can be inserted.
- [ ] `{office_name}` can be inserted.
- [ ] `{phone}` can be inserted.
- [ ] `{website}` can be inserted.
- [ ] `{business_card}` can be inserted.
- [ ] `{signature}` can be inserted.
- [ ] `{property_name}` can be inserted.
- [ ] `{property_link}` can be inserted.
- [ ] Edited template text can be saved locally.
- [ ] Saved template text remains after leaving and reopening template management.
- [ ] Saved template text remains after app restart.
- [ ] Built-in fallback/reset works.
- [ ] Unknown manually typed tags remain visible.
- [ ] Preview renders Sprint 3 My Details tags from saved local details.
- [ ] Preview renders Sprint 5 `{property_name}` from the active property.
- [ ] Preview renders Sprint 5 `{property_link}` from the active property.
- [ ] Empty My Details or property fields do not crash preview.
- [ ] Manual composer uses the saved template text.
- [ ] Reset in the composer restores the selected saved template text or built-in fallback.
- [ ] Blank rendered output is blocked by existing validation before copy/share/Open WhatsApp.
- [ ] Copy message uses the rendered saved template text.
- [ ] Manual share opens the Android chooser with the same rendered saved template text.
- [ ] Open WhatsApp uses the same rendered saved template text.
- [ ] WhatsApp still requires the user to press Send manually.
- [ ] No contact-reading prompt appears.
- [ ] No new permission prompt appears.
- [ ] No backend/API, auth, CRM, scheduled-send, contact database, or automation UI appears.
- [ ] Hebrew RTL layout remains readable with long text, tags, phone numbers, URLs, and multiline template content.

## Source And Build Evidence
- `.\gradlew.bat test assembleDebug`: PASS after rerun with elevated access to the Gradle wrapper lock.
- Template store unit tests: PASS as part of `.\gradlew.bat test assembleDebug`.
- Renderer regression tests: PASS as part of existing `TemplateTagRendererTest` coverage in `.\gradlew.bat test assembleDebug`.
- Manifest and Gradle diff check: PASS; no output, unchanged.
- Forbidden-scope grep: PASS; no matches in app/build files.
- Manual phone smoke: NOT RUN.

## Result
NOT RUN.

Do not change this to PASS unless a real Android phone smoke test was actually performed and the exact result is recorded.
