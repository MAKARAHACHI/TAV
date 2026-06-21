# PLAN: Sprint 15 Missed Call Auto Response

**Status**: Done
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: High
**Generated**: 2026-06-21

## Goal Statement
Add an explicit opt-in missed-call auto SMS response that sends a user-approved Hebrew template after unanswered incoming calls, while preserving the existing manual WhatsApp follow-up, post-call card, snooze, lead pipeline, and local-first behavior.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed after human approval
Reason: The sprint adds a controlled direct-APK SMS layer on top of the existing call pipeline. `SEND_SMS` is normally forbidden by the existing constitution/docs, but this /goal gives explicit written approval for an opt-in, disclosed, permission-gated SMS exception. WhatsApp remains user-driven only.

## Non-Goals
- No WhatsApp auto-send, WhatsApp screen automation, AccessibilityService, overlays, or forced Activity launch.
- No backend, CRM sync, cloud log upload, analytics, campaigns, bulk messaging, or contact import.
- No `READ_SMS`, `RECEIVE_SMS`, `WRITE_CALL_LOG`, `QUERY_ALL_PACKAGES`, or broad package visibility.
- No delivery-confirmation claim unless a future sprint implements Android sent/delivery `PendingIntent` handling.
- No real SMS during automated tests or setup self-test.
- No Play Store compliance redesign; keep the direct-APK / controlled beta framing.
- No rewrite of the Sprint 14 lead pipeline, post-call card, templates, snooze, or setup wizard.
- No automatic response for outgoing, answered incoming, unknown/private/empty-number, or duplicate missed calls within cooldown.

