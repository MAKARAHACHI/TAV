---
name: followup-nadlan-git-worktree
description: Use when FollowUp Nadlan work should be isolated into a separate git worktree/branch, especially for parallel Codex/Claude work or risky Android changes.
---

# FollowUp Nadlan Git Worktree Skill

This skill adapts using-git-worktree to the FollowUp Nadlan workflow.
It isolates agent work and prevents file conflicts.

## When to use

Use a worktree when:

- two agents may work in parallel
- a sprint is risky
- AndroidManifest, Gradle, Room migrations, Foreground Service, or WorkManager are being touched
- the user wants Codex to execute while Claude Code may continue later
- a feature branch should remain clean for review

Do not use a worktree for tiny documentation-only edits unless requested.

## Branch naming

Use:

```text
fdn/<goal-slug>
```

Examples:

```text
fdn/template-engine
fdn/snooze-core
fdn/post-call-engine
fdn/setup-wizard-self-test
```

## Worktree naming

Use sibling directory:

```text
../followup-nadlan-<goal-slug>
```

Example:

```bash
git worktree add -b fdn/snooze-core ../followup-nadlan-snooze-core
```

## Pre-flight

Before creating a worktree:

1. Check `git status` in the main repo.
2. Ensure no uncommitted changes will be stranded.
3. Confirm the base branch.
4. Confirm the selected PLAN.md.
5. Record the worktree path in `tasks/<goal-slug>/EXECUTION_LOG.md`.

## Rules inside worktree

The worktree must still obey:

- product constitution
- approved PLAN.md
- one sprint at a time
- validation after milestones
- no scope expansion

Do not edit files outside the worktree.
Do not share generated build outputs across worktrees.
Do not let two worktrees edit the same Room migration or AndroidManifest in parallel without coordination.

## Collision risk files

Treat these as high-conflict:

- `AndroidManifest.xml`
- Gradle files
- navigation graph / app root
- Room database class
- migration files
- shared design system/theme
- shared template engine
- post-call service
- notification helper

If two branches must edit these, sequence the work instead of running fully parallel.

## Merge-back checklist

Before merging:

- run build/tests in the worktree
- run followup-nadlan-reviewer
- check manifest diff
- check dependency diff
- check generated files are not committed accidentally
- update task logs

## Cleanup

After merge or abandonment:

```bash
git worktree list
git worktree remove ../followup-nadlan-<goal-slug>
git branch -d fdn/<goal-slug>
```

Use `-D` only if the human confirms the branch should be discarded.
