# PLAN: Sprint 12 Snooze + Local Lead + Reminder Notification on Room

**Status**: Human-approved
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: High
**Generated**: 2026-05-20

## Goal Statement
Close the core post-call loop end to end: from any of the four follow-up cards, let the agent press "הזכר לי אחר כך", pick a snooze preset, persist a full FollowUpTask in Room, schedule a WorkManager reminder, and when it fires post a rich reminder notification that reopens the exact same prepared card with the same draft, lead name, phone, and template — and let the agent optionally save the lead locally.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes — this sprint completes it
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This implements Layer 3 from `context/ROADMAP.md` and the Snooze contract from `context/SNOOZE_REMINDERS.md`, which the constitution marks as Core (not optional). It introduces Room and WorkManager — the two stack components the constitution and `context/ARCHITECTURE.md` already require for MVP. It uses WorkManager (not SCHEDULE_EXACT_ALARM) per the explicit constitution rule, and stores data locally with no backend.

## Scope Note: Large Sprint, Hard Gates
This sprint is deliberately larger than prior sprints because the human chose the full scope (Snooze + Lead + rich Reminder notification) in one pass. To control the risk, execution is split into five sub-sprints with mandatory validation gates. Do NOT begin a sub-sprint until the previous one's gate passes. The two Gradle dependency additions (Room, WorkManager) are isolated to Sub-sprint 1 and Sub-sprint 3 respectively, each validated alone before any feature code uses them.

## Non-Goals
- Do not use `SCHEDULE_EXACT_ALARM`, `AlarmManager`, `setExact`, or `setExactAndAllowWhileIdle`. WorkManager only. Reminders are approximate by design.
- Do not auto-restart reminders via `RECEIVE_BOOT_COMPLETED` in this sprint. WorkManager already persists across process death; full boot-restart hardening is a later sprint.
- Do not add backend/API, cloud sync, accounts, Firestore, or Supabase.
- Do not add `READ_CALL_LOG` or `READ_CONTACTS` changes — they already exist from Sprint 11 and must not be touched.
- Do not auto-send WhatsApp. Do not add Accessibility, SYSTEM_ALERT_WINDOW, QUERY_ALL_PACKAGES, SMS, NotificationListenerService.
- Do not build a full CRM, lead pipeline, dashboard, analytics, or lead list/search UI. Lead save in this sprint is a single explicit "save as lead" action plus minimal status — not a management surface.
- Do not redesign the four cards, My Details, templates, or the manual composer. Reuse them.
- Do not migrate existing SharedPreferences data (profile, templates, property links, action log) into Room. Those stay where they are. Room is introduced ONLY for FollowUpTask and Lead.
- Do not change the existing Action Log behavior or its three action types, except to add `REMINDER_SCHEDULED` and `REMINDER_FIRED` if and only if Sub-sprint 5 explicitly requires them. If unsure, leave the Action Log untouched.
- Do not mark manual smoke PASS without real Android phone evidence.

