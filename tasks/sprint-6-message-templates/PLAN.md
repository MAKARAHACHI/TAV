# PLAN: Sprint 6 Message Templates Management

**Status**: Draft
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Medium
**Generated**: 2026-05-19

## Goal Statement
Add local-only editable message template management so the agent can view existing templates, edit template text, insert supported tags, save locally, preview rendered output, and continue using the existing copy/share/WhatsApp flows.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This sprint improves the local template workflow only. It keeps WhatsApp sending user-driven, reuses the Sprint 4 `TemplateTagRenderer`, reuses Sprint 5 active property tags, preserves Sprint 3 My Details storage/behavior, and avoids backend/API, auth, CRM, automation, scheduled sending, contact database work, Manifest changes, and Gradle changes.

## Non-Goals
- Do not implement app source in this planning turn.
- Do not add backend/API, cloud sync, user accounts, auth, CRM, analytics, campaigns, contact database, lead database, property database, property search, URL metadata fetching, image upload, AI writing, template marketplace, imports, exports, or scheduled sending.
- Do not read contacts, call logs, SMS, notifications, WhatsApp state, installed packages, browser history, or external files.
- Do not add permissions, package queries, Manifest changes, Gradle changes, dependencies, Room entities, migrations, WorkManager, Foreground Service, notifications, setup wizard, post-call detection, or Snooze implementation in this sprint.
- Do not change Sprint 3 My Details fields or behavior except reading existing saved values for preview/rendering.
- Do not change Sprint 5 property-link storage or active-property behavior.
- Do not expand supported tags beyond the current app renderer without a separate approved plan.
- Do not detect whether WhatsApp exists.
- Do not auto-send WhatsApp messages or automate the WhatsApp UI.
- Do not mark manual smoke PASS without explicit real-phone evidence.

## Assumptions
- Implementation must wait for explicit human approval.
- Built-in templates remain available as defaults even if the user edits local copies.
- Local editable template data can use `SharedPreferences` with encoded text because the current app already uses simple local persistence patterns and this sprint is intentionally small.
- The supported tag list for Sprint 6 is the current app renderer list: `{lead_name}`, `{agent_name}`, `{office_name}`, `{phone}`, `{website}`, `{business_card}`, `{signature}`, `{property_name}`, and `{property_link}`.
- Unknown tags must remain visible, matching Sprint 4 renderer behavior.
- Existing copy/share/WhatsApp actions should continue to use one rendered message value.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `context/TEMPLATE_ENGINE.md`
- `context/DATA_CONTRACTS.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/DO_NOT_BUILD.md`
- `tasks/sprint-3-my-details/PLAN.md`
- `tasks/sprint-3-my-details/REVIEW.md`
- `tasks/sprint-4-template-tags/PLAN.md`
- `tasks/sprint-4-template-tags/REVIEW.md`
- `tasks/sprint-5-property-links/PLAN.md`
- `tasks/sprint-5-property-links/REVIEW.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/templates/MessageTemplate.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`

