# Codex /goal - Sprint 14 Lead Pipeline

Paste the block below into Codex 5.3 High as a single `/goal` message. The existing PLAN.md is approved for full Sprint 14 scope, but execution must remain gated and sequential.

---

/goal

You are Codex 5.3 High executing on the FollowUp Nadlan Android repo at `F:\followup`.

## Mandatory first reads, in order
1. `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
2. `.agents/skills/followup-nadlan-goal-router/SKILL.md`
3. `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
4. `.agents/skills/followup-nadlan-reviewer/SKILL.md`
5. `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
6. `tasks/sprint-14-lead-pipeline/PLAN.md`
7. `context/DATA_CONTRACTS.md`
8. `context/POST_CALL_ENGINE.md`
9. `context/SNOOZE_REMINDERS.md`
10. `context/FOLLOW_UP_CARD.md`
11. `context/PERMISSIONS_AND_PRIVACY.md`
12. `context/ARCHITECTURE.md`
13. `context/DO_NOT_BUILD.md`
14. `app/src/main/java/com/followupnadlan/MainActivity.kt`
15. `app/src/main/java/com/followupnadlan/data/AppDatabase.kt`
16. `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskEntity.kt`
17. `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt`
18. `app/src/main/java/com/followupnadlan/data/lead/LeadEntity.kt`
19. `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt`
20. `app/src/main/java/com/followupnadlan/snooze/ReminderScheduler.kt`
21. `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`
22. `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
23. `app/src/main/java/com/followupnadlan/notifications/ReminderNotificationHelper.kt`
24. `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt`

## Approval and task

Treat `tasks/sprint-14-lead-pipeline/PLAN.md` as Human-approved by this `/goal` message.

If the file still says `Status: Draft`, update only that status line to `Status: Human-approved by current /goal request` before execution.

Then execute Sprint 14 according to the plan via the goal-executor skill.

## Product goal

Turn FollowUp Nadlan from "prepare a WhatsApp message after a call" into a local lead pipeline:

```text
call ended
-> notification tap
-> compact post-call decision card
-> WhatsApp / snooze / save-track / close
-> local task or lead state is remembered
-> reminder returns only when needed
```

This sprint should reduce Michael's fear of forgetting follow-up. It is not an AI sprint.

## Strict sub-sprint order

Execute these sub-sprints sequentially. You may continue to the next sub-sprint only if the current gate passes.

1. Sprint 14.1: Pipeline contract and pure domain logic.
   - Add/align status vocabulary.
   - Add pure Kotlin pipeline helper if useful.
   - Add unit tests.
   - Gate: `.\gradlew.bat test` passes, and no UI code is touched except compile-only adjustments if unavoidable.

2. Sprint 14.2: DAO operations and duplicate control.
   - Add only narrow DAO methods needed by pipeline.
   - Prevent obvious duplicate reminders/tasks.
   - Gate: tests pass, no Room schema change unless explicitly justified and approved by the plan's stop condition.

3. Sprint 14.3: Post-call pop-out decision card UI.
   - Build an in-app Compose decision card that feels like a pop-out after notification tap.
   - It must be opened by existing notification PendingIntent or foreground app state only.
   - Wire actions: WhatsApp, snooze, save/track, close.
   - Gate: source review confirms no overlay, no background Activity launch, no auto-send.

4. Sprint 14.4: Reminder return flow and close actions.
   - Reminder notification restores the same task/card.
   - Resnooze preserves task identity when appropriate.
   - Close cancels future reminders and marks terminal state.
   - Gate: reminder restore path is source-reviewed and tested where feasible.

5. Sprint 14.5: Docs, validation, and review.
   - Update only relevant context docs.
   - Create/update `EXECUTION_LOG.md`, `MANUAL_SMOKE_TEST.md`, and `REVIEW.md`.
   - Record truthful validation and manual smoke state.

## Hard rules

- Do NOT add GPT-4o mini, OpenRouter, LLM calls, AI analysis, prompt files, or network AI code.
- Do NOT add transcription, SpeechRecognizer, microphone permission, or `RECORD_AUDIO`.
- Do NOT add `SYSTEM_ALERT_WINDOW`, overlay, full-screen intent, or forced Activity launch.
- Do NOT add `NotificationListenerService` or WhatsApp reply detection.
- Do NOT add AccessibilityService or any WhatsApp auto-send behavior.
- Do NOT add backend/API/cloud sync/CRM sync/Firestore/Supabase/account system.
- Do NOT add a CRM dashboard, analytics, campaign/bulk messaging, or property-management module.
- Do NOT add multi-property/entity graph handling for wife/owner/second-apartment scenarios.
- Do NOT add new permissions.
- Do NOT break manual mode if `READ_CALL_LOG`, `READ_CONTACTS`, or notifications are denied.
- Do NOT migrate profile/templates/property/log into Room.
- Do NOT store all calls automatically.
- Do NOT silently change Gradle or dependencies.
- Do NOT change the notification contract unless it is backward-compatible:
  - `ACTION_OPEN_FOLLOW_UP`
  - existing extras
  - request code `8001`
  - notification id `8001`
  - user-tap PendingIntent behavior
- Do NOT mark manual smoke PASS without explicit real Android phone evidence from the human.

