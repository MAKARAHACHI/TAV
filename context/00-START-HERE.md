# FollowUp Nadlan - Start Here

You are working on FollowUp Nadlan: an Android native post-call WhatsApp assistant for Israeli real-estate agents.

Before doing anything:

1. Read `.agents/skills/followup-nadlan-product-constitution/SKILL.md`.
2. Read `.agents/skills/followup-nadlan-goal-router/SKILL.md`.
3. Read the current task/PLAN.md if one exists.
4. Do not implement before a clear plan exists unless the user explicitly asked for a tiny edit.

## Product fixed points

- Android native, Kotlin, Jetpack Compose.
- Direct APK MVP, outside Google Play.
- Hebrew-only UI in MVP, full RTL.
- Local-first, Room database.
- WorkManager for Snooze.
- Foreground Service + TelephonyCallback for call-state detection.
- wa.me / ACTION_VIEW only; user presses Send inside WhatsApp.
- Snooze is Core, not nice-to-have.
- No AccessibilityService.
- No backend/API in MVP.
- No WhatsApp auto-send.

## Model workflow

- Complex plans: GPT-5.5 High.
- Simpler plans: GPT-5.4 High.
- Final execution: Codex 5.3 High.

## Working mode

For `/goal`:

1. Use `followup-nadlan-goal-planner`.
2. Create `tasks/<goal-slug>/PLAN.md`.
3. Stop for approval.

For execution:

1. Use `followup-nadlan-goal-executor`.
2. Execute one sprint at a time.
3. Update `tasks/<goal-slug>/EXECUTION_LOG.md`.
4. Run validation.
5. Stop for review.

For completion:

1. Use `followup-nadlan-reviewer`.
2. Write `tasks/<goal-slug>/REVIEW.md`.
3. Do not claim done without evidence.
