# Sprint 16: WhatsApp-First Missed Call Response

## Scope

Sprint 16 changes the missed-call response priority from SMS-first to WhatsApp-first while preserving Sprint 15 SMS behavior as fallback.

## Implemented Behavior

- Reuses Sprint 15 missed-call detection, template rendering, cooldown, duplicate suppression, and guarded SMS sender.
- Adds a pure channel decision layer for WhatsApp-first, SMS-only, fallback SMS, manual SMS, duplicate, no-number, outgoing-call, and answered-call states.
- Adds prepared WhatsApp mode: opens WhatsApp or WhatsApp Business with the missed-call response prefilled. The user still taps Send.
- Adds opt-in Accessibility WhatsApp auto-send mode. It must be explicitly selected by the user and requires Android Accessibility settings approval.
- Adds package resolution for `com.whatsapp` and `com.whatsapp.w4b`.
- Adds truthful logs for WhatsApp prepared/opened/failed, auto attempted/sent/failed, accessibility missing, and SMS fallback states.
- Keeps SMS fallback behind configuration and runtime `SEND_SMS` permission.

## Safety Notes

- This is on-device Android automation for controlled beta/private APK use.
- This is not WhatsApp Business API integration.
- Manual WhatsApp mode does not log sent.
- `WHATSAPP_AUTO_SENT` is logged only by the Accessibility service after a send-button click succeeds.
- The Accessibility service reacts only to WhatsApp package windows and only while a short-lived pending missed-call send exists.

## Manual QA Checklist

- Fresh install.
- Enable missed-call response.
- Select WhatsApp first.
- Test with WhatsApp Messenger installed.
- Test with WhatsApp Business installed if available.
- Miss an incoming call.
- Verify WhatsApp opens with the correct number and message.
- Verify SMS is not sent when WhatsApp opens successfully.
- Enable Accessibility auto-send.
- Miss an incoming call.
- Verify WhatsApp sends automatically.
- Verify logs.
- Disable Accessibility.
- Verify safe fallback to prepared WhatsApp and truthful accessibility-missing log.
- Uninstall or disable WhatsApp if possible.
- Verify SMS fallback.
- Deny SMS permission.
- Verify SMS manual fallback.
- Duplicate call within 6 hours.
- Same number after cooldown.
- Unknown or private number if possible.
- Samsung/OEM background behavior.

## Validation

- `.\gradlew.bat test`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat lintDebug`
