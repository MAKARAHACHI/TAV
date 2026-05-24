# MANUAL SMOKE TEST: Sprint 13 Setup Wizard + OEM Battery Guidance + Self-Test

**Status**: NOT RUN
**Last updated**: 2026-05-24

## Scope
Real-phone checklist for Sprint 13. This document is intentionally not marked PASS until a human reports explicit device evidence.

## Checklist
- [ ] Fresh install or cleared app data: setup wizard appears automatically on first launch.
- [ ] Welcome step renders in Hebrew and RTL.
- [ ] Each permission step shows a Hebrew explanation before the request button.
- [ ] Denying `READ_CALL_LOG` still allows the wizard to complete and manual mode to work.
- [ ] Denying `READ_CONTACTS` still allows the wizard to complete and manual mode to work.
- [ ] Denying `READ_PHONE_STATE` shows clear Hebrew status and still allows the wizard to complete.
- [ ] Agent profile step shows existing saved values.
- [ ] Agent profile save updates the existing profile values.
- [ ] Agent profile step does not reset existing data.
- [ ] OEM/Battery step shows manufacturer-appropriate Hebrew guidance text.
- [ ] `פתח הגדרות סוללה` either opens a system settings screen or shows Hebrew text fallback without crashing.
- [ ] Self-test screen is reachable from the wizard handoff step.
- [ ] Self-test reports correct `PASS`/`FAIL` for currently granted and denied permissions.
- [ ] Battery optimization status is shown as `PASS`, `FAIL`, or `UNKNOWN`; all are acceptable if they match device behavior.
- [ ] Detection-disabled state is shown as actionable.
- [ ] `זה עבד ✓` records the user's observation only.
- [ ] `זה לא עבד ✗` shows troubleshooting text only.
- [ ] `זה עבד ✓` and `זה לא עבד ✗` do not claim the app detected a call.
- [ ] `בדוק שוב` refreshes check results.
- [ ] `הגדרה ובדיקה` button in the main app re-runs the wizard from any screen.
- [ ] Completing the wizard sets the completed flag.
- [ ] Subsequent launches after completion go directly to the main app.
- [ ] Existing post-call notification flow remains intact.
- [ ] Existing four post-call cards remain intact.
- [ ] Existing snooze flow remains intact.
- [ ] Existing manual composer remains intact.
- [ ] Existing WhatsApp open flow remains intact.
- [ ] Existing share flow remains intact.
- [ ] Existing copy flow remains intact.
- [ ] Existing My Details screen remains intact.
- [ ] Existing templates screen remains intact.

## Must Not Claim
- Do not claim WhatsApp messages were sent or delivered.
- Do not claim a phone call was detected by the app.
- Do not mark PASS without explicit real-phone evidence.

## Result
- Manual smoke result: NOT RUN
- Evidence: None yet.
- Notes: Source/build validation passed separately in `EXECUTION_LOG.md`; this file is reserved for real-phone evidence.
