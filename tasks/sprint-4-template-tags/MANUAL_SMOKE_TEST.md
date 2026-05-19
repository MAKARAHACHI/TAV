# MANUAL SMOKE TEST: Sprint 4 Template Tags

**Status**: NOT RUN
**Device**: Not tested
**Tester**: Not tested
**Date**: Not tested

## Scope
Verify that supported template tags render from local manual/profile values before every outgoing action: Open WhatsApp, manual share, and copy message.

## Preconditions
- Sprint 4 implementation has been completed.
- App is installed on a real Android phone.
- My Details has saved local values for:
  - `agent_name`
  - `office_name`
  - `phone`
  - `website`
  - `business_card`
  - `signature`
- Manual composer contains a lead name value for `{lead_name}`.

## Checks
- [ ] App opens without crash.
- [ ] Manual composer is reachable.
- [ ] My Details saved values remain local and available after app restart.
- [ ] A message containing `{lead_name}` renders the manually typed lead name.
- [ ] A message containing `{agent_name}` renders the saved agent name.
- [ ] A message containing `{office_name}` renders the saved office name.
- [ ] A message containing `{phone}` renders the saved profile phone.
- [ ] A message containing `{website}` renders the saved website.
- [ ] A message containing `{business_card}` renders the saved business-card text/link.
- [ ] A message containing `{signature}` renders the saved signature.
- [ ] Repeated supported tags render every occurrence.
- [ ] Unknown tags remain visible and are not silently deleted.
- [ ] Empty profile fields do not crash the composer.
- [ ] Open WhatsApp uses the rendered message in the `wa.me` text.
- [ ] WhatsApp still requires the user to press Send manually.
- [ ] Manual share opens the Android chooser with the same rendered message.
- [ ] Copy message places the same rendered message on the clipboard.
- [ ] Reset message restores the raw selected template text.
- [ ] Empty rendered message is blocked before WhatsApp/share/copy.
- [ ] Hebrew RTL layout remains readable with tags, phone numbers, URLs, and multiline signature/business-card text.
- [ ] No contact-reading prompt appears.
- [ ] No new permission prompt appears.
- [ ] No auto-send behavior exists.

## Source And Build Evidence
- `.\gradlew.bat test assembleDebug`: PASS after rerun with access to the Gradle wrapper lock.
- Renderer unit tests cover all supported tags, repeated tags, unknown tags, empty values, and mixed Hebrew/ASCII text.
- Manifest and Gradle diffs: no output; unchanged.
- Manual phone smoke: NOT RUN.

## Result
NOT RUN.

Do not change this to PASS unless a real Android phone smoke test was actually performed and the exact result is recorded.
