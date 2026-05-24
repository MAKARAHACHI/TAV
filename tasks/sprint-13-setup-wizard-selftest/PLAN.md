# PLAN: Sprint 13 Setup Wizard + OEM Battery Guidance + Self-Test

**Status**: Human-approved
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core (Roadmap Layer 5)
**Risk**: Medium
**Generated**: 2026-05-21

## Goal Statement
Make the post-call engine trustworthy on real phones by adding a first-run Setup Wizard that explains and requests the already-declared permissions, guides OEM battery/autostart settings per manufacturer, lets the user save the agent profile, and runs a Self-Test that reports the real on-device readiness of the post-call notification flow — all without adding new permissions, dependencies, backend, licensing, or boot-restart.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes — wizard wraps it, does not change it
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes — wizard never blocks manual mode

Decision: Proceed
Reason: This implements Roadmap Layer 5. `context/OEM_SETUP_WIZARD.md` makes the setup wizard and self-test mandatory for the external-APK MVP, because the post-call engine is unreliable without battery/OEM guidance. It adds no new permissions (all are already declared from Sprints 8–12), no dependencies, no backend, and no licensing/update scope (that is Sprint 14).

## Scope Note: Large Sprint, Hard Gates
This sprint is larger than a single feature because a credible wizard needs welcome, permissions, profile, OEM guidance, and self-test in one coherent flow. To control risk, execution is split into five sub-sprints with mandatory gates. No Gradle dependency is added at any point. Pure logic (status model, OEM mapping) is built and unit-tested before any UI depends on it.

## Non-Goals
- Do not add any new permission. Reuse only what Sprints 8–12 already declared.
- Do not add licensing, trial, activation code, or `version.json` update check (Sprint 14 / Layer 6).
- Do not add `RECEIVE_BOOT_COMPLETED` or any boot-restart of the service.
- Do not add `SCHEDULE_EXACT_ALARM`, `AlarmManager`, Accessibility, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`, full-screen intent, SMS, or `NotificationListenerService`.
- Do not add backend/API, network calls, analytics, Room migration, or any dependency.
- Do not place a phone call programmatically inside the self-test.
- Do not rebuild My Details, templates, property links, the four cards, the snooze loop, or the Action Log. The wizard reuses them.
- Do not change WhatsApp open/share/copy behavior.
- Do not mark manual smoke PASS without real-phone evidence.

## Assumptions
- minSdk 26, targetSdk 34 unchanged.
- All permissions the wizard requests/explains already exist in the Manifest from prior sprints, so the Manifest diff should be EMPTY. The wizard organizes runtime requests and disclosure, not new declarations.
- The self-test checks CONDITIONS, not a live call: notification permission state, `READ_PHONE_STATE` state, `READ_CALL_LOG` state (optional, for number auto-fill), whether the call-detection service is currently enabled per `CallDetectionPreferences`, whether the notification channel is enabled, and battery optimization status where queryable. It then guides the user to make a manual test call, matching `context/OEM_SETUP_WIZARD.md` Step 5.
- Battery-optimization guidance opens the standard system settings screen via a safe intent (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` / `ACTION_APPLICATION_DETAILS_SETTINGS`) wrapped in try/catch; if no activity resolves, the wizard shows manual textual guidance instead and never crashes. Do NOT add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; only open settings screens the user acts on themselves.
- OEM detection uses `Build.MANUFACTURER` / `Build.BRAND` only; no package queries.
- First-run detection uses a SharedPreferences flag such as `setup_completed`. The wizard is re-runnable from a Settings/entry button; existing agent-profile data is reused, not reset.
- The agent-profile step reuses the existing `MyDetailsStore` and existing profile screen logic; the wizard does not create a second profile store.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
- `context/OEM_SETUP_WIZARD.md`
- `context/POST_CALL_ENGINE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/ARCHITECTURE.md`
- `context/DO_NOT_BUILD.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionPreferences.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/setup/SetupStatus.kt` (new, pure model)
- `app/src/main/java/com/followupnadlan/setup/SetupReadinessLogic.kt` (new, pure logic)
- `app/src/main/java/com/followupnadlan/setup/OemGuidance.kt` (new, pure OEM mapping)
- `app/src/main/java/com/followupnadlan/setup/BatteryOptimizationIntents.kt` (new, safe intent builder)
- `app/src/main/java/com/followupnadlan/setup/SetupPreferences.kt` (new, SharedPreferences flag)
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt` (new, reads real conditions)
- `app/src/main/java/com/followupnadlan/MainActivity.kt` (wizard nav state + entry from settings/first run + self-test screen)
- `app/src/test/java/com/followupnadlan/setup/SetupReadinessLogicTest.kt` (new)
- `app/src/test/java/com/followupnadlan/setup/OemGuidanceTest.kt` (new)
- `tasks/sprint-13-setup-wizard-selftest/PLAN.md`
- `tasks/sprint-13-setup-wizard-selftest/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-13-setup-wizard-selftest/EXECUTION_LOG.md`
- `tasks/sprint-13-setup-wizard-selftest/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `app/src/main/java/com/followupnadlan/whatsapp/*`
- `app/src/main/java/com/followupnadlan/templates/*`
- `app/src/main/java/com/followupnadlan/followuplog/*`
- `app/src/main/java/com/followupnadlan/snooze/*`
- `app/src/main/java/com/followupnadlan/data/*`
- `app/src/main/java/com/followupnadlan/postcall/*` except read-only reuse
- `app/src/main/java/com/followupnadlan/notifications/*`
- `context/*` unless behavior contradicts a context doc, in which case STOP and re-enter the planner.
- All prior sprint task folders.

