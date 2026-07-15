# PLAN: Custom Message Persistence and Home Shortcut

**Status**: Human-approved
**Planning model**: GPT-5.4 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Medium
**Generated**: 2026-07-01

## Goal Statement
Make edited accessibility default messages persist and make the current message directly editable from Home.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: No, existing branch already contains an accessibility service; this task will not add or expand it.
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: The change is limited to local template persistence, Home navigation, and tests.

## Non-Goals
- No missed-call detection changes.
- No recipient rule, exclusion, or cooldown changes.
- No WhatsApp/SMS behavior changes except routing through the saved message body.
- No AI, recording, transcription, caller ID, scraping, CRM, backend, analytics, or new permissions.

## Assumptions
- The current prompt is explicit approval to execute this focused fix in one sprint.
- Existing accessibility service/permission files are out of scope and will not be modified.

## Files To Read First
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- `app/src/main/java/com/followupnadlan/templates/LegacyTemplateMigration.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseSettings.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateStoreTest.kt`
- `app/src/test/java/com/followupnadlan/templates/LegacyTemplateMigrationTest.kt`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- `app/src/main/java/com/followupnadlan/templates/LegacyTemplateMigration.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseSettings.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateStoreTest.kt`
- `app/src/test/java/com/followupnadlan/templates/LegacyTemplateMigrationTest.kt`
- new focused unit test files if needed

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- call detection, recipient scope, exclusions, cooldown, snooze, and package resolution logic
- Gradle dependencies unless validation proves a missing existing test dependency

## Sprint 1: Persistence and Home Shortcut
**Goal**: Persist custom template bodies reliably, refresh UI from saved state, show Home preview, and confirm send paths use saved bodies.
**Demo / Validation**: Unit tests plus `test`, `assembleDebug`, `lintDebug`, `git diff --check`, `git status`.
**Stop condition**: Any need for new permission, backend, sending automation, or broad send-path rewrite.

### Task 1.1: Fix template save/reload state
- Location: `TemplateStore.kt`, `AccessibilityApp.kt`, `MissedCallAutoResponseSettings.kt`
- Description: Use synchronous commits for core message settings and refresh Compose template state after save.
- Dependencies: Existing SharedPreferences keys and SprintOne template ids.
- Acceptance criteria: Editing any accessibility template reloads the custom body after leaving/reopening.
- Validation command or manual check: Unit tests and source review.
- Rollback: Revert template store/settings write changes and UI state refresh.

### Task 1.2: Keep migration from deleting user edits
- Location: `LegacyTemplateMigration.kt`, `TemplateStore.kt`
- Description: Tighten legacy default detection so only known legacy defaults are removed.
- Dependencies: Existing legacy template ids and default text tests.
- Acceptance criteria: User-custom accessibility text wins over built-in defaults.
- Validation command or manual check: Migration unit tests.
- Rollback: Restore previous legacy marker matching.

### Task 1.3: Add Home shortcut
- Location: `AccessibilityApp.kt`
- Description: Add a compact Home card with title, preview, and edit button opening the existing template screen.
- Dependencies: Existing card/button styles.
- Acceptance criteria: Home stays calm and only shows title, short preview, edit button.
- Validation command or manual check: Build and manual QA checklist.
- Rollback: Remove Home card and callback.

### Task 1.4: Confirm send render source
- Location: `MissedCallAutoResponseHandler.kt`
- Description: Keep WhatsApp and SMS branches on one resolved message body from `TemplateStore`.
- Dependencies: Existing send decision and fallback paths.
- Acceptance criteria: Render path test proves selected custom body is used.
- Validation command or manual check: Unit tests.
- Rollback: Restore previous inline template lookup.

## Testing Strategy
- Unit tests: requested template, migration, render path, and Home preview tests.
- Instrumented tests: Not added; no Compose UI test dependency exists and task can be covered with pure logic.
- Manual QA: Edit each template, leave/reopen, relaunch, trigger missed-call/manual prompt path if available.
- Device/OEM checks: Not required for this local persistence/UI fix.

## Permission Impact
- Added permissions: None
- Removed permissions: None
- Manifest risk: No manifest changes planned
- User disclosure required: No

## Data/Schema Impact
- Room entities: None
- migrations: None
- local data retention: Existing SharedPreferences template body keys remain the persistence surface

## UX Impact
- Screens affected: Home and existing default-message editor
- RTL/Hebrew checks: Home card and existing editor remain RTL/Hebrew
- Empty/fallback states: Empty preview falls back to existing default selected template body
- Error states: No new error state

## Rollback Plan
Revert this task folder and the touched Kotlin/test files. No schema or manifest rollback required.

## Review Checklist
- AndroidManifest is clean.
- No AccessibilityService added.
- No auto-send WhatsApp added.
- wa.me / ACTION_VIEW remains user-driven where applicable.
- Snooze untouched.
- Fallback untouched.
- Setup/self-test status not broken.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas
- Android background restrictions: Not touched.
- notification permission denial: Not touched.
- READ_CALL_LOG denial fallback: Not touched.
- OEM battery killing FGS: Not touched.
- duplicate reminders: Not touched.
- invalid phone formatting for wa.me: Not touched.
- RTL text and mixed Hebrew/phone-number layout: Home preview uses short text only.
- Room migration risk: None.
- direct-APK update/install friction: None.
