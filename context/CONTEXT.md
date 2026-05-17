# Project Context

## Product identity

**FollowUp Nadlan** is an Android follow-up assistant for Israeli real-estate agents.

The product solves one painful moment:

> A call ends. The agent is busy, driving, or between clients. If the follow-up is not handled now or snoozed, the lead may be forgotten.

The app must feel like a small professional action tool, not a CRM.

## Core promise

```txt
Every real-estate call can become a clean WhatsApp follow-up now or later.
```

Marketing phrase:

```txt
סיימת שיחה עם לקוח? שלח לו WhatsApp מקצועי תוך 10 שניות — או קבל תזכורת בזמן שנוח לך.
```

## MVP product shape

The MVP has five product surfaces:

1. Manual WhatsApp composer.
2. Agent profile and message templates.
3. Follow-Up Card.
4. Snooze reminders.
5. Post-call detection and external-distribution setup.

## Strategic decision

Version 1 starts outside Google Play. This allows the app to request `READ_CALL_LOG` and test automatic last-number detection. However, user trust must remain high:

- Explain why each permission is needed.
- Store data locally.
- Do not send messages automatically.
- Always allow fallback mode without call-log access.

## Non-negotiable user experience

The app must remain fast:

```txt
Call ended -> visible reminder -> one card -> one message -> WhatsApp
```

The agent should not feel like they are entering a CRM. The app should not ask for long forms after every call.

## Key product decisions

- Native Android, Kotlin, Jetpack Compose.
- Hebrew-only MVP with full RTL.
- Local-only storage in Room.
- External APK distribution in v1.
- One-time activation code after 7-day trial.
- Notification-only post-call surface; no forced background Activity launch.
- Snooze is mandatory in MVP.
- WhatsApp opens via `wa.me` or equivalent `ACTION_VIEW` intent.
- User manually presses Send in WhatsApp.

## Main risk areas

1. Android permissions and user trust.
2. Foreground service reliability on Samsung/Xiaomi/other OEMs.
3. Background activity launch restrictions.
4. Call-log detection edge cases.
5. APK installation friction outside the store.
6. WhatsApp anti-spam/account-safety risk if users send repetitive messages.

## Success criteria for MVP

The MVP is successful if a real-estate agent can:

1. Install and activate the app outside the store.
2. Complete setup wizard.
3. Save agent profile and signature.
4. Use 12 built-in Hebrew real-estate templates.
5. Open WhatsApp from a manual number field.
6. End a phone call and get a notification.
7. Open a Follow-Up Card with the last call number when permission works.
8. Snooze the card and receive it again later.
9. Save a lead only after action.
10. Use the app for a day without feeling it is a CRM.

## Current active phase

Before app code: create and review the context system.

Next implementation phase after context approval:

```txt
Layer 1: Manual WhatsApp Screen + minimal Template Engine + wa.me opening
```
