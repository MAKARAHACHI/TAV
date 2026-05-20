# Codex /goal — Sprint 11 CallLog Auto-Fill

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
6. `BRIEF/FollowUp_Nadlan_MVP_PRD.docx`
7. `context/POST_CALL_ENGINE.md`
8. `context/PERMISSIONS_AND_PRIVACY.md`
9. `context/ARCHITECTURE.md`
10. `context/DO_NOT_BUILD.md`
11. `tasks/sprint-9-call-detection/PLAN.md`
12. `tasks/sprint-9-call-detection/REVIEW.md`
13. `tasks/sprint-10-post-call-decision/PLAN.md`
14. `tasks/sprint-10-post-call-decision/REVIEW.md`
15. `tasks/sprint-10-post-call-decision/MANUAL_SMOKE_TEST.md`
16. `app/src/main/AndroidManifest.xml`
17. `app/src/main/java/com/followupnadlan/MainActivity.kt`
18. `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
19. `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
20. `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt`

## Task

Create or align `tasks/sprint-11-call-log-autofill/PLAN.md` with the content I provide below. Treat that PLAN.md as Human-approved. Then execute it via the goal-executor skill, one sub-sprint at a time.

## Product goal

After a real incoming or outgoing call ends, tapping the follow-up notification should open the `מה קרה בשיחה?` decision screen with the phone number already filled. If the number is saved in contacts and `READ_CONTACTS` is granted, prefill the first name too. If permissions are denied, keep the manual fallback working.

## Sub-sprint order (strict)

1. Sub-sprint 1: add `READ_CALL_LOG`, implement latest-call reader, and pure tests for recency/type classification. Run `.\gradlew.bat test` and confirm PASS before continuing.
2. Sub-sprint 2: wire call-end service -> latest CallLog result -> existing notification extras -> decision screen/composer. Run `.\gradlew.bat test assembleDebug`.
3. Sub-sprint 3: optional contact first-name auto-fill using `READ_CONTACTS`, fully fallback-safe. Run `.\gradlew.bat test assembleDebug`.
4. Sub-sprint 4: write `MANUAL_SMOKE_TEST.md`, `EXECUTION_LOG.md`, `REVIEW.md`. Manual smoke status MUST start as NOT RUN unless the human explicitly reports real-phone evidence.

## Hard rules

- Allowed new permissions for Sprint 11: `READ_CALL_LOG`; optional `READ_CONTACTS` only for first-name auto-fill.
- Do NOT add: `WRITE_CALL_LOG`, `SEND_SMS`, `READ_SMS`, `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`, `USE_FULL_SCREEN_INTENT`, `fullScreenIntent`, `AccessibilityService`, `NotificationListenerService`, `WorkManager`, `Room`, `AlarmManager`, `setExact`, `setExactAndAllowWhileIdle`, `RECEIVE_BOOT_COMPLETED`, any backend, any network call, any analytics, any dependency, any Gradle change.
- Do NOT add Snooze, reminders, full card forms, CRM/history UI, lead pipeline, or scheduling.
- Do NOT store all calls. Query only the latest CallLog row after a detected call end and reject stale rows.
- Do NOT log `MESSAGE_SENT` or `CALL_DETECTED`. Do NOT log raw phone numbers. Do NOT log contact names. Do NOT log full message text.
- Do NOT change WhatsApp open/share/copy helpers except if a compile-only call-site adjustment is unavoidable. User must still press Send inside WhatsApp.
- Do NOT launch any Activity directly from the service or any BroadcastReceiver. Notification entry remains user-tap driven through PendingIntent.
- Do NOT break Sprint 8/9/10 notification contract: action string, extras, channel id, notification id `8001`, request code `8001`, and user-tap behavior must be preserved or compatibly extended.
- Do NOT block the app if `READ_CALL_LOG` or `READ_CONTACTS` is denied.
- Do NOT mark manual smoke PASS without explicit real Android phone evidence.

## Required flow

