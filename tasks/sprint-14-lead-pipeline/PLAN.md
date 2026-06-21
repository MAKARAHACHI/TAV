# PLAN: Sprint 14 Lead Pipeline

**Status**: Human-approved by current /goal request  
**Planning model**: GPT-5.5 High  
**Execution model**: Codex 5.3 High  
**Layer**: Core  
**Risk**: Medium  
**Generated**: 2026-05-25

## Goal Statement
Turn the existing post-call follow-up card into a local lead pipeline that remembers who needs follow-up, when to return, and whether the lead is pending, snoozed, or closed, while keeping WhatsApp sending fully user-controlled.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed  
Reason: This sprint strengthens the core post-call follow-up promise without AI, transcription, backend, overlays, auto-send, or new sensitive permissions.

## Product Definition For This Sprint

This sprint is the product shift from:

```txt
call ended -> prepare WhatsApp message
```

to:

```txt
call ended -> prepare next step -> remember the lead route -> remind only when needed
```

The UI may feel like a pop-out decision card after a notification tap, but it must not use a real Android overlay.

## Non-Goals
- No GPT-4o mini, OpenRouter, LLM calls, prompt design, or AI analysis.
- No live call transcription, microphone permission, or SpeechRecognizer integration.
- No `SYSTEM_ALERT_WINDOW`, full-screen intent, or forced Activity launch from background.
- No `NotificationListenerService` or automatic WhatsApp reply detection.
- No AccessibilityService and no WhatsApp auto-send.
- No backend, API, cloud sync, CRM sync, Firestore, Supabase, or account system.
- No full CRM, analytics dashboard, bulk messaging, campaigns, or property-management module.
- No multi-property/entity graph for wife/owner/second-apartment scenarios; those belong to the future AI/transcription layer.
- No new dangerous permissions.
- No Room ownership migration for profile/templates/property/log; keep existing SharedPreferences-backed features untouched unless strictly required by this sprint.

## Assumptions
- Existing Room entities remain the storage base: `FollowUpTaskEntity` and `LeadEntity`.
- The current notification contract remains stable: `ACTION_OPEN_FOLLOW_UP`, extras, request code `8001`, notification id `8001`.
- The current reminder path remains WorkManager-based and does not promise exact-to-the-minute reminders.
- "Pop-out card" means an in-app Compose decision card opened by notification tap or foreground state, not a system overlay.
- Pipeline states can be represented using existing string status fields first; Room schema changes require a separate Gate approval inside this sprint.
- Manual fallback must continue to work when `READ_CALL_LOG`, `READ_CONTACTS`, or notifications are denied.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-vision-guardian/SKILL.md`
- `context/DATA_CONTRACTS.md`
- `context/POST_CALL_ENGINE.md`
- `context/SNOOZE_REMINDERS.md`
- `context/FOLLOW_UP_CARD.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/data/AppDatabase.kt`
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskEntity.kt`
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt`
- `app/src/main/java/com/followupnadlan/data/lead/LeadEntity.kt`
- `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt`
- `app/src/main/java/com/followupnadlan/snooze/ReminderScheduler.kt`
- `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/notifications/ReminderNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt`
- existing tests under `app/src/test/java/com/followupnadlan/`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt`
- `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt`
- `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`
- `app/src/main/java/com/followupnadlan/notifications/ReminderNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt`
- `app/src/test/java/com/followupnadlan/data/FollowUpRoomContractTest.kt`
- `app/src/test/java/com/followupnadlan/snooze/ReminderSchedulerTest.kt`
- new tests under `app/src/test/java/com/followupnadlan/pipeline/` if a domain component is introduced
- `context/DATA_CONTRACTS.md`
- `context/SNOOZE_REMINDERS.md`
- `context/FOLLOW_UP_CARD.md`
- `tasks/sprint-14-lead-pipeline/EXECUTION_LOG.md`
- `tasks/sprint-14-lead-pipeline/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-14-lead-pipeline/REVIEW.md`

