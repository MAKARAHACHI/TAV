---
name: followup-nadlan-reviewer
description: Use after FollowUp Nadlan execution to verify scope, Android permissions, Manifest safety, wa.me flow, Snooze behavior, fallback behavior, tests, and QA evidence before marking work complete.
---

# FollowUp Nadlan Reviewer

This skill adapts verification-before-completion and requesting-code-review into a product-specific review gate.
It does not trust completion claims. It verifies them.

## Inputs to review

Read:

1. `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
2. the relevant `PLAN.md`
3. the relevant `EXECUTION_LOG.md`
4. git diff
5. changed files
6. test output or logs

## Required output

Create or update:

```text
tasks/<goal-slug>/REVIEW.md
```

Use this structure:

```md
# REVIEW: <Goal Name>

**Decision**: PASS | PASS WITH NOTES | FAIL | BLOCKED
**Reviewed at**: <date>

## Scope Review
- Matches PLAN.md: Yes/No
- Scope expanded: Yes/No
- Files changed match expected list: Yes/No
- Unexpected files:

## Product Constitution Review
- Android native/Kotlin/Compose preserved: Yes/No
- Local-first MVP preserved: Yes/No
- No backend/API introduced: Yes/No
- Hebrew RTL preserved where applicable: Yes/No
- Snooze remains Core: Yes/No

## Permission and Manifest Review
- No AccessibilityService: Yes/No
- No auto-send WhatsApp automation: Yes/No
- No QUERY_ALL_PACKAGES: Yes/No
- No SMS permissions: Yes/No
- No WRITE_CALL_LOG: Yes/No
- READ_CALL_LOG only if planned and disclosed: Yes/No/Not applicable
- POST_NOTIFICATIONS handled: Yes/No/Not applicable
- FOREGROUND_SERVICE_PHONE_CALL handled: Yes/No/Not applicable

## WhatsApp Flow Review
- Uses wa.me/ACTION_VIEW: Yes/No
- User must press Send in WhatsApp: Yes/No
- Handles invalid/missing number: Yes/No
- Handles WhatsApp not installed if relevant: Yes/No/Not applicable

## Post-Call Review
- No direct Activity launch from background receiver: Yes/No/Not applicable
- Notification opens Activity via user action: Yes/No/Not applicable
- Fallback without number works: Yes/No/Not applicable
- Setup/self-test not broken: Yes/No/Not applicable

## Snooze Review
- Prepared card state persists: Yes/No/Not applicable
- Reminder notification restores card: Yes/No/Not applicable
- Duplicate reminders avoided: Yes/No/Not applicable
- Time options match MVP: Yes/No/Not applicable

## Template Review
- Blocks can compose a message: Yes/No/Not applicable
- Agent profile placeholders resolve: Yes/No/Not applicable
- Message is editable before WhatsApp: Yes/No/Not applicable
- Hebrew RTL and phone-number mixed layout acceptable: Yes/No/Not applicable

## Tests and Validation
- Unit tests:
- Build:
- Manual QA:
- Device/OEM QA:
- Evidence missing:

## Blockers
- <list>

## Required fixes before merge
- <list>
```

## Review method

Do not review only the final UI.
Review source-level behavior.

Must inspect:

- AndroidManifest.xml
- Gradle dependency changes
- service/receiver registration
- notification code and PendingIntent path
- Room schema/migrations if changed
- WorkManager workers if changed
- phone formatting helpers if changed
- template interpolation logic if changed
- privacy/setup screens if changed

## Pass criteria

A PASS requires:

- implementation matches PLAN.md
- no forbidden permissions or APIs
- no WhatsApp auto-send
- fallback states exist
- validation evidence exists or missing evidence is explicitly accepted by the human
- no hidden backend/API expansion

## Fail criteria

Fail the review if:

- code uses AccessibilityService for WhatsApp
- code sends WhatsApp automatically
- AndroidManifest includes forbidden permissions without explicit plan approval
- app breaks when READ_CALL_LOG is denied
- Snooze was removed or made non-core
- backend/API was added for MVP without approval
- tests/build were skipped without explanation

## Review summary style

Be direct and operational.
Do not soften blockers.
Do not mark work complete because "it looks fine".
Mark as PASS only when evidence supports it.
