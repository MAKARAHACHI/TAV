# PLAN: Sprint 1 Manual WhatsApp Screen

**Status**: Human-approved
**Planning model**: GPT-5.4 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Medium
**Generated**: 2026-05-17

## Goal Statement
Build the first runnable Android MVP screen where a Hebrew RTL user can enter a phone number, choose or edit a real-estate message template, generate a `wa.me` link, and manually open WhatsApp without any automatic sending.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes, by implementing the manual fallback foundation and not removing later snooze requirements
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes, the screen works without sensitive permissions

Decision: Proceed
Reason: Sprint 1 matches Layer 1 from `context/ROADMAP.md` and avoids call detection, sensitive permissions, persistence, licensing, updates, and backend scope.

## Non-Goals
- No call detection.
- No `READ_CALL_LOG`.
- No `READ_CONTACTS`.
- No `READ_PHONE_STATE`.
- No notifications.
- No WorkManager.
- No Room.
- No leads module.
- No snooze implementation.
- No payment, license, activation, or update checker.
- No backend/API/cloud integration.
- No WhatsApp auto-send or AccessibilityService.
- No Sprint 2 work such as agent profile, 12-template library, or block composer.

## Assumptions
- Because this repo currently contains only context/docs and no Android app scaffold, Sprint 1 may create the minimal Android project files required to run the first screen.
- The current user request is explicit approval to plan and execute Sprint 1 in one pass.
- If local build tooling cannot resolve Android/Gradle dependencies, record the exact blocker in `EXECUTION_LOG.md` and still keep source changes reviewable.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-vision-guardian/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `context/CONTEXT.md`
- `context/PROJECT.md`
- `context/ARCHITECTURE.md`
- `context/ROADMAP.md`
- `context/DATA_CONTRACTS.md`
- `context/TEMPLATE_ENGINE.md`
- `context/DO_NOT_BUILD.md`
- `context/PERMISSIONS_AND_PRIVACY.md`

## Files Expected To Change
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/...`
- `app/src/test/java/...`
- `tasks/sprint-1-manual-whatsapp/PLAN.md`
- `tasks/sprint-1-manual-whatsapp/EXECUTION_LOG.md`
- `tasks/sprint-1-manual-whatsapp/REVIEW.md`

## Files That Must Not Change
- No existing context files unless an implementation behavior contradicts them.
- No dependency changes beyond the minimal initial Android scaffold dependencies.
- No Android permissions for call log, contacts, phone state, notifications, foreground service, SMS, overlays, or Accessibility.

## Sprint 1: Manual WhatsApp Screen + Template Engine + wa.me
**Goal**: Deliver a runnable first Android screen for manual WhatsApp follow-up.
**Demo / Validation**: User can type a phone number, select a built-in Hebrew template, edit the message, see the generated `wa.me` link, and tap a button that opens an `ACTION_VIEW` intent.
**Stop condition**: Stop after Sprint 1 review. Do not proceed to agent profile, 12 templates, block composer, snooze, leads, call detection, licensing, or updates.

### Task 1.1: Create minimal Android scaffold
- Location: repo root and `app/`
- Description: Add the smallest native Android app project needed for a debug build.
- Dependencies: Android Gradle plugin, Kotlin/Compose if available through Gradle resolution.
- Acceptance criteria: Project contains one app module and a clean manifest with no sensitive permissions.
- Validation command or manual check: `./gradlew assembleDebug` or local equivalent.
- Rollback: Remove newly created scaffold files.

### Task 1.2: Implement template/domain helpers
- Location: `app/src/main/java/...`
- Description: Add a small template list, phone normalization, and `wa.me` URI generation.
- Dependencies: Kotlin standard library only.
- Acceptance criteria: Phone normalization handles Israeli local numbers and already-international numbers; message text is URL encoded in the generated link.
- Validation command or manual check: unit tests for phone and link helpers.
- Rollback: Remove helper files and tests.

### Task 1.3: Implement Hebrew RTL manual composer screen
- Location: `app/src/main/java/...`
- Description: Add a single Compose screen with phone input, template selector, editable message, generated link preview, and open WhatsApp action.
- Dependencies: Jetpack Compose.
- Acceptance criteria: UI is Hebrew RTL, manual-only, editable before opening WhatsApp, and never sends automatically.
- Validation command or manual check: build plus source review of the intent path.
- Rollback: Remove UI screen and restore placeholder app if needed.

### Task 1.4: Validate scope and document execution
- Location: `tasks/sprint-1-manual-whatsapp/`
- Description: Run available validation, create `EXECUTION_LOG.md`, and run the reviewer checklist into `REVIEW.md`.
- Dependencies: Existing repo tools.
- Acceptance criteria: Validation evidence is recorded and review confirms no Sprint 2 or forbidden scope was added.
- Validation command or manual check: inspect git diff, manifest, build/test output, and generated files.
- Rollback: Update logs to reflect failure and stop for human decision.

## Testing Strategy
- Unit tests: phone normalization and `wa.me` URI generation.
- Instrumented tests: Not required for Sprint 1 unless scaffold makes them trivial.
- Manual QA: Source-level smoke checklist for Hebrew RTL, template editability, invalid phone handling, link generation, and user-driven WhatsApp open.
- Device/OEM checks: Not required for Sprint 1 because there is no post-call engine, notification, or background service.

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: Low; manifest must contain no sensitive permissions.
- User disclosure required: None for Sprint 1 because no sensitive permission is requested.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: None; Sprint 1 keeps state in memory only.

## UX Impact
- Screens affected: New manual composer only.
- RTL/Hebrew checks: Whole screen must use RTL layout direction and Hebrew copy.
- Empty/fallback states: Empty or invalid phone should show a Hebrew error and avoid launching.
- Error states: If no app/browser can handle the `wa.me` URI, show a Hebrew error.

## Rollback Plan
Remove the new `app/` module and root Gradle scaffold files, leaving the context system and task artifacts intact.

## Review Checklist
- AndroidManifest is clean.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Snooze restores prepared card: Not applicable in Sprint 1.
- Fallback works without READ_CALL_LOG.
- Setup/self-test status is not broken.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas
- Android background restrictions: Not applicable in Sprint 1 because no background work is implemented.
- notification permission denial: Not applicable in Sprint 1 because no notifications are implemented.
- READ_CALL_LOG denial fallback: Sprint 1 is manual-only and must not request `READ_CALL_LOG`.
- OEM battery killing FGS: Not applicable in Sprint 1 because no Foreground Service is implemented.
- duplicate reminders: Not applicable in Sprint 1 because no reminders are implemented.
- invalid phone formatting for wa.me: Must be handled by normalization plus validation before launching.
- RTL text and mixed Hebrew/phone-number layout: Must be reviewed in the manual composer source.
- Room migration risk: Not applicable in Sprint 1 because no Room is implemented.
- direct-APK update/install friction: Not applicable in Sprint 1 because no update/distribution flow is implemented.
