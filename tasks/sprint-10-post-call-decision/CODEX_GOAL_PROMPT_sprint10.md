# Codex /goal — Sprint 10 Post-Call Decision Screen

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
13. `tasks/sprint-9-call-detection/MANUAL_SMOKE_TEST.md`
14. `app/src/main/AndroidManifest.xml`
15. `app/src/main/java/com/followupnadlan/MainActivity.kt`
16. `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
17. `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`

## Task

Create `tasks/sprint-10-post-call-decision/PLAN.md` with the content I provide below. Treat that PLAN.md as Human-approved. Then execute it via the goal-executor skill, one sub-sprint at a time.

## Product goal

Build the first real post-call decision screen: when the user taps the follow-up notification, the app opens a Hebrew RTL screen titled `מה קרה בשיחה?` with the four MVP decision cards. Each card routes into the existing manual composer using existing local templates/message flow. Do not add call-log auto-fill, reminders, Snooze, backend, or WhatsApp automation.

## Sub-sprint order (strict)

1. Sub-sprint 1: define the post-call card model and routing state. Run `.\gradlew.bat test` and confirm PASS before continuing.
2. Sub-sprint 2: add `PostCallScreen` UI with four cards and source-level preview/fallback state. Run `.\gradlew.bat test assembleDebug`.
3. Sub-sprint 3: wire notification tap / `FollowUpLaunchState` into `PostCallScreen`, and route each card into the existing composer without changing WhatsApp helpers. Run `.\gradlew.bat test assembleDebug`.
4. Sub-sprint 4: write `MANUAL_SMOKE_TEST.md`, `EXECUTION_LOG.md`, `REVIEW.md`. Manual smoke status MUST start as NOT RUN unless the human explicitly reports real-phone evidence.

## Hard rules

- Do NOT add: `READ_CALL_LOG`, `READ_CONTACTS`, `WRITE_CALL_LOG`, `SEND_SMS`, `READ_SMS`, `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`, `USE_FULL_SCREEN_INTENT`, `fullScreenIntent`, `AccessibilityService`, `NotificationListenerService`, `WorkManager`, `Room`, `AlarmManager`, `setExact`, `setExactAndAllowWhileIdle`, `RECEIVE_BOOT_COMPLETED`, any backend, any network call, any analytics, any dependency, any Gradle change.
- Do NOT add Snooze, reminders, `ReminderScheduler`, `ReminderReceiver`, boot restart, exact alarms, or any scheduling.
- Do NOT add call-log reading or phone-number auto-fill. Sprint 10 may preserve any phone/name extras already delivered by Sprint 8/9, but it must not query `CallLog.Calls`.
- Do NOT change the existing WhatsApp open/share/copy behavior except where strictly necessary to route from the selected card into the existing composer.
- Do NOT log `MESSAGE_SENT` or `CALL_DETECTED`. Do NOT log raw phone numbers. Do NOT log full message text.
- Do NOT launch any Activity directly from the service or any BroadcastReceiver. Notification entry remains user-tap driven through PendingIntent.
- Do NOT break Sprint 8/9 notification contract: action string, extras, channel id, notification id `8001`, request code `8001`, and user-tap behavior must be preserved unless the PLAN explicitly defines a compatible migration.
- Do NOT redesign My Details, Properties, Template Management, template storage, tag insertion, Action Log storage, or the call-detection service.
- Do NOT mark manual smoke PASS without explicit real Android phone evidence.

## Required card set

The `PostCallScreen` must show exactly these four MVP cards, in this order:

1. `שיחה קרה נפתחה`
   - Subtitle: `בעל נכס הסכים להמשך`
2. `שיחה קרה נסגרה בנימוס`
   - Subtitle: `להשאיר דלת פתוחה`
3. `קונה מתעניין`
   - Subtitle: `לשלוח פרטי נכס`
4. `צריך לחזור אליו`
   - Subtitle: `רק תזכורת — בלי הודעה`

For Sprint 10 only, card selection should route into the existing composer with an appropriate initial template/message context. Full per-card forms, secondary contact fields, reminders, and Snooze are Sprint 11/12 work.

## UX requirements

- Hebrew-only UI.
- RTL layout preserved.
- Screen title: `מה קרה בשיחה?`
- If phone/name extras are available from an existing notification path, show them as context. If not, show a clear fallback line that the user can continue manually.
- The four cards should be large, tappable, and readable on a phone screen.
- No nested card-heavy redesign of the existing app shell.
- Manual entry from launcher must remain usable.
- The existing Sprint 8 manual notification self-test must still work.
- The Sprint 9 call-detection toggle/status must still work and must not display mojibake.

## Validation order at every sub-sprint

1. `.\gradlew.bat test` (rerun with elevated access if Gradle wrapper lock fails — this is a known Windows/Gradle cache quirk).
2. `.\gradlew.bat assembleDebug` when UI or manifest-adjacent behavior changed.
3. `git diff -- app/src/main/AndroidManifest.xml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties` — manifest should not gain new permissions in Sprint 10; Gradle/wrapper diff must be empty.
4. Forbidden-scope grep (must be clean against `app/` and build files):
```powershell
rg -n "READ_CALL_LOG|READ_CONTACTS|WRITE_CALL_LOG|SEND_SMS|READ_SMS|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|USE_FULL_SCREEN_INTENT|fullScreenIntent|AccessibilityService|NotificationListenerService|WorkManager|Room|AlarmManager|setExact|setExactAndAllowWhileIdle|RECEIVE_BOOT_COMPLETED|backend|server|CRM|auto-send|autosend|MESSAGE_SENT|CALL_DETECTED" app build.gradle.kts settings.gradle.kts gradle.properties
```
5. Hebrew mojibake check for changed Kotlin files:
```powershell
Select-String -Path app\src\main\java\com\followupnadlan\*.kt,app\src\main\java\com\followupnadlan\**\*.kt -Pattern "×|Ã"
```
Only known pre-existing encoded docs/comments may be explained. New Sprint 10 UI strings must be valid Hebrew.
6. Sprint 8/9 contract regression:
   - Confirm `FollowUpNotificationHelper.ACTION_OPEN_FOLLOW_UP`, `EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`, channel id, notification id `8001`, request code `8001` are unchanged or compatibly preserved.
   - Confirm `CallDetectionService` still posts the existing follow-up notification only after detected call end.
   - Confirm no direct background Activity launch was introduced.

## Stop conditions

Stop and ask before proceeding if:
- Any forbidden API appears unavoidable.
- `READ_CALL_LOG` feels necessary to complete the screen.
- The implementation would require changing Gradle/dependencies.
- The implementation would require rewriting WhatsApp open/share/copy helpers.
- The Sprint 8/9 notification contract would need to break.
- The plan drifts into reminders, Snooze, full card forms, CRM, history UI, contacts, or auto-fill from CallLog.
- `assembleDebug` fails for a reason unrelated to the known Gradle wrapper lock.

## Completion response

When all four sub-sprints are done, report:
- Files changed (with absolute paths).
- Test command results.
- Manifest/Gradle diff result.
- Forbidden-scope grep result.
- Hebrew mojibake check result.
- Sprint 8/9 contract regression result.
- Manual smoke status.
- Whether work is ready for `followup-nadlan-reviewer`.

The full PLAN.md content follows. Treat it as approved. Begin execution.

---

# PLAN: Sprint 10 Post-Call Decision Screen

**Status**: Human-approved by current `/goal` request  
**Planning model**: GPT-5.5 High  
**Execution model**: Codex 5.3 High  
**Layer**: Core  
**Risk**: Medium  
**Generated**: 2026-05-20

## Goal Statement
Add the first real post-call decision screen so tapping a follow-up notification opens `מה קרה בשיחה?` with the four MVP cards, and each card routes into the existing manual WhatsApp composer without adding call-log auto-fill, reminders, Snooze, backend, or WhatsApp automation.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed  
Reason: Sprint 9 created the call-end notification trigger. Sprint 10 should make that tap land on the product's core question, `מה קרה בשיחה?`, while keeping the implementation narrow: no new sensitive permissions, no CallLog reading, no reminders, no Snooze, and no change to the user-controlled WhatsApp send flow.

## Non-Goals
- Do not add `READ_CALL_LOG`, call-log querying, phone-number auto-fill, duration auto-fill, or contact lookup.
- Do not add `READ_CONTACTS`.
- Do not add Snooze, reminders, `AlarmManager`, `WorkManager`, `ReminderScheduler`, `ReminderReceiver`, exact alarms, or boot restore.
- Do not add full per-card forms, secondary contact fields, custom reminder times, or card-specific validation beyond what is needed to enter the existing composer.
- Do not add Room, DataStore, migrations, backend/API, network calls, analytics, or dependencies.
- Do not change WhatsApp send behavior. The app may prepare text and open WhatsApp; the user must press Send.
- Do not auto-launch an Activity from the service or any BroadcastReceiver.
- Do not change Sprint 9 call detection service internals except for a compatible notification routing extra if needed.
- Do not redesign My Details, Properties, Template Management, template storage, template tag insertion, or Action Log storage.
- Do not claim real-phone manual smoke PASS unless the human explicitly reports real Android phone evidence.

## Assumptions
- The current repo includes Sprint 9 call detection work and the fixed Hebrew call-detection strings.
- `FollowUpNotificationHelper` remains the central follow-up notification entry point with notification id/request code `8001`.
- Sprint 10 can route notification taps to a new in-app screen by interpreting existing notification extras and/or adding a compatible in-app launch marker, without changing the public notification constants.
- Existing template/message infrastructure is sufficient to seed the manual composer after a card is selected.
- Card selection can use predefined initial message text or existing template ids. If matching existing template ids are unavailable, use local card-specific default message text without changing template storage.
- Manual launcher entry remains the existing manual composer unless the implementation can add a clear in-app navigation path to the post-call decision screen without disrupting current flows.

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
- `tasks/sprint-9-call-detection/MANUAL_SMOKE_TEST.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/templates/*`
- `app/src/main/java/com/followupnadlan/whatsapp/*`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt` (new, if a separate model file fits the existing style)
- `app/src/test/java/com/followupnadlan/postcall/PostCallCardTest.kt` (new, if model/default-message logic is extracted and testable)
- `tasks/sprint-10-post-call-decision/PLAN.md`
- `tasks/sprint-10-post-call-decision/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-10-post-call-decision/EXECUTION_LOG.md`
- `tasks/sprint-10-post-call-decision/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml` except if source review proves no actual diff is needed; Sprint 10 should add no permissions or services.
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt` except for a strictly compatible launch-extra handoff if unavoidable.
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt` constants and public contract.
- `app/src/main/java/com/followupnadlan/followuplog/*`
- `app/src/main/java/com/followupnadlan/profile/*`
- `app/src/main/java/com/followupnadlan/templates/*` unless only reading/reusing existing template ids requires a tiny reference update.
- `app/src/main/java/com/followupnadlan/whatsapp/*`
- All prior sprint task folders.

## Sprint 1: Card Model And Routing State
**Goal**: Define the four MVP post-call cards and the minimal state needed to route a selected card into the existing composer.  
**Demo / Validation**: Unit/source review proves the card set has exactly four cards in the required order, with stable ids, Hebrew titles, subtitles, and deterministic initial message/template mapping.  
**Stop condition**: Stop if this requires changing template storage, adding a database, or implementing full per-card forms.

### Task 1.1: Define Post-Call Card Model
- Location: `app/src/main/java/com/followupnadlan/postcall/PostCallCard.kt`
- Description: Add a small Kotlin model for the four MVP cards. Include stable ids, title, subtitle, and a Sprint 10 routing hint for initial composer state.
- Dependencies: Kotlin stdlib only.
- Acceptance criteria: Exactly four cards; order matches the PRD; no Android framework dependency if possible; no reminders, Snooze, lead persistence, or CallLog fields.
- Validation command or manual check: `.\gradlew.bat test`; source review.
- Rollback: Delete `PostCallCard.kt`.

### Task 1.2: Unit Tests for Card Set
- Location: `app/src/test/java/com/followupnadlan/postcall/PostCallCardTest.kt`
- Description: Test that all four required cards exist in order, have nonblank Hebrew titles/subtitles, stable ids, and no accidental extra card.
- Dependencies: Existing JUnit setup.
- Acceptance criteria: Tests pass without Android framework dependency.
- Validation command or manual check: `.\gradlew.bat test`.
- Rollback: Delete the test file.

## Sprint 2: PostCallScreen UI
**Goal**: Add a Hebrew RTL `PostCallScreen` that asks `מה קרה בשיחה?` and displays the four large card buttons.  
**Demo / Validation**: `assembleDebug` builds; source review confirms the screen is readable, phone-sized, and does not redesign the existing app shell.  
**Stop condition**: Stop if the UI requires adding a new navigation framework, a new dependency, or moving large parts of the existing composer.

### Task 2.1: Add PostCallScreen Composable
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt` or a new nearby Compose file if that matches existing style.
- Description: Add a composable with title `מה קרה בשיחה?`, optional context line for phone/name if present, fallback text if no context exists, and four full-width tappable cards.
- Dependencies: Existing Compose Material 3 imports and app theme.
- Acceptance criteria: Hebrew-only UI; RTL preserved; no nested-card-heavy redesign; four cards visible and tappable; no Snooze/reminder controls.
- Validation command or manual check: `.\gradlew.bat test assembleDebug`; source review.
- Rollback: Remove the composable.

### Task 2.2: Add Card Selection Callback
- Location: Same as `PostCallScreen`.
- Description: Each card invokes a callback with its stable card id/model. The callback must not directly open WhatsApp.
- Dependencies: Task 1.1.
- Acceptance criteria: UI only selects a card and routes internally; no WhatsApp intent, no logging, no scheduling from the card list screen.
- Validation command or manual check: Source review.
- Rollback: Remove callback wiring.

## Sprint 3: Notification Tap Routing And Composer Handoff
**Goal**: Make notification tap open `PostCallScreen`, and make each card route into the existing composer with appropriate initial message context.  
**Demo / Validation**: Sprint 8 manual notification self-test and Sprint 9 call detection notification both land on the decision screen; selecting a card lands in the existing composer; WhatsApp/share/copy behavior remains unchanged.  
**Stop condition**: Stop if this requires breaking `FollowUpNotificationHelper` constants, changing notification id/request code, or rewriting WhatsApp helpers.

### Task 3.1: Add App Screen State For PostCall
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Extend existing internal screen state so the app can display `PostCallScreen` when launched from a follow-up notification.
- Dependencies: Existing `FollowUpLaunchState`.
- Acceptance criteria: Manual launcher flow still opens the existing manual composer; notification launch opens `PostCallScreen`; no new Activity; no background Activity launch.
- Validation command or manual check: `.\gradlew.bat test assembleDebug`; source review.
- Rollback: Revert screen-state changes.

### Task 3.2: Preserve Notification Contract
- Location: `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt` and `MainActivity.kt`
- Description: Preserve existing action/extras/channel/id/request code. If an extra route marker is necessary, add it compatibly without renaming or removing existing constants.
- Dependencies: Sprint 8/9 notification behavior.
- Acceptance criteria: `ACTION_OPEN_FOLLOW_UP`, `EXTRA_PHONE`, `EXTRA_LEAD_NAME`, `EXTRA_TEMPLATE_ID`, channel id, notification id `8001`, and request code `8001` remain valid; Sprint 8 self-test still works.
- Validation command or manual check: Source review and contract grep.
- Rollback: Revert notification-routing changes.

### Task 3.3: Route Selected Card Into Existing Composer
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: On card selection, return to the existing composer with the phone/name context preserved if available and the message/template initialized for the selected card as narrowly as possible.
- Dependencies: Task 1.1, existing composer state.
- Acceptance criteria: Existing phone/name/manual validation still applies; Open WhatsApp/share/copy helpers unchanged; Action Log still records only explicit outgoing actions; no per-card form, reminder, or Snooze.
- Validation command or manual check: `.\gradlew.bat test assembleDebug`; source review.
- Rollback: Remove card-to-composer handoff.

## Sprint 4: Evidence Docs And Review
**Goal**: Write truthful Sprint 10 closure artifacts.  
**Demo / Validation**: Docs capture implementation, validation, forbidden-scope grep, Hebrew mojibake check, contract regression, and manual QA status.  
**Stop condition**: Stop if build/source validation fails or manual smoke is missing but docs claim PASS.

### Task 4.1: Write Manual Smoke Checklist
- Location: `tasks/sprint-10-post-call-decision/MANUAL_SMOKE_TEST.md`
- Description: Cover manual launcher still works; Sprint 8 test notification opens decision screen; Sprint 9 real-call notification opens decision screen if available; all four cards route to composer; WhatsApp/share/copy still work; call-detection toggle/status still displays valid Hebrew; permission-denied/manual mode still works.
- Dependencies: Implementation complete.
- Acceptance criteria: Status starts as `NOT RUN` unless the human explicitly reports real-phone evidence.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

### Task 4.2: Write Execution Log
- Location: `tasks/sprint-10-post-call-decision/EXECUTION_LOG.md`
- Description: Record pre-flight state, files changed, validation commands/results, manifest/Gradle diff, forbidden-scope grep, Hebrew mojibake check, Sprint 8/9 contract check, deviations, blockers, and next action.
- Dependencies: All implementation tasks complete.
- Acceptance criteria: Exact command results included; manual QA truthfully recorded.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

### Task 4.3: Write Review File
- Location: `tasks/sprint-10-post-call-decision/REVIEW.md`
- Description: Use the reviewer skill format. Decision should be `PASS WITH NOTES` if source/build passes but real-phone smoke remains missing.
- Dependencies: Tasks 4.1 and 4.2.
- Acceptance criteria: Honest verdict; no source/build success is inflated into real-device PASS.
- Validation command or manual check: Read the file.
- Rollback: Delete the file.

## Testing Strategy
- Unit tests:
  - `PostCallCardTest` for the exact four-card set and stable ordering.
  - Existing tests must continue passing.
- Instrumented tests:
  - Not required for Sprint 10.
- Build:
  - `.\gradlew.bat test assembleDebug`.
  - If Gradle wrapper lock fails because of the known Windows cache issue, rerun with the same accepted retry pattern and document it.
- Manual QA:
  - Must remain `NOT RUN` until tested on a real Android phone.
  - Minimum manual checks: launcher manual composer still works; manual notification test opens decision screen; all four cards route to composer; WhatsApp/share/copy still work; call-detection toggle still displays valid Hebrew.
- Device/OEM checks:
  - Real-call notification path should be checked on a Pixel-class phone if available.
  - Samsung/Xiaomi OEM battery behavior remains out of Sprint 10 scope.

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: Low. Sprint 10 should not modify manifest permissions/services.
- User disclosure required: No new disclosure. Existing Sprint 9 permission disclosure remains.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: No new persisted data required.
- Action Log: Existing behavior preserved; only explicit WhatsApp/share/copy actions may be logged.

## UX Impact
- Screens affected:
  - New `PostCallScreen`.
  - Existing manual composer receives selected-card context.
- RTL/Hebrew checks:
  - New strings must be valid Hebrew, not mojibake.
  - Mixed phone/Hebrew context line should remain readable.
- Empty/fallback states:
  - If no phone/name exists, show a clear manual fallback line.
  - Manual composer still supports typed/pasted phone.
- Error states:
  - WhatsApp missing/invalid phone behavior remains the existing composer behavior.
  - Notification permission denial remains Sprint 9 behavior.

## Rollback Plan
1. Remove `PostCallCard.kt` and `PostCallCardTest.kt` if added.
2. Revert `MainActivity.kt` changes for `PostCallScreen`, screen state, and card-to-composer routing.
3. Revert any compatible notification-routing extra if added.
4. Delete `tasks/sprint-10-post-call-decision/`.
5. Sprint 9 notification behavior should return to opening the existing composer exactly as before.

## Review Checklist
- AndroidManifest has no new Sprint 10 permissions.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- No `READ_CALL_LOG`, `READ_CONTACTS`, Snooze, reminders, WorkManager, Room, AlarmManager, or backend/API.
- Notification tap opens UI only through user action.
- Sprint 8/9 notification constants and id/request code are preserved.
- Existing manual composer still works.
- Four-card screen has exactly the approved card set.
- Fallback works without phone/name context.
- Hebrew strings are valid and no new mojibake appears.
- Manual smoke status is truthful.

## Agent Handoff
- Planning model: GPT-5.5 High.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sub-sprint at a time.
- Expansion rule: no scope expansion without human approval. Specifically, do not add `READ_CALL_LOG`, full per-card forms, reminders, Snooze, contacts, backend/API, or any WhatsApp automation in Sprint 10.

## Potential Gotchas
- Android background restrictions: Sprint 10 must keep notification entry user-tap driven and must not add background Activity launch.
- Notification permission denial: manual mode still works; Sprint 10 should not imply notification flow is enabled when Android blocks notifications.
- READ_CALL_LOG denial fallback: Sprint 10 does not request CallLog; fallback is the default state.
- OEM battery killing FGS: still relevant to Sprint 9 real-call detection, but out of Sprint 10 UI scope.
- Duplicate reminders: not applicable because Sprint 10 must not add reminders.
- Invalid phone formatting for wa.me: preserve existing composer validation/fallback behavior.
- RTL text and mixed Hebrew/phone-number layout: verify context line and card labels on device.
- Room migration risk: none; no Room changes.
- Direct-APK update/install friction: none; no distribution changes.
- Product drift risk: the four-card screen can tempt full forms and Snooze; keep those for Sprint 11/12.