## Pop-out card interpretation

The user wants a "pop-out card" after the call.

For Sprint 14, this means:

```text
call ends
-> notification appears
-> user taps notification
-> in-app compact decision card opens
```

It does NOT mean Android overlay, `SYSTEM_ALERT_WINDOW`, `USE_FULL_SCREEN_INTENT`, or opening an Activity directly from a service/receiver.

## Expected implementation direction

Prefer the smallest stable architecture:

- Keep Room entities as the existing `FollowUpTaskEntity` and `LeadEntity` if possible.
- Add pure pipeline helper under `app/src/main/java/com/followupnadlan/pipeline/` if it reduces Compose/Room coupling.
- Keep status strings centralized enough to prevent drift.
- Keep reminders WorkManager-based through existing `ReminderScheduler`.
- Keep WhatsApp opening through existing wa.me / `ACTION_VIEW` user-driven path.
- Preserve empty-phone fallback.
- Preserve existing manual composer, templates, share, copy, My Details, setup wizard, and self-test flows.

## Validation after every meaningful milestone

Run and record:

```powershell
.\gradlew.bat test
```

Run `assembleDebug` after UI, notification, reminder, or database behavior changes:

```powershell
.\gradlew.bat assembleDebug
```

If Gradle wrapper lock fails due to the known Windows/Gradle cache issue, rerun with the required approved/escalated access and record the exact evidence. Do not treat wrapper lock as a code failure.

Check Manifest/Gradle diff:

```powershell
git diff -- app/src/main/AndroidManifest.xml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties
```

Expected: empty. If not empty, stop unless the change is compile-only and clearly justified.

Run forbidden-scope grep:

```powershell
rg -n "RECORD_AUDIO|FOREGROUND_SERVICE_TYPE_MICROPHONE|SpeechRecognizer|OpenRouter|GPT|LLM|SYSTEM_ALERT_WINDOW|USE_FULL_SCREEN_INTENT|fullScreenIntent|BIND_ACCESSIBILITY_SERVICE|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|QUERY_ALL_PACKAGES|backend|server|Firestore|Supabase|retrofit|okhttp|auto-send|autosend|SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager" app build.gradle.kts settings.gradle.kts gradle.properties context tasks/sprint-14-lead-pipeline
```

Expected: no implementation use. Mentions in PLAN/prompt/docs as forbidden scope are acceptable if clearly framed as non-goals.

Run notification contract review:

```powershell
rg -n "ACTION_OPEN_FOLLOW_UP|EXTRA_PHONE|EXTRA_LEAD_NAME|EXTRA_TEMPLATE_ID|REQUEST_CODE_OPEN_FOLLOW_UP|notify\\(8001|NOTIFICATION_ID" app/src/main/java/com/followupnadlan
```

Confirm compatibility with Sprint 8/10/11 routing.

Run status review:

```powershell
rg -n "FOLLOW_UP_STATUS|SNOOZED|WHATSAPP_OPENED|SAVED_AS_LEAD|DISMISSED|CLOSED|PENDING" app/src/main/java app/src/test/java context
```

Confirm statuses are coherent and documented.

Check Hebrew/mojibake risk:

```powershell
Select-String -Path app\src\main\java\com\followupnadlan\*.kt,app\src\main\java\com\followupnadlan\**\*.kt,app\src\test\java\com\followupnadlan\**\*.kt -Pattern "Ã—|Ãƒ|Ã¢"
```

PowerShell may render existing Hebrew incorrectly. If output is ambiguous, verify actual UTF-8 file contents before claiming a new mojibake regression.

## Required evidence files

Create or update:

- `tasks/sprint-14-lead-pipeline/EXECUTION_LOG.md`
- `tasks/sprint-14-lead-pipeline/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-14-lead-pipeline/REVIEW.md`

Use truthful language:

- Unit/build/source checks can be PASS if actually run and passing.
- Manual phone smoke remains `NOT RUN` unless the human provides explicit real-device evidence.
- If only emulator/source validation is done, mark `PASS WITH NOTES` where appropriate.

## Stop conditions

Stop and report before continuing if:

- Any forbidden permission or API appears necessary.
- A Room schema migration appears necessary.
- A Gradle/dependency change appears necessary.
- Notification tap contract would break.
- Manual fallback would break.
- WhatsApp auto-send or Accessibility becomes tempting.
- Background Activity launch or overlay becomes necessary.
- AI/transcription/OpenRouter is needed to complete this sprint.
- Tests fail for an unrelated reason that cannot be isolated.
- The implementation requires touching files marked "Must Not Change" in the PLAN.

## Completion response

When Sprint 14 is complete, report:

- Sub-sprints completed and any gates that blocked.
- Files changed, with absolute paths.
- Test command results.
- `assembleDebug` result if run.
- Manifest/Gradle diff result.
- Forbidden-scope grep result.
- Notification contract review result.
- Status review result.
- Hebrew/mojibake check result.
- Manual smoke status.
- Deviations from PLAN.md.
- Whether work is ready for `followup-nadlan-reviewer`.

Begin by reading the mandatory files, updating PLAN status to Human-approved if needed, creating `EXECUTION_LOG.md`, and executing Sprint 14.1 only. Continue to later sub-sprints only when each gate passes.
