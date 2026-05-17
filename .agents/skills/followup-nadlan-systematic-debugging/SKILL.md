---
name: followup-nadlan-systematic-debugging
description: Use when FollowUp Nadlan has a bug, failing build, failing test, post-call detection issue, OEM/background issue, permission issue, Snooze issue, or WhatsApp opening issue. Debug systematically before patching.
---

# FollowUp Nadlan Systematic Debugging

This skill adapts systematic-debugging to the FollowUp Nadlan Android product.
It prevents random fixes and scope creep.

## Debugging principle

Do not patch before you understand the failure.
Do not introduce new product behavior while fixing a bug.
Do not add permissions to make a bug disappear unless the plan explicitly allows it.

## Required debug log

Create or update:

```text
tasks/<goal-slug>/DEBUG_LOG.md
```

Use this format:

```md
# DEBUG LOG: <Issue Name>

## Symptom
What the user sees.

## Expected behavior
What should happen according to the constitution/PLAN.md.

## Reproduction steps
Exact steps, device, Android version, app version.

## Evidence collected
- logs
- screenshots if available
- test output
- relevant files inspected

## Hypotheses
1. <hypothesis> - evidence for/against
2. <hypothesis> - evidence for/against

## Root cause
Confirmed root cause or current best explanation.

## Minimal fix
Smallest code change required.

## Validation
How the fix was verified.

## Regression risk
What could break.
```

## Required investigation order

### 1. Reproduce

Define exact steps.
If device-specific, identify OEM and Android version.

### 2. Classify the failure

Choose one primary class:

- build/compile
- runtime crash
- permission denied
- call-state not detected
- notification not shown
- notification click not opening card
- READ_CALL_LOG number missing
- fallback card broken
- wa.me/WhatsApp opening issue
- template/block issue
- snooze scheduling issue
- WorkManager execution issue
- Room/schema issue
- OEM battery/background issue
- update/license issue

### 3. Inspect the right layer

Do not jump to UI if the bug is a service bug.
Do not jump to permissions if the bug is phone formatting.

Recommended layers:

- AndroidManifest and permissions
- runtime permission request state
- Foreground Service lifecycle
- TelephonyCallback registration/unregistration
- CallLog query behavior
- notification channel and POST_NOTIFICATIONS state
- PendingIntent and Activity launch path
- Room persistence
- WorkManager scheduling constraints
- wa.me URI building and phone normalization
- Compose state and navigation

### 4. Form hypotheses

List at least two hypotheses unless the root cause is obvious from logs.
For each, gather evidence before changing code.

### 5. Apply minimal fix

Fix only the root cause.
Do not add features during debugging.

### 6. Validate

Run the smallest validation that proves the fix.
Then run broader regression checks if the touched area is core.

## Product-specific debug gotchas

### Post-call detection

Common failures:

- READ_PHONE_STATE not granted.
- service not running.
- FGS type mismatch.
- OEM killed service.
- callback not registered after reboot.
- notification permission denied.
- testing with VoIP/WhatsApp calls instead of cellular calls.

### Number missing

Common failures:

- READ_CALL_LOG denied.
- CallLog query timing too early.
- unknown/private number.
- dual SIM behavior.
- phone normalization issue for Israeli numbers.

Fallback must still work.

### Snooze

Common failures:

- WorkManager constraints too strict.
- reminder payload not persisted.
- notification channel disabled.
- duplicate worker names not unique or not replaced correctly.
- timezone/date handling.

### wa.me

Common failures:

- number contains leading zero instead of country code.
- spaces/dashes not stripped.
- message not URI encoded.
- WhatsApp not installed.
- WhatsApp Business package not considered.

## Stop conditions

Stop and request planning/review if the fix requires:

- new dangerous permission
- AccessibilityService
- backend/API
- large architecture change
- removing fallback
- changing product scope
