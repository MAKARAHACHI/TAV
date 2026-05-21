# EXECUTION LOG: Sprint 12 Snooze + Local Lead + Reminder Notification on Room

## Sub-sprint 1: Introduce Room (Empty DB, No Features)
Status: In progress
Started: 2026-05-20
Completed: 2026-05-20

### Changes made
- `tasks/sprint-12-snooze-lead-reminder/PLAN.md`: marked the supplied plan as Human-approved per the current instruction.
- `build.gradle.kts`: added the KSP plugin version for Kotlin 2.0.21.
- `app/build.gradle.kts`: applied KSP, added Room runtime/ktx/compiler, and configured schema export.
- `app/src/main/java/com/followupnadlan/data/AppDatabase.kt`: added Room database version 1 for follow-up tasks and leads only.
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskEntity.kt`: added the FollowUpTask Room entity per the data contract.
- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt`: added minimal task DAO methods for this sprint.
- `app/src/main/java/com/followupnadlan/data/lead/LeadEntity.kt`: added the Lead Room entity per the data contract.
- `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt`: added minimal lead DAO methods for this sprint.
- `app/schemas/com.followupnadlan.data.AppDatabase/1.json`: generated Room version 1 schema for reviewable future migrations.
- `app/src/test/java/com/followupnadlan/data/FollowUpRoomContractTest.kt`: added JVM contract tests for the entity fields because no androidTest source set exists in this checkout.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: First run failed because the new test compared Int literals to Long entity fields. After fixing the test literals, rerun passed: `BUILD SUCCESSFUL in 3s`; `51 actionable tasks: 7 executed, 44 up-to-date`.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: First sandbox run failed on known Gradle wrapper lock access: `gradle-8.9-bin.zip.lck (Access is denied)`. Elevated rerun passed: `BUILD SUCCESSFUL in 23s`; `37 actionable tasks: 19 executed, 18 up-to-date`.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence: Diff shows only KSP plugin/application, Room runtime/ktx/compiler, and Room schema export arg. No WorkManager or other dependency added.

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence: Empty diff.

- Command: forbidden-scope grep from PLAN.md
- Result: PASS
- Evidence: No matches.

### Manual QA
- Check: Real-phone/manual smoke
- Result: NOT RUN
- Notes: Sub-sprint 1 wires no feature behavior. Manual smoke remains NOT RUN.

### Deviations from plan
- No androidTest source set exists, so Sub-sprint 1 uses a pure JVM entity-contract test instead of an instrumented Room round-trip.

### Blockers
- None.

### Next recommended action
- Stop here per the user's "Begin with Sub-sprint 1 only" instruction, or proceed to Sub-sprint 2 only after human confirmation that Gate 1 evidence is accepted.

## Sub-sprint 2: Pure Snooze Time Logic
Status: Completed
Started: 2026-05-21
Completed: 2026-05-21

