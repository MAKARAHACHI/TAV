# New Chat Bootstrap

You are joining the **FollowUp Nadlan** project.

This repo is for a native Android app that helps Israeli real-estate agents turn phone calls into WhatsApp follow-up actions, immediately or later, without becoming a full CRM and without unsafe WhatsApp automation.

## Required reading order

1. `context/CONTEXT.md`
2. `.agents/skills/followup-nadlan-vision-guardian/SKILL.md`
3. `context/PROJECT.md`
4. `context/ARCHITECTURE.md`
5. `context/ROADMAP.md`
6. `context/DATA_CONTRACTS.md`
7. `context/UPDATE_MAP.md`
8. Task-specific docs:
   - `context/FOLLOW_UP_CARD.md`
   - `context/TEMPLATE_ENGINE.md`
   - `context/SNOOZE_REMINDERS.md`
   - `context/POST_CALL_ENGINE.md`
   - `context/PERMISSIONS_AND_PRIVACY.md`
   - `context/OEM_SETUP_WIZARD.md`
   - `context/EXTERNAL_DISTRIBUTION.md`
   - `context/DO_NOT_BUILD.md`

## Current product truth

FollowUp Nadlan is a lightweight post-call assistant for Israeli real-estate agents. After a phone call ends, the app helps the agent send a professional WhatsApp follow-up using prepared real-estate templates, quick message blocks, agent profile fields, and snooze reminders. WhatsApp is opened with a prepared message via `wa.me`; the user always presses Send manually inside WhatsApp.

Target flow:

```txt
Phone call ends -> app detects call end -> read last call number if permission exists -> show notification -> user opens Follow-Up Card -> choose/edit/snooze message -> open WhatsApp via wa.me -> user manually sends
```

Fallback flow when call-log access is missing or fails:

```txt
Phone call ends -> notification -> Follow-Up Card opens with empty phone field -> user enters/pastes number -> choose/edit/snooze message -> open WhatsApp
```

## Current repo reality

At the beginning of implementation, assume no production app exists yet unless repo inspection proves otherwise. The context system is the source of truth until code exists. Build layer by layer from `context/ROADMAP.md`; do not jump directly to a full app.

## Guardrail

Use `.agents/skills/followup-nadlan-vision-guardian/SKILL.md` before planning, before coding, after coding, and when reviewing any implementation.

## Do not add yet

- No full CRM.
- No cloud backend in MVP.
- No automatic WhatsApp sending.
- No Accessibility Service.
- No Play Store-first constraints in v1, but keep privacy and user trust high.
- No AI message generation in MVP.
- No Bmby/PowerLink integration in MVP.
- No account system in MVP.
- No subscription billing in MVP.
