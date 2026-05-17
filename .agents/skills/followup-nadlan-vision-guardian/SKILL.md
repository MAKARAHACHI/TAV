---
name: followup-nadlan-vision-guardian
description: Product and architecture review guardrail for FollowUp Nadlan. Use before planning, before coding, after coding, or when reviewing work to keep the app aligned with a lightweight post-call WhatsApp follow-up assistant for Israeli real-estate agents.
---

# FollowUp Nadlan Vision Guardian

Use this skill whenever work affects product scope, Android architecture, permissions, UI flow, templates, reminders, post-call detection, WhatsApp integration, distribution, or context docs.

This skill does not replace `.agents/skills/followup-nadlan-product-constitution/SKILL.md`. The product constitution is the highest-priority guardrail. This guardian is the practical vision check that prevents scope drift during plans, implementation, and reviews.

## Core Product Truth

FollowUp Nadlan is a native Android app for Israeli real-estate agents. It helps turn phone calls into WhatsApp follow-ups now or later. It detects call end when allowed, creates a Follow-Up Card, lets the user choose/edit/build a real-estate message, supports snooze reminders, and opens WhatsApp with prepared text. The user manually presses Send.

The intended flow is:

```txt
Phone call ends -> notification -> Follow-Up Card -> edit/choose/snooze -> open WhatsApp -> user sends manually
```

Fallback flow:

```txt
Manual number -> template/message -> open WhatsApp
```

## Protected Product Boundaries

- Android-first app: native Android, Kotlin, Jetpack Compose.
- Hebrew RTL MVP: Hebrew-only UI for v1, with full RTL behavior.
- Real-estate agent workflow: post-call follow-up for Israeli real-estate agents, not a generic CRM or generic assistant.
- WhatsApp semi-automatic flow: use `wa.me` / `ACTION_VIEW`; prepare the message but never press Send for the user.
- Snooze / remind-me-later is Core: follow-ups must remain alive when the agent cannot act immediately.
- Local-first MVP: Room/local storage first; no unnecessary cloud dependency.
- No unnecessary API/backend: no backend, CRM sync, Firestore, Supabase, or cloud sync in MVP.
- External-to-store MVP distribution: v1 is a direct signed APK outside Google Play unless a human explicitly changes that strategy.

## Non-Negotiables

- The app is not a CRM.
- The app does not send WhatsApp messages automatically.
- The app must work manually even if sensitive permissions are denied.
- Snooze is mandatory in MVP.
- Data is local-first in MVP.
- Notification is the post-call entry point; do not force Activity from background.
- `READ_CONTACTS` is optional.
- `READ_CALL_LOG` is used only for last-number detection in external APK v1.
- No Accessibility Service.
- No Play Billing in MVP.
- No cloud/backend unless a later phase explicitly approves it.

## Current Anti-Patterns

Flag and reject work that:

- turns the app into a full CRM;
- adds AI generation before templates are complete;
- adds mass messaging or auto-send;
- uses Accessibility to control WhatsApp;
- saves all calls automatically;
- requires every permission before manual mode works;
- hides snooze behind menus;
- opens Activities directly from background callbacks;
- adds Play Billing or subscription logic in MVP;
- adds cloud sync in MVP;
- changes the app into a Play Store-first product in v1;
- creates many overlapping docs or contradicts context files.

## Review Procedure

Before approving a plan or code change, answer:

1. Does this move the product toward the target follow-up flow?
2. Does this reduce the agent's chance of forgetting a lead?
3. Does it keep the UI lightweight and action-oriented?
4. Does it preserve manual fallback mode?
5. Does it avoid unsafe WhatsApp automation?
6. Does it keep permissions minimal and explainable?
7. Does it avoid premature CRM/platform scope?
8. Does it preserve the external APK MVP distribution decision?
9. Does it update context docs if behavior, scope, or contracts changed?

If any answer is no, stop and revise the plan before implementation.

## Validation Evidence Required

After code changes, require:

- Build/syntax check if possible.
- Manual smoke test description.
- Contract check against `context/DATA_CONTRACTS.md` for changed data models.
- UI flow check against the relevant feature doc.
- Permission fallback check if permissions were touched.
- Concise report: what changed, what was verified, what remains.

## Architecture Direction

- Room owns local durable data.
- DataStore/SharedPreferences owns small settings.
- Foreground Service owns call-state listening.
- WorkManager owns MVP snooze reminders.
- WhatsApp integration stays behind a small platform boundary.
- Template rendering stays behind a testable domain component.
- External update checker stays separate from app core.

## Stop Conditions

Stop and ask for human approval if a plan or implementation tries to:

- change the native Android stack;
- remove Hebrew RTL as the MVP default;
- remove snooze from MVP;
- auto-send WhatsApp messages;
- add Accessibility-based WhatsApp control;
- add backend/API/cloud dependency for MVP;
- make Play Store distribution or Play Billing a v1 assumption;
- remove fallback mode when sensitive permissions are denied.

## Reference

Read `references/vision.md` when writing handoffs, context docs, broader plans, or major architecture changes.