## Sub-sprint 1: Pure Setup/Self-Test Status Model + Readiness Logic
**Goal**: Define a testable model of what "ready for post-call" means and compute a readiness verdict from boolean inputs, with no Android dependency in the pure layer.
**Demo / Validation**: `.\gradlew.bat test` proves readiness logic returns the correct overall status and per-check status from combinations of granted/denied conditions.
**Stop condition**: Stop if the pure logic needs Android imports. Those belong in Sub-sprint 4's checker.

### Task 1.1: Define SetupStatus model
- Location: `app/src/main/java/com/followupnadlan/setup/SetupStatus.kt`
- Description: Pure enums/data classes: a per-check result (`CheckId`, `CheckState { PASS, FAIL, OPTIONAL_MISSING, UNKNOWN }`) and an overall readiness verdict (`READY`, `PARTIAL`, `MANUAL_ONLY`).
- Acceptance criteria: No Android imports; checks cover notifications, phone-state, call-log (optional), contacts (optional), detection-enabled, channel-enabled, battery-optimization (unknown allowed).
- Validation: source review + `.\gradlew.bat test`.
- Rollback: delete the file.

### Task 1.2: Define SetupReadinessLogic (pure)
- Location: `app/src/main/java/com/followupnadlan/setup/SetupReadinessLogic.kt`
- Description: Pure function mapping boolean/tri-state inputs to per-check `CheckState` and an overall verdict. Optional permissions denied -> `OPTIONAL_MISSING`, not `FAIL`. Notifications + phone-state + detection-enabled are required for `READY`.
- Acceptance criteria: deterministic; no Android imports; optional vs required logic matches `context/PERMISSIONS_AND_PRIVACY.md` degradation rules.
- Validation: `.\gradlew.bat test` via Task 1.3.
- Rollback: delete the file.

### Task 1.3: SetupReadinessLogic tests
- Location: `app/src/test/java/com/followupnadlan/setup/SetupReadinessLogicTest.kt`
- Description: Cover: all granted -> READY; notifications denied -> not READY; phone-state denied -> MANUAL_ONLY or PARTIAL per rule; call-log denied but rest granted -> READY with OPTIONAL_MISSING on call-log; detection toggle off -> PARTIAL with a clear actionable check; battery UNKNOWN does not block READY.
- Acceptance criteria: all pass; no Android dependency.
- Validation: `.\gradlew.bat test`.
- Rollback: delete the test.

**GATE 1**: status-logic tests pass; assembleDebug clean; Manifest/Gradle diff empty.

## Sub-sprint 2: OEM Detection + Guidance + Safe Battery Intent
**Goal**: Map manufacturer to the right Hebrew battery/autostart guidance and provide a crash-safe way to open the relevant settings screen.
**Demo / Validation**: OEM mapping tests pass; the intent helper returns a resolvable intent or signals "show text guidance" without throwing.
**Stop condition**: Stop if any path needs `QUERY_ALL_PACKAGES`, a new permission, or vendor-specific component names that risk crashing.

### Task 2.1: OemGuidance (pure)
- Location: `app/src/main/java/com/followupnadlan/setup/OemGuidance.kt`
- Description: Pure mapping from normalized manufacturer string to a guidance block: Samsung, Xiaomi/Redmi, Realme, OnePlus, with a generic fallback. Hebrew text mirrors `context/OEM_SETUP_WIZARD.md` Step 4.
- Acceptance criteria: no Android imports; deterministic; unknown manufacturer -> generic guidance.
- Validation: `.\gradlew.bat test` via Task 2.3.