### Changes made
- `app/src/main/java/com/followupnadlan/snooze/SnoozeOption.kt`: added the five approved snooze presets with Hebrew labels.
- `app/src/main/java/com/followupnadlan/snooze/SnoozeTimeCalculator.kt`: added pure `java.time` timestamp calculation for relative presets, tonight, and tomorrow morning.
- `app/src/test/java/com/followupnadlan/snooze/SnoozeTimeCalculatorTest.kt`: added JVM tests for the exact preset set, Hebrew labels, relative deltas, tonight before/at/after 20:00, tomorrow morning, and a DST-boundary case.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 24s`; `51 actionable tasks: 18 executed, 33 up-to-date`.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: First sandbox run failed on the known Gradle wrapper lock access: `gradle-8.9-bin.zip.lck (Access is denied)`. Elevated rerun passed: `BUILD SUCCESSFUL in 3s`; `37 actionable tasks: 4 executed, 33 up-to-date`.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence: Diff remains the Sub-sprint 1 Room/KSP-only diff. No WorkManager or other dependency added in Sub-sprint 2.

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence: Empty diff.

- Command: forbidden-scope grep from PLAN.md
- Result: PASS
- Evidence: No matches.

- Command: `Select-String -Path app\src\main\java\com\followupnadlan\*.kt,app\src\main\java\com\followupnadlan\**\*.kt,app\src\test\java\com\followupnadlan\**\*.kt -Pattern "×|Ã|â"`
- Result: PASS
- Evidence: No matches.

- Command: `rg -n "androidx|android\." app\src\main\java\com\followupnadlan\snooze app\src\test\java\com\followupnadlan\snooze`
- Result: PASS
- Evidence: No matches; snooze time logic has no Android dependency.

- Command: `rg -n "WorkManager|OneTimeWorkRequest|CoroutineWorker|Room\.databaseBuilder|AppDatabase\.getInstance" app\src\main\java\com\followupnadlan\snooze app\src\main\java\com\followupnadlan\MainActivity.kt app\src\main\java\com\followupnadlan\postcall`
- Result: PASS
- Evidence: No matches; no WorkManager or feature wiring was introduced in Sub-sprint 2.

### Manual QA
- Check: Real-phone/manual smoke
- Result: NOT RUN
- Notes: Sub-sprint 2 is pure calculation logic and does not expose new app behavior.

### Deviations from plan
- None.

### Blockers
- None.

### Next recommended action
- Stop here for Gate 2 review. Proceed to Sub-sprint 3 only after human confirmation, because Sub-sprint 3 introduces WorkManager and requires real-phone short-delay reminder evidence.

## Sub-sprint 3: Introduce WorkManager + Reminder Scheduling
Status: Completed
Started: 2026-05-21
Completed: 2026-05-21

### Changes made
- `app/build.gradle.kts`: added `androidx.work:work-runtime-ktx:2.9.1` only, on top of the existing Room/KSP additions.
- `app/src/main/java/com/followupnadlan/snooze/ReminderScheduler.kt`: added WorkManager scheduling/cancel API with a unique work name per task and `ExistingWorkPolicy.REPLACE`.
- `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`: added a `CoroutineWorker` that reads `task_id`, loads the FollowUpTask from Room, exits safely for missing or non-snoozed tasks, and does not launch an Activity.
- `app/src/test/java/com/followupnadlan/snooze/ReminderSchedulerTest.kt`: added a JVM test for stable per-task unique work names.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 22s`; `51 actionable tasks: 30 executed, 21 up-to-date`. Existing `PhoneStateListener` deprecation warnings remain pre-existing and unrelated.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: First sandbox run failed on the known Gradle wrapper lock access: `gradle-8.9-bin.zip.lck (Access is denied)`. Elevated rerun passed: `BUILD SUCCESSFUL in 8s`; `37 actionable tasks: 6 executed, 31 up-to-date`.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence: Diff shows only KSP, Room runtime/ktx/compiler, Room schema export, and WorkManager runtime-ktx. No other dependency was added.

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence: Empty diff. WorkManager auto-initialization required no explicit manifest change.

- Command: forbidden-scope grep from PLAN.md
- Result: PASS
- Evidence: No matches.

- Command: `Select-String -Path app\src\main\java\com\followupnadlan\*.kt,app\src\main\java\com\followupnadlan\**\*.kt,app\src\test\java\com\followupnadlan\**\*.kt -Pattern "×|Ã|â"`
- Result: PASS
- Evidence: No matches.

- Command: `rg -n "startActivity|startForeground|fullScreenIntent|AlarmManager|setExact|SCHEDULE_EXACT_ALARM" app\src\main\java\com\followupnadlan\snooze`
- Result: PASS
- Evidence: No matches. ReminderWorker does not launch an Activity and does not use exact alarms.

### Manual QA
- Check: Real-phone short-delay WorkManager reminder fires the worker
- Result: PASS by human report
- Notes: Human approved merging Gate 3 into Gate 4, then reported the implemented app flow works. This is accepted as real-phone evidence that WorkManager fired through the production snooze loop, not through a temporary test-only trigger.

