# PLAN: Sprint 2B Manual Share Fallback

**Status**: Draft
**Planning model**: GPT-5.4 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Low
**Generated**: 2026-05-19

## Goal Statement
Make manual recovery explicit and reliable when `wa.me` opens WhatsApp but WhatsApp itself cannot open the requested chat.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This keeps the existing user-driven WhatsApp path and adds explicit user-controlled recovery actions without permissions, automation, backend work, contact-reading, or CRM scope.

## Non-Goals
- Do not detect whether a phone number exists in WhatsApp.
- Do not auto-send or automate WhatsApp UI.
- Do not add AccessibilityService, package-wide queries, SMS, contacts, call log, or notification permissions.
- Do not add CRM, lead management, backend/API, cloud sync, Snooze, or post-call detection work.
- Do not change Gradle dependencies unless a validation blocker proves it is strictly necessary.
- Do not mark manual smoke as PASS without fresh real Android phone evidence.

## Assumptions
- Sprint 2B continues the existing uncommitted `tasks/sprint-2-manual-contact-send/` work.
- The existing `wa.me` `ACTION_VIEW` path remains the primary WhatsApp flow.
- The current hidden `ACTION_SEND` fallback should become a visible secondary action, because Android cannot catch WhatsApp-internal invalid-recipient errors.
- The copy action should copy only the composed message text, not the phone number or `wa.me` link.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `context/00-START-HERE.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/ROADMAP.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `tasks/sprint-2-manual-contact-send/EXECUTION_LOG.md`
- `tasks/sprint-2-manual-contact-send/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-2-manual-contact-send/REVIEW.md`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `tasks/sprint-2-manual-contact-send/EXECUTION_LOG.md`
- `tasks/sprint-2-manual-contact-send/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-2-manual-contact-send/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- Any Room, WorkManager, call-state, contact-reading, backend/API, CRM, or post-call engine files.

## Sprint 1: Explicit Manual Recovery
**Goal**: Keep WhatsApp as the primary action while exposing manual share and copy as visible recovery options.
**Demo / Validation**: The screen shows `פתח WhatsApp`, `שיתוף ידני`, and `העתק הודעה`; WhatsApp still opens through `wa.me`; manual share opens an Android text chooser without a valid WhatsApp number; copy places the message text on the clipboard and shows confirmation.
**Stop condition**: Stop if the implementation requires a new permission, hidden WhatsApp existence detection, package scraping, auto-send behavior, or dependency changes.

### Task 1.1: Restore Primary WhatsApp Action
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Keep the primary button label exactly `פתח WhatsApp` and use the existing `wa.me` `ACTION_VIEW` path. It may validate that phone and message are present before building the link, but it must not fall back silently to share for WhatsApp-internal recipient errors.
- Dependencies: Existing `PhoneNumberNormalizer` and `WhatsAppLinkBuilder`.
- Acceptance criteria: Valid input starts an `ACTION_VIEW` intent with the built `wa.me` link; user must press Send inside WhatsApp; no auto-send exists.
- Validation command or manual check: `git diff -- app/src/main/java/com/followupnadlan/MainActivity.kt`; source review confirms `ACTION_VIEW` remains the primary path.
- Rollback: Revert only the primary-button branch in `MainActivity.kt`.

### Task 1.2: Add Visible Manual Share Button
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a visible secondary button labeled exactly `שיתוף ידני`. The action uses `Intent.ACTION_SEND`, `type = "text/plain"`, and `Intent.EXTRA_TEXT` with only the composed message text. Use `Intent.createChooser` where possible.
- Dependencies: Existing Compose screen state and composed `message`.
- Acceptance criteria: Manual share does not require a valid WhatsApp number; empty message is blocked with clear visible validation; the chooser/share sheet opens if a capable app exists; no new permissions or package queries are added.
- Validation command or manual check: `git diff -- app/src/main/java/com/followupnadlan/MainActivity.kt`; real-phone smoke confirms the share sheet opens after WhatsApp shows an internal invalid-recipient error.
- Rollback: Remove only the manual share button and helper while keeping the primary WhatsApp path.

### Task 1.3: Add Or Keep Visible Copy Button
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add or keep a visible button labeled exactly `העתק הודעה`. It copies only the composed message text to the Android clipboard.
- Dependencies: Android clipboard service or Compose clipboard API already available from the Android framework.
- Acceptance criteria: Copy works without phone validation; empty message is blocked or produces a clear message; after copy, the UI shows a clear confirmation.
- Validation command or manual check: `git diff -- app/src/main/java/com/followupnadlan/MainActivity.kt`; real-phone smoke pastes the copied text into another app.
- Rollback: Remove only the copy button/helper.

