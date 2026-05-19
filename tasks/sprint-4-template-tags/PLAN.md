# PLAN: Sprint 4 Template Tags Rendering

**Status**: Draft
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Low
**Generated**: 2026-05-19

## Goal Statement
Add a small local template-tag rendering layer so outgoing manual messages replace approved lead/profile tags before Open WhatsApp, manual share, and copy actions.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This sprint only renders local text values already typed or stored on-device, keeps all sending user-driven, and does not require permissions, backend/API, contacts, CRM, properties, history, post-call work, Manifest changes, or Gradle changes.

## Non-Goals
- Do not implement app source in this planning turn.
- Do not add backend/API, cloud sync, accounts, CRM, properties, history/log, analytics, campaigns, exports, or lead database work.
- Do not read contacts, call logs, SMS, notifications, WhatsApp state, installed packages, or device profile data.
- Do not add post-call detection, Snooze, reminders, WorkManager, Foreground Service, notifications, setup wizard, or self-test work.
- Do not add new permissions, package queries, Manifest changes, or Gradle/dependency changes unless implementation proves an unavoidable need and returns for human approval first.
- Do not auto-send WhatsApp messages or automate WhatsApp UI.
- Do not add complex template editing, block composer, conditional logic, property tags, attachment sending, or business-card file handling.
- Do not claim manual smoke PASS without explicit real-phone evidence.

## Assumptions
- Sprint 3 profile fields exist in `SharedPreferences` via `MyDetailsStore` with keys `agent_name`, `office_name`, `phone`, `website`, `business_card`, and `signature`.
- `{lead_name}` should come from a simple manual composer field in this sprint, not from contact reading or CRM.
- Known supported tags should be replaced in the final outgoing text. Empty local values may render as empty strings.
- Unknown tags should remain visible in the editable message and should not be silently deleted.
- The message remains editable before the user opens WhatsApp, shares, or copies.

## Supported Tags
- `{lead_name}`
- `{agent_name}`
- `{office_name}`
- `{phone}`
- `{website}`
- `{business_card}`
- `{signature}`

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `context/TEMPLATE_ENGINE.md`
- `context/DATA_CONTRACTS.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/DO_NOT_BUILD.md`
- `tasks/sprint-2-manual-contact-send/PLAN.md`
- `tasks/sprint-2-manual-contact-send/REVIEW.md`
- `tasks/sprint-3-my-details/PLAN.md`
- `tasks/sprint-3-my-details/REVIEW.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/whatsapp/WhatsAppLinkBuilder.kt`

