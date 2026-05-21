# Codex /goal — Sprint 12 Snooze + Lead + Reminder on Room

Paste the block below into Codex 5.3 High as a single `/goal` message. The plan is approved at full scope. It is large on purpose — the gates keep it safe.

---

/goal

You are Codex 5.3 High executing on the FollowUp Nadlan Android repo.

## Mandatory first reads (in order)
1. `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
2. `.agents/skills/followup-nadlan-goal-router/SKILL.md`
3. `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
4. `.agents/skills/followup-nadlan-reviewer/SKILL.md`
5. `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
6. `context/SNOOZE_REMINDERS.md`
7. `context/FOLLOW_UP_CARD.md`
8. `context/DATA_CONTRACTS.md`
9. `context/ARCHITECTURE.md`
10. `context/PERMISSIONS_AND_PRIVACY.md`
11. `context/DO_NOT_BUILD.md`
12. `app/build.gradle.kts`
13. `app/src/main/AndroidManifest.xml`
14. `app/src/main/java/com/followupnadlan/MainActivity.kt`
15. `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
16. The four card screens and post-call wiring from Sprint 10/11
17. `app/src/main/java/com/followupnadlan/postcall/*` and `app/src/main/java/com/followupnadlan/log/*`

## Task

Create `tasks/sprint-12-snooze-lead-reminder/PLAN.md` with the content I provide below. Treat it as Human-approved at full scope. Then execute it via the goal-executor skill, ONE SUB-SPRINT AT A TIME with hard gates.

## Sub-sprint order (STRICT — gates are mandatory)
1. Sub-sprint 1: introduce Room (empty DB, two entities, DAOs) — wired to NO feature yet. GATE 1: assembleDebug passes with Room; Gradle diff is Room+KSP only.
2. Sub-sprint 2: pure SnoozeTimeCalculator + tests. GATE 2: all snooze-time tests pass.
3. Sub-sprint 3: introduce WorkManager + ReminderScheduler + ReminderWorker. GATE 3: a short-delay reminder fires the worker on a real phone.
4. Sub-sprint 4: snooze entry point + task persistence + reminder reopens the SAME card. GATE 4: full snooze loop works on a real phone.
5. Sub-sprint 5: optional local lead save + status transitions. GATE 5: lead persists across restart; full regression clean.

Do NOT start a sub-sprint until the previous gate passes. If a gate fails for a reason unrelated to the current sub-sprint, STOP and invoke systematic-debugging.

## Hard rules

- WorkManager ONLY for reminders. Do NOT add `SCHEDULE_EXACT_ALARM`, `AlarmManager`, `setExact`, `setExactAndAllowWhileIdle`, or `RECEIVE_BOOT_COMPLETED`. Reminders are approximate by design — this is a constitution requirement.
- Gradle may add ONLY: Room, KSP (if needed for Room on Kotlin 2.0), and `androidx.work:work-runtime-ktx`. No other dependency. No networking library. Room goes in Sub-sprint 1; WorkManager goes in Sub-sprint 3; each validated alone before feature code uses it.
- Do NOT touch `READ_CALL_LOG` or `READ_CONTACTS` — they already exist from Sprint 11.
- Do NOT migrate existing SharedPreferences data into Room. Room is ONLY for FollowUpTask and Lead. Profile/templates/property/log stay in SharedPreferences, untouched.
- Do NOT launch any Activity from the worker. Only post a notification with a PendingIntent.
- Do NOT add Accessibility, SYSTEM_ALERT_WINDOW, QUERY_ALL_PACKAGES, USE_FULL_SCREEN_INTENT, fullScreenIntent, SMS, NotificationListenerService, backend, or auto-send.
- Do NOT build a lead list/management/search UI, dashboard, or analytics. Lead save is a single optional action plus minimal status.
- Reminder notification uses a SEPARATE channel `snooze_reminders` and a notification id distinct from the post-call id `8001`.
- Snooze must work with an empty phone (reminder returns with empty phone field).
- Use a UNIQUE WorkManager work name per task + `ExistingWorkPolicy.REPLACE` to prevent duplicate reminders.
- Preserve the Sprint 8/11 notification contract and the four cards' behavior.
- Do NOT mark manual smoke PASS. Leave it NOT RUN — the human runs the real timed-reminder smoke separately.

## Validation order at every sub-sprint

1. `.\gradlew.bat test` (rerun with elevated access if the Gradle wrapper lock fails — known sandbox quirk).
2. `.\gradlew.bat assembleDebug` (same retry pattern).
3. Dependency diff: `git diff -- app/build.gradle.kts build.gradle.kts` — must show ONLY Room/KSP/WorkManager additions. Anything else is a failure.
4. Manifest diff: `git diff -- app/src/main/AndroidManifest.xml` — ideally empty; if WorkManager init requires a change, explain it.
5. Forbidden-scope grep (clean against `app/` and build files):
```
rg -n "SCHEDULE_EXACT_ALARM|setExactAndAllowWhileIdle|setExact\b|AlarmManager|RECEIVE_BOOT_COMPLETED|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|SEND_SMS|READ_SMS|backend|server|Firestore|Supabase|retrofit|okhttp|auto-send|autosend|MESSAGE_SENT" app build.gradle.kts settings.gradle.kts gradle.properties
```
6. Contract regression: Sprint 8/11 post-call notification still works; the four cards still open and prepare messages; manual composer/share/copy/reset/My Details/templates/property links all still work.

## Stop conditions

Stop and ask before proceeding if:
- Room forces a toolchain change beyond Room + KSP, or KSP/kapt fails on Kotlin 2.0 in a way you cannot resolve cleanly.
- WorkManager appears to need an exact-alarm API or a user permission.
- Any forbidden API or second dependency seems unavoidable.
- A gate fails for a reason unrelated to the current sub-sprint.
- The plan requires touching files marked "Must Not Change".

## Completion response

When all five sub-sprints and gates pass, report:
- Files changed (absolute paths).
- Test command results per sub-sprint.
- Dependency diff (paste verbatim).
- Manifest diff (paste verbatim).
- Forbidden-scope grep result (paste verbatim).
- Whether work is ready for `followup-nadlan-reviewer`.

The full PLAN.md content follows. Treat it as approved. Begin with Sub-sprint 1 only.

---

[PASTE THE ENTIRE CONTENT OF tasks/sprint-12-snooze-lead-reminder/PLAN.md HERE]
