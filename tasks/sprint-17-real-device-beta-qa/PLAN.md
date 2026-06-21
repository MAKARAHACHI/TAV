# PLAN: Sprint 17 Real Device QA Hardening and Beta Release Candidate

**Status**: Human-approved by current /goal request
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Support
**Risk**: Medium
**Generated**: 2026-06-21

## Goal Statement
Prepare the existing WhatsApp-first missed-call response for controlled real-device beta testing without adding new product features.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Partially by mode; existing Sprint 16 Accessibility auto-send is preserved only because the current task explicitly treats it as completed scope.
- Accessibility avoided: No new Accessibility scope added; existing Sprint 16 service is not expanded.
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This sprint is release-candidate hardening, QA readiness, truthful status/logging, and documentation. It does not add CRM/backend/billing/AI/product expansion.

## Non-Goals
- No new CRM, dashboard, backend, billing, cloud sync, or AI feature.
- No rewrite of Sprint 14, Sprint 15, or Sprint 16.
- No new permission.
- No broad redesign of settings/setup.
- No Google Play readiness claim.
- No WhatsApp Business API claim.

## Assumptions
- Sprint 15 `SEND_SMS` and Sprint 16 Accessibility auto-send are already explicitly approved in prior sprint scope and remain dirty in this worktree.
- The developer/test fake missed-call trigger is skipped unless it can reuse the exact production pipeline behind a clear non-production guard.
- Manual QA cannot be completed by Codex because it requires a physical Android device, SIM, WhatsApp install states, and OEM/background checks.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-router/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `context/00-START-HERE.md`
- `context/ARCHITECTURE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `tasks/sprint-15-missed-call-auto-response/PLAN.md`
- `tasks/sprint-15-missed-call-auto-response/REVIEW.md`
- `tasks/sprint-16-whatsapp-first-missed-call-response/README.md`
- `tasks/sprint-16-whatsapp-first-missed-call-response/REVIEW.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAccessibilityService.kt`
- `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt`
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAccessibilityService.kt`
- `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt`
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`
- `tasks/sprint-17-real-device-beta-qa/PLAN.md`
- `tasks/sprint-17-real-device-beta-qa/WORKTREE_REVIEW.md`
- `tasks/sprint-17-real-device-beta-qa/README.md`
- `tasks/sprint-17-real-device-beta-qa/MANUAL_QA_CHECKLIST.md`
- `tasks/sprint-17-real-device-beta-qa/BETA_RC.md`
- `tasks/sprint-17-real-device-beta-qa/EXECUTION_LOG.md`
- `tasks/sprint-17-real-device-beta-qa/REVIEW.md`

## Files That Must Not Change
- Root Gradle files unless validation reveals an existing build issue.
- Room schema and migrations.
- AndroidManifest unless validation reveals an existing manifest/lint issue.
- Sprint 14, Sprint 15, and Sprint 16 implementation behavior except for narrow QA hardening.

## Sprint 17.1: Worktree And QA Readiness
**Goal**: Classify the existing dirty tree, add a user-facing readiness status, harden truthful logs, and prepare beta QA docs.
**Demo / Validation**: Settings show readiness statuses; tests/build/lint pass; docs state what still needs real-device QA.
**Stop condition**: Stop if any fix requires new permissions, a backend, a product rewrite, or a new production feature.

### Task 17.1.1: Worktree Review
- Location: `tasks/sprint-17-real-device-beta-qa/WORKTREE_REVIEW.md`
- Description: Record Sprint 14, Sprint 15, Sprint 16, Sprint 17, untracked commit candidates, and suspicious/unrelated files.
- Dependencies: none
- Acceptance criteria: `git status` is represented truthfully and no unrelated dirty file is reverted.
- Validation command or manual check: `git status --short --branch`
- Rollback: delete the review note only.

### Task 17.1.2: Readiness Status UI
- Location: `MainActivity.kt`
- Description: Add a minimal Hebrew readiness status section to the existing missed-call response settings card.
- Dependencies: Task 17.1.1
- Acceptance criteria: Status covers active/disabled, primary channel, WhatsApp packages, prepared reply availability, Accessibility status, SMS fallback and permission, and 6-hour duplicate protection.
- Validation command or manual check: `.\gradlew.bat test` and source review.
- Rollback: remove the helper composables and call site.

### Task 17.1.3: Test Mode Decision
- Location: Sprint 17 docs
- Description: Decide whether to add a fake missed-call trigger. Skip if it would expose a production feature or bypass the real call pipeline.
- Dependencies: Task 17.1.1
- Acceptance criteria: Decision is documented.
- Validation command or manual check: source review.
- Rollback: doc-only.

### Task 17.1.4: Truthful Logs And Tests
- Location: `WhatsAppAccessibilityService.kt`, `MissedCallAutoResponseDecisionTest.kt`, `FollowUpLogStoreTest.kt`
- Description: Ensure manual WhatsApp never claims sent, Accessibility logs sent only after click success and failed after a visible no-button/click-failed state, and SMS decisions remain safe.
- Dependencies: Task 17.1.2
- Acceptance criteria: Focused JVM tests cover decision/log vocabulary; Accessibility failure branch is explicit.
- Validation command or manual check: `.\gradlew.bat test`
- Rollback: revert the narrow test and service changes.

### Task 17.1.5: QA And Beta RC Docs
- Location: `README.md`, `MANUAL_QA_CHECKLIST.md`, `BETA_RC.md`, `EXECUTION_LOG.md`, `REVIEW.md`
- Description: Create Sprint 17 release-candidate documentation and real-device checklist.
- Dependencies: Tasks 17.1.1-17.1.4
- Acceptance criteria: Docs do not claim commercial, Google Play, or WhatsApp API readiness.
- Validation command or manual check: source/doc review.
- Rollback: delete Sprint 17 task folder.

## Testing Strategy
- Unit tests: missed-call decision coverage, truthful log vocabulary, existing call-state/template/pipeline tests.
- Instrumented tests: Not added in Sprint 17.
- Manual QA: Required on real devices using `MANUAL_QA_CHECKLIST.md`.
- Device/OEM checks: Required before beta confidence on Samsung/Xiaomi/Pixel or available target devices.

## Permission Impact
- Added permissions: none.
- Removed permissions: none.
- Manifest risk: existing Sprint 15/16 `SEND_SMS` and Accessibility service remain high-risk but are not expanded by Sprint 17.
- User disclosure required: readiness section surfaces missing Accessibility/SMS permission states.

## Data/Schema Impact
- Room entities: none.
- migrations: none.
- local data retention: unchanged.

## UX Impact
- Screens affected: existing setup/settings area in `MainActivity.kt`.
- RTL/Hebrew checks: new copy is Hebrew and minimal.
- Empty/fallback states: missing WhatsApp, missing Accessibility, and missing SMS permission are visible.
- Error states: no new runtime errors intentionally introduced.

## Rollback Plan
1. Revert Sprint 17 code files only.
2. Delete `tasks/sprint-17-real-device-beta-qa/`.
3. Preserve Sprint 14, Sprint 15, Sprint 16 dirty files and untracked folders.
4. Re-run `.\gradlew.bat test`, `.\gradlew.bat assembleDebug`, and `.\gradlew.bat lintDebug`.

## Review Checklist
- AndroidManifest is reviewed and unchanged by Sprint 17.
- No new AccessibilityService.
- No new WhatsApp auto-send capability.
- No backend/API/CRM/billing/AI introduced.
- wa.me / ACTION_VIEW prepared reply still exists.
- SMS fallback remains permission-gated.
- Duplicate cooldown still blocks both WhatsApp and SMS.
- Logs remain truthful.
- Manual QA gaps are explicit.

## Potential Gotchas
- Android background restrictions may prevent service or Activity behavior from matching local source expectations.
- Notification permission denial can hide fallback notifications.
- `READ_CALL_LOG` denial or OEM call-log latency can affect missed-call detection.
- OEM battery management can kill foreground service behavior.
- Duplicate cooldown depends on normalized phone numbers.
- Invalid phone formatting can break WhatsApp/SMS intents.
- RTL text and mixed Hebrew/phone-number layout still need device visual QA.
- Room migration risk is avoided by not changing schema.
- Direct-APK install/update friction remains outside Sprint 17 scope.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.
