---
name: followup-nadlan-product-constitution
description: Use before any FollowUp Nadlan planning, coding, review, or debugging. Enforces the product constitution, MVP boundaries, Android/WhatsApp permission rules, and anti-feature-creep guardrails.
---

# FollowUp Nadlan Product Constitution

This skill is the always-on constitution layer for FollowUp Nadlan.
It must be applied before planning, executing, debugging, reviewing, or proposing changes.

## Product identity

FollowUp Nadlan is a native Android post-call WhatsApp assistant for Israeli real-estate agents.

The product promise:

- After a phone call, help the agent send a fast, professional WhatsApp follow-up.
- Keep the flow semi-automatic: the app prepares the card and message; the user opens WhatsApp and presses Send.
- Reduce the professional anxiety of forgetting to follow up.
- Keep the MVP simple, stable, local-first, and market-ready.

## MVP execution mode

Default MVP distribution is outside Google Play by direct APK link.

This enables a stronger post-call flow while still preserving user control and avoiding WhatsApp automation.
Do not design the MVP around Play Billing, Play Store approval, or Play-only constraints unless a human explicitly changes the distribution strategy.

## Fixed platform and stack

Do not switch the platform or stack without explicit human approval.

Required defaults:

- Android native app.
- Kotlin.
- Jetpack Compose.
- Room for local storage.
- WorkManager for snooze/reminder jobs.
- Foreground Service for call-state monitoring.
- TelephonyCallback / CallStateListener for call-state detection on modern Android.
- Local-first data. No backend API for MVP.
- Hebrew-only UI in MVP, full RTL.

Explicitly do not introduce:

- Flutter / React Native / web app replacement.
- Backend API, CRM sync, Firestore, Supabase, or cloud sync in MVP.
- WhatsApp Business API in MVP.
- Play Billing in MVP.
- Multi-language localization in MVP.

## Core MVP flows

### 1. Manual WhatsApp screen

The app must work even without call permissions.
The user can manually enter a phone number, choose or build a message, and open WhatsApp via wa.me.

### 2. Template engine and block composer

The MVP must include real-estate-focused templates and reusable blocks.
The user can save an agent profile with fields such as:

- full name
- phone
- website
- agency name
- default greeting
- default closing line

Template blocks should support fast construction such as:

- greeting
- "Glad speaking with you about the apartment"
- property details
- next step
- agent signature

### 3. Post-call card

After a detected call ends, the app should present a notification, not force-open an Activity from the background.
The notification opens the post-call card by user action.

The card should contain:

- recipient name if available
- phone number if available
- selected template
- editable message preview
- Open WhatsApp button
- Snooze / remind me later button
- save lead action when relevant

### 4. Snooze is Core

Snooze is not optional polish. It is core MVP behavior.

Reason: a real-estate agent may be driving, with another client, or in a meeting when the call ends. Without snooze, the follow-up opportunity can be lost.

Required quick options:

- Later today
- In 1 hour
- Tonight
- Tomorrow morning
- Custom time, if simple to implement

WorkManager should persist the prepared card state required to restore the reminder.

### 5. Fallback-first behavior

The app must never collapse if permissions are denied.

If READ_CALL_LOG is unavailable or denied:

- Show post-call card with empty phone field.
- Let user paste or type phone manually.
- Continue to support templates, wa.me, and snooze.

If READ_CONTACTS is unavailable:

- Show number only.
- Do not block the core flow.

If notifications are denied:

- Show clear setup status and self-test failure explanation.
- Do not silently pretend the post-call flow is enabled.

## Permission rules

Allowed for the direct-APK MVP when justified and disclosed:

- READ_PHONE_STATE
- READ_CALL_LOG
- POST_NOTIFICATIONS
- FOREGROUND_SERVICE
- FOREGROUND_SERVICE_PHONE_CALL
- RECEIVE_BOOT_COMPLETED
- INTERNET only for version check / license check if implemented
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS only if implemented with clear user explanation
- READ_CONTACTS only as optional secondary improvement, never a startup blocker

Forbidden unless the human gives explicit written approval in the task:

- BIND_ACCESSIBILITY_SERVICE
- SYSTEM_ALERT_WINDOW
- QUERY_ALL_PACKAGES
- WRITE_CALL_LOG
- SEND_SMS / READ_SMS
- WhatsApp automation that presses Send for the user
- NotificationListenerService unless a separate reviewed design exists
- Any attempt to bypass WhatsApp, Android, or user consent

## WhatsApp rules

Only use official user-driven opening flows such as wa.me / ACTION_VIEW.

The product may prepare the message, but must not send it automatically.
The user must press Send inside WhatsApp.

Do not implement Accessibility-based WhatsApp clicking.
Do not implement screen scraping of WhatsApp.
Do not add mass-blast or spam features.

## Data and privacy rules

MVP is local-first.

Allowed local data:

- templates
- template blocks
- agent profile
- local leads saved by explicit user action
- snoozed cards
- sent/opened history when the user acted
- license/trial state

Avoid storing every call automatically.
Store call-related information only when needed for a post-call card, snooze, saved lead, or user action.

## Licensing and updates

Default commercial model:

- 7-day full trial.
- One-time activation code for version 1.x.
- No subscription in MVP.
- No Play Billing.

Default update model:

- app checks a small version.json on launch.
- show update banner if a new APK is available.
- force update only for critical fixes.
- download/install is user-driven through browser or OS installer.

## Setup wizard is mandatory

The first-run wizard must include:

- permission setup
- agent profile setup
- notification setup
- battery/OEM guidance for Samsung, Xiaomi, Redmi, Realme, OnePlus where possible
- self-test for post-call detection and notification flow

Self-test must be available again from Settings.

## Product guardrail questions

Before approving any plan or implementation, answer:

1. Does this reduce immediate follow-up friction for a real-estate agent?
2. Does this keep the user in control of sending WhatsApp messages?
3. Does this avoid Accessibility and unauthorized automation?
4. Does this keep MVP local-first and simple?
5. Does this preserve fallback mode if permissions fail?
6. Does this protect the core post-call + template + snooze loop?
7. Does this avoid adding backend/API/CRM work too early?

If any answer is no, stop and revise the plan.

## Stop conditions

Stop and ask for human approval if a task tries to:

- change the app stack
- remove fallback mode
- remove snooze from MVP
- add backend API or CRM integration
- add AccessibilityService
- auto-send WhatsApp messages
- add Play Store assumptions to direct-APK MVP
- add broad permissions without a matching UX disclosure
- skip setup wizard or self-test