### Task 1.4: Preserve Layout And Existing Manual Composer Behavior
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Keep template selection, editable message, reset behavior, generated `wa.me` preview, Hebrew RTL, and field validation intact. Adjust the action row/layout only as needed so the three actions are visible and usable on a phone.
- Dependencies: Existing Compose Material3 controls.
- Acceptance criteria: Buttons do not overlap; text remains readable; reset still restores the selected template default text; phone field is not cleared by reset.
- Validation command or manual check: `./gradlew.bat test assembleDebug`; real-phone visual check.
- Rollback: Revert only the action layout changes.

### Task 1.5: Update Sprint 2B Evidence Docs
- Location: `tasks/sprint-2-manual-contact-send/`
- Description: Update `MANUAL_SMOKE_TEST.md`, `EXECUTION_LOG.md`, and `REVIEW.md` for Sprint 2B.
- Dependencies: Code changes and validation output.
- Acceptance criteria: Manual smoke checklist includes valid WhatsApp number, invalid/non-WhatsApp number with possible WhatsApp error, manual share opening the share sheet, and copy button copying the message. Execution log records commands and source/build evidence. Review records PASS/FAIL with manual QA truthfully marked NOT RUN unless real-phone testing occurred.
- Validation command or manual check: Read each doc after editing; confirm no false PASS is claimed.
- Rollback: Revert only the Sprint 2B doc sections.

## Testing Strategy
- Unit tests: Run `./gradlew.bat test assembleDebug`.
- Instrumented tests: Not required for this narrow UI intent change.
- Manual QA: Required on a real Android phone before marking manual smoke PASS.
- Device/OEM checks: At minimum, validate on one real Android phone for button visibility, WhatsApp open, share chooser, and clipboard paste.

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; `AndroidManifest.xml` must not change.
- User disclosure required: None for this sprint.

## Data/Schema Impact
- Room entities: None.
- migrations: None.
- local data retention: None.

## UX Impact
- Screens affected: Manual WhatsApp composer only.
- RTL/Hebrew checks: Three action labels must fit and remain readable: `פתח WhatsApp`, `שיתוף ידני`, `העתק הודעה`.
- Empty/fallback states: Manual share and copy should handle empty message with clear feedback; primary WhatsApp should keep phone/message validation.
- Error states: If WhatsApp shows an internal invalid-number error, the app does not detect it; the user returns and chooses `שיתוף ידני` or `העתק הודעה`.

## Rollback Plan
Revert the Sprint 2B hunks in `MainActivity.kt` and the Sprint 2B doc sections in `tasks/sprint-2-manual-contact-send/`. Do not touch unrelated dirty files, Gradle files, manifest, or Sprint 1 artifacts.

## Review Checklist
- AndroidManifest is clean.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Manual share uses ACTION_SEND text/plain with only composed message text.
- Copy uses only composed message text and shows confirmation.
- Manual share does not require a valid WhatsApp number.
- Snooze restores prepared card: Not applicable, and not changed.
- Fallback works without READ_CALL_LOG: Manual composer remains usable without call permissions.
- Setup/self-test status is not broken.
- Manual smoke is not marked PASS without real-phone evidence.

## Agent Handoff
- Planning model: GPT-5.5 High for complex tasks, GPT-5.4 High for simple tasks.
- Execution model: Codex 5.3 High.
- Required first read: .agents/skills/followup-nadlan-product-constitution/SKILL.md
- Execution mode: one sprint at a time.
- Expansion rule: no scope expansion without human approval.

## Potential Gotchas
- Android background restrictions: Not directly touched; do not add background starts.
- notification permission denial: Not directly touched; do not add notification behavior.
- READ_CALL_LOG denial fallback: Manual composer must remain usable without call permissions.
- OEM battery killing FGS: Not directly touched; do not add Foreground Service work.
- duplicate reminders: Not directly touched; do not add reminders.
- invalid phone formatting for wa.me: Primary WhatsApp path can still validate formatting, but must not try to prove the number exists in WhatsApp.
- RTL text and mixed Hebrew/phone-number layout: Three action buttons may need wrapping or vertical stacking on small phones.
- Room migration risk: None; do not add persistence.
- direct-APK update/install friction: Not directly touched; no distribution changes.
