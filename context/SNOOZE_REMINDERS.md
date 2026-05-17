# Snooze Reminders

## Product truth

Snooze is mandatory in MVP.

Without snooze, the app only helps agents who can act immediately. With snooze, it prevents forgotten follow-ups when the agent is driving, in a meeting, or with another client.

Core anxiety solved:

```txt
שכחתי לחזור אל מישהו.
```

## UX

On the Follow-Up Card, next to `פתח WhatsApp`, show:

```txt
הזכר לי אחר כך
```

Snooze sheet options:

```txt
בעוד 15 דקות
בעוד 30 דקות
בעוד שעה
בערב
מחר בבוקר
```

Optional later:

```txt
בחר שעה
```

## Technical decision

Use WorkManager in MVP.

Reason:

- Good enough for deferrable reminders.
- Survives app process death.
- Simpler than exact alarms.

Important limitation:

- Do not promise exact-to-the-minute reminders.
- Android/OEM battery behavior may delay work.

Do not use `SCHEDULE_EXACT_ALARM` in MVP unless the user explicitly decides exact reminders are required.

## FollowUpTask preservation

A snooze must save the full task, not just a timestamp.

Required preserved data:

- phone.
- contactName.
- callEndedAt.
- selectedTemplateId.
- draftText.
- leadType.
- propertyLink.
- reminderAt.
- status.

When reminder fires, the same Follow-Up Card opens with the same draft.

## Reminder notification copy

```txt
יוסי כהן מחכה להמשך טיפול
[פתח כרטיס] [הזכר שוב] [סגור]
```

If no name:

```txt
יש שיחת נדל״ן שמחכה להמשך טיפול
[פתח כרטיס] [הזכר שוב] [סגור]
```

## Status transitions

```txt
DRAFT -> SNOOZED
SNOOZED -> OPENED
SNOOZED -> DISMISSED
OPENED -> WHATSAPP_OPENED
OPENED -> SNOOZED
OPENED -> SAVED_AS_LEAD
```

## Edge cases

### User snoozes without phone number

Allow it. The reminder returns with empty phone field.

### User changes template before snooze

Save final draft text and selected template ID.

### User opens WhatsApp after snooze

Mark task `WHATSAPP_OPENED`.

### App is rebooted

WorkManager should restore eligible work. If using additional boot receiver for service restart, keep it minimal.

## Anti-patterns

Reject implementations that:

- only save reminder time without draft;
- lose the message when reminder returns;
- require exact alarm permission for MVP;
- create multiple duplicate reminders for same task;
- hide snooze behind menus.
