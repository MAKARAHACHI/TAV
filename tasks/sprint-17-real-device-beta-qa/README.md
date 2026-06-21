# Sprint 17: Real Device QA Hardening and Beta Release Candidate

## Scope

Sprint 17 prepares the existing app for controlled real-device beta testing.

The beta promise is:

> When a business owner misses a call, the app responds by WhatsApp first, and falls back to SMS only when WhatsApp is unavailable or fails.

## Changes

- Added a compact readiness status section to the existing missed-call response settings card.
- Hardened Accessibility auto-send logging so a visible WhatsApp screen with no reliable send button or a failed click records failure.
- Added focused JVM tests for safe SMS no-permission fallback and manual WhatsApp log vocabulary.
- Added worktree review, manual QA checklist, and beta release-candidate notes.

## Readiness Status Covered

- Missed-call response active/disabled.
- Primary channel: WhatsApp first or SMS only.
- WhatsApp Messenger installed.
- WhatsApp Business installed.
- Manual WhatsApp prepared reply availability.
- WhatsApp auto-send active/inactive.
- Accessibility service enabled/missing.
- SMS fallback active/off.
- `SEND_SMS` permission granted/missing.
- 6-hour duplicate protection active.

## Test Mode Decision

A developer/test-only fake missed-call trigger was not added.

Reason: the app already has a test notification helper for the post-call card, but Sprint 17 needs QA of the real missed-call response decision pipeline. Adding a production-visible button that fabricates missed-call events would be a new product surface and could accidentally bypass real call-log, cooldown, package, permission, and background behavior. The safer beta approach is the manual checklist in this folder plus real missed calls on a physical device.

## Validation

Required before closeout:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

## Status

- Local code/build validation: recorded in `EXECUTION_LOG.md`.
- Manual real-device QA: not run by Codex; required before beta confidence.
- Commercial readiness: not claimed.
- Google Play readiness: not claimed.
- WhatsApp API integration: not claimed.
