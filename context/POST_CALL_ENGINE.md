# Post-Call Engine

## Purpose

The Post-Call Engine creates the product magic:

```txt
call ends -> app notices -> follow-up opportunity appears
```

But it must remain reliable, permission-aware, and non-aggressive.

## Architecture decision

Use:

- Foreground Service.
- TelephonyCallback.CallStateListener on Android 12+.
- CallLog query after call ends when `READ_CALL_LOG` is granted.
- Notification as the user-facing entry point.

Do not:

- open Activity directly from background;
- use overlays;
- use Accessibility;
- automatically send WhatsApp;
- store all calls automatically.

## Detection flow

```txt
Service active
-> Call state changes to OFFHOOK/RINGING/IDLE
-> track whether there was an active call
-> when state returns to IDLE after active call
-> wait a short debounce delay
-> query most recent CallLog row if permission exists
-> apply call duration threshold
-> create FollowUpTask
-> show notification
```

## Call duration threshold

Default setting:

```txt
Show follow-up only after calls longer than 20 seconds.
```

Reason:

- Avoid noise from short calls, spam, wrong numbers, quick missed calls.

Allow user to change later.

## Last number detection

If `READ_CALL_LOG` exists:

- Query latest call ordered by date desc.
- Check it is close to now, e.g. within last 1-2 minutes.
- Extract number, type, duration, date.
- Normalize phone number.

If latest call is too old:

- Do not trust it.
- Show fallback card without number.

## Contacts resolution

If `READ_CONTACTS` exists:

- Resolve display name for number.

If not:

- Show number only.

## Notification-only rule

The engine posts notification. It does not force UI.

Primary notification action opens Follow-Up Card via PendingIntent.

## Fallback mode

If permission denied or call log query fails:

```txt
שיחה הסתיימה - פתח כרטיס המשך טיפול
```

Card opens with empty phone field.

## Service survival

- Start service after onboarding.
- Restart on boot if user enabled app.
- Show persistent low-noise service notification if required by Android.
- Setup wizard guides battery optimization.

## Self-test

The app must include a self-test:

```txt
חייג למספר כלשהו, נתק אחרי 5 שניות, ונבדוק אם הכרטיס עלה.
```

Test results:

- permission missing;
- notification missing;
- service inactive;
- battery optimization likely blocking;
- call detected successfully.

## Edge cases

- Missed call: can show a different template later; MVP may ignore or support with template.
- Unknown/private number: show fallback without number.
- Dual SIM: do not overbuild; record if available, ignore otherwise.
- WhatsApp missing: show manual error.
- Multiple calls close together: avoid duplicate tasks; use debounce and recent timestamp.

## Anti-patterns

Reject implementations that:

- require Play Store policy compatibility for v1 at the cost of core external APK decision;
- add `READ_CALL_LOG` but crash if denied;
- launch Activity directly after every call;
- show card after every 2-second call;
- save entire call history;
- use Accessibility to send messages.