## Assumptions
- minSdk 26, targetSdk 34 unchanged.
- Room version compatible with the existing Kotlin 2.0.21 / AGP 8.7.3 toolchain. Use the Room version that matches the Compose BOM era already in the project; if KSP is needed, prefer KSP over kapt for Kotlin 2.0 compatibility. The exact versions are an implementation detail to confirm at build time, but no other dependencies may be added.
- WorkManager `androidx.work:work-runtime-ktx` at a version compatible with the current AndroidX stack.
- The four cards from Sprint 10/11 already produce a prepared message in the composer. This sprint adds a snooze entry point alongside the existing "Open WhatsApp" path on the card/composer surface.
- Snooze presets follow `context/SNOOZE_REMINDERS.md`: בעוד 15 דקות, בעוד 30 דקות, בעוד שעה, בערב (default 20:00 local), מחר בבוקר (default 09:00 local). "בחר שעה" is optional and only if trivial; if it risks pulling in exact-alarm expectations, omit it.
- A snoozed FollowUpTask preserves: phone, contactName, callEndedAt, selectedTemplateId, draftText, leadType, propertyLink, reminderAt, status — per `context/DATA_CONTRACTS.md` FollowUpTask schema.
- The reminder notification uses a separate channel `snooze_reminders` distinct from the post-call channel.
- Tapping the reminder notification reopens the composer via a PendingIntent carrying the FollowUpTask id; the composer loads the task from Room and restores state. No Activity is launched from the worker directly.
- Lead save reuses the same Room database with a separate `leads` table per `context/DATA_CONTRACTS.md` LeadEntity. Saving a lead is optional and never blocks sending or snoozing.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
- `context/SNOOZE_REMINDERS.md`
- `context/FOLLOW_UP_CARD.md`
- `context/DATA_CONTRACTS.md`
- `context/ARCHITECTURE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/DO_NOT_BUILD.md`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- The four card screens and post-call wiring added in Sprint 10/11
- `app/src/main/java/com/followupnadlan/postcall/*`
- `app/src/main/java/com/followupnadlan/log/*`

## Files Expected To Change
- `app/build.gradle.kts` (add Room + WorkManager dependencies, KSP plugin if needed)
- `build.gradle.kts` (KSP plugin classpath if needed)
- `app/src/main/AndroidManifest.xml` (no new permission expected; WorkManager self-registers. Verify.)
- `app/src/main/java/com/followupnadlan/data/AppDatabase.kt` (new)
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskEntity.kt` (new)
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt` (new)
- `app/src/main/java/com/followupnadlan/data/lead/LeadEntity.kt` (new)
- `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt` (new)
- `app/src/main/java/com/followupnadlan/snooze/SnoozeOption.kt` (new, pure)
- `app/src/main/java/com/followupnadlan/snooze/SnoozeTimeCalculator.kt` (new, pure)
- `app/src/main/java/com/followupnadlan/snooze/ReminderScheduler.kt` (new)
- `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt` (new)
- `app/src/main/java/com/followupnadlan/notifications/ReminderNotificationHelper.kt` (new)
- `app/src/main/java/com/followupnadlan/MainActivity.kt` (snooze entry point, task restore, lead save action)
- `app/src/test/java/com/followupnadlan/snooze/SnoozeTimeCalculatorTest.kt` (new)
- `app/src/test/java/com/followupnadlan/data/FollowUpTaskMappingTest.kt` (new, pure mapping logic)
- `tasks/sprint-12-snooze-lead-reminder/PLAN.md`
- `tasks/sprint-12-snooze-lead-reminder/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-12-snooze-lead-reminder/EXECUTION_LOG.md`
- `tasks/sprint-12-snooze-lead-reminder/REVIEW.md`

## Files That Must Not Change
- `app/src/main/java/com/followupnadlan/profile/*`
- `app/src/main/java/com/followupnadlan/templates/*`
- `app/src/main/java/com/followupnadlan/whatsapp/*`
- `app/src/main/java/com/followupnadlan/log/*` (unless Sub-sprint 5 explicitly adds two log types with human confirmation)
- The Sprint 8 / Sprint 11 notification contract (`FollowUpNotificationHelper` public surface)
- `gradle/wrapper/*`, `settings.gradle.kts`, `gradle.properties` (KSP/Room repos already available via google()/mavenCentral())
- All prior sprint task folders.
- `context/*` unless behavior contradicts a context doc, in which case stop and re-enter the planner.

## Sub-sprint 1: Introduce Room (Empty DB, No Features)
**Goal**: Add Room as a dependency and create an empty, compiling, migration-safe database with the two entities and DAOs — wired to nothing yet.
**Demo / Validation**: `.\gradlew.bat assembleDebug` compiles with Room. A trivial DAO insert/read works in an instrumented or in-memory test. No feature uses the DB yet.
**Stop condition**: Stop if Room forces a Gradle toolchain change beyond adding the Room artifacts and (if required) the KSP plugin. Do not add unrelated dependencies. If kapt vs KSP causes Kotlin 2.0 friction, prefer KSP and record the decision; if neither works cleanly, stop and report.