### Task 2.2: BatteryOptimizationIntents (safe)
- Location: `app/src/main/java/com/followupnadlan/setup/BatteryOptimizationIntents.kt`
- Description: Build a best-effort intent to open battery-optimization or app-details settings, wrapped so the caller can check resolvability. Do NOT add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Prefer standard settings; never hard-code fragile OEM activities.
- Acceptance criteria: never throws to the caller; returns a resolvable intent or a clear "no intent" signal.

### Task 2.3: OemGuidance tests
- Location: `app/src/test/java/com/followupnadlan/setup/OemGuidanceTest.kt`
- Description: Cover Samsung, Xiaomi, Redmi, Realme, OnePlus, and unknown -> generic; verify Hebrew text keys are non-empty and distinct where expected.

**GATE 2**: OEM tests pass; intent helper degrades safely; assembleDebug clean; diffs empty.

## Sub-sprint 3: Setup Wizard UI
**Goal**: A first-run, re-runnable Hebrew RTL wizard: welcome -> permissions -> agent profile (reuse) -> OEM battery guidance -> self-test handoff.
**Demo / Validation**: assembleDebug clean; wizard appears on first run (flag unset) and is reachable again from a Settings/entry button; existing screens unaffected.
**Stop condition**: Stop if the wizard needs a navigation library, a dependency, or a manifest change.

### Task 3.1: SetupPreferences
- Location: `app/src/main/java/com/followupnadlan/setup/SetupPreferences.kt`
- Description: Tiny SharedPreferences wrapper: `isSetupCompleted()`, `setSetupCompleted(Boolean)`. Local only.

### Task 3.2: Wizard screen state + steps
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a wizard screen state with steps: Welcome, Permissions, Agent Profile, OEM/Battery guidance, and a button into the Self-Test screen. On finishing, set `setup_completed=true`. Show automatically when unset; otherwise normal app entry. Add a Hebrew entry button, e.g. `הגדרה ובדיקה`, to re-run it.
- Acceptance criteria: Hebrew RTL; no new permission; denial never blocks finishing the wizard or manual mode; existing composer/cards/snooze remain reachable; no manifest/gradle change.

### Task 3.3: Reuse agent profile inside the wizard
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: The profile step renders existing My Details fields via the existing store; it must not create a second profile model or reset existing values.

**GATE 3**: assembleDebug clean; wizard reachable first-run and from Settings; diffs empty; Hebrew valid.

## Sub-sprint 4: Self-Test Screen
**Goal**: Report real on-device readiness per check and guide a manual test call.
**Demo / Validation**: On a real phone, self-test shows correct PASS/FAIL for granted vs denied permissions, shows whether detection is enabled, and explains the manual test-call step. No call is placed by the app.
**Stop condition**: Stop if a check requires reading data the app is not permitted to read, or placing a call.

