# PLAN: Home Quick WhatsApp Number

**Status**: Done
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Medium
**Generated**: 2026-07-02

## Goal Statement
Add a compact editable Home phone field that auto-fills from the latest missed caller when available, allows manual typing/paste, and opens WhatsApp with the saved template as a user-controlled composer only.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This reduces immediate WhatsApp follow-up friction while keeping the user in control and avoiding CRM, auto-send, SMS fallback, schema, and permission expansion.

## Non-Goals
- Do not add CRM fields, lead status, tags, pipeline, contact management, or sales wording.
- Do not change missed-call detection, recipient rules, exclusion rules, cooldown, or post-call reminder behavior.
- Do not enqueue `WhatsAppAutoSendController` or use Accessibility auto-click from the Home button.
- Do not send SMS fallback from this Home button, even if WhatsApp fails.
- Do not mark a message as sent automatically.
- Do not add permissions, manifest entries, backend sync, analytics, or Room migrations.
- Do not refactor unrelated Home/settings/activity UI beyond removing advanced Home explanations if needed for compactness.

## Assumptions
- The target worktree is `F:\followup-accessibility-final` and the active branch is `feature/accessibility-design-compose-final`.
- Existing dirty files in the worktree are inherited baseline work and must be preserved.
- The selected/saved template remains sourced from `TemplateStore` plus `MissedCallAutoResponseSettings.selectedTemplateId`.
- "Keep the edited value while the app is open" means Compose state only; no persistence is required for the manually typed Home number.
- If a newer missed call arrives while the user is not actively editing, the field updates to that caller's phone; if the user is editing/focused, it does not overwrite the draft.
- Logging `MANUAL_WHATSAPP_COMPOSER_OPENED` is acceptable if existing friendly UI maps it as manual WhatsApp opened. If needed, update the friendly label to `WhatsApp נפתח ידנית`.

