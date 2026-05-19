# PLAN: Sprint 5 Property Links / Active Property

**Status**: Draft
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Low
**Generated**: 2026-05-19

## Goal Statement
Add local-only support for up to 3 property names and links in "הפרטים שלי", let the agent choose one active property, and later render `{property_name}` and `{property_link}` from that active property in existing message templates.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This sprint is a local settings and template-rendering extension only. It uses `SharedPreferences`, keeps WhatsApp user-driven, and excludes backend/API, contacts, permissions, WhatsApp automation, CRM, analytics, property database, image upload, and lead-management scope.

## Non-Goals
- Do not implement app source in this planning turn.
- Do not add backend/API, cloud sync, user accounts, CRM, analytics, lead management, property database, property search, metadata fetching, image upload, or URL preview work.
- Do not read contacts, call logs, SMS, notifications, WhatsApp state, installed packages, device profile data, or browser history.
- Do not add permissions, package queries, Manifest changes, Gradle changes, dependencies, Room entities, migrations, WorkManager, Foreground Service, notifications, setup wizard, or post-call detection work.
- Do not build multiple template management.
- Do not validate real-estate links beyond simple non-empty text handling.
- Do not detect whether WhatsApp exists.
- Do not auto-send WhatsApp messages or automate the WhatsApp UI.
- Do not change send behavior except routing existing template rendering through active-property values after approval.
- Do not mark manual smoke PASS without explicit real-phone evidence.

## Assumptions
- This is a plan-only step; implementation must wait for explicit human approval.
- Sprint 3 `MyDetailsStore` remains the right storage owner because the requested fields are small local settings.
- `active_property_index` can be stored as an integer-like preference value. Implementation should define a clear value range, such as `1`, `2`, `3`, or `0` for no active property, and document that choice in code/tests.
- Empty property names and links are allowed while editing. Rendering should only use the selected active property values; if the active slot is empty, `{property_name}` and `{property_link}` may render as empty strings or remain visibly unresolved based on the current renderer convention approved during implementation.
- Existing message templates remain editable before WhatsApp opens.

## Required Planned Fields
- `property_1_name`
- `property_1_link`
- `property_2_name`
- `property_2_link`
- `property_3_name`
- `property_3_link`
- `active_property_index`

## New Template Tags
- `{property_name}`
- `{property_link}`

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `context/PROJECT.md`
- `context/ARCHITECTURE.md`
- `context/DATA_CONTRACTS.md`
- `context/TEMPLATE_ENGINE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/DO_NOT_BUILD.md`
- `tasks/sprint-3-my-details/PLAN.md`
- `tasks/sprint-3-my-details/REVIEW.md`
- `tasks/sprint-4-template-tags/PLAN.md`
- `tasks/sprint-4-template-tags/REVIEW.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`