### Task 4.1: SelfTestChecker
- Location: `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- Description: Reads real conditions: notification permission (API 33+), notification channel enabled, `READ_PHONE_STATE`, optional `READ_CALL_LOG`, optional `READ_CONTACTS`, detection-enabled flag from `CallDetectionPreferences`, and battery-optimization status where queryable (UNKNOWN allowed). Feeds these into `SetupReadinessLogic`.
- Acceptance criteria: no detection-logic changes; no call placed; safe when status is unqueryable; no crash on any OEM.

### Task 4.2: Self-Test screen UI
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Hebrew RTL screen listing each check with clear PASS/FAIL/optional/unknown indicator and a short actionable line. Include manual test-call guidance from `OEM_SETUP_WIZARD.md` Step 5 with `התחל בדיקה / זה עבד / זה לא עבד` framing, where observation is user-reported only.

**GATE 4**: self-test reports correct denied-vs-granted on a real phone; diffs empty; Hebrew valid.

## Sub-sprint 5: Evidence Docs + Review
**Goal**: Truthful closure artifacts.
**Demo / Validation**: Docs capture validation, diffs, forbidden-scope grep, Hebrew check, Sprint 8–12 contract regression, and manual QA status.
**Stop condition**: Stop if docs claim PASS without real-phone evidence.

### Task 5.1: MANUAL_SMOKE_TEST.md
- Location: `tasks/sprint-13-setup-wizard-selftest/MANUAL_SMOKE_TEST.md`
- Description: Real-phone checklist. Status starts NOT RUN unless the human reports real-phone evidence.

### Task 5.2: EXECUTION_LOG.md
- Location: `tasks/sprint-13-setup-wizard-selftest/EXECUTION_LOG.md`
- Description: Per sub-sprint: changes, exact command results, dependency diff, manifest diff, forbidden-scope grep, Hebrew check, Sprint 8–12 contract check, deviations, blockers, next action.

### Task 5.3: REVIEW.md
- Location: `tasks/sprint-13-setup-wizard-selftest/REVIEW.md`
- Description: Reviewer-skill format. Decision PASS only if source/build/unit pass; record manual smoke separately; PASS WITH NOTES if real-phone smoke is missing.

**GATE 5**: docs truthful; review complete; ready for `followup-nadlan-reviewer`.

## Testing Strategy
- Unit tests: `SetupReadinessLogicTest` and `OemGuidanceTest`; existing Sprint 1–12 tests must keep passing.
- Instrumented/device: wizard renders and is re-runnable; self-test reports correct statuses; battery settings link opens or falls back without crashing.
- Build: `.\gradlew.bat test assembleDebug`.
- Manual QA: required on a real Android phone before PASS.
- Dependency diff check: `git diff -- app/build.gradle.kts build.gradle.kts` must be EMPTY.
- Manifest diff check: `git diff -- app/src/main/AndroidManifest.xml` must be EMPTY.
- Contract regression: Sprint 8–12 flows still work as listed in the validation order.

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; manifest diff must be empty. If a change seems required, STOP.
- User disclosure required: Yes — the wizard provides Hebrew prominent-disclosure copy before sensitive runtime requests. This is UI text, not a new permission.

## Data/Schema Impact
- Room entities: None.
- Migrations: None.
- Local data retention: one new SharedPreferences flag (`setup_completed`). No new sensitive data stored.
- Storage mechanism: SharedPreferences only; reuse existing `MyDetailsStore` and `CallDetectionPreferences`.

## UX Impact
- Screens affected: new Setup Wizard flow and Self-Test screen; existing app shell gains a `הגדרה ובדיקה` entry. Existing composer, cards, snooze, My Details, templates, property links unchanged.
- RTL/Hebrew checks: all new strings valid Hebrew; mixed Hebrew/permission-name layout readable.
- Empty/fallback states: permission denied -> manual mode still works; battery deep link unavailable -> text guidance shown; status unqueryable -> UNKNOWN, not FAIL.
- Error states: settings intents wrapped in try/catch with Hebrew fallback text; no crash on any OEM.

## Rollback Plan
1. Delete `app/src/main/java/com/followupnadlan/setup/`.
2. Revert Sprint 13 hunks in `MainActivity.kt`.
3. Delete `tasks/sprint-13-setup-wizard-selftest/`.
4. No Gradle/manifest changes were made, so prior sprints are unaffected.

## Review Checklist
- No new permission; manifest diff empty.
- No new dependency; Gradle diff empty.
- No licensing/trial/activation/version.json.
- No RECEIVE_BOOT_COMPLETED, AlarmManager, exact alarms, Accessibility, SYSTEM_ALERT_WINDOW, QUERY_ALL_PACKAGES, full-screen intent, SMS, NotificationListenerService.
- No backend/API, network, analytics.
- Wizard never blocks manual mode; denial of any permission still completes setup.
- Self-test reads conditions only; never places a call; never claims detection that did not happen.
- OEM guidance shows manufacturer-appropriate Hebrew text; battery deep link safe with text fallback.
- Agent profile reuses existing `MyDetailsStore`; no duplicate store; no data loss.
- Sprint 8–12 contracts preserved.
- Pure logic is unit-tested.
- Manual smoke not claimed PASS without real-phone evidence.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sub-sprint at a time, gates 1->2->3->4->5 in strict order.
- Expansion rule: no scope expansion without human approval. Specifically, do not add licensing/update, boot-restart, exact alarms, or any new permission.

## Potential Gotchas
- Android background restrictions: mitigate via guidance, not code.
- Notification permission denial: self-test must report it clearly; wizard still completes.
- READ_CALL_LOG denied: optional; phone auto-fill simply will not happen.
- Battery-optimization deep links vary: wrap in try/catch and fall back to text guidance.
- Build.MANUFACTURER casing/spelling varies: normalize and fall back to generic.
- Re-running the wizard must not reset the saved agent profile or detection toggle.
- Self-test must not imply it placed or detected a call; `זה עבד / זה לא עבד` records user observation.
- Do not let the wizard become licensing/onboarding-paywall scope; that is Sprint 14.
