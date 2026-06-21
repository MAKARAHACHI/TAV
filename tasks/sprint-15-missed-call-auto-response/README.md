# Sprint 15: Missed Call Auto Response

## Product Goal

When a business owner misses an incoming call, the app can immediately create a feeling of availability by sending an automatic SMS reply, but only after the user explicitly enables the feature and approves the SMS permission.

Product promise:

```txt
Even when you cannot answer, your business responds immediately.
```

## Permissions

Added permissions:

- `SEND_SMS`: used only for the opt-in missed-call auto response.
- `MANAGE_OWN_CALLS`: added because Android 14 lint requires it with the existing `foregroundServiceType="phoneCall"` service declaration.

Manifest also declares:

- `android.hardware.telephony` with `required="false"` so `SEND_SMS` does not imply telephony hardware is mandatory for install targets.

Not added:

- `READ_SMS`
- `RECEIVE_SMS`
- `WRITE_CALL_LOG`
- `QUERY_ALL_PACKAGES`
- Accessibility or overlay permissions

## Consent Behavior

The feature defaults to disabled.

The app asks for `SEND_SMS` only when the user presses the enable button for missed-call auto response.

Hebrew consent copy implemented in the settings card:

```txt
תגובה אוטומטית לשיחה שלא נענתה

כאשר שיחה נכנסת לא נענית, האפליקציה יכולה לשלוח הודעת SMS אוטומטית בשם העסק שלך.

ההודעה תישלח רק לאחר שתפעיל את האפשרות ותאשר את ההרשאות הנדרשות.

אפשר לכבות את האפשרות בכל רגע.
```

Permission explanation:

```txt
האפליקציה צריכה הרשאת SMS כדי לשלוח תגובה אוטומטית לשיחות שלא נענו.
```

Denied permission state:

```txt
לא ניתן לשלוח SMS אוטומטי ללא הרשאה. אפשר עדיין לפתוח הודעה מוכנה לשליחה ידנית.
```

## Technical Behavior

- `CallStateMonitor` now detects `RINGING -> IDLE` without `OFFHOOK` as a missed incoming-call candidate.
- Answered incoming calls and outgoing/offhook-only calls do not trigger auto SMS.
- `MissedCallAutoResponseDecision` is pure Kotlin and covers send/skip/fallback decisions.
- Cooldown defaults to 6 hours per normalized phone number.
- Auto SMS uses `SmsSender`, guarded by `SEND_SMS`.
- Long SMS messages use Android multipart SMS APIs.
- If auto sending is unavailable, the app posts a manual SMS fallback notification.
- The fallback notification opens `ManualSmsReplyActivity`, which logs `MANUAL_REPLY_OPENED` only after the SMS composer actually opens.
- Logs store action label, timestamp, phone, source, and message preview only.

## Template

New built-in template id:

```txt
missed_call_auto_response
```

Default body:

```txt
שלום, תודה שפניתם ל{{businessName}}.
אנחנו כרגע בשטח או בעבודה ולכן לא תמיד יכולים לענות מיד.
קיבלנו את פנייתכם ונחזור אליכם בהקדם.
אפשר לשלוח כאן בקצרה מה צריך לבצע או לצרף תמונה.
```

If business name is missing, the first line renders as:

```txt
שלום, תודה שפניתם אלינו.
```

## Logging

New action labels:

- `MISSED_CALL_DETECTED`
- `AUTO_SMS_SENT`
- `AUTO_SMS_FAILED`
- `AUTO_SMS_SKIPPED_NO_PERMISSION`
- `AUTO_SMS_SKIPPED_DISABLED`
- `AUTO_SMS_SKIPPED_DUPLICATE`
- `AUTO_SMS_SKIPPED_NO_NUMBER`
- `MANUAL_REPLY_OPENED`

No delivery confirmation is implemented, so the app does not claim delivered status.

## Test Results

- `.\gradlew.bat test`: PASS
- `.\gradlew.bat assembleDebug`: PASS
- `.\gradlew.bat lintDebug`: PASS

## Manual QA Checklist

- Fresh install: NOT RUN
- Enable feature: NOT RUN
- Grant SMS permission: NOT RUN
- Missed incoming call from known number: NOT RUN
- Verify SMS is sent: NOT RUN
- Verify log entry: NOT RUN
- Verify duplicate call within cooldown does not send again: NOT RUN
- Disable feature: NOT RUN
- Missed call again: NOT RUN
- Verify no auto SMS: NOT RUN
- Deny SMS permission: NOT RUN
- Verify fallback/manual path: NOT RUN
- Test with unknown/private number if possible: NOT RUN
- Test on at least one Samsung device if available: NOT RUN

## Known Limitations

Automatic SMS depends on Android permissions and device policy. If the permission is denied or the system blocks sending, the app falls back to a manual prepared reply instead of silently failing.

Real SMS sending, call-log behavior, OEM background behavior, and Samsung-specific behavior still require physical-device QA with SIM/SMS capability.

This is not a commercial-readiness claim; release signing and real-device QA were not completed in this sprint.
