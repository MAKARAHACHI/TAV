# Codex Goal

## Current first goal

Create the context system only.

Do not implement app code until the human reviews and approves the context folder.

## Build goal after context approval

When approved, implement the app layer by layer according to `context/ROADMAP.md`.

Recommended first implementation prompt:

```txt
/goal
You are building FollowUp Nadlan, a native Android Kotlin + Jetpack Compose app.
Read context/NEW_CHAT_BOOTSTRAP.md and .agents/skills/followup-nadlan-vision-guardian/SKILL.md first.
Implement only Layer 1 from context/ROADMAP.md: Manual WhatsApp Screen + minimal Template Engine + wa.me opening.
Do not implement call detection, call log, snooze, leads, activation, or external update yet.
Preserve Hebrew RTL UX.
After coding, run the available build/syntax checks and report what changed.
Update context docs only if behavior/contracts changed.
```

## Full product target

The final MVP should support:

1. Manual composer.
2. Agent profile.
3. Template engine.
4. Block composer.
5. Follow-Up Card.
6. Snooze reminders.
7. Local leads.
8. Post-call detection.
9. CallLog last-number reading.
10. Fallback mode.
11. Setup wizard.
12. Self-test.
13. External APK activation and update.

## Implementation rules

- Small phases.
- Validate after each phase.
- Do not skip context guardrails.
- Do not add non-MVP features.
- Prefer working simple code over platform complexity.
- Keep sensitive permissions optional/degradable.