## Files To Read First
- `context/PROJECT.md`
- `context/ARCHITECTURE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/TEMPLATE_ENGINE.md`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityActions.kt`
- `app/src/main/java/com/followupnadlan/accessibility/LastMissedCaller.kt`
- `app/src/main/java/com/followupnadlan/accessibility/ActivityFeed.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAutoSendController.kt`
- `app/src/main/java/com/followupnadlan/whatsapp/PhoneNumberNormalizer.kt`
- `app/src/main/java/com/followupnadlan/whatsapp/WhatsAppLinkBuilder.kt`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityApp.kt`
- `app/src/main/java/com/followupnadlan/accessibility/AccessibilityActions.kt`
- `app/src/main/java/com/followupnadlan/accessibility/ActivityFeed.kt`
- `app/src/test/java/com/followupnadlan/accessibility/HomeQuickWhatsAppNumberLogicTest.kt` or an equivalent focused unit test file
- `app/src/test/java/com/followupnadlan/accessibility/HomeMessagePreviewLogicTest.kt`
- `app/src/test/java/com/followupnadlan/whatsapp/PhoneNumberNormalizerTest.kt` only if current invalid/manual cases need extra coverage
- `tasks/home-quick-whatsapp-number/EXECUTION_LOG.md`
- `tasks/home-quick-whatsapp-number/REVIEW.md` after execution

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAutoSendController.kt`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAccessibilityService.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt` unless tests expose an existing compile mismatch
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt` unless tests expose an existing compile mismatch
- Room entities, DAO files, and migrations
- Setup wizard, notification helpers, and snooze/reminder workers

## Sprint 1: Editable Home WhatsApp Composer
**Goal**: Replace the static "last missed caller" Home area with an editable compact WhatsApp opener that supports both missed-call auto-fill and manual entry.
**Demo / Validation**: On Home, the user sees `מספר לפתיחת WhatsApp`, can edit/paste a number, and tapping `פתח WhatsApp` opens a `wa.me` composer with the selected template and never sends or falls back to SMS.
**Stop condition**: Stop if implementation requires a new permission, Accessibility enqueue, SMS fallback, Room migration, or CRM/contact-management UI.

### Task 1.1: Add pure Home quick-number state logic
- Location: new small helper near existing Home preview logic or a new focused file under `accessibility`.
- Description: Model when the editable field should auto-fill from `LastMissedCaller.phone`, when manual edits should be preserved, and when a newer missed caller may replace the value.
- Dependencies: `LastMissedCaller`.
- Acceptance criteria: A newer missed caller updates the field only when the user is not actively editing; manual text remains in memory while the app is open.
- Validation command or manual check: `.\gradlew.bat test --tests *HomeQuickWhatsAppNumberLogicTest*`
- Rollback: Remove helper/tests and revert Home to passing `LastMissedCallerCard`.

### Task 1.2: Replace `LastMissedCallerCard` with editable quick card
- Location: `AccessibilityApp.kt`.
- Description: Add a compact Home card titled `מספר לפתיחת WhatsApp` with editable phone field, helper text `מתמלא אוטומטית מהשיחה האחרונה שלא נענתה. אפשר לערוך ידנית.`, and button `פתח WhatsApp`.
- Dependencies: Task 1.1, current `HomeScreen`.
- Acceptance criteria: Field auto-fills from last missed caller, accepts manual typing/paste, and does not introduce CRM/business wording or visible advanced settings.
- Validation command or manual check: Manual Compose inspection plus tests covering text/copy where feasible.
- Rollback: Restore old `LastMissedCallerCard` call and function.

### Task 1.3: Wire composer-only WhatsApp open
- Location: `AccessibilityApp.kt` and possibly `AccessibilityActions.kt`.
- Description: Normalize the field value with `PhoneNumberNormalizer`, build the URL via `WhatsAppLinkBuilder`, open via ACTION_VIEW, and log only composer-open/failure outcomes.
- Dependencies: selected template body from `TemplateStore`, `AccessibilityActions.openWhatsApp`.
- Acceptance criteria: Invalid number shows `בדוק/י שהמספר תקין`; open failure shows `לא הצלחנו לפתוח WhatsApp למספר הזה`; success logs a friendly manual-open outcome without claiming sent.
- Validation command or manual check: Unit tests plus manual tap with valid/invalid numbers.
- Rollback: Remove quick-open handler and restore previous last-caller action.

### Task 1.4: Prove no auto-send, no SMS fallback, no sent marking
- Location: tests around Home action and existing log/action tests.
- Description: Add tests that the Home quick action never calls `enqueuePendingSend`, never opens SMS, never logs `WHATSAPP_AUTO_SENT`, `AUTO_SMS_SENT`, `FALLBACK_SMS_SENT`, or any "sent" friendly label.
- Dependencies: May require extracting a small pure action planner from UI code to make side effects testable.
- Acceptance criteria: Tests demonstrate composer-only behavior and saved custom template use.
- Validation command or manual check: `.\gradlew.bat test`
- Rollback: Remove extracted planner/tests if too invasive and keep a narrower UI-only patch only with explicit approval.

### Task 1.5: Keep Home compact
- Location: `AccessibilityApp.kt`.
- Description: Preserve the order: header/status, message preview, quick WhatsApp number card. Move or omit advanced explanations/settings from Home if they cause normal phone scrolling.
- Dependencies: current Home layout.
- Acceptance criteria: Home does not expose CRM/business wording and remains compact on normal phone dimensions by inspection.
- Validation command or manual check: Manual QA on a normal phone or emulator viewport.
- Rollback: Revert layout changes while keeping logic if layout causes regressions.

## Testing Strategy
- Unit tests:
  - Last missed caller auto-fills Home quick number field.
  - User manual edits are preserved while the app is open.
  - Newer missed caller replaces the field only when not actively editing.
  - Invalid number blocks open and returns `בדוק/י שהמספר תקין`.
  - Valid open uses saved custom template body.
  - Home quick open produces composer-open/failure only and no sent/SMS/Accessibility enqueue outcome.
  - Home wording contains no CRM/business terms such as CRM, ליד, pipeline, סטטוס, מכירות.
- Instrumented tests: Not required unless the current project already has Compose UI test infrastructure; avoid adding heavy new test dependencies for this sprint.
- Manual QA:
  - Launch app with no missed caller and type/paste a number manually.
  - Simulate or log a missed caller, verify field auto-fills.
  - Edit the field, then verify the typed value remains until a newer missed caller arrives while not editing.
  - Tap `פתח WhatsApp` and confirm WhatsApp opens with prepared text and waits for the user to press Send.
  - Try invalid text and confirm validation message.
  - Force/open on a device without WhatsApp handler if possible and confirm failure message.
- Device/OEM checks: One normal Android phone/emulator is enough for this Home-only sprint; no OEM background behavior changes are expected.

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; do not touch manifest.
- User disclosure required: None beyond existing WhatsApp/manual-send wording.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: Manual Home phone value is in-memory Compose state only; no new persisted phone-number store.

## UX Impact
- Screens affected: Home only.
- RTL/Hebrew checks: Hebrew labels must render correctly with phone-number LTR input; keep field readable and compact.
- Empty/fallback states: If no missed caller exists, field starts empty and remains manually usable.
- Error states: Invalid number and WhatsApp open failure use the exact friendly messages requested.

## Rollback Plan
Revert the changes to `AccessibilityApp.kt`, `AccessibilityActions.kt`, any new Home quick-number helper/tests, and task docs. Because there is no schema or permission impact, rollback is code-only.

## Review Checklist
- AndroidManifest is clean.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Snooze restores prepared card.
- Fallback works without READ_CALL_LOG.
- Setup/self-test status is not broken.
- Home quick field does not call `WhatsAppAutoSendController.enqueuePendingSend`.
- Home quick field does not call SMS fallback.
- Home quick field does not mark anything as sent.
- Friendly log says/open-label means manual WhatsApp opened, not sent.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas
- Android background restrictions: Home opens from foreground user tap, so background launch limits should not apply.
- notification permission denial: Does not block manual Home quick open, but can reduce auto-fill events if missed-call logging depends on notification path.
- READ_CALL_LOG denial fallback: Field must still be manually editable and usable without a last missed caller.
- OEM battery killing FGS: May prevent newer missed-call auto-fill events, but manual entry remains the fallback.
- duplicate reminders: No reminder code should change.
- invalid phone formatting for wa.me: Use `PhoneNumberNormalizer.normalizeForWhatsApp` and block null results before opening.
- RTL text and mixed Hebrew/phone-number layout: Use a phone keyboard/type and ensure the phone field does not visually jumble Hebrew labels.
- Room migration risk: None expected; stop if a migration appears necessary.
- direct-APK update/install friction: Not affected by this Home-only change.
