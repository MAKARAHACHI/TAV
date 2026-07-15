# PLAN: Accessibility UX Polish Final

**Status**: Done
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: High
**Generated**: 2026-07-02

## Goal Statement
Make the "אני זמין/ה בכתב" missed-call bridge feel compact, trustworthy, and user-controlled by fixing Home, message persistence, exclusions/contact picking, friendly tracking, and manual-mode safety.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: No, this branch already contains `WhatsAppAccessibilityService`; this plan does not add or expand it and must harden manual mode so it never triggers.
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed with constrained remediation
Reason: The user explicitly scoped this branch to the accessibility UX final pass. The sprint must not add AI, CRM, backend, analytics, caller ID scraping, recording, transcription, new sending channels, overlay UI, or new permissions. Pre-existing `SEND_SMS`, `READ_CONTACTS`, and `WhatsAppAccessibilityService` are branch facts, not new plan additions.

## Current Repo State
- Worktree: `F:\followup-accessibility-final`
- Branch: `feature/accessibility-design-compose-final`
- Existing dirty task: `tasks/custom-message-persistence-home-shortcut/`
- Existing dirty files already address part of custom message persistence and Home preview.
- Do not revert, reset, clean, or overwrite the existing dirty work.

## Non-Goals
- No CRM, leads pipeline, sales automation, monitoring, backend, analytics, AI, recording, transcription, caller-ID scraping, or new channels.
- No in-call overlay or floating UI.
- No new Android permissions.
- No automatic WhatsApp send or Accessibility auto-click in manual mode.
- No silent SMS fallback in manual mode.
- No Play Store, billing, cloud, or external service changes.
- No broad rewrite of `MainActivity.kt` or unrelated Sprint 15-20 flows.

## Assumptions
- "שאל אותי לפני שליחה" means the missed-call handler may only show a user-facing prompt/notification after the missed call ends.
- In manual mode, tapping `פתח WhatsApp` or `שלח ב־WhatsApp` opens a prepared composer only; the user still presses Send in WhatsApp.
- In manual mode, tapping `שלח ב־SMS` opens a composer/manual flow only; no direct `SmsManager.sendTextMessage` call.
- Contact picker uses the existing manifest `READ_CONTACTS` permission but requests it only when the user chooses "add from contacts".
- The existing custom-message persistence work is preserved and extended, not duplicated.