1. User finishes an incoming, outgoing, or unavailable outgoing call.
2. Existing Sprint 9 call detection fires.
3. Service reads only the latest CallLog row if `READ_CALL_LOG` is granted.
4. If latest row is recent, pass phone number plus call metadata through the follow-up notification.
5. If `READ_CONTACTS` is granted and a contact match exists, pass first name through existing lead-name path.
6. User taps notification.
7. Sprint 10 `מה קרה בשיחה?` screen opens with phone visible.
8. Selecting a card routes to existing composer with phone prefilled and name prefilled if available.
9. If any permission is denied, the flow still opens with manual fields.

## Validation order at every sub-sprint

1. `.\gradlew.bat test` (rerun with elevated access if Gradle wrapper lock fails — known Windows/Gradle cache quirk).
2. `.\gradlew.bat assembleDebug` when service/UI/manifest behavior changed.
3. Manifest/Gradle diff:
```powershell
git diff -- app/src/main/AndroidManifest.xml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties
```
Expected: only planned permission additions in manifest; no Gradle/settings/properties diff.
4. Forbidden-scope grep:
```powershell
rg -n "WRITE_CALL_LOG|SEND_SMS|READ_SMS|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|WorkManager|Room|AlarmManager|setExact|setExactAndAllowWhileIdle|RECEIVE_BOOT_COMPLETED|backend|server|CRM|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties
```
5. Privacy grep/review:
```powershell
rg -n "CallLog|ContactsContract|READ_CALL_LOG|READ_CONTACTS|phoneNumber|displayName|messagePreview|FollowUpLogStore" app/src/main/java app/src/test/java
```
Confirm CallLog/contact data is not stored in Action Log and only flows into notification/launch/composer state.
6. Hebrew mojibake check:
```powershell
Select-String -Path app\src\main\java\com\followupnadlan\*.kt,app\src\main\java\com\followupnadlan\**\*.kt,app\src\test\java\com\followupnadlan\**\*.kt -Pattern "×|Ã|â"
```
If PowerShell console rendering is ambiguous, verify UTF-8/codepoints before claiming a failure.
7. Sprint 8/9/10 contract regression:
   - `FollowUpNotificationHelper` action/extras/channel/id/request code remain compatible.
   - `CallDetectionService` still posts notification only after detected call end.
   - `PostCallScreen` still shows exactly four cards.
   - Existing WhatsApp/share/copy behavior is unchanged.

## Stop conditions

Stop and ask before proceeding if:
- Any forbidden API appears unavoidable.
- `READ_CONTACTS` becomes required for the core flow.
- The implementation would require changing Gradle/dependencies.
- The implementation would require storing call history or contact data.
- The implementation would require rewriting WhatsApp helpers.
- The Sprint 8/9/10 notification contract would need to break.
- The plan drifts into reminders, Snooze, full card forms, CRM, history UI, backend/API, or WhatsApp automation.
- `assembleDebug` fails for a reason unrelated to the known Gradle wrapper lock.

## Completion response

When all four sub-sprints are done, report:
- Files changed (with absolute paths).
- Test command results.
- Manifest/Gradle diff result.
- Forbidden-scope grep result.
- Privacy grep/review result.
- Hebrew mojibake check result.
- Sprint 8/9/10 contract regression result.
- Manual smoke status.
- Whether work is ready for `followup-nadlan-reviewer`.

The full PLAN.md content follows. Treat it as approved. Begin execution.

---

# PLAN: Sprint 11 CallLog Auto-Fill

**Status**: Human-approved by current `/goal` request  
**Planning model**: GPT-5.5 High  
**Execution model**: Codex 5.3 High  
**Layer**: Core  
**Risk**: High  
**Generated**: 2026-05-20

## Goal Statement
After a real incoming or outgoing call ends, the follow-up notification and decision screen should open with the latest call phone number already filled, and with a first contact name filled only when contact permission is granted and a matching contact exists.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed  
Reason: Manual smoke after Sprint 10 confirmed the current gap: call-end detection and notification work, but the phone number is not auto-filled. Sprint 11 closes that MVP-critical gap by reading only the most recent CallLog row after a detected call, validating it is close to the event, passing phone metadata through the existing notification tap path, and keeping manual fallback if permissions are denied.

