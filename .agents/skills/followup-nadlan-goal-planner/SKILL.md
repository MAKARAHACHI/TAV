---
name: followup-nadlan-goal-planner
description: Use for /goal planning in FollowUp Nadlan. Produces a strict, sprint-based PLAN.md with atomic tasks, non-goals, files to change, validation, rollback, and stop conditions. Does not implement.
---

# FollowUp Nadlan Goal Planner

This skill adapts the generic writing-plans process to the FollowUp Nadlan Android product.
It creates implementation plans only. It must not edit product code.

## Mandatory first steps

Before planning:

1. Read `.agents/skills/followup-nadlan-product-constitution/SKILL.md`.
2. Read the current context folder, especially product overview, architecture, decisions, permissions, UX, roadmap, and any active PLAN.md.
3. Inspect the repo structure and existing implementation patterns.
4. Identify the smallest sprint that can produce a demoable, testable result.

## Clarification policy

Do not interview the user by default.
Ask only if a missing decision would create dangerous implementation ambiguity.

Dangerous ambiguity examples:

- adding or removing a dangerous permission
- changing distribution from direct APK to Play Store
- adding backend/API/CRM integration
- changing Android stack
- auto-sending WhatsApp messages
- changing the core post-call + snooze loop

Otherwise, make an explicit assumption and continue.

## Required plan file location

Create a new folder under:

```text
tasks/<goal-slug>/
```

Write the plan to:

```text
tasks/<goal-slug>/PLAN.md
```

If the task is a fix pass or continuation, preserve the existing task folder and add an appendix instead of creating a duplicate.

## Required PLAN.md template

```md
# PLAN: <Goal Name>

**Status**: Draft | Human-approved | In progress | Blocked | Done
**Planning model**: GPT-5.5 High | GPT-5.4 High
**Execution model**: Codex 5.3 High
**Layer**: Core | Support | Later
**Risk**: Low | Medium | High
**Generated**: <date>

## Goal Statement
One sentence that defines the exact user/product outcome.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes/No
- Core post-call + template + snooze loop preserved: Yes/No
- User-controlled WhatsApp send preserved: Yes/No
- Accessibility avoided: Yes/No
- Backend/API avoided for MVP: Yes/No
- Fallback mode preserved: Yes/No

Decision: Proceed | Revise | Reject
Reason:

## Non-Goals
Things this plan explicitly will not build.

## Assumptions
Smallest assumptions made to avoid unnecessary blocking questions.

## Files To Read First
- <paths>

## Files Expected To Change
- <paths>

## Files That Must Not Change
- <paths>

## Sprint 1: <Name>
**Goal**:
**Demo / Validation**:
**Stop condition**:

### Task 1.1: <Atomic task>
- Location:
- Description:
- Dependencies:
- Acceptance criteria:
- Validation command or manual check:
- Rollback:

### Task 1.2: <Atomic task>
...

## Sprint 2: <Name>
...

## Testing Strategy
- Unit tests:
- Instrumented tests:
- Manual QA:
- Device/OEM checks:

## Permission Impact
- Added permissions:
- Removed permissions:
- Manifest risk:
- User disclosure required:

## Data/Schema Impact
- Room entities:
- migrations:
- local data retention:

## UX Impact
- Screens affected:
- RTL/Hebrew checks:
- Empty/fallback states:
- Error states:

## Rollback Plan
Exact rollback path if the sprint fails.

## Review Checklist
- AndroidManifest is clean.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Snooze restores prepared card.
- Fallback works without READ_CALL_LOG.
- Setup/self-test status is not broken.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.
```

## Sprint rules

Every sprint must be:

- demoable
- runnable
- testable
- independently committable
- small enough to review

Do not make a sprint that only says "implement feature".
Break it into atomic tasks with files and validation.

## Required planning order for app buildout

For a fresh build, prefer this order:

1. Manual WhatsApp screen + template engine + wa.me.
2. Agent profile + real-estate templates + block composer.
3. Local leads + snooze/reminder with WorkManager.
4. Post-call engine with FGS + TelephonyCallback + CallLog where available.
5. Setup wizard + OEM battery guidance + self-test.
6. Signing + update checker + landing/privacy screens.
7. QA on Pixel, Samsung, Xiaomi/Redmi, and real calls.

## Gotchas section

After saving PLAN.md, add a final `Potential Gotchas` section.
Include at least:

- Android background restrictions.
- notification permission denial.
- READ_CALL_LOG denial fallback.
- OEM battery killing FGS.
- duplicate reminders.
- invalid phone formatting for wa.me.
- RTL text and mixed Hebrew/phone-number layout.
- Room migration risk.
- direct-APK update/install friction.

If a gotcha invalidates the plan, mark the plan `Blocked` and stop.

## Do not implement

This skill stops after creating or updating PLAN.md.
Implementation belongs to followup-nadlan-goal-executor.
