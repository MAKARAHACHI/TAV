# Do Not Build

This file protects the MVP from feature creep.

## Do not build in MVP

- Full CRM.
- Cloud backend.
- User accounts.
- Team management.
- Dashboard analytics.
- AI message generation.
- Automatic WhatsApp sending.
- Accessibility Service.
- Overlay permission.
- SMS sending.
- Mass messaging.
- Campaign sequences.
- Contact scraping.
- Property inventory system.
- Bmby/PowerLink/Fireberry integration.
- Play Store-first build constraints.
- Subscription billing.
- Exact alarm reminders.
- Complex lead pipelines.
- Multi-language localization beyond Hebrew.

## Do not change product identity

Do not turn this into:

- a generic auto-responder;
- a generic CRM;
- a WhatsApp bot;
- a sales spam tool;
- a personal assistant app for all industries.

## Do not violate WhatsApp safety posture

The app opens WhatsApp with text. It does not press Send.

Reject any implementation that:

- uses Accessibility to click Send;
- sends to many contacts automatically;
- hides user confirmation;
- encourages spam-like usage.

## Do not create trust problems

Reject any implementation that:

- stores all call history automatically;
- uploads call logs;
- hides why permissions are needed;
- fails when permissions are denied;
- distributes unsigned/unexplained APKs.

## Do not overcomplicate first build

The first implementation must proceed layer by layer:

```txt
Manual composer -> templates -> follow-up card + snooze -> post-call engine -> setup -> distribution
```

Do not build Layer 4 before Layers 1-3 work manually.