### Task 1.1: Add Room + KSP to Gradle
- Location: `app/build.gradle.kts`, `build.gradle.kts`
- Description: Add `androidx.room:room-runtime`, `androidx.room:room-ktx`, and the Room compiler via KSP. Add the KSP plugin to the version catalog/plugins blocks. Add nothing else.
- Acceptance criteria: `git diff` on Gradle files shows only Room + KSP additions. No WorkManager yet (that is Sub-sprint 3). No other library.
- Validation command or manual check: `.\gradlew.bat assembleDebug`; `git diff -- app/build.gradle.kts build.gradle.kts`.
- Rollback: Remove the Room/KSP lines.

### Task 1.2: Define FollowUpTaskEntity + LeadEntity
- Location: `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskEntity.kt`, `app/src/main/java/com/followupnadlan/data/lead/LeadEntity.kt`
- Description: Implement the two entities exactly per `context/DATA_CONTRACTS.md` (epoch-millis fields, string-encoded enums, indices). Status and source stored as strings.
- Acceptance criteria: Field names and types match the data contract; no extra fields invented; enums round-trip via string.
- Validation command or manual check: Source review against `DATA_CONTRACTS.md`.
- Rollback: Delete the entity files.

### Task 1.3: Define DAOs + AppDatabase
- Location: `FollowUpTaskDao.kt`, `LeadDao.kt`, `AppDatabase.kt`
- Description: DAO methods needed for this sprint only: insert/update task, get task by id, list tasks by status, delete task; insert lead, get lead by phone, list leads. `AppDatabase` with version 1, exportSchema true, singleton accessor.
- Acceptance criteria: No queries beyond what the sprint uses; version 1; no premature migration code.
- Validation command or manual check: `.\gradlew.bat assembleDebug`.
- Rollback: Delete the DAO/DB files.

### Task 1.4: Minimal Room Round-Trip Test
- Location: instrumented test or in-memory `Room.inMemoryDatabaseBuilder` test under androidTest, OR a pure mapping test if instrumented infra is not set up.
- Description: Prove insert + read of one FollowUpTask and one Lead. If androidTest infra is missing and adding it would expand scope, instead extract entity↔domain mapping into a pure function and unit-test that, and verify the DB path manually in smoke.
- Acceptance criteria: Either an instrumented round-trip passes, or pure mapping is unit-tested and DB round-trip is added to the manual smoke checklist with explicit justification.
- Validation command or manual check: `.\gradlew.bat test` or `connectedAndroidTest` if available.
- Rollback: Delete the test.

**GATE 1**: assembleDebug passes with Room; Gradle diff is Room+KSP only; entities match the data contract. Do not proceed otherwise.

## Sub-sprint 2: Pure Snooze Time Logic
**Goal**: Compute reminder timestamps for each preset, deterministically and testably, with no Android/WorkManager dependency.
**Demo / Validation**: `.\gradlew.bat test` proves each preset maps to the correct future timestamp given a fixed "now".

### Task 2.1: Define SnoozeOption
- Location: `app/src/main/java/com/followupnadlan/snooze/SnoozeOption.kt`
- Description: `enum class SnoozeOption { IN_15_MIN, IN_30_MIN, IN_1_HOUR, TONIGHT, TOMORROW_MORNING }` with Hebrew display labels matching `SNOOZE_REMINDERS.md`.
- Acceptance criteria: Exactly these presets; Hebrew labels correct.
- Validation command or manual check: Source review.
- Rollback: Delete the file.