## Non-Goals
- Do not add Snooze, reminders, `AlarmManager`, `WorkManager`, `ReminderScheduler`, `ReminderReceiver`, exact alarms, or boot restore.
- Do not add full per-card forms, secondary contact fields, CRM, history UI, lead pipeline, or analytics.
- Do not store all calls or create a call-history database.
- Do not log `CALL_DETECTED`, raw phone numbers, contact names, full message text, or `MESSAGE_SENT`.
- Do not add backend/API, network calls, Room, DataStore, migrations, dependencies, or Gradle changes.
- Do not auto-send WhatsApp, use Accessibility, scrape WhatsApp, or bypass user confirmation.
- Do not launch an Activity directly from the service or any receiver. Notification tap remains the only UI entry.
- Do not require contact permission for the core flow. Contact name auto-fill is optional and must degrade to manual name entry.
- Do not block the app if `READ_CALL_LOG` or `READ_CONTACTS` is denied.

## Assumptions
- Sprint 9 call detection and Sprint 10 decision screen are present and source/build reviewed.
- `READ_PHONE_STATE`, foreground service permissions, and `POST_NOTIFICATIONS` already exist from earlier sprints.
- Sprint 11 may add exactly `READ_CALL_LOG` and optionally `READ_CONTACTS`, with clear Hebrew disclosure and runtime permission handling.
- The app should query only the latest CallLog row after a detected call-end event and reject stale rows outside a short recency window.
- Both incoming and outgoing answered calls should auto-fill the phone number when CallLog permission is granted.
- Outgoing calls with duration `0` or very short duration are still useful follow-up opportunities; they should be allowed to open the decision flow with the number, while the UI may show they were not a full conversation.
- If a contact match exists, use a simple first-name extraction from the display name. If not, leave the name field blank for manual entry.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
- `BRIEF/FollowUp_Nadlan_MVP_PRD.docx`
- `context/POST_CALL_ENGINE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/ARCHITECTURE.md`
- `context/DO_NOT_BUILD.md`
- `tasks/sprint-9-call-detection/PLAN.md`
- `tasks/sprint-9-call-detection/REVIEW.md`
- `tasks/sprint-10-post-call-decision/PLAN.md`
- `tasks/sprint-10-post-call-decision/REVIEW.md`
- `tasks/sprint-10-post-call-decision/MANUAL_SMOKE_TEST.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt`

