# Permissions And Privacy

## Privacy posture

The app must be local-first in MVP.

User-facing principle:

```txt
האפליקציה קוראת את יומן השיחות רק כדי לזהות את המספר האחרון אחרי שיחה.
המידע נשמר במכשיר בלבד.
לא נשלחות הודעות אוטומטית.
המשתמש תמיד מאשר את השליחה בעצמו ב-WhatsApp.
```

## Required MVP permissions

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_CALL_LOG"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

Depending on implementation and API targets, the exact foreground service permission names may require adjustment. Do not add extra permissions without need.

## Optional permissions

```xml
<uses-permission android:name="android.permission.READ_CONTACTS"/>
```

Use only to show contact names. The app must work without it.

## Package queries

Use specific package queries, not `QUERY_ALL_PACKAGES`:

```xml
<queries>
  <package android:name="com.whatsapp" />
  <package android:name="com.whatsapp.w4b" />
</queries>
```

## Explicitly forbidden in MVP

Do not add:

```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
<uses-permission android:name="android.permission.WRITE_CALL_LOG"/>
<uses-permission android:name="android.permission.SEND_SMS"/>
```

Do not use Accessibility to press Send in WhatsApp.

## Permission degradation rules

### READ_CALL_LOG denied

Allowed behavior:

- Detect call state if possible.
- Show notification after call.
- Open Follow-Up Card with empty phone field.
- Ask user to type/paste number.

Do not crash. Do not block the entire app.

### READ_PHONE_STATE denied

Allowed behavior:

- Manual composer still works.
- Explain post-call detection requires permission.
- Do not repeatedly nag.

### POST_NOTIFICATIONS denied

Allowed behavior:

- Manual composer works.
- Snooze and post-call reminders are weakened.
- Explain that reminders require notification permission.

### READ_CONTACTS denied

Allowed behavior:

- Show phone number only.
- Do not require contact access.

## Prominent disclosure copy

Use clear Hebrew before requesting sensitive permissions:

```txt
כדי לפתוח כרטיס המשך שיחה אחרי שיחה, האפליקציה צריכה לזהות שהשיחה הסתיימה ולקרוא את המספר האחרון מיומן השיחות.
המידע נשמר במכשיר שלך בלבד.
אנחנו לא שולחים הודעות אוטומטית ולא מעלים את יומן השיחות לשרת.
```

Buttons:

```txt
אשר והרשה
לא עכשיו - מצב ידני
```

## Data retention

MVP should not store every call.

Store only when:

- user opens WhatsApp;
- user snoozes;
- user saves as lead;
- user edits/saves a draft.

## Sensitive UX rule

Never present permission denial as failure. Present it as mode selection:

```txt
אפשר להמשיך במצב ידני.
```
