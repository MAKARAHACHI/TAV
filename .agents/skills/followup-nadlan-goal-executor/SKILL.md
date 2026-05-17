---
name: followup-nadlan-goal-executor
description: Use to execute an approved FollowUp Nadlan PLAN.md. Runs one sprint at a time, updates EXECUTION_LOG.md, validates after milestones, and prevents scope expansion.
---

# FollowUp Nadlan Goal Executor

This skill adapts the generic executing-plans process to FollowUp Nadlan.
It executes only an approved plan.

## Pre-flight checks

Before changing files:

1. Read `.agents/skills/followup-nadlan-product-constitution/SKILL.md`.
2. Read the selected `tasks/<goal-slug>/PLAN.md`.
3. Confirm PLAN.md status is `Human-approved` or explicitly approved in the current task.
4. Read the files listed under `Files To Read First`.
5. Check current git status.
6. Create or update `tasks/<goal-slug>/EXECUTION_LOG.md`.

If the plan is missing, vague, or unapproved, stop and invoke the planner.

## Execution discipline

Execute one sprint at a time.
Do not silently continue to the next sprint after a meaningful failure.
Do not expand scope to adjacent features.
Do not refactor unrelated code.
Do not add a dependency unless the PLAN.md allows it or the human approves.

## Required execution log format

Append to:

```text
tasks/<goal-slug>/EXECUTION_LOG.md
```

Use this structure:

```md
# EXECUTION LOG: <Goal Name>

## Sprint <n>: <name>
Status: Not started | In progress | Blocked | Completed
Started:
Completed:

### Changes made
- <file>: <summary>

### Validation run
- Command: `<command>`
- Result: PASS | FAIL | NOT RUN
- Evidence:

### Manual QA
- Check:
- Result:
- Notes:

### Deviations from plan
- None | <details>

### Blockers
- None | <details>

### Next recommended action
- Continue | Review | Fix | Stop
```

## Mandatory validation moments

Run validation after each meaningful milestone.

Prefer:

- `./gradlew test`
- `./gradlew assembleDebug`
- `./gradlew connectedAndroidTest` when device/emulator is available and relevant
- lint checks if configured
- manual QA checklist for notification, wa.me, snooze, and fallback flows

If a validation command cannot run, record exactly why.
Do not mark it PASS without evidence.

## Product-specific execution constraints

### AndroidManifest

Do not add forbidden permissions.
If touching AndroidManifest, explicitly review permissions in EXECUTION_LOG.md.

### WhatsApp

Use only user-driven wa.me / ACTION_VIEW.
Do not automate clicking Send.

### Snooze

If touching snooze/reminder code, validate:

- prepared card state persists
- notification reopens the same card
- duplicate reminders are avoided
- cancellation works if implemented

### Templates

If touching templates/block composer, validate:

- Hebrew RTL UI is readable
- placeholders resolve correctly
- missing profile fields do not crash the message
- message can be edited before opening WhatsApp

### Post-call engine

If touching call detection, validate:

- service starts correctly
- notification appears after call-end event
- fallback card works without number
- denied permissions show clear status
- no Activity launch happens directly from receiver/background

### Updates/licensing

If touching update or license code, validate:

- offline behavior after activation
- trial state
- invalid code state
- version.json network failure state

## Stop conditions

Stop immediately if:

- the implementation requires a forbidden permission
- AndroidManifest risk changes beyond the plan
- AccessibilityService becomes necessary
- WhatsApp auto-send is suggested
- a backend/API is needed for the sprint
- tests fail in a way unrelated to the sprint and cannot be isolated
- the plan is insufficient for the next step

## Completion response

When done, summarize:

- sprint completed
- files changed
- validation commands and results
- blockers or deviations
- whether the work is ready for followup-nadlan-reviewer