## Files To Read First
- `tasks/custom-message-persistence-home-shortcut/PLAN.md`
- `tasks/custom-message-persistence-home-shortcut/EXECUTION_LOG.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/accessibility/ActivityFeed.kt`
- `app/src/main/java/com/followupnadlan/accessibility/ExclusionsStore.kt`
- `app/src/main/java/com/followupnadlan/accessibility/RecipientScopeSettings.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseSettings.kt`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppReplySender.kt`
- `app/src/main/java/com/followupnadlan/missedcall/SmsSender.kt`
- `app/src/main/java/com/followupnadlan/notifications/MissedCallManualReplyNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/postcall/ContactNameResolver.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/accessibility/ActivityFeed.kt`
- `app/src/main/java/com/followupnadlan/accessibility/ExclusionsStore.kt`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityActions.kt`
- new `app/src/main/java/com/followupnadlan/accessibility/ContactsPicker.kt` or similarly narrow helper
- new `app/src/main/java/com/followupnadlan/accessibility/LastMissedCaller.kt` or similarly narrow helper
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpActionType.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`
- `app/src/main/java/com/followupnadlan/notifications/MissedCallManualReplyNotificationHelper.kt`
- existing/new focused unit tests under `app/src/test/java/com/followupnadlan/accessibility/`
- existing/new focused unit tests under `app/src/test/java/com/followupnadlan/missedcall/`
- existing/new focused unit tests under `app/src/test/java/com/followupnadlan/followuplog/`
- `tasks/accessibility-ux-polish-final/EXECUTION_LOG.md`
- `tasks/accessibility-ux-polish-final/REVIEW.md` after execution review

## Files That Must Not Change
- Gradle dependency files unless a missing existing test dependency blocks the sprint.
- `app/src/main/AndroidManifest.xml` unless the executor first stops and gets explicit approval.
- Room database schema unless explicitly approved.
- Snooze/reminder internals unless a compile break requires a narrow import or type fix.
- `MainActivity.kt` unless it is only needed to route existing app entry state.

## Sprint 1: Manual-Mode Safety and Prompt Routing
**Goal**: Make "שאל אותי לפני שליחה" proveably non-automatic.
**Demo / Validation**: Unit tests show manual mode produces prompt/manual actions only and never calls WhatsApp auto-send, Accessibility auto-click, or direct SMS send.
**Stop condition**: Any need to add a permission, background launch an Activity, or expand Accessibility automation.

### Task 1.1: Split manual prompt from automatic send decisions
- Location: `MissedCallAutoResponseDecision.kt`
- Description: Add or repurpose a decision outcome so `PREPARED_MANUAL` routes to a prompt/notification instead of `OPEN_PREPARED_WHATSAPP` from the missed-call handler.
- Dependencies: Existing `MissedCallManualReplyNotificationHelper` or a new narrow helper with WhatsApp/SMS/cancel actions.
- Acceptance criteria: Manual mode never returns `ATTEMPT_WHATSAPP_AUTO_SEND`, `OPEN_PREPARED_WHATSAPP`, or `SEND_AUTOMATIC_SMS` from the automatic missed-call path.
- Validation command or manual check: `.\gradlew.bat app:testDebugUnitTest --tests com.followupnadlan.missedcall.MissedCallAutoResponseDecisionTest`
- Rollback: Restore previous decision enum and handler routing.

### Task 1.2: Make the handler show only a user prompt in manual mode
- Location: `MissedCallAutoResponseHandler.kt`, notification helper
- Description: After a missed call, show notification/prompt actions `שלח ב־WhatsApp`, `שלח ב־SMS`, `בטל`; do not open WhatsApp/SMS automatically.
- Dependencies: Existing notification and `MissedCallPromptScreen` route.
- Acceptance criteria: Handler calls no `WhatsAppReplySender.openPreparedReply`, no `WhatsAppAutoSendController.enqueuePendingSend`, and no `SmsSender.send` for manual-mode decisions.
- Validation command or manual check: Focused handler test or pure routing test.
- Rollback: Revert handler switch changes and helper additions.

### Task 1.3: Log manual prompt outcomes clearly
- Location: `FollowUpActionType.kt`, `ActivityFeed.kt`, prompt actions
- Description: Log `ממתין לאישור שלך`, `המשתמש ביטל`, and manual composer opens without claiming a message was sent.
- Dependencies: Existing append-only log format tolerates new enum labels.
- Acceptance criteria: Activity screen never shows "נשלחה" for a manual composer open.
- Validation command or manual check: Activity feed tests.
- Rollback: Remove new enum values and mappings.

## Sprint 2: Compact Home and Last Missed Caller
**Goal**: Fit primary Home content on one normal phone screen with no required scrolling.
**Demo / Validation**: Home contains title, short explanation, visible bridge button, compact message preview, and last missed caller card when available.
**Stop condition**: If Compose UI cannot fit without hiding required content, stop and reduce Home scope rather than adding more cards.

### Task 2.1: Reduce Home to the required content
- Location: `AccessibilityApp.kt`
- Description: Remove Home clutter, collapse status chips to one compact row, move advanced controls to Settings, and place the bridge active/off button above fold.
- Dependencies: Existing `HomeMessagePreviewLogic`.
- Acceptance criteria: No CRM/business wording, no extra explanations, button visible without scrolling on a normal phone viewport.
- Validation command or manual check: Manual QA on emulator/device; build validation.
- Rollback: Restore previous `HomeScreen`.

### Task 2.2: Add latest missed caller model
- Location: new `LastMissedCaller.kt`, `ActivityFeed.kt` or log helper
- Description: Derive latest missed caller from local logs and expose phone, optional contact name, and status.
- Dependencies: Existing `FollowUpLogStore`; optional contact name resolution only if permission exists.
- Acceptance criteria: Status maps to `נשלחה הודעה`, `לא נשלח`, or `ממתין לאישור`; no raw enum labels.
- Validation command or manual check: Unit tests for status derivation.
- Rollback: Remove helper and Home card usage.

### Task 2.3: Add Home last-caller WhatsApp button
- Location: `AccessibilityApp.kt`, `AccessibilityActions.kt`
- Description: Add card `השיחה האחרונה שלא נענתה` with phone/contact, status, and `פתח WhatsApp`.
- Dependencies: Current saved template from `TemplateStore`.
- Acceptance criteria: Button opens WhatsApp composer only with current saved template; it does not auto-send or mark sent automatically.
- Validation command or manual check: Unit test for prepared-message source plus manual QA.
- Rollback: Remove card and action callback.

## Sprint 3: Exclusions Manager and Contact Picker
**Goal**: Let the user manage people/numbers that should never receive automatic replies.
**Demo / Validation**: User can add manual number, add from contacts with search, see exclusions, remove exclusions, and excluded numbers block WhatsApp and SMS.
**Stop condition**: If contact access requires requesting permission before the user chooses contact picking.

### Task 3.1: Persist exclusions immediately and refresh UI
- Location: `ExclusionsStore.kt`, `AccessibilityApp.kt`
- Description: Save on add/remove or keep explicit save with reliable dirty-state handling; avoid losing edits when navigating away.
- Dependencies: Existing `ExclusionsCodec`.
- Acceptance criteria: Added/removed exclusions persist after leaving and reopening the screen.
- Validation command or manual check: Store/codec tests.
- Rollback: Revert UI state/persistence changes.

### Task 3.2: Add contact picker with permission-on-demand
- Location: new contact picker helper and `AccessibilityApp.kt`
- Description: Request `READ_CONTACTS` only after tapping add-from-contacts; show friendly denied/no-permission message when unavailable.
- Dependencies: Existing manifest already includes `READ_CONTACTS`.
- Acceptance criteria: Denied permission does not crash and does not block manual number entry.
- Validation command or manual check: Pure filtering tests plus manual permission-denied QA.
- Rollback: Remove picker modal/helper.

### Task 3.3: Implement search by contact name or number
- Location: contact picker helper/test
- Description: Query contacts into a simple local list and filter by case-insensitive name or digit-normalized phone.
- Dependencies: Android ContactsContract for runtime path; pure filter function for unit tests.
- Acceptance criteria: Search matches Hebrew/Latin names and phone digits; empty result shows friendly message.
- Validation command or manual check: `ContactsPickerSearchTest`.
- Rollback: Remove search helper.

### Task 3.4: Confirm exclusions block all send/open paths
- Location: `MissedCallAutoResponseDecision.kt`, `MissedCallAutoResponseHandler.kt`
- Description: Ensure `SKIP_EXCLUDED` prevents WhatsApp open, Accessibility enqueue, automatic SMS, and manual fallback prompt.
- Dependencies: Existing decision order already checks `excluded` before channel routing.
- Acceptance criteria: Tests prove excluded number blocks WhatsApp and SMS and logs `לא נשלח — בחרת לא לשלוח למספר הזה`.
- Validation command or manual check: Decision and ActivityFeed tests.
- Rollback: Restore previous exclusion handling.

## Sprint 4: Activity Tracking and Clear History
**Goal**: Make Activity answer "מה קרה עם השיחות שלא עניתי להן?" in friendly Hebrew.
**Demo / Validation**: Rows show only friendly outcomes and clear history immediately empties the UI/store.
**Stop condition**: If tracking requires schema migration or broader history redesign.

### Task 4.1: Complete friendly log mappings
- Location: `ActivityFeed.kt`, `FollowUpActionType.kt`
- Description: Map required rows: missed call detected, WhatsApp sent/opened, SMS fallback, pending approval, user cancelled, excluded, WhatsApp failed, SMS failed.
- Dependencies: Existing append-only log storage.
- Acceptance criteria: No raw labels, underscores, source ids, or internal debug actions in Activity.
- Validation command or manual check: `ActivityFeedTest`.
- Rollback: Revert mapping additions.

### Task 4.2: Clear history refreshes UI
- Location: `AccessibilityApp.kt`, `FollowUpLogStore.kt`
- Description: Keep log rows as Compose state or refresh key so `מחק היסטוריה` empties Activity immediately after clear.
- Dependencies: Existing `FollowUpLogStore.clear()`.
- Acceptance criteria: Store is empty and Activity UI shows empty state after clear without app restart.
- Validation command or manual check: Unit/store test plus manual QA.
- Rollback: Revert state refresh wiring.

## Sprint 5: Message Persistence Completion
**Goal**: Ensure the existing custom-message fix fully satisfies persistence and send-path usage.
**Demo / Validation**: Editing `גלוי`, `עדין`, or `פרטי` persists across screen navigation and app restart, and missed-call WhatsApp/SMS paths use the saved text.
**Stop condition**: If fixing persistence requires migrating Room schema or deleting user-edited data.

### Task 5.1: Preserve existing custom-message work
- Location: current dirty files from `tasks/custom-message-persistence-home-shortcut/`
- Description: Keep the committed SharedPreferences writes, migration exact matching, Home preview logic, and send resolver changes.
- Dependencies: Existing task log says validation passed.
- Acceptance criteria: No regression to migration overwrite behavior.
- Validation command or manual check: Existing template tests.
- Rollback: Restore only if a failing test proves a regression.

### Task 5.2: Add final integration coverage for selected template usage
- Location: `MissedCallMessageResolverTest.kt`, `HomeMessagePreviewLogicTest.kt`
- Description: Confirm Home preview and missed-call send path use the selected saved template body.
- Dependencies: Existing pure logic helpers.
- Acceptance criteria: Tests cover all three accessibility templates where practical.
- Validation command or manual check: Focused unit tests.
- Rollback: Remove added tests and related helper changes.

## Testing Strategy
- Unit tests:
  - manual mode never auto-sends WhatsApp/SMS
  - auto mode sends only after missed-call rules pass
  - edited message persists and is used by send path
  - Home preview uses saved custom message
  - clear history clears UI/store helper state
  - excluded contact/number blocks WhatsApp and SMS
  - contacts picker search filters by name/number
  - contacts permission denied fails gracefully through pure UI state
  - last caller WhatsApp button opens composer only, no auto-send marker
  - friendly logs do not expose internal labels
- Instrumented tests: Not required unless runtime contact query cannot be reasonably isolated behind a pure mapper/filter.
- Manual QA:
  - Edit each message, leave and return, force-close/reopen, verify text remains.
  - Trigger manual-mode missed-call flow and confirm only prompt/notification appears.
  - Tap WhatsApp from Home last-caller card and confirm composer opens with current template only.
  - Add manual exclusion; add contact exclusion; remove exclusion.
  - Deny contacts permission and verify friendly message.
  - Clear history and confirm Activity empty state without restart.
- Device/OEM checks:
  - Basic real-device missed-call smoke recommended after build because notification and contact permission flows are runtime behavior.

## Permission Impact
- Added permissions: None.
- Removed permissions: None in this plan.
- Manifest risk: High because this branch already declares `READ_CONTACTS`, `SEND_SMS`, and an Accessibility service. Executor must not add or expand permissions/services without approval.
- User disclosure required: Contacts picker must explain contact access only when user chooses contact picking. Manual mode must clearly state that sending requires user action.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: Existing SharedPreferences for templates, settings, exclusions, and log store only.
- migration risk: Template migration must not overwrite user-edited accessibility messages.

## UX Impact
- Screens affected: Home, Settings, Exclusions, contact picker, Activity, missed-call prompt/notification.
- RTL/Hebrew checks: Required for mixed Hebrew and phone numbers on Home, Activity, and contact picker rows.
- Empty/fallback states: no last caller, no contacts permission, empty contacts search, empty exclusions, empty Activity.
- Error states: WhatsApp open failed, SMS composer failed, contacts permission denied, excluded recipient.

## Rollback Plan
Revert changes from `tasks/accessibility-ux-polish-final/` and the touched Kotlin/test files. Do not revert the pre-existing dirty `custom-message-persistence-home-shortcut` changes unless explicitly asked. No schema rollback should be required.

## Review Checklist
- AndroidManifest is clean relative to plan: no new permissions or services.
- Existing AccessibilityService was not expanded.
- Manual mode has zero automatic WhatsApp send, SMS send, Accessibility click, or silent fallback.
- WhatsApp composer open is user-driven.
- SMS in manual mode opens a user action path only.
- Excluded number blocks WhatsApp and SMS.
- Contacts permission requested only from contact picking.
- Home primary content fits without required scrolling.
- Friendly logs hide internal labels.
- Snooze and core post-call fallback flows are not broken.
- No AI/recording/transcription/caller-ID scraping/CRM/backend/analytics added.

## Required Validation Commands
- `.\gradlew.bat test`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat lintDebug`
- `git diff --check`
- `git status --short --branch`

## Final Report Requirements
- root cause fixes
- Home layout changes
- message persistence fix
- exclusions/contact picker implementation
- clear history result
- manual-mode safety proof
- files changed
- validation results
- manual QA steps

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas
- Android background restrictions: missed-call flow must use notification/prompt, not direct background Activity launch.
- notification permission denial: manual prompt may not be visible if notifications are denied; fallback UI must remain honest.
- READ_CALL_LOG denial fallback: no-number state must not crash or send.
- OEM battery killing FGS: real-device post-call smoke can differ from unit tests.
- duplicate reminders: do not touch snooze/reminder scheduling in this sprint.
- invalid phone formatting for wa.me: Home and prompt WhatsApp actions must normalize and fail gracefully.
- RTL text and mixed Hebrew/phone-number layout: use compact rows and avoid clipping phone numbers.
- Room migration risk: avoid Room changes entirely.
- direct-APK update/install friction: not in scope.
- Existing branch risk: Accessibility service and SEND_SMS are already present, so tests must specifically prove manual mode cannot reach those automatic paths.
