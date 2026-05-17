# Readiness Audit

Generated: 2026-05-17

Scope: repository readiness after the readiness-fix pass. This audit verifies that the project is ready to start Sprint 1 planning/execution, but does not start Sprint 1.

Files reviewed:

- `context/00-START-HERE.md`
- all `context/*.md` files
- all repo-local skills under `.agents/skills/*/SKILL.md`
- `.gitignore`
- `README.md`
- `CODEX_INSTALL_CONTEXT_PROMPT.md`

No app source code was modified. No Android implementation was started. No dependencies were added.

## 1. Context Completeness

Status: PASS

Findings:

- Product goal is clear: FollowUp Nadlan is a native Android post-call WhatsApp assistant for Israeli real-estate agents.
- MVP scope is clear: manual composer, templates/profile, Follow-Up Card, Snooze, post-call detection, setup/self-test, external APK activation/update.
- Post-call WhatsApp flow is clear: prepare the card/message, open WhatsApp through `wa.me` / `ACTION_VIEW`, and require the user to press Send manually.
- Snooze / remind-me-later is explicitly Core in the constitution, roadmap, Follow-Up Card doc, Snooze doc, and guardian skill.
- Fallback mode is clear: manual composer works without sensitive permissions, and post-call cards can open with an empty phone field.
- External-to-Play-Store distribution is clear: v1 is a signed direct APK outside Google Play.
- Hebrew RTL MVP is clear.
- One-time activation code model is clear: 7-day trial, local activation validation, no Play Billing.

Blocking issues: None.

Recommended fixes: None.

## 2. Context Consistency

Status: PASS

Findings:

- Skill references now consistently use the repo-root `.agents/skills/` path.
- The missing `followup-nadlan-vision-guardian` reference is resolved at `.agents/skills/followup-nadlan-vision-guardian/SKILL.md`.
- `context/NEW_CHAT_BOOTSTRAP.md`, `context/CODEX_GOAL.md`, `context/UPDATE_MAP.md`, `README.md`, and `CODEX_INSTALL_CONTEXT_PROMPT.md` now point to `.agents/skills/...`.
- No old API-first architecture remains. `context/DATA_CONTRACTS.md` explicitly says there is no backend API, and the update JSON is documented as a static manifest rather than an app backend.
- No Play Billing dependency remains in the MVP context.
- No WhatsApp Business API dependency remains.
- No automatic WhatsApp sending through Accessibility remains.

Blocking issues: None.

Recommended fixes: None.

## 3. Android Policy Guardrails

Status: PASS

Findings:

- AccessibilityService for WhatsApp sending is forbidden.
- Automatic WhatsApp sending without the user pressing Send is forbidden.
- Play Store-first assumptions are forbidden for v1.
- Unnecessary backend/API dependency is forbidden.
- Unnecessary cloud dependency is forbidden.
- The allowed WhatsApp integration is user-driven `wa.me` / `ACTION_VIEW` only.
- Background Activity launch after call end is forbidden; notification-based user action is required.

Blocking issues: None.

Recommended fixes: None.

## 4. Permissions Guardrails

Status: PASS

Findings:

- Required MVP permissions are documented in `context/PERMISSIONS_AND_PRIVACY.md`.
- Optional permissions are documented: `READ_CONTACTS` only for contact name resolution.
- Forbidden permissions are documented: `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`, `WRITE_CALL_LOG`, `SEND_SMS`, and SMS-reading behavior is forbidden by the constitution.
- Package visibility is constrained to specific WhatsApp package queries instead of `QUERY_ALL_PACKAGES`.
- Fallback behavior is documented for denied `READ_CALL_LOG`, `READ_PHONE_STATE`, `POST_NOTIFICATIONS`, and `READ_CONTACTS`.
- Sprint 1 should remain permission-free because it is only Manual WhatsApp Screen + Template Engine + `wa.me` flow.

Blocking issues: None.

Recommended fixes: None.

## 5. Skills Readiness

Status: PASS

Skills reviewed:

- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `.agents/skills/followup-nadlan-goal-router/SKILL.md`
- `.agents/skills/followup-nadlan-goal-planner/SKILL.md`
- `.agents/skills/followup-nadlan-goal-executor/SKILL.md`
- `.agents/skills/followup-nadlan-reviewer/SKILL.md`
- `.agents/skills/followup-nadlan-systematic-debugging/SKILL.md`
- `.agents/skills/followup-nadlan-git-worktree/SKILL.md`
- `.agents/skills/followup-nadlan-vision-guardian/SKILL.md`

Findings:

- Each available skill has a clear purpose.
- Activation timing is clear by skill: constitution first, router for broad `/goal`, planner for plans, executor for approved plans, reviewer after execution, debugger for failures, worktree for isolated risky/parallel work, guardian for product-vision checks.
- Stop conditions are clear and aligned with the product constitution.
- No reviewed skill authorizes scope expansion without human approval.
- No reviewed skill contradicts the product constitution.
- The vision guardian explicitly protects Android-first app direction, Hebrew RTL MVP, real-estate follow-up workflow, user-driven `wa.me`, Snooze as Core, local-first MVP, no unnecessary backend/API, and external-to-store MVP distribution.

Blocking issues: None.

Recommended fixes: None.

## 6. Development Readiness For Sprint 1

Status: PASS

Sprint 1 target:

```txt
Manual WhatsApp Screen + Template Engine + wa.me flow only.
```

Findings:

- The manual composer is clearly defined.
- The template engine is clearly defined.
- The `wa.me` contract and phone normalization expectations are clear.
- Hebrew RTL is required.
- Sensitive permissions, call detection, snooze, leads, activation, and update checking are explicitly out of Sprint 1.
- Git repository initialization was performed for baseline tracking.
- `.gitignore` exists and excludes common local/build artifacts.
- The legacy root-level `followup-nadlan-vision-guardian/` package artifact is ignored; the canonical skill lives under `.agents/skills/followup-nadlan-vision-guardian/`.

Blocking issues: None.

Recommended fixes:

- Start Sprint 1 only from an explicit approved Sprint 1 goal/plan.
- Keep Sprint 1 restricted to Manual WhatsApp Screen + Template Engine + `wa.me` flow.

## Final Decision

The readiness blockers from the prior audit are resolved. The repository is ready for Sprint 1 planning/execution under the documented workflow, but Sprint 1 has not been started.

READY_FOR_SPRINT_1: YES