## Files Expected To Change After Human Approval
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- `tasks/sprint-4-template-tags/EXECUTION_LOG.md`
- `tasks/sprint-4-template-tags/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-4-template-tags/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `context/*`
- Sprint 1, Sprint 2, and Sprint 3 task folders unless only referenced by review evidence.
- Any Room, WorkManager, call-state, contact-reading, backend/API, CRM, property, history/log, export, Snooze, notification, or WhatsApp automation files.

## Sprint 1: Local Template Tags Rendering
**Goal**: Render the approved Sprint 4 tags from local manual/profile values before every outgoing message action.
**Demo / Validation**: User saves My Details, writes a message containing supported tags, enters a manual lead name, then sees WhatsApp, manual share, and copy all use the same rendered text with the supported tags replaced.
**Stop condition**: Stop if implementation requires permissions, Manifest/Gradle changes, backend/API, contact reading, CRM, properties, history/log, post-call work, Snooze, WhatsApp detection, or auto-send behavior.

### Task 1.1: Add Pure Template Tag Renderer
- Location: `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- Description: Add a small pure Kotlin renderer that accepts raw message text plus a value map or typed context for the seven supported tags and returns rendered text.
- Dependencies: Kotlin standard library only.
- Acceptance criteria: Replaces exactly the supported tags listed in this plan; supports repeated tags; leaves unknown tags visible; does not mutate storage; does not know about Android intents.
- Validation command or manual check: Unit tests for all supported tags, repeated tags, unknown tags, empty values, and mixed Hebrew/ASCII text.
- Rollback: Delete `TemplateTagRenderer.kt` and its tests.

### Task 1.2: Add Renderer Unit Tests
- Location: `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- Description: Cover rendering behavior without Android framework dependencies.
- Dependencies: Existing unit test setup only.
- Acceptance criteria: Tests prove `{lead_name}`, `{agent_name}`, `{office_name}`, `{phone}`, `{website}`, `{business_card}`, and `{signature}` render from supplied values; unknown tags remain unchanged; empty values do not crash.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete the renderer test file.

### Task 1.3: Load Local Profile Values In Manual Composer
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Read the current saved `MyDetailsStore` profile inside the manual composer path and pass its values into the renderer.
- Dependencies: Existing Sprint 3 `MyDetailsStore` and `MyDetailsProfile`.
- Acceptance criteria: Uses local SharedPreferences profile values only; no contact, CRM, backend/API, or device data lookup is added.
- Validation command or manual check: Source review confirms `MyDetailsStore.load()` is the only profile source.
- Rollback: Remove profile loading from `ManualWhatsAppScreen`.

### Task 1.4: Add Manual Lead Name Input
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a simple optional manual lead-name field for `{lead_name}` rendering. It must not read contacts or persist lead data.
- Dependencies: Existing Compose UI.
- Acceptance criteria: The field is local screen state only; empty field is allowed; no contact picker, READ_CONTACTS, CRM, lead model, or storage is introduced.
- Validation command or manual check: Manual UI check and source review.
- Rollback: Remove the lead-name state and field.

### Task 1.5: Use One Rendered Message For All Outgoing Actions
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Compute one `renderedMessage` from the editable raw message, manual lead name, and saved profile. Use that same value for `WhatsAppLinkBuilder.build(...)`, `openShareSheet(...)`, and `copyMessageToClipboard(...)`.
- Dependencies: Tasks 1.1, 1.3, and 1.4.
- Acceptance criteria: Open WhatsApp, manual share, and copy cannot drift; all three receive the same rendered message; validation still blocks blank outgoing rendered text; reset behavior still restores the selected template default raw message.
- Validation command or manual check: Source review plus manual test that the copied/shared/WhatsApp text match.
- Rollback: Restore all three actions to use the raw `message` state.

### Task 1.6: Keep Preview And Editing Clear
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Keep the editable message as the raw template text and add a small rendered preview only if needed for clarity. Do not make a large UI redesign.
- Dependencies: Task 1.5.
- Acceptance criteria: User can still edit tags before sending; the final rendered outgoing text is visible or otherwise easy to verify; unknown tags stay visible.
- Validation command or manual check: Real-phone visual check for RTL and mixed Hebrew/tag/URL/phone layout.
- Rollback: Remove preview additions and keep only rendered outgoing action wiring.

### Task 1.7: Preserve Scope And Update Evidence Docs
- Location: Whole repo plus `tasks/sprint-4-template-tags/`
- Description: After implementation, update execution/review/manual-smoke artifacts with exact evidence. Do not mark manual smoke PASS unless a real Android phone was used.
- Dependencies: Completed implementation and validation.
- Acceptance criteria: Manifest/Gradle unchanged; forbidden-scope grep clean; `EXECUTION_LOG.md` records commands; `REVIEW.md` states PASS, PASS WITH NOTES, or FIX_REQUIRED based on evidence; `MANUAL_SMOKE_TEST.md` truthfully records NOT RUN or real-phone result.
- Validation command or manual check: Read all Sprint 4 docs after editing.
- Rollback: Revert incorrect Sprint 4 docs only.

## Testing Strategy
- Unit tests: `.\gradlew.bat test` for the pure renderer.
- Build: `.\gradlew.bat assembleDebug` or `.\gradlew.bat test assembleDebug`.
- Instrumented tests: Not required for this small local renderer unless implementation introduces Android-specific logic beyond the planned source review.
- Manual QA: Required on a real Android phone before marking smoke PASS.
- Regression checks:
  - Save My Details values, return to manual composer, and verify tags render.
  - Verify `Open WhatsApp` uses rendered message in the `wa.me` text.
  - Verify `Manual share` uses the same rendered message.
  - Verify `Copy message` places the same rendered message on the clipboard.
  - Verify reset restores raw selected template text.
  - Verify empty message validation still works.
  - Verify empty profile fields do not crash.
- Forbidden-scope grep:

```powershell
rg -n "uses-permission|READ_CALL_LOG|READ_CONTACTS|READ_PHONE_STATE|POST_NOTIFICATIONS|FOREGROUND_SERVICE|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|WRITE_CALL_LOG|SEND_SMS|READ_SMS|WorkManager|Room|CallLog|TelephonyCallback|NotificationListenerService|AccessibilityService|auto-send|autosend|CRM|backend|server|export|history|snooze|property|properties" app build.gradle.kts settings.gradle.kts gradle.properties
```

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; `AndroidManifest.xml` must not change.
- User disclosure required: None for Sprint 4 because it uses local manual/profile values only.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: No new durable data. `{lead_name}` is screen state only; profile values remain in existing Sprint 3 SharedPreferences.
- Storage mechanism: Existing `SharedPreferences` only.

## UX Impact
- Screens affected: Existing manual composer only.
- RTL/Hebrew checks: Message editing and rendered output must remain readable with Hebrew text, ASCII tags, phone numbers, URLs, and multiline signature/business-card values.
- Empty/fallback states: Empty profile fields and empty lead name render safely without crash.
- Error states: If rendered outgoing message is blank, existing message validation must block WhatsApp/share/copy.

## Rollback Plan
Delete `TemplateTagRenderer.kt` and `TemplateTagRendererTest.kt`, revert the `MainActivity.kt` integration to use raw `message` for WhatsApp/share/copy, and update only Sprint 4 docs to record the rollback. Do not touch Manifest, Gradle files, context docs, or prior sprint folders.

## Review Checklist
- AndroidManifest is clean.
- Gradle files are unchanged.
- No new permissions or package queries.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Manual share still uses ACTION_SEND text/plain and user chooser.
- Copy action copies only rendered message text.
- The same rendered message is used by WhatsApp, share, and copy.
- Unknown tags remain visible; supported tags render.
- Local storage only; no backend/API/cloud sync.
- No contact reading, CRM, properties, history/log, Snooze, post-call work, or WhatsApp detection.
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
- invalid phone formatting for wa.me: Rendering must not change phone normalization or validation.
- RTL text and mixed Hebrew/phone-number layout: Rendered preview and output may contain Hebrew, tags, URLs, and phone numbers; verify on a real phone before claiming manual PASS.
- Room migration risk: Avoided; no Room schema change.
- direct-APK update/install friction: Not touched; no distribution changes.