## Files That Must Not Change
- `AndroidManifest.xml`, unless the executor finds a pre-existing compile issue; no permission additions are allowed in this sprint.
- root `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts`, unless a missing existing WorkManager/Room setup prevents compilation.
- `context/API.md`; this project uses `context/DATA_CONTRACTS.md`.
- profile/template/property/log storage files unless an existing compile break requires a narrow fix.
- WhatsApp automation boundaries; do not add Accessibility or screen control code.

## Sprint 14.1: Pipeline Contract And Domain Logic

**Goal**: Define a small local pipeline vocabulary and deterministic status transitions over existing `FollowUpTaskEntity` and `LeadEntity`.  
**Demo / Validation**: Unit tests prove that post-call tasks can become pending, snoozed, WhatsApp-opened, saved-as-lead, or closed without schema ambiguity.  
**Stop condition**: Stop after tests pass and docs record the final status names. Do not touch UI in this sub-sprint.

### Task 14.1.1: Audit Existing Status Strings
- Location: `MainActivity.kt`, `ReminderWorker.kt`, `context/DATA_CONTRACTS.md`
- Description: List all current task statuses and lead statuses, then decide whether to reuse them or add pipeline-specific constants.
- Dependencies: none
- Acceptance criteria:
  - No duplicate/conflicting status names are introduced.
  - The plan for `PENDING_RESPONSE`, `SNOOZED`, and `CLOSED` maps clearly onto existing or new local constants.
  - `SNOOZED`, `WHATSAPP_OPENED`, and `SAVED_AS_LEAD` behavior remains compatible with Sprint 12.
- Validation command or manual check: `rg -n "FOLLOW_UP_STATUS|LeadStatus|SNOOZED|CLOSED|PENDING" app/src/main/java context`
- Rollback: revert only the status-constant/doc changes from this task.

### Task 14.1.2: Add Pipeline Domain Helper
- Location: preferably new `app/src/main/java/com/followupnadlan/pipeline/LeadPipeline.kt`
- Description: Add pure Kotlin functions for status decisions and timestamp updates, keeping Android/Room out of the core logic.
- Dependencies: Task 14.1.1
- Acceptance criteria:
  - Functions cover: create pending post-call task, snooze task, mark WhatsApp opened, mark saved as lead, close/dismiss task.
  - No Android context, no Compose state, no database access inside the pure helper.
  - Uses epoch millis consistently.
- Validation command or manual check: targeted unit test for the helper.
- Rollback: delete helper and related tests.

### Task 14.1.3: Add Pipeline Unit Tests
- Location: `app/src/test/java/com/followupnadlan/pipeline/LeadPipelineTest.kt`
- Description: Test deterministic status transitions and timestamp preservation.
- Dependencies: Task 14.1.2
- Acceptance criteria:
  - Pending task preserves phone, contact name, template, draft, call metadata.
  - Snooze sets `SNOOZED` and `reminderAtEpochMs`.
  - Close sets terminal state and clears future reminder when applicable.
  - WhatsApp-opened updates only the expected status/time fields.
- Validation command or manual check: `.\gradlew.bat test`
- Rollback: delete new test/helper files.

## Sprint 14.2: DAO Operations And Duplicate Control

**Goal**: Make local storage support the pipeline without saving noisy duplicate tasks.  
**Demo / Validation**: Tests or source review show one active follow-up per relevant task path and lookup by active statuses works.  
**Stop condition**: Stop before UI changes.

### Task 14.2.1: Extend DAO Queries Narrowly
- Location: `FollowUpTaskDao.kt`, `LeadDao.kt`
- Description: Add only the queries required by the pipeline.
- Dependencies: Sprint 14.1
- Acceptance criteria:
  - Query active tasks by phone/status when phone exists.
  - Query due/active tasks if needed by the reminder flow.
  - Update existing lead by phone without creating avoidable duplicates, or document why insert-only remains acceptable for this sprint.
  - No broad CRM list/filter API.
