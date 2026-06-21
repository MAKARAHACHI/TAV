# Codex /goal - Sprint 14 Fix: Reused Task Must Preserve Current Card State

Paste the block below into Codex 5.3 High as a single `/goal` message. This is a narrow fix pass for the failed Sprint 14 review. Do not reopen Sprint 14 scope.

---

/goal

You are Codex 5.3 High executing a narrow fix on the FollowUp Nadlan Android repo at `F:\followup`.

## Mandatory first reads, in order
1. `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
2. `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
3. `.agents/skills/followup-nadlan-reviewer/SKILL.md`
4. `tasks/sprint-14-lead-pipeline/PLAN.md`
5. `tasks/sprint-14-lead-pipeline/REVIEW.md`
6. `tasks/sprint-14-lead-pipeline/EXECUTION_LOG.md`
7. `context/DATA_CONTRACTS.md`
8. `context/SNOOZE_REMINDERS.md`
9. `context/FOLLOW_UP_CARD.md`
10. `app/src/main/java/com/followupnadlan/MainActivity.kt`
11. `app/src/main/java/com/followupnadlan/pipeline/LeadPipeline.kt`
12. `app/src/test/java/com/followupnadlan/pipeline/LeadPipelineTest.kt`
13. `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt`
14. `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt`

## Task

Fix only the blocker recorded in `tasks/sprint-14-lead-pipeline/REVIEW.md`.

Current review decision is `FAIL` because `PostCallScreen.ensureTask()` reuses an existing active task by phone and returns it before copying the current decision-card state into it. This can make snooze/save/open preserve a stale template or stale draft from a previous active task.

## Expected behavior

When the user is on the Sprint 14 post-call decision card and selects a card/draft, every action must operate on the current prepared card state:

- `selectedTemplateId`
- `draftText`
- `contactName`
- `phone`
- `callEndedAtEpochMs`
- `callDurationSeconds`
- `leadType`, if available
- `propertyLink`
- current source where appropriate

This must be true even when `ensureTask()` reuses:

1. a restored task by id, or
2. an existing active task found by phone/status.

## Required fix shape

Use the smallest safe change.

Preferred implementation:

1. Add a pure helper in `LeadPipeline.kt`, for example:

```kotlin
fun mergeCurrentCardState(
    task: FollowUpTaskEntity,
    phone: String?,
    contactName: String?,
    selectedTemplateId: String?,
    draftText: String?,
    callEndedAtEpochMs: Long?,
    callDurationSeconds: Long?,
    leadType: String?,
    propertyLink: String?,
    source: String?,
    nowEpochMs: Long
): FollowUpTaskEntity
```

2. Preserve task identity and `createdAtEpochMs`.
3. Update only the current card-related fields and `updatedAtEpochMs`.
4. In `MainActivity.kt`, make `ensureTask()` update and return the merged task before any downstream action:
   - restored task by id
   - reused active task by phone
   - newly inserted task already has current fields
5. Add unit coverage proving a reused/restored active task with an old draft/template becomes current before snooze.

If you choose a different shape, keep it equally small and explain it in `DEBUG_LOG.md`.

## Files allowed to change

- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/pipeline/LeadPipeline.kt`
- `app/src/test/java/com/followupnadlan/pipeline/LeadPipelineTest.kt`
- `tasks/sprint-14-lead-pipeline/DEBUG_LOG.md`
- `tasks/sprint-14-lead-pipeline/EXECUTION_LOG.md`
- `tasks/sprint-14-lead-pipeline/REVIEW.md`

Do not change other files unless a compile-only issue makes it unavoidable. If that happens, stop and explain before widening scope.

## Hard rules

- Do NOT add permissions.
- Do NOT touch `AndroidManifest.xml`.
- Do NOT change Gradle, dependencies, or Room schema.
- Do NOT add AI, OpenRouter, transcription, microphone, network calls, backend, or cloud.
- Do NOT add overlay, `SYSTEM_ALERT_WINDOW`, full-screen intent, or background Activity launch.
- Do NOT add WhatsApp auto-send or Accessibility.
- Do NOT rewrite the post-call UI.
- Do NOT change notification action strings, extras, request code `8001`, notification id `8001`, or PendingIntent behavior.
- Do NOT mark manual smoke PASS.

## Required debugging artifact

Create or update:

```text
tasks/sprint-14-lead-pipeline/DEBUG_LOG.md
```

Use the systematic debugging format:

- Symptom
- Expected behavior
- Reproduction steps
- Evidence collected
- Hypotheses
- Root cause
- Minimal fix
- Validation
- Regression risk

## Required review update

After the fix, update `tasks/sprint-14-lead-pipeline/REVIEW.md`.

If the blocker is fixed and validation passes:

- Change `Decision` from `FAIL` to `PASS WITH NOTES`.
- Keep manual QA as `NOT RUN`.
- Keep real-device smoke listed as missing evidence.
- Add a note that reviewer blocker was fixed with source/tests.

If the blocker is not fixed, leave `Decision: FAIL`.

## Validation commands

Run and record:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

Check Manifest/Gradle diff:

```powershell
git diff -- app/src/main/AndroidManifest.xml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties
```

Expected: empty.

Run focused status/source checks:

```powershell
rg -n "mergeCurrentCardState|ensureTask|getLatestByPhoneAndStatuses|selectedTemplateId|draftText|snoozeTask|markWhatsAppOpened|markSavedAsLead|closeTask" app/src/main/java/com/followupnadlan app/src/test/java/com/followupnadlan
```

Run forbidden-scope grep:

```powershell
rg -n "RECORD_AUDIO|FOREGROUND_SERVICE_TYPE_MICROPHONE|SpeechRecognizer|OpenRouter|GPT|LLM|SYSTEM_ALERT_WINDOW|USE_FULL_SCREEN_INTENT|fullScreenIntent|BIND_ACCESSIBILITY_SERVICE|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|WRITE_CALL_LOG|QUERY_ALL_PACKAGES|backend|server|Firestore|Supabase|retrofit|okhttp|auto-send|autosend|SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager" app build.gradle.kts settings.gradle.kts gradle.properties context tasks/sprint-14-lead-pipeline
```

Expected: no implementation use. Mentions in PLAN/prompt/docs as forbidden scope are acceptable if clearly framed as non-goals.

## Acceptance criteria

- A reused active task by phone is updated with the current decision-card selected template and draft before snooze/save/open/close.
- A restored task by id is also updated with current card state before action.
- Newly created task behavior remains unchanged.
- Snooze persists the selected current draft/template.
- Save/track uses the current task data.
- WhatsApp-opened marks the current task.
- Close cancels by the correct task id.
- Unit tests cover stale task -> current card merge.
- `.\gradlew.bat test` passes.
- `.\gradlew.bat assembleDebug` passes.
- Manifest/Gradle diff is empty.
- `REVIEW.md` no longer lists the stale prepared-card blocker if fixed.

## Stop conditions

Stop and report if:

- Fix requires schema migration.
- Fix requires changing DAO signatures beyond current need.
- Fix requires new permissions, Gradle/dependencies, or notification contract change.
- Tests fail for unrelated reasons that cannot be isolated.
- You cannot prove the reused-task stale-card bug is fixed with source and tests.

## Completion response

Report:

- Root cause.
- Files changed.
- Exact fix.
- Test results.
- Manifest/Gradle diff result.
- Forbidden-scope grep result.
- Whether `REVIEW.md` is now `PASS WITH NOTES` or still `FAIL`.
- Manual smoke status, which must remain `NOT RUN` unless the human explicitly provides real-device evidence.

Begin with `DEBUG_LOG.md`, then implement the minimal fix, then validate.
