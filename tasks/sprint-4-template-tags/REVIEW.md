# REVIEW: Sprint 4 Template Tags

**Decision**: PASS WITH NOTES
**Reviewed at**: 2026-05-19

## Scope Review
- Matches PLAN.md: Yes
- Scope expanded: No
- Files changed match expected list: Yes
- Unexpected files: None
- One sprint only: Yes

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes
- Local-first MVP preserved: Yes
- No backend/API introduced: Yes
- Hebrew RTL preserved where applicable: Yes by source review; real-phone visual check still required.
- Snooze remains Core: Not applicable. Sprint 4 does not implement or weaken Snooze.
- User-controlled WhatsApp send preserved: Yes

## Permission and Manifest Review
- AndroidManifest changed: No
- Gradle dependencies changed: No
- New permissions added: No
- No AccessibilityService: Yes
- No auto-send WhatsApp automation: Yes
- No QUERY_ALL_PACKAGES: Yes
- No SMS permissions: Yes
- No WRITE_CALL_LOG: Yes
- READ_CALL_LOG only if planned and disclosed: Not applicable; not added.
- READ_CONTACTS only if planned and disclosed: Not applicable; not added.
- POST_NOTIFICATIONS handled: Not applicable; not added.
- FOREGROUND_SERVICE_PHONE_CALL handled: Not applicable; not added.

## Template Tags Review
- `{lead_name}` renders from manual input: Yes by source review and unit test.
- `{agent_name}` renders from local profile: Yes by source review and unit test.
- `{office_name}` renders from local profile: Yes by source review and unit test.
- `{phone}` renders from local profile: Yes by source review and unit test.
- `{website}` renders from local profile: Yes by source review and unit test.
- `{business_card}` renders from local profile: Yes by source review and unit test.
- `{signature}` renders from local profile: Yes by source review and unit test.
- Unknown tags remain visible: Yes by unit test.
- Repeated tags render: Yes by unit test.
- Empty values are safe: Yes by unit test.
- Renderer is local/pure and does not touch intents/storage directly: Yes.

## Outgoing Action Review
- Open WhatsApp uses rendered message: Yes by source review.
- Manual share uses rendered message: Yes by source review.
- Copy message uses rendered message: Yes by source review.
- All three actions use the same rendered value: Yes, `renderedMessage`.
- User must press Send in WhatsApp: Yes; existing `ACTION_VIEW` flow preserved.
- Manual share remains `ACTION_SEND` text/plain with chooser: Yes.
- Clipboard copies only message text: Yes.
- WhatsApp detection added: No.
- Auto-send behavior added: No.

## Forbidden Scope Review
- CRM: Not added.
- Backend/API: Not added.
- New database/Room migration: Not added.
- Contact reading: Not added.
- Call log reading: Not added.
- Properties: Not added.
- Snooze: Not added.
- Post-call detection: Not added.
- Lead statuses/history/export/analytics: Not added.
- Manifest/Gradle changes: Not added.
- AI: Not added.

## Tests and Validation
- Unit tests: PASS via `.\gradlew.bat test assembleDebug`.
- Build: PASS via `.\gradlew.bat test assembleDebug`.
- Required Gradle evidence: First sandbox run failed due Gradle wrapper lock access; elevated rerun ended with `BUILD SUCCESSFUL in 14s`.
- Required app diff: PASS; app-only git diffs reviewed for `MainActivity.kt`, `TemplateTagRenderer.kt`, and `TemplateTagRendererTest.kt`.
- Manifest/Gradle diff: PASS; no output.
- Forbidden-scope grep: PASS WITH NOTES; matches were limited to the unknown-tag test fixture `{property_link}`, pre-existing `android:exported`, and pre-existing `buyer_property_details`.
- Manual QA: NOT RUN.
- Device/OEM QA: NOT RUN.
- Evidence missing: Real Android phone smoke for RTL layout, profile reload after app restart, and actual WhatsApp/share/copy behavior.

## Blockers
- None for source/build completion.

## Required fixes before merge
- None from source/build review.
- Real-phone smoke remains required before claiming manual PASS.