- Validation command or manual check: DAO signatures compile; update/add JVM contract tests if no instrumented Room test exists.
- Rollback: remove added DAO methods.

### Task 14.2.2: Prevent Obvious Duplicate Reminders
- Location: `ReminderScheduler.kt`, pipeline helper, or narrow caller-side logic
- Description: Ensure snoozing the same restored task replaces existing work instead of stacking reminders.
- Dependencies: Task 14.2.1
- Acceptance criteria:
  - Existing `ReminderScheduler.workNameFor(taskId)` remains stable.
  - Updating a restored task reuses its id.
  - A new task is created only when no restored task exists.
- Validation command or manual check: `ReminderSchedulerTest` plus source review of insert/update path.
- Rollback: revert scheduler/caller changes.

## Sprint 14.3: Post-Call Pop-Out Decision Card UI

**Goal**: Replace the generic "what happened?" decision flow with a compact, action-oriented post-call card that feels like a pop-out after notification tap while staying in-app and user-driven.  
**Demo / Validation**: Notification tap opens the decision card; the card lets the user choose WhatsApp, snooze, save/track, or close.  
**Stop condition**: Stop before changing reminder notification behavior.

### Task 14.3.1: Preserve Notification-Tap Entry
- Location: `FollowUpLaunchState`, `FollowUpApp`, `FollowUpNotificationHelper.kt`
- Description: Keep post-call entry tap-driven through existing `PendingIntent` contract and route it into the decision card.
- Dependencies: Sprint 14.2
- Acceptance criteria:
  - No Activity launch from service/receiver.
  - No overlay or full-screen intent.
  - Existing extras remain backward compatible.
  - Fallback empty-phone card still opens if call log data is unavailable.
- Validation command or manual check: app-only source review and `rg -n "SYSTEM_ALERT_WINDOW|fullScreenIntent|startActivity" app/src/main`
- Rollback: restore previous notification routing.

### Task 14.3.2: Build The Decision Card Experience
- Location: `MainActivity.kt`, possibly extracted `postcall/PostCallDecisionUi.kt` if keeping `MainActivity.kt` readable
- Description: Add a Hebrew RTL card for post-call next steps.
- Dependencies: Task 14.3.1
- Acceptance criteria:
  - Card shows contact/name/phone when available.
  - Card presents the next actions clearly:
    - open WhatsApp with prepared message
    - snooze/remind later
    - save/track lead
    - close/no follow-up
  - Visual style is compact and phone-readable; no nested card clutter.
  - If the app is foreground, the same card can be shown without notification.
  - If the app is background, user must tap notification.
- Validation command or manual check: manual emulator/phone visual check; verify RTL and mixed phone-number layout.
- Rollback: revert UI extraction/changes.

### Task 14.3.3: Write User Actions To Pipeline State
- Location: `MainActivity.kt`, pipeline helper, DAOs
- Description: Wire decision-card actions to `FollowUpTaskEntity` and `LeadEntity`.
- Dependencies: Task 14.3.2
- Acceptance criteria:
  - Opening WhatsApp marks task as `WHATSAPP_OPENED`.
  - Snooze persists full card state and schedules WorkManager.
  - Save/track creates or updates lead and marks task saved/tracked.
  - Close marks task terminal and cancels active reminder if one exists.
  - User still manually presses Send inside WhatsApp.
- Validation command or manual check: unit tests for pure logic; manual app flow check.
- Rollback: revert action handlers.

## Sprint 14.4: Reminder Return Flow And Close Actions

**Goal**: A snoozed card comes back cleanly and can be opened, snoozed again, completed, or closed.  
**Demo / Validation**: A reminder notification opens the original task with draft restored.  
**Stop condition**: Stop before any AI/transcription or WhatsApp reply detection.