### Deviations from plan
- `ReminderWorker` currently verifies the task and logs a safe worker-fire path, but does not post the rich reminder notification yet. `ReminderNotificationHelper` and restore PendingIntent remain in Sub-sprint 4 per the PLAN.

### Blockers
- None.

### Next recommended action
- Gate 3 accepted through the merged Gate 4 manual smoke. Continue only within the approved sprint scope.

## Sub-sprint 4: Snooze Entry Point + Task Persistence + Card Restore
Status: Completed
Started: 2026-05-21
Completed: 2026-05-21

### Changes made
- Gate decision: the human approved merging Gate 3 worker-fire validation with Gate 4 full-loop validation, avoiding a temporary test-only trigger.
- `app/src/main/java/com/followupnadlan/notifications/ReminderNotificationHelper.kt`: added a separate `snooze_reminders` channel, distinct notification id namespace, immutable PendingIntent, and `ACTION_OPEN_SNOOZED_TASK` / `EXTRA_TASK_ID` for user-tap restore.
- `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`: now posts the reminder notification after safely loading a still-SNOOZED task from Room. It still does not launch any Activity directly.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: added snooze preset UI on the composer surface, persists FollowUpTask rows to Room, schedules WorkManager reminders, restores snoozed tasks from reminder tap, and updates restored tasks to `OPENED` / `WHATSAPP_OPENED` where reached.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 9s`; `51 actionable tasks: 14 executed, 37 up-to-date`.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: First sandbox run failed on the known Gradle wrapper lock access: `gradle-8.9-bin.zip.lck (Access is denied)`. Elevated rerun passed: `BUILD SUCCESSFUL in 3s`; `37 actionable tasks: 3 executed, 34 up-to-date`.

- Command: `git diff -- app/build.gradle.kts build.gradle.kts`
- Result: PASS
- Evidence: Diff shows only KSP, Room runtime/ktx/compiler, Room schema export, and WorkManager runtime-ktx. No other dependency was added.

- Command: `git diff -- app/src/main/AndroidManifest.xml`
- Result: PASS
- Evidence: Empty diff.

- Command: forbidden-scope grep from PLAN.md
- Result: PASS
- Evidence: No matches.

- Command: `Select-String -Path app\src\main\java\com\followupnadlan\*.kt,app\src\main\java\com\followupnadlan\**\*.kt,app\src\test\java\com\followupnadlan\**\*.kt -Pattern "×|Ã|â"`
- Result: PASS
- Evidence: No matches.

- Command: `rg -n "startActivity|fullScreenIntent|AlarmManager|setExact|SCHEDULE_EXACT_ALARM|RECEIVE_BOOT_COMPLETED" app\src\main\java\com\followupnadlan\snooze app\src\main\java\com\followupnadlan\notifications\ReminderNotificationHelper.kt app\src\main\java\com\followupnadlan\MainActivity.kt`
- Result: PASS WITH NOTES
- Evidence: Matches are only the pre-existing user-driven WhatsApp/share `startActivity` helpers in `MainActivity.kt`. No worker/direct reminder Activity launch, full-screen intent, exact alarm, or boot receiver was introduced.

- Command: `git diff --check`
- Result: PASS
- Evidence: No whitespace errors; only expected Windows CRLF warnings.

### Manual QA
- Check: Real-phone full snooze loop
- Result: PASS by human report
- Notes: Human reported `מצויין הכל עובד` after testing the app. Treat this as Gate 4 evidence for the implemented snooze loop only. It does not cover Sub-sprint 5 lead save because that work has not been implemented yet.

### Deviations from plan
- Gate 3 real-phone worker validation was intentionally merged into Gate 4 by human approval to avoid building a temporary test-only trigger.

### Blockers
- None for Sub-sprint 4.

### Next recommended action
- Commit the current Sprint 12 state through Sub-sprint 4. Sub-sprint 5 remains not started.
