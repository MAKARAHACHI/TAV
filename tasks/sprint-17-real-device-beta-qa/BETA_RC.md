# Beta Release Candidate Note

## What Is Ready

- WhatsApp-first missed-call decision logic exists.
- Manual WhatsApp prepared reply mode exists.
- Optional Accessibility auto-send mode exists from Sprint 16 and remains opt-in.
- SMS fallback exists from Sprint 15 and remains permission-gated.
- Duplicate cooldown defaults to 6 hours.
- Readiness status now exposes the main beta prerequisites in the app.
- Local JVM tests, debug build, and lint are required before closeout.

## What Still Requires Real-Device QA

- Real missed-call detection on target Android versions.
- WhatsApp Messenger open behavior.
- WhatsApp Business open behavior.
- Accessibility send-button detection and click behavior.
- SMS fallback with active SIM and real permission states.
- Permission-denied behavior.
- Duplicate cooldown on the same normalized number.
- Private/unknown number behavior.
- Screen-locked, app-swiped-away, and OEM battery optimization behavior.

## Known Risks

This build uses on-device Android automation for WhatsApp. WhatsApp UI changes, Android version differences, OEM background restrictions, and Accessibility settings may affect reliability. This is intended for controlled beta/private APK testing, not broad commercial distribution yet.

Additional risks:

- Some devices may delay or restrict call-log updates after a missed call.
- Notifications may not appear if notification permission is denied.
- SMS fallback depends on SIM, carrier behavior, and Android SMS permission.
- Accessibility services require explicit user approval and may be disabled by OEM/security settings.
- WhatsApp and WhatsApp Business package behavior can differ by installed version.

## Install Requirements

- Android device for controlled private APK testing.
- Active SIM for SMS fallback QA.
- WhatsApp Messenger and/or WhatsApp Business for WhatsApp path QA.
- Notification permission enabled for fallback/manual notifications.
- Battery/OEM settings reviewed where needed.

## Permissions Required For Full Beta Coverage

- Phone state / call log permissions for missed-call detection.
- Notification permission for fallback/status notifications.
- SMS permission only for automatic SMS fallback.
- Accessibility service only for optional WhatsApp auto-send mode.

## Release Position

- Controlled beta/private APK only: Yes.
- Google Play ready: No.
- Broad commercial distribution ready: No.
- WhatsApp Business API integration: No.
- Backend/CRM/cloud dependency: No.