### Task 14.4.1: Harden Reminder Notification Actions
- Location: `ReminderNotificationHelper.kt`, `ReminderWorker.kt`
- Description: Keep reminder notification simple and compatible with notification permission denial.
- Dependencies: Sprint 14.3
- Acceptance criteria:
  - Notification opens task by id.
  - Missing task or non-snoozed task is skipped safely.
  - Notification copy is Hebrew and not mojibake in source.
  - If notification permission is denied, worker exits gracefully and does not falsely claim delivery.
- Validation command or manual check: source review plus worker tests where feasible.
- Rollback: restore prior reminder helper/worker.

### Task 14.4.2: Restore Card From Snoozed Task
- Location: `FollowUpLaunchState`, `MainActivity.kt`
- Description: Opening reminder should restore phone, name, template, draft, task id, and route into the same pipeline card/composer path.
- Dependencies: Task 14.4.1
- Acceptance criteria:
  - Restored task does not create a duplicate task just by opening.
  - Resnooze uses the same task id when appropriate.
  - Close cancels future reminders.
  - WhatsApp-opened updates the restored task.
- Validation command or manual check: manual reminder smoke, source review of id preservation.
- Rollback: revert launch-state/reminder restore changes.

## Sprint 14.5: Docs, Validation, And Review

**Goal**: Close Sprint 14 truthfully with evidence, without overclaiming real-phone behavior.  
**Demo / Validation**: Build/tests pass where available; manual smoke status is explicitly PASS/NOT RUN/PASS WITH NOTES.  
**Stop condition**: Stop after `REVIEW.md` records pass/fail and remaining risks.

### Task 14.5.1: Update Context Docs
- Location: `context/DATA_CONTRACTS.md`, `context/FOLLOW_UP_CARD.md`, `context/SNOOZE_REMINDERS.md`
- Description: Update only the docs whose contracts changed.
- Dependencies: Sprint 14.4
- Acceptance criteria:
  - Pipeline statuses and transitions are documented.
  - Notification-only rule remains clear.
  - No backend/API language is introduced.
  - AI/transcription are explicitly later layers if mentioned.
- Validation command or manual check: `rg -n "backend|API|SYSTEM_ALERT_WINDOW|Accessibility|auto-send|OpenRouter|GPT|transcription" context tasks/sprint-14-lead-pipeline`
- Rollback: revert doc changes.

### Task 14.5.2: Create Sprint Evidence Docs
- Location:
  - `tasks/sprint-14-lead-pipeline/EXECUTION_LOG.md`
  - `tasks/sprint-14-lead-pipeline/MANUAL_SMOKE_TEST.md`
  - `tasks/sprint-14-lead-pipeline/REVIEW.md`
- Description: Record implementation evidence and review results.
- Dependencies: all prior sub-sprints
- Acceptance criteria:
  - Execution log lists each sub-sprint and validation.
  - Manual smoke does not claim PASS without actual device evidence.
  - Review checks manifest/permissions, WhatsApp user control, fallback, snooze, reminders, and post-call route.
- Validation command or manual check: source review.
- Rollback: delete sprint evidence docs if execution is abandoned.

### Task 14.5.3: Run Validation
- Location: repo root
- Description: Run source/build/test checks.
- Dependencies: all prior sub-sprints
- Acceptance criteria:
  - `.\gradlew.bat test` result recorded.
  - `.\gradlew.bat assembleDebug` result recorded if run.
  - Manifest diff reviewed and expected to be empty.
  - Gradle diff reviewed and expected to be empty.
  - Dirty unrelated files reported separately.
- Validation command or manual check:
  - `git diff -- app/src/main/AndroidManifest.xml`
  - `git diff -- build.gradle.kts app/build.gradle.kts settings.gradle.kts`
  - `.\gradlew.bat test`
  - `.\gradlew.bat assembleDebug`
- Rollback: revert Sprint 14 code/doc files only; do not touch unrelated dirty files.

## Testing Strategy
- Unit tests:
  - Pipeline status transitions.
  - Reminder work-name stability.
  - Room contract field preservation if schema changes.
  - Existing WhatsApp link and phone normalization tests must remain green.
