# Follow-Up Card

## Purpose

The Follow-Up Card is the core product surface.

It appears after a call or from a reminder and lets the agent handle the next action quickly:

- open WhatsApp;
- edit message;
- assemble message from blocks;
- snooze;
- save as lead;
- dismiss.

## Product rule

The card must feel like a lightweight action card, not a CRM form.

## Entry points

- Post-call notification.
- Snooze notification.
- Manual composer.
- Lead history item.

## Default card layout

```txt
שיחה הסתיימה עם יוסי כהן
050-1234567

הודעה מוצעת:
שלום יוסי, שמחתי לדבר איתך על הדירה...

[פתח WhatsApp]
[הזכר לי אחר כך]
[ערוך הודעה]

פעולות נוספות:
[שמור כליד] [החלף תבנית] [סגור]
```

If there is no contact name:

```txt
שיחה הסתיימה
050-1234567

[הוסף שם]
[פתח WhatsApp]
[הזכר לי אחר כך]
```

If there is no number:

```txt
שיחה הסתיימה

מספר טלפון
[________________]

[פתח WhatsApp]
[הזכר לי אחר כך]
```

## Required fields

Minimum:

- phone number.
- selected template.
- final message text.

Optional:

- lead name.
- lead type.
- property link.
- note.
- status.

## Primary action

The primary action is:

```txt
פתח WhatsApp
```

This opens WhatsApp with prepared message. The app does not send.

## Secondary action

The second action is mandatory:

```txt
הזכר לי אחר כך
```

Snooze keeps the follow-up alive when the agent cannot act now.

## Edit message

The user can edit the final message before opening WhatsApp.

Rules:

- Keep message editing simple.
- Show unresolved placeholders clearly.
- Keep the rendered message visible.
- Do not force property details.

## Save as lead

Saving as lead is optional and should not block sending.

Minimal lead save:

```txt
שם
טלפון
סוג לקוח
סטטוס
הערה קצרה
```

Default status:

```txt
חדש
```

## Dismiss behavior

If user dismisses a post-call card:

- Do not save lead automatically.
- Optionally keep a temporary dismissed task for short time to avoid duplicate notifications.

If user dismisses a snooze reminder:

- Mark FollowUpTask as `DISMISSED`.

## Anti-patterns

Reject card designs that:

- require filling long forms;
- hide the WhatsApp action;
- make snooze hard to find;
- save all calls automatically;
- show CRM-like pipelines in the card;
- add AI generation before templates are done.
