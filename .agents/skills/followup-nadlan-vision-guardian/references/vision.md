# FollowUp Nadlan Vision Reference

## Product Target

FollowUp Nadlan is a practical native Android utility for Israeli real-estate agents. Its job is to prevent post-call lead loss.

The product should feel like:

```txt
Call ended -> small card -> message almost ready -> WhatsApp
```

Not like:

```txt
Call ended -> CRM management -> many fields -> operational burden
```

## Target User

Israeli real-estate agent using Android.

They may be:

- driving;
- entering a property;
- speaking with another client;
- between showings;
- handling sellers, buyers, investors, renters, or landlords.

They need speed, confidence, and memory support.

## Core Emotional Job

The anxiety is:

```txt
I forgot to follow up.
```

Snooze directly solves this and is Core for the MVP.

## Core Workflow

1. A call ends.
2. App detects call end when permissions and Android behavior allow it.
3. App reads the last call number if allowed.
4. Notification appears.
5. User opens card or snoozes.
6. Card shows number/name and suggested message.
7. User edits or composes from blocks.
8. User opens WhatsApp through `wa.me` / `ACTION_VIEW`.
9. User sends manually inside WhatsApp.
10. App optionally saves local lead/history only after user action.

## Domain Model

### AgentProfile

The user's business identity:

- full name;
- business name;
- phone;
- website;
- signature.

### Lead

A person the agent may follow up with.

Minimal fields:

- name;
- phone;
- type;
- status;
- notes;
- last follow-up.

### Template

Reusable message body with placeholders.

### MessageBlock

Reusable fragment for quick composition.

### FollowUpTask

The current actionable reminder created manually, after call, or from snooze.

This is the central object that prevents forgetting.

## MVP Templates

12 built-in Hebrew templates:

1. Buyer - thanks + property details.
2. Buyer - schedule visit.
3. Buyer - similar properties.
4. Buyer - budget/financing check.
5. Seller - thanks + valuation offer.
6. Seller - schedule valuation meeting.
7. Seller - selling process explanation.
8. Investor - yield details.
9. Investor - investment opportunity.
10. Missed call - tried to reach you.
11. Interest - saw you asked about property.
12. After meeting - thanks + next step.

## Distribution Vision

V1 is an external signed APK because automatic last-number detection needs CallLog access. This is a business and trust challenge, not just a technical shortcut.

The product must therefore include:

- clear landing page;
- signed APK;
- privacy explanation;
- setup wizard;
- self-test;
- update checker;
- activation code.

## Future Integrations

Later only:

- CRM webhooks;
- Bmby/PowerLink/Fireberry export/integration;
- cloud template sync;
- Russian localization;
- online activation;
- usage analytics.

These must not block MVP.

## Non-Goals

- Full CRM.
- Auto-send bot.
- Accessibility automation.
- Mass messaging.
- Cloud SaaS.
- Team dashboard.
- AI copywriter.
- Play Store-first version.
- Exact-alarm scheduling.

## Core Sentence

```txt
The app helps a real-estate agent turn every phone call into a WhatsApp follow-up now or later, without forgetting, without a heavy CRM, and without automatic sending.
```
