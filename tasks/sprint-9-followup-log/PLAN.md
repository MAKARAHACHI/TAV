# PLAN: Sprint 9 Follow-Up Action Log Foundation

**Status**: Human-approved
**Approved at**: 2026-05-20
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core MVP foundation
**Risk**: Low-Medium

## Goal Statement

Add the smallest truthful local Follow-Up Action Log foundation that records explicit app-initiated follow-up actions from the existing manual composer.

## Product Guardrail Check

- FollowUp Nadlan constitution read: Yes.
- Core manual WhatsApp/share/copy flow preserved: Yes.
- User-controlled WhatsApp send preserved: Yes.
- Accessibility avoided: Yes.
- Backend/API avoided for MVP: Yes.
- Fallback mode preserved: Yes.
- No new Android permission: Yes.

Decision: Proceed.
Reason: The sprint records local action initiation only and does not claim WhatsApp send or delivery.

## Truthful Naming Decision

This sprint creates a Follow-Up Action Log.

The app cannot verify that a WhatsApp message was actually sent or delivered inside WhatsApp. A log entry means only that the user initiated one of the explicit follow-up actions from FollowUp Nadlan:

- `WHATSAPP_OPENED`
- `SHARE_OPENED`
- `COPY_USED`

## Non-Goals

- No History or Summary screen.
- No export.
- No filters.
- No CRM status.
- No lead pipeline.
- No backend/API.
- No Room/database unless SharedPreferences proves insufficient.
- No AI.
- No analytics.
- No WhatsApp auto-send.
- No Accessibility Service.
- No `READ_CALL_LOG`.
- No `READ_CONTACTS`.
- No real call detection.
- No notification changes unless strictly required for compilation.
- No new Android permission.

## Assumptions

- SharedPreferences-style storage is sufficient for this foundation sprint because no History/Summary UI exists yet and the log can be capped.
- Store `messagePreview` by default, not full rendered message text, to avoid retaining unnecessary sensitive client/property text.
- Manual smoke cannot inspect the log through UI in Sprint 9; source review and unit tests validate persistence until a future approved Summary/History screen exists.

## Files To Read First

- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- `context/DATA_CONTRACTS.md`
- `context/ARCHITECTURE.md`
- `tasks/sprint-8-notification-followup-card/PLAN.md`
- `tasks/sprint-8-notification-followup-card/REVIEW.md`

## Files Expected To Change

- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`
- `tasks/sprint-9-followup-log/PLAN.md`
- `tasks/sprint-9-followup-log/EXECUTION_LOG.md`
- `tasks/sprint-9-followup-log/REVIEW.md`
- `tasks/sprint-9-followup-log/MANUAL_SMOKE_TEST.md`

## Files That Must Not Change

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- Notification code unless strictly required for compilation.

## Sprint 9: Follow-Up Action Log Foundation

**Goal**: Persist a small local record when the user opens WhatsApp, opens manual share, or copies the message.

**Demo / Validation**: Unit tests prove append/load/serialization/trimming/action labels; build proves the existing composer still compiles.

**Stop condition**: Stop if implementation requires a new permission, backend, Room migration, History/Summary UI, export, CRM concept, call detection, or WhatsApp automation.

### Task 9.1: Add action log model

- Location: `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- Description: Add `FollowUpLogEntry` and `FollowUpActionType`.
- Acceptance criteria:
  - Includes lead name, phone number, template id/title, property name/link, `messagePreview`, action type, and timestamp.
  - Action labels are truthful and do not imply send/delivery.
- Validation: Unit tests compile and action-label test passes.
- Rollback: Remove the new file/package.

### Task 9.2: Add SharedPreferences-style store

- Location: `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- Description: Add local append/load storage with deterministic serialization and capped retention.
- Acceptance criteria:
  - Uses local SharedPreferences.
  - No Room, backend, dependency, or permission.
  - Stores a short message preview, not full rendered text.
  - Trims old entries.
- Validation: Unit tests for round-trip, malformed rows, preview, and trimming.
- Rollback: Remove store and references.

### Task 9.3: Record explicit action initiation

- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Append log entry only after explicit actions are initiated.
- Acceptance criteria:
  - `WHATSAPP_OPENED` after successful `ACTION_VIEW` launch.
  - `SHARE_OPENED` after share chooser launch.
  - `COPY_USED` after clipboard copy.
  - Failed validation attempts do not log.
  - Existing WhatsApp/share/copy UX remains unchanged.
- Validation: Build plus source review.
- Rollback: Remove log-store parameter and append calls.

### Task 9.4: Add sprint artifacts

- Location: `tasks/sprint-9-followup-log/`
- Description: Add plan, execution log, review, and manual smoke checklist.
- Acceptance criteria:
  - Artifacts clearly state no manual smoke PASS unless human real-phone test happens.
  - Artifacts list scope exclusions and validation evidence.
- Validation: Source review.
- Rollback: Remove sprint folder if sprint is abandoned before approval.

## Testing Strategy

- Unit tests:
  - Append/load serialization round-trip.
  - Trimming keeps latest entries.
  - Action labels remain `WHATSAPP_OPENED`, `SHARE_OPENED`, `COPY_USED`.
  - Message preview compacts whitespace and trims.
  - Malformed rows are ignored safely.
- Instrumented tests: Not required for this foundation sprint.
- Manual QA: Real Android smoke checklist in `MANUAL_SMOKE_TEST.md`.
- Device/OEM checks: Not required unless human performs manual smoke.

## Permission Impact

- Added permissions: None.
- Removed permissions: None.
- Manifest risk: No planned Manifest change.
- User disclosure required: No new permission disclosure.

## Data/Schema Impact

- Room entities: None.
- Migrations: None.
- Local data retention: SharedPreferences capped local action log, latest 50 entries.
- Privacy note: Store `messagePreview`, not full message text by default.

## UX Impact

- Screens affected: Existing manual composer only through action hooks.
- RTL/Hebrew checks: Existing UI preserved.
- Empty/fallback states: Existing validation remains.
- Error states: Failed open/share validation does not log.

## Rollback Plan

Remove the `followuplog` package, remove the store from `FollowUpApp`, remove `followUpLogStore` from `ManualWhatsAppScreen`, remove append calls from the three action handlers, and remove Sprint 9 task artifacts if the sprint is abandoned.

## Review Checklist

- AndroidManifest is clean.
- No new Android permission.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Share and copy remain explicit actions.
- No backend/API.
- No Room migration.
- No Summary/History screen.
- No export/filter/CRM/lead-pipeline scope.
- Log language does not claim WhatsApp delivery.

## Agent Handoff

- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas

- Android background restrictions are not relevant because this sprint does not add background work.
- Notification permission denial remains Sprint 8 behavior; this sprint must not alter it.
- READ_CALL_LOG denial fallback remains unchanged; this sprint must not add call detection.
- OEM battery behavior is not in scope.
- Duplicate reminders are not in scope.
- Invalid phone formatting should remain gated by existing WhatsApp validation.
- RTL text and mixed Hebrew/phone-number layout should remain unchanged.
- Room migration risk is avoided by not adding Room.
- Direct-APK update/install friction is not in scope.