- Instrumented tests:
  - Not required unless DAO behavior cannot be meaningfully verified with JVM tests.
- Manual QA:
  - Manual composer still opens WhatsApp with user pressing Send.
  - Post-call notification tap opens decision card.
  - Empty phone fallback works.
  - Snooze restores draft and selected template.
  - Close stops the task from reappearing.
  - Save/track lead preserves phone/name.
- Device/OEM checks:
  - Android 13+ notification denial path.
  - Samsung/Xiaomi battery guidance not regressed.
  - Real call smoke only if human performs it; do not infer PASS from source/build.

## Permission Impact
- Added permissions: none.
- Removed permissions: none.
- Manifest risk: should be empty.
- User disclosure required: no new disclosure; existing notification/call-log explanations remain.

## Data/Schema Impact
- Room entities:
  - Prefer no schema change.
  - If new fields are absolutely required, stop and get human approval before migration.
- migrations:
  - Expected: none.
  - If schema version changes, add migration and update schema JSON deliberately.
- local data retention:
  - Store follow-up tasks only when a post-call card, snooze, saved lead, or user action justifies it.
  - Do not store entire call history automatically.

## UX Impact
- Screens affected:
  - Post-call decision screen.
  - Manual composer action area.
  - Reminder restore flow.
- RTL/Hebrew checks:
  - All visible strings must remain Hebrew and source-readable.
  - Phone numbers must not break RTL layout.
- Empty/fallback states:
  - No phone number.
  - No contact name.
  - Notification permission denied.
  - Reminder task missing.
  - WhatsApp not installed.
- Error states:
  - Invalid phone before WhatsApp.
  - Blank message before WhatsApp/share/copy.
  - Reminder scheduled but notification permission missing.

## Rollback Plan
1. Revert Sprint 14 files only.
2. If no Room schema changed, rollback is code/doc only.
3. If Room schema changed after explicit approval, rollback must include schema version/migration reversal instructions.
4. Preserve unrelated user changes and unrelated dirty files.
5. Confirm `git diff -- app/src/main/AndroidManifest.xml` and Gradle diffs are either empty or intentionally documented.

## Review Checklist
- AndroidManifest is clean.
- No AccessibilityService.
- No `SYSTEM_ALERT_WINDOW`.
- No full-screen intent.
- No Activity launch directly from service/receiver/background callback.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Snooze restores prepared card.
- Fallback works without `READ_CALL_LOG`.
- Fallback works without `READ_CONTACTS`.
- Notification-denied state is visible and truthful.
- Setup/self-test status is not broken.
- Room stores only approved local data.
- No backend/API/cloud dependency.
- No AI/transcription/OpenRouter code.
- Existing notification contract remains compatible.
- Manual smoke claims are truthful.

## Potential Gotchas
- Android background restrictions: the card cannot be force-opened after a call; notification tap is the safe entry.
- Notification permission denial: WorkManager can fire while no reminder notification is shown.
- `READ_CALL_LOG` denial fallback: card must still work with empty phone.
- OEM battery killing FGS: Sprint 13 self-test/guidance must not be bypassed.
- Duplicate reminders: resnoozing a restored task must replace existing work, not stack reminders.
- Invalid phone formatting for wa.me: normalize and validate before opening WhatsApp.
- RTL text and mixed Hebrew/phone-number layout: decision card must be checked visually.
- Room migration risk: avoid schema change unless absolutely necessary.
- Direct-APK update/install friction: no Play Store assumptions.
- Mojibake risk: Hebrew files should be inspected in UTF-8-aware editor before claiming text quality.
- Scope creep risk: second-property extraction, AI classification, and WhatsApp reply detection are future layers, not Sprint 14.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.
- First execution target: Sprint 14.1 only.
- Required stop after Sprint 14.1: report diff, tests, and whether a Room schema change is still avoidable.
