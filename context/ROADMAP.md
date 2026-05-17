# Roadmap

## Phase 0: Context system

Status: active until human confirms context folder is correct.

Tasks:

- Create `context/` docs.
- Create vision guardian skill.
- Do not implement app code.

Exit criteria:

- Human approves context files.
- Next build phase is clear.

## Layer 1: Manual WhatsApp Screen + minimal Template Engine

Goal:

Prove the core action without sensitive permissions.

Build:

- Native Android project setup.
- Hebrew RTL app shell.
- Manual phone input.
- Message text area.
- Basic template selector.
- Open WhatsApp via `wa.me`.
- Error when no WhatsApp package/browser can handle intent.

Do not build yet:

- Call detection.
- Call log reading.
- Snooze.
- Full leads module.

Validation:

- User can type `972501234567` and open WhatsApp with a prepared message.
- Hebrew RTL display is acceptable.
- Message is URL encoded correctly.

## Layer 2: Agent Profile + 12 Templates + Block Composer

Goal:

Make the manual composer useful for real estate.

Build:

- Agent profile fields.
- Persistent signature.
- 12 default templates.
- Placeholder rendering.
- Quick block composer.
- Template edit/save.

Validation:

- `{agent_name}`, `{website}`, `{signature}`, `{lead_name}`, `{property_link}` render correctly.
- User can build a message without typing from scratch.

## Layer 3: Local Leads + Follow-Up Card + Snooze

Goal:

Turn the app from a sender into a lead-saving follow-up tool.

Build:

- Follow-Up Card UI.
- Save as lead.
- Lead status fields.
- Snooze options: 15 min, 30 min, 1 hour, evening, tomorrow morning.
- WorkManager reminders.
- Reminder notification opens same card.

Validation:

- Snoozed task returns with same phone/message/template.
- User can dismiss, open WhatsApp, or snooze again.

## Layer 4: Post-Call Engine

Goal:

Create the post-call magic.

Build:

- Foreground Service.
- TelephonyCallback listener.
- Detect call ended.
- Query last CallLog entry if `READ_CALL_LOG` granted.
- Create FollowUpTask from latest call.
- Post notification.
- Fallback mode without number.
- Call duration threshold setting.

Validation:

- Real call test on Pixel/Samsung/Xiaomi.
- Notification appears after call ends.
- Last number is correct when permission exists.
- No crash when permission denied.

## Layer 5: Setup Wizard + OEM Battery Fix + Self-Test

Goal:

Reduce support problems before marketing.

Build:

- Permission wizard.
- Agent profile step.
- Samsung/Xiaomi battery/autostart guidance.
- Self-test flow.
- Troubleshooting screen.

Validation:

- New user can configure permissions step by step.
- User can run test and know whether post-call detection works.

## Layer 6: External Distribution + Activation + Update Checker

Goal:

Prepare sellable external APK.

Build:

- Signed release build pipeline.
- 7-day trial.
- Activation code entry.
- Local validation.
- Version checker against JSON.
- Privacy screen.
- Landing-page download instructions.

Validation:

- Fresh install -> trial active.
- Expired trial -> activation required.
- Valid code unlocks.
- Update banner appears when remote JSON is higher.

## QA phase

Minimum real-device test matrix:

- Pixel 7 or equivalent stock Android.
- Samsung Galaxy S23 / S24 or similar One UI 6/7.
- Xiaomi Redmi Note 12 or similar MIUI/HyperOS.
- Optional: OnePlus/Realme/Vivo if available.

Scenarios:

- Incoming call answered and ended.
- Outgoing call ended.
- Missed call.
- Short call below threshold.
- Permission denied.
- Notifications disabled.
- Battery optimization active.
- WhatsApp missing.
- WhatsApp Business installed.
- Contact permission granted/denied.

## Later roadmap

- Russian localization.
- Online activation.
- Cloud sync.
- CRM webhooks.
- Template sharing.
- Analytics.
- Property mini-cards.
- Play Store-compatible fallback version.

## Stop conditions

Stop and rethink if:

- Post-call detection is unreliable on most target devices.
- Users refuse external APK installation.
- Users do not understand why permissions are needed.
- Agents prefer CRM integration over standalone app.