### Task 2.2: Define SnoozeTimeCalculator (Pure)
- Location: `app/src/main/java/com/followupnadlan/snooze/SnoozeTimeCalculator.kt`
- Description: Pure function `computeTriggerAt(option: SnoozeOption, nowMillis: Long, zoneId: ZoneId): Long`. Relative options add the delta. TONIGHT resolves to 20:00 local today, or tomorrow 20:00 if already past. TOMORROW_MORNING resolves to 09:00 local next day.
- Acceptance criteria: No Android imports; uses `java.time`; deterministic given inputs.
- Validation command or manual check: `.\gradlew.bat test` via Task 2.3.
- Rollback: Delete the file.

### Task 2.3: SnoozeTimeCalculator Tests
- Location: `app/src/test/java/com/followupnadlan/snooze/SnoozeTimeCalculatorTest.kt`
- Description: Cover each preset with a fixed clock; TONIGHT before 20:00 → today 20:00; TONIGHT after 20:00 → tomorrow 20:00; TOMORROW_MORNING → next day 09:00; relative deltas exact; DST-edge case at least noted if not fully covered.
- Acceptance criteria: All cases pass; no Android dependency.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete the test.

**GATE 2**: All snooze time tests pass. Do not proceed otherwise.

## Sub-sprint 3: Introduce WorkManager + Reminder Scheduling
**Goal**: Add WorkManager and schedule/cancel reminders that persist a task and fire a worker.
**Demo / Validation**: A scheduled reminder with a short delay fires the worker on a real phone; the worker reads the FollowUpTask from Room and posts the reminder notification.
**Stop condition**: Stop if WorkManager pulls in exact-alarm APIs or requires `SCHEDULE_EXACT_ALARM`. It must not.

