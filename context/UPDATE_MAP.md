# Update Map

## Global rule

If code behavior, product scope, data contracts, architecture, permissions, roadmap status, or distribution behavior changes, update the matching context files before closing the task.

## If product scope changes

Update:

- `context/CONTEXT.md`
- `context/PROJECT.md`
- `context/ROADMAP.md`
- `context/DO_NOT_BUILD.md`
- `context/NEW_CHAT_BOOTSTRAP.md`
- `.agents/skills/followup-nadlan-vision-guardian/SKILL.md` if guardrails changed
- `.agents/skills/followup-nadlan-vision-guardian/references/vision.md` if broader product truth changed

## If architecture changes

Update:

- `context/ARCHITECTURE.md`
- `context/CONTEXT.md`
- `context/ROADMAP.md`
- `context/DATA_CONTRACTS.md` if contracts changed

## If data models or contracts change

Update:

- `context/DATA_CONTRACTS.md`
- `context/ARCHITECTURE.md`
- Any specific feature doc that owns the model, e.g. `SNOOZE_REMINDERS.md` or `TEMPLATE_ENGINE.md`

## If permissions change

Update:

- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/OEM_SETUP_WIZARD.md` if onboarding changes
- `context/ARCHITECTURE.md`
- `context/CONTEXT.md`

## If Follow-Up Card behavior changes

Update:

- `context/FOLLOW_UP_CARD.md`
- `context/DATA_CONTRACTS.md` if `FollowUpTask` changes
- `context/ROADMAP.md` if phase status changes

## If Snooze behavior changes

Update:

- `context/SNOOZE_REMINDERS.md`
- `context/DATA_CONTRACTS.md`
- `context/ARCHITECTURE.md`
- `context/ROADMAP.md`

## If Post-Call behavior changes

Update:

- `context/POST_CALL_ENGINE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/OEM_SETUP_WIZARD.md`
- `context/DATA_CONTRACTS.md` if task source/status changes

## If distribution/update behavior changes

Update:

- `context/EXTERNAL_DISTRIBUTION.md`
- `context/DATA_CONTRACTS.md` if version JSON changes
- `context/PROJECT.md` if business model changes

## If roadmap phase/status changes

Update:

- `context/ROADMAP.md`
- `context/CONTEXT.md`
- `context/NEW_CHAT_BOOTSTRAP.md`

## Pre-close checklist

Before closing any implementation task, answer:

1. Does the code still match `context/CONTEXT.md`?
2. Does it avoid the non-goals in `context/DO_NOT_BUILD.md`?
3. Does data match `context/DATA_CONTRACTS.md`?
4. Does the UI match the relevant feature doc?
5. Did permissions remain minimal?
6. Did the guardian skill approve the plan and result?
7. Were affected context files updated?
8. Was a local validation or smoke test run?