## Assumptions
- `SEND_SMS` is approved only for this opt-in auto-response feature and must be requested only after the user enables it.
- Existing `READ_PHONE_STATE` and `READ_CALL_LOG` remain the call-detection substrate; no new call-log permissions are needed.
- A missed call can be determined from both the live state sequence (`RINGING` without `OFFHOOK` before `IDLE`) and the latest call-log type when available.
- Cooldown can be stored locally in SharedPreferences for MVP unless execution proves Room is necessary.
- The business name maps to the current profile office/business field first, then agent name only if the app has no explicit business field.
- The existing template editing screen can host the new default template by adding a built-in template id.
- Existing action-log storage can be extended in a backward-compatible way, or a new versioned log codec can be added if preserving old log rows becomes cleaner.
- Manual fallback should prefer SMS composer for this feature; existing WhatsApp/manual composer remains the secondary fallback if no SMS composer is available.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `context/00-START-HERE.md`
- `context/ARCHITECTURE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/POST_CALL_ENGINE.md`
- `context/TEMPLATE_ENGINE.md`
- `context/FOLLOW_UP_CARD.md`
- `tasks/sprint-14-lead-pipeline/PLAN.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- `app/src/main/java/com/followupnadlan/pipeline/LeadPipeline.kt`
- existing tests under `app/src/test/java/com/followupnadlan/`

## Files Expected To Change
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallLogReader.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTags.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- new `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseSettings.kt`
- new `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt`
- new `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- new `app/src/main/java/com/followupnadlan/missedcall/MissedCallCooldownStore.kt`
- new `app/src/main/java/com/followupnadlan/missedcall/SmsSender.kt`
- new tests under `app/src/test/java/com/followupnadlan/missedcall/`
- `app/src/test/java/com/followupnadlan/postcall/CallStateMonitorTest.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateStoreTest.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`
- `app/src/test/java/com/followupnadlan/data/FollowUpRoomContractTest.kt` only if the log/task data contract changes
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/POST_CALL_ENGINE.md`
- `context/TEMPLATE_ENGINE.md`
- `tasks/sprint-15-missed-call-auto-response/README.md`
- `tasks/sprint-15-missed-call-auto-response/EXECUTION_LOG.md`
- `tasks/sprint-15-missed-call-auto-response/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-15-missed-call-auto-response/REVIEW.md`

## Files That Must Not Change
- `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts`, unless compilation proves a missing AndroidX API already present in the SDK cannot be used.
- Room schema files and `AppDatabase.kt`, unless execution proves cooldown/logging needs Room; if so, stop for explicit migration approval before changing schema.
- WhatsApp link helpers except for source-compatible fallback usage.
- Snooze scheduler and worker unless a compile issue forces a narrow compatibility edit.
- Existing lead pipeline status constants unless adding a source constant is strictly necessary.
- Any signing, release, licensing, update-check, or distribution files.

## Sprint 15.1: Missed-Call Detection And Decision Core
**Goal**: Add a pure, testable missed-call decision layer and a call-session tracker that can distinguish unanswered incoming calls from answered or outgoing calls.
**Demo / Validation**: JVM tests prove send/skip/fallback decisions without Android SMS APIs.
**Stop condition**: Stop if missed-call detection cannot be implemented without adding unapproved permissions or destabilizing the existing post-call notification path.

### Task 15.1.1: Add Missed-Call Session Tracking
- Location: `CallStateMonitor.kt`, `CallStateMonitorTest.kt`
- Description: Extend or wrap the current monitor so `RINGING -> IDLE` without `OFFHOOK` emits a missed-call candidate, while `OFFHOOK -> IDLE` continues to drive the existing answered-call notification flow.
- Dependencies: none
- Acceptance criteria:
  - Incoming ringing then idle emits missed-call candidate.
  - Ringing then offhook then idle does not emit missed-call auto-response candidate.
  - Offhook-only/outgoing-style transitions do not emit missed-call auto-response candidate.
  - Existing answered-call tests remain green.
- Validation command or manual check: `.\gradlew.bat test --tests "*CallStateMonitorTest*"`
- Rollback: revert monitor changes and tests.

### Task 15.1.2: Add Decision Engine
- Location: new `missedcall/MissedCallAutoResponseDecision.kt`
- Description: Add pure Kotlin input/output models for call direction, answered state, phone availability, feature enabled, SMS permission, cooldown, template availability, and fallback availability.
- Dependencies: Task 15.1.1
- Acceptance criteria:
  - Decision outputs include `SEND_AUTOMATIC_SMS`, `SKIP_DISABLED`, `SKIP_NO_PERMISSION`, `SKIP_DUPLICATE`, `SKIP_NO_NUMBER`, `SKIP_NOT_MISSED_CALL`, `OPEN_MANUAL_FALLBACK`, and `SKIP_NO_TEMPLATE`.
  - Cooldown default is 6 hours per normalized phone number.
  - No Android Context, SmsManager, Compose, Room, or SharedPreferences in the pure decision object.
- Validation command or manual check: targeted unit tests.
- Rollback: delete decision files/tests.

### Task 15.1.3: Add Decision Unit Tests
- Location: new `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt`
- Description: Cover all required send/skip/fallback cases from the /goal.
- Dependencies: Task 15.1.2
- Acceptance criteria:
  - Tests cover enabled + permission + no duplicate sends.
  - Tests cover outgoing, answered incoming, disabled, no permission, no number, duplicate within 6 hours, same number after cooldown.
  - Tests assert no full rendered SMS body is required for decision/logging.
- Validation command or manual check: `.\gradlew.bat test --tests "*MissedCallAutoResponseDecisionTest*"`
- Rollback: delete tests and decision code.

## Sprint 15.2: Settings, Template, Cooldown, And Logging
**Goal**: Add local settings, default template, cooldown persistence, and truthful action labels while preserving existing storage conventions.
**Demo / Validation**: Unit tests prove template rendering and log preview behavior.
**Stop condition**: Stop before Android SMS integration if the log format cannot be extended backward-compatibly.

### Task 15.2.1: Add Settings And Cooldown Store
- Location: new `MissedCallAutoResponseSettings.kt`, new `MissedCallCooldownStore.kt`
- Description: Store feature enabled flag, selected template id, cooldown hours, and last auto-response timestamp per normalized phone.
- Dependencies: Sprint 15.1
- Acceptance criteria:
  - Feature defaults to disabled.
  - Cooldown defaults to 6 hours.
  - Store is local-only SharedPreferences and handles empty/malformed entries safely.
  - Phone keys follow existing normalization/privacy conventions as closely as available.
- Validation command or manual check: JVM tests for pure codec/helpers; source review for SharedPreferences wrapper.
- Rollback: delete settings/cooldown files and callers.

### Task 15.2.2: Add Missed-Call SMS Template
- Location: `SprintOneTemplates.kt`, `TemplateTagRenderer.kt`, `TemplateTags.kt`, template tests
- Description: Add built-in `missed_call_auto_response` template with `{{businessName}}` rendering and safe fallback body when business name is missing.
- Dependencies: none
- Acceptance criteria:
  - Default Hebrew body matches the /goal copy.
  - Missing business name renders the fallback opening, not a blank placeholder.
  - Existing `{lead_name}` style tags keep working.
  - Template editing works through the existing template store.
- Validation command or manual check: `.\gradlew.bat test --tests "*Template*"`
- Rollback: remove template/tag additions.

### Task 15.2.3: Extend Truthful Action Log
- Location: `FollowUpLogEntry.kt`, `FollowUpLogStore.kt`, `FollowUpLogStoreTest.kt`
- Description: Add required Sprint 15 labels and store source/phone/timestamp/messagePreview without full message body.
- Dependencies: none
- Acceptance criteria:
  - New labels: `MISSED_CALL_DETECTED`, `AUTO_SMS_SENT`, `AUTO_SMS_FAILED`, `AUTO_SMS_SKIPPED_NO_PERMISSION`, `AUTO_SMS_SKIPPED_DISABLED`, `AUTO_SMS_SKIPPED_DUPLICATE`, `AUTO_SMS_SKIPPED_NO_NUMBER`, `MANUAL_REPLY_OPENED`.
  - Existing labels continue decoding or legacy rows are skipped only if explicitly documented.
  - Store includes source `missed_call_auto_response` for new entries.
  - Message preview is capped with the existing 80-character convention and never stores the full rendered body by design.
  - Tests assert no `DELIVERED` label exists.
- Validation command or manual check: `.\gradlew.bat test --tests "*FollowUpLogStoreTest*"`
- Rollback: revert log model/store/tests.

## Sprint 15.3: Android SMS Sender And Manual Fallback
**Goal**: Wire Android SMS sending behind explicit enablement and permission checks, with a truthful fallback intent.
**Demo / Validation**: Unit tests cover handler decisions; source review proves no auto-send without enabled flag and permission.
**Stop condition**: Stop if Android requires a broader SMS permission or if SmsManager integration cannot be guarded safely.

### Task 15.3.1: Add SmsSender Wrapper
- Location: new `SmsSender.kt`
- Description: Wrap `SmsManager.sendTextMessage` or the current API-level equivalent with a narrow result type and no delivery claim.
- Dependencies: Sprint 15.2
- Acceptance criteria:
  - Checks `SEND_SMS` before attempting send.
  - Returns immediate success/failed result only for attempted API call.
  - Catches `SecurityException`, `IllegalArgumentException`, and runtime SMS failures.
  - Does not request `READ_SMS`, receive SMS, or claim delivery.
- Validation command or manual check: source review plus JVM-testable adapter boundary where possible.
- Rollback: delete wrapper and callers.

### Task 15.3.2: Add Handler For Missed-Call Pipeline
- Location: new `MissedCallAutoResponseHandler.kt`, `CallDetectionService.kt`
- Description: On missed-call candidate, read latest call log, resolve number/type, settings, permission, cooldown, template/profile, then send, skip, or fallback.
- Dependencies: Task 15.3.1
- Acceptance criteria:
  - Does not trigger for outgoing or answered incoming calls.
  - Does not trigger for unknown/private/empty numbers.
  - Logs `MISSED_CALL_DETECTED` before final send/skip decision when a missed-call candidate exists.
  - Logs skip/fail/send labels truthfully.
  - Writes cooldown only after an attempted/successful automatic SMS according to the final sender result contract.
  - Existing post-call notification still fires for answered calls as before.
- Validation command or manual check: targeted unit tests for pure parts and source review of service path.
- Rollback: disconnect handler from service and delete new files.

### Task 15.3.3: Add Manual SMS Fallback
- Location: `MissedCallAutoResponseHandler.kt`, `MainActivity.kt` if UI action is needed
- Description: Build `ACTION_SENDTO` `smsto:` intent with prefilled text when auto-send cannot happen, and fall back to existing manual composer/WhatsApp path if no SMS app resolves.
- Dependencies: Task 15.3.2
- Acceptance criteria:
  - `MANUAL_REPLY_OPENED` is logged only after `startActivity` succeeds.
  - No direct background Activity launch from service; if background fallback is needed, use a notification/PendingIntent or existing user-tap path.
  - Permission denied and system-blocked cases are visible, not silent.
- Validation command or manual check: source review for background-launch safety; manual QA later on device.
- Rollback: remove fallback integration.

## Sprint 15.4: Consent UI, Setup Section, And Self-Test
**Goal**: Add Hebrew opt-in controls and setup/self-test checks for the new feature.
**Demo / Validation**: User can enable/disable the feature, request SMS permission only on enable, select/edit template, and see truthful status.
**Stop condition**: Stop if the UI would require a navigation rewrite or make setup completion depend on SMS auto-response.

### Task 15.4.1: Add Consent/Settings Section
- Location: `MainActivity.kt`
- Description: Add a simple settings/setup section named `תגובה אוטומטית לשיחה שלא נענתה` with the exact consent copy and enable/cancel buttons from the /goal.
- Dependencies: Sprint 15.2
- Acceptance criteria:
  - Feature defaults disabled.
  - Sensitive `SEND_SMS` permission is requested only when enabling.
  - Status states cover active, permission missing, disabled, and fallback available.
  - Denied permission copy matches the /goal.
  - Disabling immediately prevents future auto SMS.
- Validation command or manual check: source review and manual UI smoke after build.
- Rollback: remove UI section and permission launcher.

### Task 15.4.2: Add Self-Test Checks
- Location: `SelfTestChecker.kt`, `SetupReadinessLogic.kt` only if needed, `MainActivity.kt`
- Description: Add read-only checks for feature toggle, SMS permission, template existence, and cooldown store accessibility.
- Dependencies: Sprint 15.4.1
- Acceptance criteria:
  - Self-test does not send a real SMS.
  - Missing SMS permission reports optional/fixable state without breaking manual WhatsApp readiness.
  - Existing setup wizard completion gate is not made dependent on auto SMS unless explicitly enabled.
- Validation command or manual check: existing setup tests plus source review.
- Rollback: revert self-test additions.

## Sprint 15.5: Docs, QA Checklist, Validation, And Review
**Goal**: Close Sprint 15 with truthful docs, test/build evidence, and a reviewer pass/fail record.
**Demo / Validation**: README, manual QA checklist, execution log, and review describe exactly what works and what still needs device verification.
**Stop condition**: Stop after review; do not claim commercial readiness without release signing and real-device QA.

### Task 15.5.1: Update Sprint And Context Docs
- Location: `tasks/sprint-15-missed-call-auto-response/README.md`, `context/PERMISSIONS_AND_PRIVACY.md`, `context/POST_CALL_ENGINE.md`, `context/TEMPLATE_ENGINE.md`
- Description: Document product goal, permissions, consent, copy, technical behavior, tests, limitations, and known limitation text from the /goal.
- Dependencies: Sprints 15.1-15.4
- Acceptance criteria:
  - README includes product goal, permissions, consent behavior, user-facing copy, technical behavior, test results, known limitations.
  - Manual QA checklist includes all requested bullets, including Samsung device if available.
  - Context docs explicitly mark `SEND_SMS` as Sprint 15 opt-in exception, not a general permission.
- Validation command or manual check: source/doc review.
- Rollback: revert Sprint 15 docs/context additions.

### Task 15.5.2: Run Validation
- Location: repo root
- Description: Run configured tests and build.
- Dependencies: all implementation tasks
- Acceptance criteria:
  - `.\gradlew.bat test` result recorded.
  - `.\gradlew.bat assembleDebug` result recorded.
  - Any lint/typecheck task discovered in Gradle is run or explicitly documented as unavailable.
  - Manifest diff reviewed for only planned `SEND_SMS`.
  - Gradle diff reviewed for no dependency creep.
- Validation command or manual check:
  - `.\gradlew.bat test`
  - `.\gradlew.bat assembleDebug`
  - inspect Gradle tasks for lint/typecheck if present
- Rollback: revert Sprint 15 files only; preserve pre-existing Sprint 14 dirty work.

### Task 15.5.3: Reviewer Gate
- Location: `tasks/sprint-15-missed-call-auto-response/REVIEW.md`
- Description: Apply followup-nadlan-reviewer after execution.
- Dependencies: Task 15.5.2
- Acceptance criteria:
  - Review checks AndroidManifest, `SEND_SMS` exception, no Accessibility, no WhatsApp auto-send, no backend, fallback behavior, setup/self-test, tests/build, and missing real-device evidence.
  - Decision is `PASS`, `PASS WITH NOTES`, `FAIL`, or `BLOCKED` based on evidence.
- Validation command or manual check: reviewer source-level pass.
- Rollback: update review after fixes.

## Testing Strategy
- Unit tests:
  - Missed-call session transitions.
  - Decision engine send/skip/fallback cases.
  - Cooldown within 6 hours and after 6 hours.
  - Template render with `businessName`.
  - Template fallback without `businessName`.
  - Log labels and preview-only storage.
  - Existing WhatsApp, template, post-call, snooze, setup, and pipeline tests remain green.
- Instrumented tests:
  - Not required for MVP unless implementation needs Android-only behavior that cannot be isolated.
- Manual QA:
  - Fresh install.
  - Enable feature.
  - Grant SMS permission.
  - Missed incoming call from known number.
  - Verify SMS is sent.
  - Verify log entry.
  - Verify duplicate call within cooldown does not send again.
  - Disable feature.
  - Missed call again.
  - Verify no auto SMS.
  - Deny SMS permission.
  - Verify fallback/manual path.
  - Test unknown/private number if possible.
  - Test on at least one Samsung device if available.
- Device/OEM checks:
  - Real SMS requires physical-device QA with SIM/SMS capability.
  - Samsung/OEM background behavior still needs human phone smoke.

## Permission Impact
- Added permissions:
  - `android.permission.SEND_SMS`, only for explicit opt-in Sprint 15 auto SMS.
- Removed permissions:
  - none.
- Manifest risk:
  - High, because SMS is a sensitive Android permission and contradicts older project docs unless explicitly documented as this sprint's approved exception.
- User disclosure required:
  - Exact Hebrew consent and denial copy from the /goal.
  - Permission requested only after enable action.

## Data/Schema Impact
- Room entities:
  - Expected none.
- migrations:
  - Expected none. Stop for approval if Room schema is needed.
- local data retention:
  - Store only auto-response settings, cooldown timestamps, and action-log entries.
  - Do not store full SMS body in log.
  - Do not store every call automatically.

## UX Impact
- Screens affected:
  - Setup/settings area in `MainActivity.kt`.
  - Self-test screen.
  - Template management screen through the new built-in template.
- RTL/Hebrew checks:
  - Use full Hebrew copy from the /goal.
  - Keep mixed phone-number/Hebrew layouts readable.
- Empty/fallback states:
  - Disabled.
  - Permission missing/denied.
  - No number/private number.
  - Duplicate within cooldown.
  - Template missing.
  - SMS app unavailable.
- Error states:
  - Android blocks SMS send.
  - No SMS capability/SIM.
  - Manual fallback intent cannot open.

## Rollback Plan
1. Revert Sprint 15 files only.
2. Remove `SEND_SMS` from `AndroidManifest.xml`.
3. Remove missedcall package and tests.
4. Remove the `missed_call_auto_response` template and tag additions.
5. Revert log model/store changes or keep backward-compatible no-op labels only if already shipped locally.
6. Revert Sprint 15 docs.
7. Preserve pre-existing Sprint 14 dirty files and unrelated user changes.
8. Re-run `.\gradlew.bat test` and `.\gradlew.bat assembleDebug`.

## Review Checklist
- AndroidManifest includes only the planned `SEND_SMS` addition and no unrelated permission creep.
- No AccessibilityService.
- No `READ_SMS`, `RECEIVE_SMS`, `WRITE_CALL_LOG`, `QUERY_ALL_PACKAGES`, `SYSTEM_ALERT_WINDOW`.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Existing manual WhatsApp composer works.
- Existing post-call notification/card flow works.
- Existing snooze/reminder flow works.
- Fallback works when `SEND_SMS` is denied.
- Feature disabled means no automatic SMS.
- Unknown/private/empty number does not send.
- Answered incoming and outgoing calls do not send.
- Duplicate cooldown is enforced per phone.
- Logs are truthful and do not claim delivery.
- Log stores message preview only, not full rendered body.
- Setup/self-test status is not broken.
- No backend/API/cloud dependency.
- Docs call out direct-APK / controlled-beta permission risk.

## Potential Gotchas
- Android background restrictions: manual fallback cannot directly launch an Activity from the service; it needs notification/user-tap behavior.
- SMS permission denial: auto-send must stop cleanly and surface fallback.
- Android/OEM SMS policy: even with permission, devices may block or fail SMS sending.
- `READ_CALL_LOG` denial fallback: without a number, no auto SMS can be sent.
- OEM battery killing FGS: missed-call detection may not fire reliably without setup guidance.
- Duplicate cooldown: phone normalization must avoid sending twice to equivalent local/international formats.
- Invalid/private numbers: call-log values like unknown, private, or blank must not be sent to.
- RTL text and mixed Hebrew/phone-number layout: consent/status screens need real-device visual QA.
- Room migration risk: avoid schema changes unless explicitly approved.
- Direct-APK update/install friction: do not imply Play Store readiness.
- Mojibake risk: Hebrew source text should be verified before claiming copy quality.
- Existing docs say no automatic messages/SEND_SMS; Sprint 15 must update that framing narrowly, not erase the original WhatsApp guardrail.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.
- First execution target: Sprint 15.1 only.
- Required stop after Sprint 15.1: report diff, targeted tests, and whether the missed-call tracker can coexist cleanly with the answered-call notification path.
