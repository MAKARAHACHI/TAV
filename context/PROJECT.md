# Project Brief

## Name

Working name: **FollowUp Nadlan**

Hebrew positioning names under consideration:

- המשך שיחה
- נדל"ן Follow-Up
- FollowUp Nadlan

## Target user

Israeli real-estate agents using Android phones.

Common situation:

- They receive many calls from buyers, sellers, renters, landlords, investors, and project leads.
- They often need to send a WhatsApp follow-up immediately after the call.
- They may be driving, in a meeting, with another client, or between property visits.
- Their fear is not only “writing takes time”; it is “I forgot to follow up.”

## User anxiety

The key emotional pain:

```txt
שכחתי לחזור אל מישהו.
```

The app must reduce this anxiety by keeping the follow-up alive until the agent acts or dismisses it.

## What we are building

A small Android utility that:

1. Detects call end.
2. Opens a notification.
3. Presents a Follow-Up Card.
4. Suggests a prepared real-estate WhatsApp message.
5. Lets the user edit or assemble the message quickly.
6. Lets the user snooze the follow-up if now is not the right time.
7. Opens WhatsApp with the message prepared.
8. Lets the user manually press Send.

## What we are not building

This is not:

- A CRM.
- A WhatsApp bot.
- An automatic sender.
- A contact scraping tool.
- A lead marketplace.
- A cloud SaaS platform.
- A sales automation spam tool.
- A replacement for Bmby, PowerLink, Fireberry, or other CRMs.

## Product principle

```txt
Small card, fast action, no heavy management.
```

If a feature makes the agent manage a system instead of continuing a conversation, it probably does not belong in the MVP.

## MVP feature set

### Must have

- Manual composer: phone + message + open WhatsApp.
- Agent profile: full name, business name, website, phone, signature.
- 12 built-in Hebrew templates for real estate.
- Template variables/placeholders.
- Block composer.
- Follow-Up Card.
- Snooze reminder.
- Local lead save after action.
- Call-end detection.
- Last call number detection when permission exists.
- Fallback mode when permission is missing.
- Setup wizard.
- OEM battery guidance.
- Self-test.
- External APK update checker.
- Privacy disclosure.

### Should have if easy

- Optional contacts permission to resolve names.
- Call duration threshold, e.g. show card only after calls longer than 20 seconds.
- Default template selection.
- Recent manually used numbers.

### Later

- Russian localization.
- CRM webhooks.
- Cloud template sync.
- Online activation verification.
- Analytics.
- Property inventory management.

## Pricing decision for v1

- 7-day free trial.
- One-time activation code.
- Suggested price: ₪149 for version 1.x.
- No Play Billing in v1.
- Local activation validation in MVP, with an upgrade path to online validation later.

## Distribution decision for v1

- External APK from landing page.
- Signed release build.
- Clear installation instructions.
- In-app update checker based on remote `version.json`.
- No Google Play listing in v1.
