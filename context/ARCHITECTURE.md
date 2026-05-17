# Architecture

## Platform

Native Android.

Recommended stack:

- Kotlin.
- Jetpack Compose.
- Material 3.
- Room.
- DataStore or SharedPreferences for small settings.
- Hilt or simple manual DI depending on project size.
- WorkManager for deferrable snooze reminders.
- Foreground Service for call-state listener.
- TelephonyCallback on Android 12+.
- PhoneStateListener fallback only if required for older supported APIs.

## High-level modules

```txt
app/
  ui/
    onboarding/
    manualComposer/
    followUpCard/
    templates/
    leads/
    settings/
  domain/
    followup/
    templates/
    leads/
    activation/
    permissions/
  data/
    room/
    datastore/
    update/
  service/
    postcall/
    reminder/
  platform/
    whatsapp/
    telephony/
    contacts/
    notifications/
```

Exact package names may differ, but keep these responsibility boundaries.

## Core runtime flow

```txt
ForegroundService starts after onboarding/boot
-> registers TelephonyCallback.CallStateListener
-> detects transition to IDLE after an active call
-> queries last call from CallLog if permission exists
-> creates FollowUpTask draft
-> posts notification
-> user taps notification
-> FollowUpCardActivity/Screen opens
-> user opens WhatsApp or snoozes
```

## Manual fallback flow

```txt
User opens app manually
-> enters/pastes phone number
-> chooses template or quick blocks
-> edits final message
-> taps Open WhatsApp
-> app opens wa.me intent
```

This flow must work even if every sensitive permission is denied.

## Notifications

Use notification actions instead of background Activity launch.

Main post-call notification:

```txt
Title: שיחה הסתיימה
Body: פתח כרטיס המשך טיפול
Actions: [פתח כרטיס] [הזכר לי אחר כך]
```

If contact/name/number exists:

```txt
Title: שיחה הסתיימה עם יוסי כהן
Body: רוצה לשלוח WhatsApp המשך?
Actions: [פתח כרטיס] [הזכר לי]
```

Do not depend on Heads-Up being guaranteed. Configure a high-importance channel, but treat Heads-Up as best effort.

## Background execution rules

- Do not launch Activity directly from a BroadcastReceiver or background callback.
- Use notification with `PendingIntent.getActivity()`.
- Keep the Foreground Service minimal.
- Do not do heavy database or network work inside telephony callbacks.
- When possible, convert events into local tasks and let UI handle them.

## Data storage

Local-first. No cloud in MVP.

Room stores:

- AgentProfile.
- Template.
- MessageBlock.
- Lead.
- FollowUpTask.
- MessageDraft / MessageHistory.
- ActivationState.
- AppSettings.

Avoid storing all calls by default. Store only after:

- user opens card and saves as lead;
- user opens WhatsApp;
- user snoozes;
- user explicitly saves.

## WhatsApp integration

Use official click-to-chat style URL:

```txt
https://wa.me/<international_number_without_plus>?text=<url_encoded_message>
```

Support packages:

- `com.whatsapp`
- `com.whatsapp.w4b`

The app should not send messages automatically. It only opens WhatsApp with prepared text.

## Permissions architecture

The app must run in degraded mode when permissions are missing.

- Without `READ_CALL_LOG`: no automatic number; manual number field.
- Without `READ_CONTACTS`: show number only; no contact name.
- Without notifications permission: app still works manually, but post-call/snooze value is weak; onboarding must explain.
- Without battery optimization exemption on aggressive OEMs: self-test may fail; wizard must guide.

## Update architecture

External update check:

```txt
Fetch https://followup-nadlan.co.il/version.json
```

Expected shape is defined in `context/DATA_CONTRACTS.md`.

MVP behavior:

- Check on app start.
- If new version exists, show banner.
- If force update, block app use except update/help screen.
- Download via browser link, not internal DownloadManager in MVP.

## Activation architecture

MVP:

- 7-day local trial.
- User enters activation code.
- App validates code locally against a hash/signature rule.
- No server dependency after activation.

Future:

- Online activation service.
- Device limit.
- License revocation.

Do not build future activation in MVP unless explicitly requested.