## Files Expected To Change After Human Approval
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- `tasks/sprint-5-property-links/EXECUTION_LOG.md`
- `tasks/sprint-5-property-links/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-5-property-links/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `context/*`
- Prior sprint task folders unless only referenced by review evidence.
- Any Room, WorkManager, call-state, contact-reading, backend/API, CRM, analytics, property database, image upload, lead-management, history/log, export, Snooze, notification, or WhatsApp automation files.

## Sprint 1: Local Active Property Links
**Goal**: Extend the local My Details profile with up to 3 property names/links, let the user choose one active property, and render property tags from that selected slot.
**Demo / Validation**: The agent opens "הפרטים שלי", edits up to 3 property names and links, chooses the active property, saves, returns to the manual composer, and a message containing `{property_name}` and `{property_link}` uses the active property's saved values for WhatsApp, manual share, and copy.
**Stop condition**: Stop if implementation requires permissions, Manifest/Gradle changes, backend/API, contacts, WhatsApp detection, auto-send, CRM, analytics, property database, property search, URL metadata fetching, image upload, lead management, Room migration, or multiple template management.

### Task 1.1: Extend Local Profile Model With Property Fields
- Location: `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- Description: Add exactly the seven planned fields for 3 local property name/link pairs and the active property index.
- Dependencies: Existing Sprint 3 profile model.
- Acceptance criteria: Model contains `property_1_name`, `property_1_link`, `property_2_name`, `property_2_link`, `property_3_name`, `property_3_link`, and `active_property_index` equivalents; no CRM, lead, property database, image, metadata, URL-fetching, contact, permission, backend/API, analytics, or WhatsApp state is added.
- Validation command or manual check: Source review of the model file.
- Rollback: Remove the added property fields from `MyDetailsProfile.kt`.

### Task 1.2: Persist Property Fields In SharedPreferences
- Location: `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- Description: Save and load all seven property fields with stable preference keys using existing `SharedPreferences`.
- Dependencies: Task 1.1.
- Acceptance criteria: Storage remains local-only `SharedPreferences`; keys match the required planned fields; app handles missing older preference values with safe defaults; no Room, DataStore dependency, backend/API, network, file upload, or migration is introduced.
- Validation command or manual check: Source review confirms no storage mechanism other than existing `SharedPreferences`.
- Rollback: Remove the new preference keys and load/save lines.

### Task 1.3: Add Obvious My Details UX For 3 Properties
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a compact section in "הפרטים שלי" where the agent can edit up to 3 property names and links, choose one active property, and save.
- Dependencies: Tasks 1.1 and 1.2.
- Acceptance criteria: The screen makes the flow obvious without explanation: edit up to 3 property names/links, choose active property, save; empty fields are allowed; active selection is visible; no property search, URL metadata fetch, image upload, database, CRM, analytics, or lead-management UI appears.
- Validation command or manual check: Real-phone visual check for Hebrew RTL readability and source review.
- Rollback: Remove the property section UI and active-property state wiring.

### Task 1.4: Extend Template Tag Values And Renderer
- Location: `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- Description: Add active-property support for `{property_name}` and `{property_link}` while preserving existing tag behavior.
- Dependencies: Tasks 1.1 and 1.2.
- Acceptance criteria: Existing Sprint 4 tags still render; `{property_name}` and `{property_link}` render from the selected active property only; repeated tags render; unknown tags remain visible; renderer stays pure and does not read Android storage, intents, contacts, network, or files.
- Validation command or manual check: Unit tests for active property rendering behavior.
- Rollback: Remove property fields from renderer values and replacement map.

### Task 1.5: Use One Active-Property Rendered Message For All Existing Actions
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Load the saved active property values from local profile state and pass them into the existing rendered-message path for Open WhatsApp, manual share, and copy.
- Dependencies: Tasks 1.1 through 1.4.
- Acceptance criteria: WhatsApp, manual share, and copy continue to use the same rendered message value; user still presses Send inside WhatsApp; blank-message validation remains; send behavior is otherwise unchanged; no WhatsApp detection or automation is added.
- Validation command or manual check: Source review plus real-phone smoke after approval.
- Rollback: Remove active-property values from the render context.

### Task 1.6: Add Unit Tests For Active Property Tags
- Location: `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- Description: Extend renderer tests to cover `{property_name}` and `{property_link}`.
- Dependencies: Task 1.4.
- Acceptance criteria: Tests cover active property name/link rendering, repeated property tags, empty active property values, unknown tags, and preservation of existing Sprint 4 tag behavior.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Remove the added test cases.

### Task 1.7: Preserve Scope And Update Evidence Docs
- Location: Whole repo plus `tasks/sprint-5-property-links/`
- Description: After approved implementation, update execution/review/manual-smoke artifacts with exact evidence. Do not mark manual smoke PASS unless a real Android phone was used.
- Dependencies: Completed implementation and validation.
- Acceptance criteria: Manifest/Gradle unchanged; forbidden-scope grep clean or explained; `EXECUTION_LOG.md` records commands and files; `REVIEW.md` states PASS, PASS WITH NOTES, or FIX_REQUIRED based on evidence; `MANUAL_SMOKE_TEST.md` truthfully records NOT RUN or real-phone result.
- Validation command or manual check: Read all Sprint 5 docs after editing.
- Rollback: Revert incorrect Sprint 5 docs only.

## Testing Strategy
- Unit tests: `.\gradlew.bat test` for active property tag rendering behavior.
- Build: `.\gradlew.bat test assembleDebug`.
- Instrumented tests: Not required for this small local settings/template change unless implementation introduces Android-specific logic that cannot be reviewed or covered locally.
- Manual QA: Required on a real Android phone before marking `MANUAL_SMOKE_TEST.md` as PASS.
- Device/OEM checks: Minimum one real Android phone for this sprint; broader Pixel/Samsung/Xiaomi QA remains later.
- Persistence checks:
  - Save all 3 property names and links.
  - Choose property 1, save, restart app, confirm selection and values remain.
  - Choose property 2 or 3, save, return to manual composer, confirm tags render from that active slot.
  - Clear an active property link, save, confirm no crash and simple empty handling.
- Outgoing action checks:
  - Open WhatsApp uses active property tags in the `wa.me` text.
  - Manual share uses the same rendered message.
  - Copy message places the same rendered message on the clipboard.
  - WhatsApp still requires the user to press Send manually.
- Forbidden-scope grep:

```powershell
rg -n "uses-permission|READ_CALL_LOG|READ_CONTACTS|READ_PHONE_STATE|POST_NOTIFICATIONS|FOREGROUND_SERVICE|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|WRITE_CALL_LOG|SEND_SMS|READ_SMS|WorkManager|Room|CallLog|TelephonyCallback|NotificationListenerService|AccessibilityService|auto-send|autosend|CRM|backend|server|analytics|metadata|image upload|lead management|property database|property search" app build.gradle.kts settings.gradle.kts gradle.properties
```

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; `AndroidManifest.xml` must not change.
- User disclosure required: None for Sprint 5 because the data is local manual profile input only.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: Up to 3 property names and 3 property links plus active index are stored locally until the user edits or clears them.
- Storage mechanism: Existing `SharedPreferences` only.
- Backend/API: None.
- Contacts: None.
- Property database: None.

## UX Impact
- Screens affected: Existing "הפרטים שלי" screen and existing manual composer rendering path.
- RTL/Hebrew checks: Property labels, active selection, URLs, and rendered message preview must remain readable with Hebrew and LTR links.
- Empty/fallback states: Empty property fields are allowed; active property with empty values must not crash.
- Error states: Save should not crash on Hebrew text, long text, pasted URLs, or empty strings. No URL metadata fetch or deep validation should run.

## Rollback Plan
Revert `MainActivity.kt`, remove property fields from `MyDetailsProfile.kt` and `MyDetailsStore.kt`, remove property tag support from `TemplateTagRenderer.kt`, remove added renderer tests, and update only Sprint 5 docs to record the rollback. Do not touch Manifest, Gradle files, context docs, prior sprint folders, or unrelated app features.

## Review Checklist
- AndroidManifest is clean.
- Gradle files are unchanged.
- No new permissions or package queries.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Manual share and copy behavior remain user-driven.
- Local storage only; `SharedPreferences` only.
- No backend/API/cloud sync.
- No contacts.
- No CRM.
- No analytics.
- No property database.
- No image upload.
- No lead management.
- No property search.
- No URL metadata fetch.
- No WhatsApp existence/error detection.
- Existing Sprint 4 tags still render.
- `{property_name}` and `{property_link}` render from the active property.
- Manual smoke is not marked PASS without real-phone evidence.

## Agent Handoff
- Planning model: GPT-5.5 High.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sprint only, after explicit human approval.
- Expansion rule: no scope expansion without human approval.
- Approval gate: Stop here until the human approves this `PLAN.md`.

## Potential Gotchas
- Android background restrictions: Not touched; do not add background work.
- notification permission denial: Not touched; do not add notification behavior.
- READ_CALL_LOG denial fallback: Existing manual composer must keep working without call permissions.
- OEM battery killing FGS: Not touched; do not add Foreground Service work.
- duplicate reminders: Not touched; do not add reminders or Snooze.
- invalid phone formatting for wa.me: Property links are free text and must not alter phone normalization.
- RTL text and mixed Hebrew/phone-number/link layout: Active property names and URLs may mix Hebrew and LTR text; verify on a real phone before claiming manual PASS.
- Room migration risk: Avoided by using existing `SharedPreferences`.
- direct-APK update/install friction: Not touched; no distribution changes.
- Empty active slot: Renderer and UX must handle a selected but empty property without crashing or hiding send controls.