## Files Expected To Change
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt` (new)
- `app/src/main/java/com/followupnadlan/postcall/ContactNameResolver.kt` (new, only if contact-name auto-fill stays small)
- `app/src/main/java/com/followupnadlan/postcall/PostCallLaunchContext.kt` (new, if useful for a pure routing model)
- `app/src/test/java/com/followupnadlan/postcall/CallLogReaderTest.kt` (new pure logic tests, not Android ContentResolver tests)
- `app/src/test/java/com/followupnadlan/postcall/ContactNameResolverTest.kt` (new pure first-name extraction tests, if resolver added)
- `tasks/sprint-11-call-log-autofill/PLAN.md`
- `tasks/sprint-11-call-log-autofill/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-11-call-log-autofill/EXECUTION_LOG.md`
- `tasks/sprint-11-call-log-autofill/REVIEW.md`

## Files That Must Not Change
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `app/src/main/java/com/followupnadlan/whatsapp/*`
- `app/src/main/java/com/followupnadlan/followuplog/*` except if review finds a compile-only import adjustment is required; do not change log semantics.
- `app/src/main/java/com/followupnadlan/templates/*`
- `app/src/main/java/com/followupnadlan/profile/*`
- All prior sprint task folders except read-only reference.

## Sprint 1: CallLog Permission And Reader
**Goal**: Add the minimum CallLog read capability needed to obtain the latest call number after a detected call.  
**Demo / Validation**: Source review shows `READ_CALL_LOG` is the only required new core permission and the reader returns either a recent latest-call result or null/failure fallback without crashing.  
**Stop condition**: Stop if the implementation starts storing call history, querying more than needed, or treating permission denial as fatal.

### Task 1.1: Add Manifest Permission
- Location: `app/src/main/AndroidManifest.xml`
- Description: Add exactly `android.permission.READ_CALL_LOG`. Do not add unrelated permissions in this task.
- Dependencies: Existing Sprint 9 phone-state permission setup.
- Acceptance criteria: Manifest diff shows only `READ_CALL_LOG` plus optional `READ_CONTACTS` if Sprint 3 is implemented in the same run; no forbidden permissions.
- Validation command or manual check: `git diff -- app/src/main/AndroidManifest.xml`.
- Rollback: Remove the permission line.

### Task 1.2: Add CallLogReader
- Location: `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt`
- Description: Implement a small reader around `ContentResolver` and `CallLog.Calls` that checks permission, queries latest 1 by `DATE DESC`, returns phone number, duration seconds, timestamp, and call type, and returns null when unavailable.
- Dependencies: Android `CallLog.Calls`, `ContentResolver`.
- Acceptance criteria: Reads only latest row; validates recency with a configurable window; never throws to callers for missing permission/security/query failure; no persistence; no contact lookup here.
- Validation command or manual check: Source review and `.\gradlew.bat test`.
- Rollback: Delete `CallLogReader.kt`.

### Task 1.3: Pure Tests For Recency And Classification Logic
- Location: `app/src/test/java/com/followupnadlan/postcall/CallLogReaderTest.kt`
- Description: Extract and test pure logic for accepting/rejecting latest rows by timestamp, mapping incoming/outgoing/missed/unknown call types, and accepting zero-duration outgoing unavailable calls as follow-up candidates when recent.
- Dependencies: JUnit only.
- Acceptance criteria: Tests do not require Android framework or device CallLog.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete the test file.

## Sprint 2: Service Handoff To Notification And Decision Screen
**Goal**: After call end, pass phone metadata through the existing notification path into `PostCallScreen` and the composer.  
**Demo / Validation**: With `READ_CALL_LOG` granted, a real incoming/outgoing call ends, notification appears, tapping it opens `מה קרה בשיחה?` with the phone already visible and preserved into card selection.  
**Stop condition**: Stop if this requires direct Activity launch, changing notification id/request code, or breaking Sprint 10 card routing.

### Task 2.1: Query Latest Call On Call End
- Location: `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- Description: In the existing monitor `onCallEnded` callback, call `CallLogReader` and use its result when building the follow-up notification. If the reader returns null, preserve the existing empty-phone fallback.
- Dependencies: Sprint 9 service, Task 1.2.
- Acceptance criteria: No direct Activity launch; no crash on permission denied; no call-log persistence; fallback notification still works.
- Validation command or manual check: `.\gradlew.bat test assembleDebug`; source review.
- Rollback: Revert service call-log integration.

### Task 2.2: Extend Notification Extras Compatibly
- Location: `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- Description: Preserve existing action/extras/channel/id/request code and add compatible extras only if needed for duration/timestamp/type display.
- Dependencies: Existing notification helper.
- Acceptance criteria: `ACTION_OPEN_FOLLOW_UP`, `EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`, notification id `8001`, request code `8001`, and channel id remain valid; new extras do not break old callers.
- Validation command or manual check: Contract grep and source review.
- Rollback: Remove newly added extras and related parsing.

### Task 2.3: Display Metadata In Decision Screen
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Parse the compatible extras into launch state and show the phone number on `PostCallScreen`; preserve it when routing into the existing composer.
- Dependencies: Sprint 10 `PostCallScreen`.
- Acceptance criteria: Phone is visible on decision screen; phone is prefilled in composer after card selection; if no phone exists, fallback line remains; no WhatsApp helper changes.
- Validation command or manual check: `.\gradlew.bat test assembleDebug`; source review.
- Rollback: Revert launch-state and UI metadata changes.

## Sprint 3: Optional Contact First Name Auto-Fill
**Goal**: If contacts permission is granted and a contact match exists, fill the first name automatically; otherwise preserve manual name entry.  
**Demo / Validation**: With contact permission denied or no match, the decision screen still works with phone only. With permission and match, first name is prefilled.  
**Stop condition**: Stop if contact lookup becomes broad contact management, requires blocking startup, or creates crashes on permission denial.

### Task 3.1: Add Optional Contacts Permission And Request Path
- Location: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add `READ_CONTACTS` only as optional. Request it only where it is explained as improving name auto-fill; denial must leave the flow usable.
- Dependencies: Existing runtime permission patterns.
- Acceptance criteria: Denial is treated as manual-name mode; no repeated nagging; no contact lookup if not granted.
- Validation command or manual check: Manifest diff and source review.
- Rollback: Remove permission and request path.

### Task 3.2: Add ContactNameResolver
- Location: `app/src/main/java/com/followupnadlan/postcall/ContactNameResolver.kt`
- Description: Resolve display name by phone number only when permission is granted, then extract the first usable name token for prefill.
- Dependencies: Android Contacts provider.
- Acceptance criteria: Returns null on missing permission, query failure, or no match; does not store contact data; no startup blocker.
- Validation command or manual check: Source review and pure tests for first-name extraction.
- Rollback: Delete resolver and remove callers.

### Task 3.3: Wire Name Into Notification/Launch Context
- Location: `CallDetectionService.kt`, `FollowUpNotificationHelper.kt`, `MainActivity.kt`
- Description: If a first name is resolved, pass it through the existing `EXTRA_LEAD_NAME` path so Sprint 10 screen/composer can reuse it.
- Dependencies: Task 3.2.
- Acceptance criteria: Existing manual notification self-test still works; phone-only flow still works; user can edit name afterward.
- Validation command or manual check: `.\gradlew.bat test assembleDebug`; source review.
- Rollback: Remove contact-name handoff.

## Sprint 4: Evidence Docs And Review
**Goal**: Write truthful Sprint 11 evidence artifacts and reviewer output.  
**Demo / Validation**: Docs capture permission impact, CallLog fallback, contacts fallback, real-phone status, and exact command evidence.  
**Stop condition**: Stop if docs claim phone/contact auto-fill PASS without real-device evidence.

### Task 4.1: Write Manual Smoke Checklist
- Location: `tasks/sprint-11-call-log-autofill/MANUAL_SMOKE_TEST.md`
- Description: Cover incoming answered call, outgoing answered call, outgoing unavailable/zero-duration call, permission denied fallback, contact permission denied fallback, contact-name prefill if available, notification tap to decision screen, card selection to composer, and WhatsApp/share/copy regression.
- Dependencies: Implementation complete.
- Acceptance criteria: Status starts as `NOT RUN` unless the human explicitly reports real-phone evidence.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

### Task 4.2: Write Execution Log
- Location: `tasks/sprint-11-call-log-autofill/EXECUTION_LOG.md`
- Description: Record pre-flight state, file changes, validation commands/results, manifest permission diff, forbidden-scope grep, privacy review, Sprint 8-10 contract checks, deviations, blockers, and next action.
- Dependencies: All implementation tasks complete.
- Acceptance criteria: Exact command results included; manual QA truthfully recorded.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

### Task 4.3: Write Review File
- Location: `tasks/sprint-11-call-log-autofill/REVIEW.md`
- Description: Use reviewer skill format. Decision should remain `PASS WITH NOTES` if source/build passes but real-phone call-log/contact smoke remains missing.
- Dependencies: Tasks 4.1 and 4.2.
- Acceptance criteria: Honest verdict; permission and privacy impact explicitly reviewed.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

## Testing Strategy
- Unit tests:
  - Pure recency/type classification for latest call row.
  - Optional first-name extraction tests if contact resolver is added.
  - Existing Sprint 9/10 tests must keep passing.
- Build:
  - `.\gradlew.bat test assembleDebug`.
- Manual QA:
  - Required before full PASS because CallLog and Contacts behavior is real-device dependent.
  - Minimum: outgoing answered call, incoming answered call, outgoing unavailable/zero-duration call, permission denied fallback.
- Device/OEM checks:
  - Pixel-class phone minimum for first smoke.
  - Samsung/Xiaomi can remain follow-up QA unless available now.

## Permission Impact
- Added permissions:
  - `READ_CALL_LOG`: required for latest call phone auto-fill.
  - `READ_CONTACTS`: optional only for first-name auto-fill if implemented in Sprint 3.
- Removed permissions: None.
- Manifest risk: High because `READ_CALL_LOG` is sensitive and Play Store-restricted, but allowed for direct-APK MVP per PRD/product strategy.
- User disclosure required: Yes. Explain in Hebrew that the app reads only the latest call after a call ends to fill the phone number locally, does not upload call logs, and does not send WhatsApp automatically.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: None required for CallLog/contact data in Sprint 11.
- Logs: Do not add raw phone/contact data to FollowUp Action Log.

## UX Impact
- Screens affected:
  - Existing call-detection permission/status area.
  - `PostCallScreen` context line.
  - Existing manual composer prefilled phone/name fields.
- RTL/Hebrew checks:
  - New disclosure/status strings must be valid Hebrew.
  - Mixed Hebrew/phone-number display must be readable.
- Empty/fallback states:
  - No `READ_CALL_LOG`: decision screen opens with manual fallback.
  - No `READ_CONTACTS`: phone appears, name remains manual.
  - No matching contact: phone appears, name remains manual.
- Error states:
  - Query failure/security exception returns null and keeps fallback flow.
  - Stale latest CallLog row is rejected instead of filling wrong number.

## Rollback Plan
1. Remove `READ_CALL_LOG` and optional `READ_CONTACTS` from `AndroidManifest.xml`.
2. Delete `CallLogReader.kt`, optional `ContactNameResolver.kt`, and their tests.
3. Revert `CallDetectionService.kt` to posting empty phone/name notification extras.
4. Revert any compatible extra additions in `FollowUpNotificationHelper.kt`.
5. Revert `MainActivity.kt` launch-state metadata display changes.
6. Delete `tasks/sprint-11-call-log-autofill/`.
7. Sprint 10 behavior should return to notification tap -> decision screen with manual phone entry.

## Review Checklist
- Manifest adds only planned permissions.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- No backend/API, network, analytics, Room, WorkManager, AlarmManager, reminders, or Snooze.
- CallLog query reads latest row only and rejects stale rows.
- Permission denial falls back to manual mode.
- Contact lookup is optional and non-blocking.
- No call history/contact data is stored or logged.
- Sprint 8/9/10 notification contract remains compatible.
- Existing manual composer still works.
- Real-phone manual smoke status is truthful.

## Agent Handoff
- Planning model: GPT-5.5 High.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sub-sprint at a time.
- Expansion rule: no scope expansion without human approval. Specifically, do not add Snooze, reminders, full card forms, CRM/history UI, backend/API, or WhatsApp automation in Sprint 11.

## Potential Gotchas
- Android background restrictions: keep notification entry user-tap driven.
- Notification permission denial: manual mode still works, but post-call value is weakened.
- READ_CALL_LOG denial fallback: must be a first-class path, not an error state.
- READ_CONTACTS denial fallback: phone auto-fill still works; name stays manual.
- OEM battery killing FGS: affects whether call-end event fires; document separately from CallLog reader bugs.
- Stale CallLog row risk: always check recency before trusting latest row.
- Zero-duration outgoing calls: useful for follow-up but may need clear UI wording.
- Invalid phone formatting for wa.me: preserve existing normalization/validation.
- RTL text and mixed Hebrew/phone-number layout: verify on device.
- Room migration risk: none; no schema changes.
- Direct-APK update/install friction: none; no distribution changes.
