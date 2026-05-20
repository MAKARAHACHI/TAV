# Codex /goal — Sprint 9 Call Detection + Action Log

Paste the block below into Codex 5.3 High as a single `/goal` message. The plan is approved.

---

/goal

You are Codex 5.3 High executing on the FollowUp Nadlan Android repo.

## Mandatory first reads (in order)
1. `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
2. `.agents/skills/followup-nadlan-goal-router/SKILL.md`
3. `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
4. `.agents/skills/followup-nadlan-reviewer/SKILL.md`
5. `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
6. `context/POST_CALL_ENGINE.md`
7. `context/PERMISSIONS_AND_PRIVACY.md`
8. `context/ARCHITECTURE.md`
9. `context/DO_NOT_BUILD.md`
10. `tasks/sprint-8-notification-followup-card/PLAN.md`
11. `tasks/sprint-8-notification-followup-card/REVIEW.md`
12. `app/src/main/AndroidManifest.xml`
13. `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
14. `app/src/main/java/com/followupnadlan/MainActivity.kt`

## Task

Create `tasks/sprint-9-call-detection/PLAN.md` with the content I provide below. Treat that PLAN.md as Human-approved. Then execute it via the goal-executor skill, one sub-sprint at a time.

## Sub-sprint order (strict)
1. Sub-sprint 1: pure `CallStateMonitor` + unit tests. Run `.\gradlew.bat test` and confirm PASS before continuing.
2. Sub-sprint 2: platform Service + manifest permissions + UI toggle. Run `.\gradlew.bat test assembleDebug`. Source-review every forbidden API in the grep list before continuing.
3. Sub-sprint 3: Action Log foundation + unit tests + wiring into the three existing outgoing helpers. Run `.\gradlew.bat test assembleDebug`.
4. Sub-sprint 4: write `MANUAL_SMOKE_TEST.md`, `EXECUTION_LOG.md`, `REVIEW.md`. Manual smoke status MUST start as NOT RUN.

## Hard rules

- Do NOT add: `READ_CALL_LOG`, `READ_CONTACTS`, `WRITE_CALL_LOG`, `SEND_SMS`, `READ_SMS`, `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`, `USE_FULL_SCREEN_INTENT`, `fullScreenIntent`, `AccessibilityService`, `NotificationListenerService`, `WorkManager`, `Room`, `AlarmManager`, `setExact`, `setExactAndAllowWhileIdle`, `RECEIVE_BOOT_COMPLETED`, any backend, any network call, any analytics, any dependency, any Gradle change.
- Do NOT log `MESSAGE_SENT` or `CALL_DETECTED`. Do NOT log raw phone numbers. Do NOT log full message text. Preview is the first 80 characters of the rendered message.
- Do NOT launch any Activity directly from the service or any BroadcastReceiver. Only post a notification with a PendingIntent.
- Do NOT implement the four post-call cards, Snooze, AlarmManager, or auto-fill from CallLog. These belong to Sprint 10+.
- Do NOT auto-restart the service on boot.
- Do NOT break Sprint 8's notification contract: action string, extras, channel id, notification id, request code must all be preserved.
- Do NOT mark manual smoke PASS. Leave it NOT RUN. The human will run the real-phone smoke separately.

## Validation order at every sub-sprint

1. `.\gradlew.bat test` (rerun with elevated access if Gradle wrapper lock fails — this is a known sandbox quirk).
2. `.\gradlew.bat assembleDebug` (same retry pattern).
3. `git diff -- app/src/main/AndroidManifest.xml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties` — must show only the planned manifest changes; Gradle/wrapper diff must be empty.
4. Forbidden-scope grep (must be clean against `app/` and build files):
```
rg -n "READ_CALL_LOG|READ_CONTACTS|WRITE_CALL_LOG|SEND_SMS|READ_SMS|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|WorkManager|Room|AlarmManager|setExact|setExactAndAllowWhileIdle|RECEIVE_BOOT_COMPLETED|backend|server|CRM|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties
```
5. Sprint 8 contract regression: confirm `FollowUpNotificationHelper.ACTION_OPEN_FOLLOW_UP`, `EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`, channel id, notification id `8001`, request code `8001` are unchanged.

## Stop conditions

Stop and ask before proceeding if:
- Any forbidden API appears unavoidable.
- `assembleDebug` fails for a reason unrelated to the Gradle wrapper lock.
- The plan requires touching files marked "Must Not Change".
- The Sprint 8 contract would need to break.

## Completion response

When all four sub-sprints are done, report:
- Files changed (with absolute paths).
- Test command results.
- Manifest diff (paste verbatim).
- Forbidden-scope grep result (paste verbatim).
- Whether work is ready for `followup-nadlan-reviewer`.

The full PLAN.md content follows. Treat it as approved. Begin execution.

---

[PASTE THE ENTIRE CONTENT OF tasks/sprint-9-call-detection/PLAN.md HERE]