### Task 3.1: Add WorkManager to Gradle
- Location: `app/build.gradle.kts`
- Description: Add `androidx.work:work-runtime-ktx` only.
- Acceptance criteria: Gradle diff shows only this addition (on top of Sub-sprint 1's Room).
- Validation command or manual check: `.\gradlew.bat assembleDebug`; `git diff -- app/build.gradle.kts`.
- Rollback: Remove the WorkManager line.

### Task 3.2: ReminderScheduler
- Location: `app/src/main/java/com/followupnadlan/snooze/ReminderScheduler.kt`
- Description: `schedule(taskId: String, triggerAtMillis: Long)` enqueues a unique `OneTimeWorkRequest` with `setInitialDelay(triggerAt - now)`, using `ExistingWorkPolicy.REPLACE` and a unique work name derived from taskId (prevents duplicate reminders per task — satisfies `SNOOZE_REMINDERS.md` anti-pattern). `cancel(taskId: String)` cancels by unique name.
- Acceptance criteria: No exact-alarm API; unique work name per task; REPLACE policy avoids duplicates.
- Validation command or manual check: Source review; real-phone smoke.
- Rollback: Delete the file.

### Task 3.3: ReminderWorker
- Location: `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`
- Description: `CoroutineWorker` that reads the FollowUpTask id from input data, loads the task from Room, and if status is still SNOOZED, calls `ReminderNotificationHelper.show(task)`. If the task is missing or already handled, returns success without notifying. Does NOT launch an Activity.
- Acceptance criteria: No direct Activity start; idempotent; safe when task is gone.
- Validation command or manual check: Real-phone smoke.
- Rollback: Delete the file.

**GATE 3**: assembleDebug passes with WorkManager; a short-delay reminder fires the worker on a real phone. Do not proceed otherwise.

## Sub-sprint 4: Snooze Entry Point + Task Persistence + Card Restore
**Goal**: Wire the UI loop — snooze from the card, persist the task, and reopen the same card from the reminder.
**Demo / Validation**: On a real phone: open a card → "הזכר לי אחר כך" → pick "בעוד 15 דקות" → notification fires later → tap it → the same card reopens with the same draft/phone/template.

### Task 4.1: ReminderNotificationHelper
- Location: `app/src/main/java/com/followupnadlan/notifications/ReminderNotificationHelper.kt`
- Description: Separate channel `snooze_reminders` (high importance). Builds a rich Hebrew notification: with a name → `{name} מחכה להמשך טיפול`; without a name → `יש שיחת נדל״ן שמחכה להמשך טיפול`. PendingIntent (immutable) opens MainActivity with an action `ACTION_OPEN_SNOOZED_TASK` and extra `EXTRA_TASK_ID`. Distinct notification id namespace from the post-call notification so they don't collide.
- Acceptance criteria: Separate channel; no full-screen intent; immutable PendingIntent; does not reuse the post-call notification id `8001`.
- Validation command or manual check: Real-phone smoke.
- Rollback: Delete the file.

### Task 4.2: Snooze Sheet UI
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a "הזכר לי אחר כך" action on the card/composer surface that opens a simple bottom-sheet/list with the five presets. Selecting a preset: build a FollowUpTask from current composer state (phone, leadName, draftText = renderedMessage source, selectedTemplateId, propertyLink, status = SNOOZED, reminderAt = computed), insert via DAO, schedule via ReminderScheduler, show a Hebrew confirmation `תזכורת נקבעה`.
- Acceptance criteria: Snooze never auto-sends; the existing Open WhatsApp path is unchanged; snooze works even with empty phone (reminder returns with empty phone field per SNOOZE_REMINDERS edge case).
- Validation command or manual check: Real-phone smoke.
- Rollback: Remove the snooze action and sheet.

### Task 4.3: Restore Task From Reminder Tap
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Handle `ACTION_OPEN_SNOOZED_TASK` in intent parsing (mirror the Sprint 8 launch-state pattern). Load the FollowUpTask by id from Room, restore composer state (phone, leadName, draft, template, property), and set status to OPENED. If the task is missing, open the composer cleanly empty.
- Acceptance criteria: Same draft/phone/template restored; status transitions SNOOZED→OPENED; missing task does not crash.
- Validation command or manual check: Real-phone smoke.
- Rollback: Remove the action handling branch.

**GATE 4**: The snooze loop works end to end on a real phone. Do not proceed otherwise.

## Sub-sprint 5: Local Lead Save
**Goal**: Let the agent optionally save the current follow-up as a local lead, without blocking any send/snooze path.
**Demo / Validation**: On a real phone: from the card/composer, "שמור כליד" → minimal lead saved to Room → reopening shows it persists across app restart.

### Task 5.1: Lead Save Action
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add an optional "שמור כליד" action. Saves a LeadEntity (name, phone, type optional default UNKNOWN, status default NEW, note optional) via LeadDao. Never required; never blocks Open WhatsApp or snooze. Show Hebrew confirmation.
- Acceptance criteria: Optional; non-blocking; default status NEW; no lead list/management/search UI; no analytics.
- Validation command or manual check: Real-phone smoke + DB persistence after restart.
- Rollback: Remove the action.

### Task 5.2: Status Transitions Wiring
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt` and DAO usage
- Description: Apply the minimal FollowUpStatus transitions from `SNOOZE_REMINDERS.md`: DRAFT→SNOOZED on snooze; SNOOZED→OPENED on reminder tap; OPENED→WHATSAPP_OPENED when Open WhatsApp is pressed for a restored task; OPENED→SAVED_AS_LEAD when saved. Only wire transitions that the UI actually reaches this sprint.
- Acceptance criteria: Transitions match the contract; no unreachable/dead transitions added.
- Validation command or manual check: Source review + smoke.
- Rollback: Remove the transition updates.

### Task 5.3: (Conditional) Action Log Extension
- Location: `app/src/main/java/com/followupnadlan/log/*`
- Description: ONLY IF needed for traceability and ONLY with the same "real action" discipline: add `REMINDER_SCHEDULED` (logged when a reminder is actually enqueued) and `REMINDER_FIRED` (logged when the worker actually posts the notification). If this risks touching the locked log module in a way that regresses Sprint 9, skip it and note the decision. Never log a reminder that was not actually scheduled.
- Acceptance criteria: If implemented, only these two types added; no false logging; existing three types unchanged.
- Validation command or manual check: `.\gradlew.bat test` for log store.
- Rollback: Revert log additions.

**GATE 5**: Lead save works and persists; status transitions correct; full sprint regression clean.

## Testing Strategy
- Unit tests (pure, must run on JVM without device):
  - `SnoozeTimeCalculatorTest`: all five presets with a fixed clock.
  - `FollowUpTaskMappingTest`: entity↔domain mapping if mapping logic is extracted.
  - Log store tests still pass unchanged (and cover new types if added).
- Instrumented / device:
  - Room round-trip (in-memory or device) for FollowUpTask and Lead.
  - WorkManager reminder fires on a real phone with a short delay (e.g. 1 minute) and a normal preset.
- Build: `.\gradlew.bat test assembleDebug`. Expect the Gradle wrapper lock retry pattern from earlier sprints; record both attempts in EXECUTION_LOG.md.
- Manual QA: Required on a real Android phone before PASS. Minimum one Pixel-class device. The full snooze loop (snooze → wait → notification → reopen same card) MUST be tested with a real wait, not just code review.
- Forbidden-scope grep (clean against `app/` and build files; `tasks/` matches are acceptable docs):

```powershell
rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|backend|server|Firestore|Supabase|retrofit|okhttp|auto-send|autosend|MESSAGE_SENT" app build.gradle.kts settings.gradle.kts gradle.properties
```

- Dependency diff check: `git diff -- app/build.gradle.kts build.gradle.kts` must show ONLY Room, KSP (if used), and WorkManager. Any other dependency is a failure.
- Manifest diff check: ideally empty (WorkManager self-registers its initializer). If a `<provider>` removal or work init change is needed, it must be explained.
- Contract regression: Sprint 8/11 post-call notification still works; the four cards still open and prepare messages; manual composer, share, copy, reset, My Details, templates, property links all still work.

## Permission Impact
- Added permissions: None expected. WorkManager does not require a user permission for approximate work. POST_NOTIFICATIONS from Sprint 8 already covers the reminder notification on Android 13+.
- Removed permissions: None.
- Manifest risk: Low-to-medium. The risk is in WorkManager's auto-initialization provider, not in permissions. Verify no permission creep.
- User disclosure required: None new; the reminder uses the existing notification permission already disclosed.

## Data/Schema Impact
- Room entities: `FollowUpTaskEntity`, `LeadEntity` per `context/DATA_CONTRACTS.md`. Database version 1.
- Migrations: None (version 1). exportSchema = true so future migrations are reviewable.
- Local data retention:
  - FollowUpTask rows persist until resolved/dismissed; consider a later cleanup sprint, not now.
  - Lead rows persist until the user deletes them (delete UI is out of scope this sprint; saving only).
  - Existing SharedPreferences data is NOT migrated.
- Storage mechanism: Room for tasks/leads; existing SharedPreferences untouched for profile/templates/property/log.

## UX Impact
- Screens affected: Card/composer surface gains "הזכר לי אחר כך" and optional "שמור כליד". A simple snooze preset sheet is added. No other screen changes.
- RTL/Hebrew checks: Snooze sheet labels, confirmation toasts, reminder notification (`{name} מחכה להמשך טיפול`), and lead-save confirmation all readable in RTL.
- Empty/fallback states:
  - Snooze with empty phone → reminder returns with empty phone field (allowed).
  - POST_NOTIFICATIONS denied → reminder is scheduled but won't surface; show a clear Hebrew warning when snoozing.
  - Task missing on reminder tap → composer opens empty, no crash.
- Error states: WorkManager enqueue failure → Hebrew error, no crash. Room insert failure → Hebrew error, snooze not silently lost.

## Rollback Plan
1. Remove `app/src/main/java/com/followupnadlan/data/`, `snooze/`, and `ReminderNotificationHelper.kt`.
2. Revert the Sprint 12 hunks in `MainActivity.kt` (snooze action, restore branch, lead save).
3. Remove Room, KSP, and WorkManager from Gradle files.
4. Delete `tasks/sprint-12-snooze-lead-reminder/`.
5. Existing SharedPreferences data is untouched, so prior sprints continue to work unchanged.

## Review Checklist
- Gradle adds ONLY Room, KSP (if used), and WorkManager. No other dependency.
- No SCHEDULE_EXACT_ALARM, AlarmManager, setExact*, RECEIVE_BOOT_COMPLETED.
- No Accessibility, SYSTEM_ALERT_WINDOW, QUERY_ALL_PACKAGES, SMS, NotificationListenerService, full-screen intent.
- No backend/API, no networking library, no cloud sync.
- WorkManager only; unique work name per task; REPLACE policy prevents duplicate reminders.
- Snooze preserves the full prepared task; reminder reopens the SAME card with the SAME draft.
- Snooze works with empty phone.
- Reminder notification uses a separate channel and a distinct notification id from post-call.
- No Activity launched from the worker; only PendingIntent on user tap.
- Lead save is optional and never blocks send/snooze.
- Existing SharedPreferences data not migrated; profile/templates/property/log untouched.
- Sprint 8/10/11 contracts preserved; four cards and post-call flow still work.
- Snooze time logic is pure and unit-tested; Room round-trip verified.
- Manual smoke not claimed PASS without real-phone evidence including a real timed reminder.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sub-sprint at a time, gates 1→2→3→4→5 in strict order. Do not start a sub-sprint until the prior gate passes. The two Gradle dependency additions are isolated (Room in Sub-sprint 1, WorkManager in Sub-sprint 3) and each is validated alone before feature code uses it.
- Expansion rule: no scope expansion without human approval. Specifically, do not add SCHEDULE_EXACT_ALARM, boot-restart, a lead management/list/search UI, analytics, or any second database.

## Potential Gotchas
- Android background restrictions: WorkManager handles deferrable work and survives process death, but OEMs (Xiaomi/MIUI, Realme) may delay or kill work under aggressive battery saving. This is expected and acceptable for approximate reminders; deep OEM hardening is the Setup Wizard sprint.
- Notification permission denial on Android 13+: A scheduled reminder will fire the worker but the notification won't appear. Warn clearly at snooze time.
- READ_CALL_LOG / READ_CONTACTS: Already present from Sprint 11. Do NOT touch them. They are not part of this sprint.
- OEM battery killing reminders: Out of scope; mitigation in Setup Wizard sprint.
- Duplicate reminders: Prevented by unique WorkManager work name per task + REPLACE policy. Verify by snoozing the same task twice.
- Invalid phone formatting for wa.me: Unchanged; snooze stores whatever phone exists, even empty.
- RTL text and mixed Hebrew/phone-number layout: Reminder notification and snooze sheet mix Hebrew and numbers; verify on a real phone.
- Room migration risk: Version 1 only; exportSchema true. Do not write speculative migrations. The real risk is the KSP/kapt + Kotlin 2.0 toolchain — isolate and validate in Sub-sprint 1 before anything depends on Room.
- WorkManager default initializer: The app uses on-demand initialization by default. If the project disables the default initializer, ReminderScheduler must initialize WorkManager correctly. Verify the manifest provider is intact.
- Time zone and DST: SnoozeTimeCalculator uses local zone; TONIGHT/TOMORROW_MORNING resolve against local time. Note DST edges in tests even if not exhaustively covered.
- Doze mode delay: Reminders may be delayed in Doze. This is acceptable because presets are framed as approximate ("בערב", "מחר בבוקר"), per the constitution's no-exact-alarm rule. Do not try to defeat Doze with exact alarms.
- Large sprint risk: Five sub-sprints, two new dependencies. If any gate fails in a way unrelated to the current sub-sprint, stop and invoke systematic-debugging rather than pushing forward.