## Files Expected To Change After Human Approval
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/templates/MessageTemplate.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateStoreTest.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- `tasks/sprint-6-message-templates/EXECUTION_LOG.md`
- `tasks/sprint-6-message-templates/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-6-message-templates/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `context/*`
- Prior sprint task folders unless only referenced by review evidence.
- Any Room, WorkManager, call-state, contact-reading, backend/API, auth, CRM, analytics, lead database, contact database, property database, history/log, export, Snooze, notification, scheduled-send, or WhatsApp automation files.

## Sprint 1: Local Editable Message Templates
**Goal**: Let the agent manage editable local template text and use saved templates in the existing manual composer.
**Demo / Validation**: The agent opens template management, views current templates, edits template text, inserts supported tags, saves locally, returns to the composer, previews the rendered output with My Details and active property values, and uses existing copy/share/Open WhatsApp flows with the same rendered message.
**Stop condition**: Stop if implementation requires Manifest/Gradle changes, permissions, backend/API, auth, CRM, contacts, contact database, scheduled sending, WorkManager, Room migration, WhatsApp detection, auto-send, or changes to Sprint 3 My Details / Sprint 5 property-link behavior.

### Task 1.1: Define Local Template Storage Contract
- Location: `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- Description: Add a small local store for editable templates using existing simple local persistence patterns. Use stable keys and keep storage local-only.
- Dependencies: Existing `MessageTemplate` and `SprintOneTemplates`.
- Acceptance criteria: Store can load built-in templates plus saved local edits; save edited template text; reset or fallback safely when no local edit exists; no backend/API, auth, Room, DataStore dependency, file storage, network, contacts, or CRM code is introduced.
- Validation command or manual check: Source review confirms `SharedPreferences` or equivalent existing simple local persistence only.
- Rollback: Delete `TemplateStore.kt` and return composer to `SprintOneTemplates.all`.

### Task 1.2: Preserve Built-In Template Identity
- Location: `app/src/main/java/com/followupnadlan/templates/MessageTemplate.kt` and `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- Description: Ensure each template has stable enough identity for local edits. Only add minimal fields if needed, such as category or editable metadata.
- Dependencies: Task 1.1.
- Acceptance criteria: Existing built-in templates remain selectable; saved edits map to a stable template id; no broad template catalog redesign; no extra templates are added unless already present in current code.
- Validation command or manual check: Unit/source review of stable ids.
- Rollback: Revert template model changes and store mapping.

### Task 1.3: Add Template Management Screen State
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a local navigation state for template management alongside the existing manual composer and My Details screens.
- Dependencies: Existing `FollowUpApp` screen state.
- Acceptance criteria: Manual composer remains the first usable workflow; My Details remains reachable and unchanged; template management is reachable without app restart; no onboarding, setup wizard, post-call, notification, or Snooze flow is changed.
- Validation command or manual check: Source review and manual UI check after approval.
- Rollback: Remove the template-management screen state and navigation entry.

### Task 1.4: Build Minimal Template Management UI
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Let the agent view existing templates, choose one, edit its text, insert supported tags, save locally, and optionally restore the built-in text for that template.
- Dependencies: Tasks 1.1 through 1.3.
- Acceptance criteria: UI is Hebrew/RTL, compact, and focused; supported tags are inserted into the editable text only by user action; unknown manually typed tags remain visible; no AI writing, marketplace, CRM sequence, scheduled-send, contact database, import/export, or mass messaging UI appears.
- Validation command or manual check: Real-phone visual check for Hebrew RTL, long text, tags, URLs, and multiline content.
- Rollback: Remove the management UI and store calls.

### Task 1.5: Use Saved Templates In Existing Composer
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Load templates from the local store in the manual composer and keep selection/reset behavior aligned with the selected saved template text.
- Dependencies: Tasks 1.1 and 1.4.
- Acceptance criteria: Existing composer still supports phone entry, lead name, message editing, preview, reset, blank validation, copy, share, and Open WhatsApp; reset restores the selected saved template text or built-in fallback; no Sprint 3 My Details or Sprint 5 property-link behavior regresses.
- Validation command or manual check: Manual composer regression check after approval.
- Rollback: Restore composer templates to `SprintOneTemplates.all`.

### Task 1.6: Preview Rendered Output With Existing Tags
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Use the existing `TemplateTagRenderer` and current render values to preview saved template output from manual lead name, My Details, and active property fields.
- Dependencies: Sprint 4 renderer and Sprint 5 active property values.
- Acceptance criteria: Preview uses exactly the same rendered text path as copy/share/WhatsApp; supported tags render; unknown tags remain visible; empty My Details/property values do not crash; no new renderer behavior is invented.
- Validation command or manual check: Unit/source review plus manual preview check.
- Rollback: Remove preview integration and keep existing composer render path.

### Task 1.7: Add Focused Tests
- Location: `app/src/test/java/com/followupnadlan/templates/TemplateStoreTest.kt` and `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- Description: Add tests for local template persistence behavior and preserve renderer behavior for supported, repeated, unknown, and property tags.
- Dependencies: Tasks 1.1 and 1.6.
- Acceptance criteria: Tests cover saving edited template text, loading saved edits over built-in defaults, safe fallback for missing saved values, supported tag rendering, active property tags, repeated tags, unknown tags, and empty values.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Remove added tests.

### Task 1.8: Preserve Scope And Update Evidence Docs
- Location: Whole repo plus `tasks/sprint-6-message-templates/`
- Description: After approved implementation, update execution/review/manual-smoke artifacts with exact evidence. Do not mark manual smoke PASS unless a real Android phone was used.
- Dependencies: Completed implementation and validation.
- Acceptance criteria: Manifest/Gradle unchanged; forbidden-scope grep clean or explained; `EXECUTION_LOG.md` records commands and files; `REVIEW.md` states PASS, PASS WITH NOTES, or FIX_REQUIRED based on evidence; `MANUAL_SMOKE_TEST.md` truthfully records NOT RUN or real-phone result.
- Validation command or manual check: Read all Sprint 6 docs after editing.
- Rollback: Revert incorrect Sprint 6 docs only.

## Testing Strategy
- Unit tests: `.\gradlew.bat test` for template store behavior and renderer regressions.
- Build: `.\gradlew.bat test assembleDebug`.
- Instrumented tests: Not required unless implementation adds Android-specific persistence logic that cannot be covered locally.
- Manual QA: Required on a real Android phone before marking smoke PASS.
- Device/OEM checks: Minimum one real Android phone for this sprint; broader Pixel/Samsung/Xiaomi QA remains later.
- Persistence checks:
  - Edit one built-in template and save.
  - Leave template management and return; saved text remains.
  - Restart app; saved text remains.
  - Restore built-in text if that UI is implemented.
- Tag checks:
  - Insert every supported tag.
  - Preview renders My Details tags from Sprint 3 fields.
  - Preview renders `{property_name}` and `{property_link}` from the Sprint 5 active property.
  - Unknown tags remain visible.
- Outgoing action checks:
  - Copy uses the rendered saved template message.
  - Manual share uses the same rendered saved template message.
  - Open WhatsApp uses the same rendered saved template message.
  - WhatsApp still requires the user to press Send manually.
- Forbidden-scope grep:

```powershell
rg -n "uses-permission|READ_CALL_LOG|READ_CONTACTS|READ_PHONE_STATE|POST_NOTIFICATIONS|FOREGROUND_SERVICE|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|WRITE_CALL_LOG|SEND_SMS|READ_SMS|WorkManager|Room|CallLog|TelephonyCallback|NotificationListenerService|AccessibilityService|auto-send|autosend|scheduled|scheduler|CRM|auth|login|backend|server|analytics|metadata|image upload|lead database|contact database|property database|property search" app build.gradle.kts settings.gradle.kts gradle.properties
```

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; `AndroidManifest.xml` must not change.
- User disclosure required: None for Sprint 6 because template text is local manual input only.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: Edited template text is stored locally until the user edits, resets, or clears it.
- Storage mechanism: Existing simple local persistence pattern, preferably `SharedPreferences`.
- Backend/API: None.
- Auth: None.
- Contacts/contact database: None.
- Scheduled sending: None.

## UX Impact
- Screens affected: Existing manual composer plus a new local template-management screen.
- RTL/Hebrew checks: Template list, editor, tag insertion controls, preview, phone numbers, URLs, and multiline text must remain readable in RTL.
- Empty/fallback states: Empty template text is allowed in editor but existing outgoing blank-message validation must still block copy/share/WhatsApp actions when rendered output is blank.
- Error states: Saving empty, long, Hebrew, mixed Hebrew/ASCII, tag-heavy, and URL-heavy template text must not crash.

## Rollback Plan
Delete `TemplateStore.kt` and `TemplateStoreTest.kt`, revert `MainActivity.kt` template-management navigation/UI and composer store integration, revert any minimal template model changes, and update only Sprint 6 docs to record the rollback. Do not touch Manifest, Gradle files, context docs, prior sprint folders, My Details behavior, property-link behavior, or WhatsApp send helpers.

## Review Checklist
- AndroidManifest is clean.
- Gradle files are unchanged.
- No new permissions or package queries.
- No AccessibilityService.
- No auto-send WhatsApp.
- No scheduled sending.
- No backend/API/cloud sync.
- No auth/login/accounts.
- No CRM.
- No contact database or contact reading.
- No Room migration.
- No WorkManager or notifications.
- Local storage only.
- Sprint 3 My Details behavior preserved.
- Sprint 5 property-link behavior preserved.
- Sprint 4 renderer behavior preserved.
- Supported tags render from existing `TemplateTagRenderer`.
- Unknown tags remain visible.
- Copy/share/Open WhatsApp use the same rendered message.
- wa.me / ACTION_VIEW remains user-driven.
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
- duplicate reminders: Not touched; do not add reminders, Snooze, or scheduled sends.
- invalid phone formatting for wa.me: Template management must not change phone normalization.
- RTL text and mixed Hebrew/phone-number/link layout: Template editor and preview may contain Hebrew, tags, URLs, and phone numbers; verify on a real phone before claiming manual PASS.
- Room migration risk: Avoided by using existing simple local persistence.
- direct-APK update/install friction: Not touched; no distribution changes.
- Saved edited template drift: Reset/fallback behavior must keep built-in templates recoverable.
- Long template text: Editor and preview must remain scrollable and must not make action buttons unreachable.
