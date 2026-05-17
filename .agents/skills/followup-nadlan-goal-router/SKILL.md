---
name: followup-nadlan-goal-router
description: Use when receiving a /goal or a broad FollowUp Nadlan request. Routes work between planning, execution, review, debugging, and worktree isolation while preserving the approved model strategy.
---

# FollowUp Nadlan Goal Router

This skill decides which FollowUp Nadlan skill should handle the current request.
It prevents multi-agent chaos by forcing a single clear phase: plan, execute, review, debug, or isolate.

## Required model strategy

Use this routing metadata in plans and handoffs:

- Complex planning: GPT-5.5 High.
- Simpler planning: GPT-5.4 High.
- Final implementation/execution: Codex 5.3 High.

Execution must ultimately be done by Codex 5.3 High against an approved plan.

Do not let the executing agent invent architecture while coding. The plan and constitution are the source of truth.

## Routing rules

### Use followup-nadlan-product-constitution first

Always load and apply the constitution before any other project skill.

### Use followup-nadlan-goal-planner when

- The user gives a /goal.
- The request is a feature, bugfix, infrastructure task, release task, or multi-step change.
- The task has uncertainty, multiple files, permissions, Android lifecycle concerns, Room schema work, WorkManager, Foreground Service, or UI flow changes.

Output: an approved-ready PLAN.md. No implementation.

### Use followup-nadlan-goal-executor when

- A PLAN.md already exists.
- The user explicitly asks to execute.
- The task has clear acceptance criteria.

Output: code changes, EXECUTION_LOG.md updates, tests/build evidence.

### Use followup-nadlan-reviewer when

- Execution claims are complete.
- The user asks whether the work is done.
- There is a diff, PR, patch, branch, or completed sprint.

Output: REVIEW.md or review summary with pass/fail/blockers.

### Use followup-nadlan-systematic-debugging when

- There is a bug, failing build, failing test, OEM issue, permission issue, notification issue, post-call issue, snooze issue, or WhatsApp opening issue.

Output: DEBUG_LOG.md plus minimal fix plan or patch.

### Use followup-nadlan-git-worktree when

- Two or more independent goals may run in parallel.
- The user wants isolated sprint branches.
- There is risk of agents editing the same files.

Output: worktree creation plan and branch hygiene instructions.

## Phase discipline

Never combine planning and broad execution in the same step unless the task is tiny and explicitly approved.

For any non-trivial task:

1. plan
2. approve
3. execute one sprint
4. validate
5. review
6. continue or stop

## Required handoff block for every /goal

Every plan must include this handoff metadata:

```md
## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.
```

## Do not ask unnecessary questions

Ask a clarifying question only if the missing answer blocks safe execution.
Otherwise, make the smallest reasonable assumption, record it in PLAN.md, and continue.
