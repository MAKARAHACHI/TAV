# DEBUG LOG: Sprint 14 Stale Prepared Card Reuse

## Symptom
The post-call decision card can reuse an existing active task and then snooze, save, open WhatsApp, or close using stale stored task fields from a previous card selection.

## Expected behavior
Every post-call decision-card action must operate on the current prepared card state: selected template, draft text, contact name, phone, call metadata, lead type when available, property link, and current source where appropriate.

## Reproduction steps
1. Have an active `FollowUpTaskEntity` for a phone with an old `selectedTemplateId` and `draftText`.
2. Open the Sprint 14 post-call decision card for the same phone or restore an existing task by id.
3. Select a different card or draft.
4. Trigger snooze, save/track, WhatsApp open, or close.

## Evidence collected
- `tasks/sprint-14-lead-pipeline/REVIEW.md` records `Decision: FAIL` because `PostCallScreen.ensureTask()` returns reused active tasks before copying current card state.
- `app/src/main/java/com/followupnadlan/MainActivity.kt` showed `ensureTask()` returned restored task by id directly.
- `app/src/main/java/com/followupnadlan/MainActivity.kt` showed `ensureTask()` returned `getLatestByPhoneAndStatuses(...)` directly.
- `app/src/main/java/com/followupnadlan/pipeline/LeadPipeline.kt` had transition helpers, but no pure helper to merge current card state into a reused task.

## Hypotheses
1. Reused task state is stale because `ensureTask()` returns before updating current card fields - supported by source review.
2. Snooze transition itself drops the draft/template - not supported; `LeadPipeline.snoozeTask()` copies the task and only updates reminder/status/timestamp.
3. DAO lookup returns the wrong active task - not the primary blocker; lookup returns latest active task, but caller still must refresh it with current decision-card state.

## Root cause
`PostCallScreen.ensureTask()` handled only the new-task path as current-card-aware. The restored-by-id and reused-by-phone paths returned persisted task rows unchanged, so downstream action helpers operated on stale `selectedTemplateId`, `draftText`, contact/call metadata, source, or property fields.

## Minimal fix
Add a pure `LeadPipeline.mergeCurrentCardState(...)` helper that preserves task identity and `createdAtEpochMs`, updates only current card-related fields plus `updatedAtEpochMs`, and call it from both reused/restored `ensureTask()` branches before returning.

## Validation
- `.\gradlew.bat test`: PASS, `BUILD SUCCESSFUL in 7s`.
- `.\gradlew.bat assembleDebug`: PASS, `BUILD SUCCESSFUL in 2s`.
- `git diff -- app/src/main/AndroidManifest.xml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties`: empty.
- Focused source grep confirmed `ensureTask()` now uses `mergeCurrentCardState` before `snoozeTask`, `markWhatsAppOpened`, `markSavedAsLead`, and `closeTask`.
- Forbidden-scope grep found only documentation/prompt/context non-goal mentions; no implementation use in `app` or Gradle files.

## Regression risk
Low to medium. The change is scoped to reused/restored active tasks and pure pipeline tests. The main risk is unintentionally replacing old fields with current blank values; this is intentional for current card fields, while lead type is preserved when no current lead type is available.
