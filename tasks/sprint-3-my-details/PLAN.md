# PLAN: Sprint 3 My Details Screen

**Status**: Draft
**Planning model**: GPT-5.5 High
**Execution model**: Codex 5.3 High
**Layer**: Core
**Risk**: Low
**Generated**: 2026-05-19

## Goal Statement
Build the first MVP version of a local-only "My Details" settings screen where the agent can save reusable profile fields for later message/template insertion.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- Core post-call + template + snooze loop preserved: Yes
- User-controlled WhatsApp send preserved: Yes
- Accessibility avoided: Yes
- Backend/API avoided for MVP: Yes
- Fallback mode preserved: Yes

Decision: Proceed
Reason: This sprint adds local agent profile settings that support the manual follow-up flow without permissions, backend/API work, CRM scope, contact reading, call-log reading, WhatsApp detection, automation, or new distribution assumptions.

## Non-Goals
- Do not add CRM, lead management, properties, exports, logs, history, analytics, campaigns, or dashboards.
- Do not add backend/API, cloud sync, accounts, remote storage, Firestore, Supabase, or CRM integration.
- Do not read contacts, call logs, SMS, notifications, WhatsApp state, or installed packages.
- Do not add call detection, post-call engine work, Snooze, reminders, WorkManager, Foreground Service, or notification behavior.
- Do not add templates, block composer, placeholder rendering, or automatic insertion into the WhatsApp message in this sprint.
- Do not add WhatsApp detection, WhatsApp Business detection, or any auto-send behavior.
- Do not add Android permissions or manifest package queries.
- Do not mark manual smoke PASS unless tested on a real Android phone.

## Assumptions
- This is a plan-only step; implementation must wait for explicit human approval.
- The first MVP version can use `SharedPreferences` because the requested fields are small local settings and the current app has no persistence dependency.
- `business_card` will be stored as free text or a link/string in Sprint 3, not as an image picker, file upload, vCard generator, QR code, or attachment workflow.
- The screen can be reached by a simple in-app toggle or button from the existing manual composer; full navigation architecture is not required yet.
- Labels can be simple and RTL-friendly. Exact final Hebrew copy can be adjusted during implementation without changing scope.

## Files To Read First
- `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- `context/PROJECT.md`
- `context/ARCHITECTURE.md`
- `context/DATA_CONTRACTS.md`
- `context/ROADMAP.md`
- `context/PERMISSIONS_AND_PRIVACY.md`
- `context/DO_NOT_BUILD.md`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `tasks/sprint-2-manual-contact-send/PLAN.md`
- `tasks/sprint-2-manual-contact-send/REVIEW.md`

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `tasks/sprint-3-my-details/PLAN.md`
- `tasks/sprint-3-my-details/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-3-my-details/EXECUTION_LOG.md`
- `tasks/sprint-3-my-details/REVIEW.md`

## Files That Must Not Change
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/*`
- `context/*`
- Sprint 1 and Sprint 2 task folders
- Any Room, WorkManager, call-state, contact-reading, backend/API, CRM, property, history/log, export, Snooze, notification, or WhatsApp detection files.

## Sprint 1: Local My Details Settings
**Goal**: Add one simple local settings screen for saving the agent's reusable profile fields.
**Demo / Validation**: The agent can open "My Details", edit `agent_name`, `office_name`, `phone`, `website`, `business_card`, and `signature`, tap Save, leave the screen, reopen it, and see the saved values still present after app restart.
**Stop condition**: Stop if the implementation requires new permissions, dependencies, backend/API, contact/call-log reads, CRM/property/log/history/export scope, Snooze, WhatsApp detection, or automatic message sending.

### Task 1.1: Add Local Profile Model
- Location: `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`
- Description: Define a small Kotlin data model with exactly the requested persisted fields: `agent_name`, `office_name`, `phone`, `website`, `business_card`, and `signature`. Kotlin property names may use camelCase if the storage keys preserve the requested field names.
- Dependencies: Kotlin only.
- Acceptance criteria: Model contains only the Sprint 3 fields; no lead, CRM, contact, property, template, export, history, Snooze, or WhatsApp state is added.
- Validation command or manual check: Source review of the model file.
- Rollback: Delete `MyDetailsProfile.kt`.

### Task 1.2: Add Local Settings Store
- Location: `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- Description: Persist the profile locally using `SharedPreferences` with stable keys for `agent_name`, `office_name`, `phone`, `website`, `business_card`, and `signature`.
- Dependencies: Android framework `SharedPreferences`.
- Acceptance criteria: Save and load work locally on-device; no network, backend/API, account, cloud sync, file picker, contact read, or permission is used.
- Validation command or manual check: Source review confirms local-only persistence and stable keys.
- Rollback: Delete `MyDetailsStore.kt` and remove calls from `MainActivity.kt`.

### Task 1.3: Add Simple My Details Screen UI
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a simple Compose screen with six editable fields and a Save action. Keep layout RTL-friendly and readable on a phone. Use multi-line input for `signature` and optionally for `business_card` if needed.
- Dependencies: Existing Compose Material3 setup.
- Acceptance criteria: All six requested fields are visible and editable; Save gives clear feedback; empty fields are allowed unless implementation discovers a strong reason for minimal validation; no field reads contacts or device profile data.
- Validation command or manual check: Real-phone visual check and source review.
- Rollback: Remove the My Details UI composable and restore the prior manual composer entry point.

### Task 1.4: Add Minimal Entry Point From Existing App
- Location: `app/src/main/java/com/followupnadlan/MainActivity.kt`
- Description: Add a simple way to switch between the existing manual composer and the My Details screen, such as two top buttons or a lightweight local screen state.
- Dependencies: Existing single-activity Compose app.
- Acceptance criteria: Existing manual WhatsApp/share/copy composer remains usable; reset behavior and Sprint 2B actions remain unchanged; My Details can be opened and closed without app restart.
- Validation command or manual check: Manual check on emulator or device plus source review of Sprint 2B actions.
- Rollback: Remove the entry point and screen-state branch.

### Task 1.5: Preserve Permissions, Manifest, And Scope Boundaries
- Location: Whole repo source/build files.
- Description: Verify that no permissions, package queries, dependencies, backend/API code, contact/call-log reads, templates, Snooze, properties, logs/history, exports, or WhatsApp detection were added.
- Dependencies: Completed code changes.
- Acceptance criteria: `AndroidManifest.xml` and Gradle files remain unchanged; forbidden-scope grep is clean except for task docs.
- Validation command or manual check: Run the forbidden-scope grep listed in Testing Strategy.
- Rollback: Revert any out-of-scope file or code before review.

### Task 1.6: Update Sprint 3 Evidence Docs
- Location: `tasks/sprint-3-my-details/`
- Description: Update `EXECUTION_LOG.md`, `MANUAL_SMOKE_TEST.md`, and `REVIEW.md` after implementation and validation.
- Dependencies: Completed implementation and validation evidence.
- Acceptance criteria: Execution log records exact changed files and commands. Manual smoke remains NOT RUN unless a real Android phone was used. Review states PASS, PASS WITH NOTES, or FIX_REQUIRED based on evidence.
- Validation command or manual check: Read the docs after editing and confirm they do not claim unperformed phone QA.
- Rollback: Revert only incorrect Sprint 3 doc updates.

## Testing Strategy
- Unit tests: Run `.\gradlew.bat test assembleDebug`.
- Instrumented tests: Not required for the first local settings UI unless implementation creates enough persistence logic to justify adding Android instrumented tests.
- Manual QA: Required on a real Android phone before marking `MANUAL_SMOKE_TEST.md` as PASS.
- Device/OEM checks: Minimum one real Android phone for this sprint; broader Pixel/Samsung/Xiaomi QA remains later.
- Persistence check: Save all six fields, navigate away, return, restart the app, and confirm values remain.
- Regression check: Existing manual composer still opens, validates, shares, copies, and resets as before.
- Forbidden-scope grep:

```powershell
rg -n "uses-permission|READ_CALL_LOG|READ_CONTACTS|READ_PHONE_STATE|POST_NOTIFICATIONS|FOREGROUND_SERVICE|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|WRITE_CALL_LOG|SEND_SMS|READ_SMS|WorkManager|Room|CallLog|TelephonyCallback|NotificationListenerService|AccessibilityService|auto-send|autosend|CRM|backend|server|export|history|snooze|property|properties" app build.gradle.kts settings.gradle.kts gradle.properties
```

## Permission Impact
- Added permissions: None.
- Removed permissions: None.
- Manifest risk: None expected; `AndroidManifest.xml` must not change.
- User disclosure required: None for Sprint 3 because no sensitive permission or remote data flow is added.

## Data/Schema Impact
- Room entities: None in Sprint 3.
- migrations: None.
- local data retention: Six profile fields are stored locally until the user edits or clears them.
- Storage mechanism: `SharedPreferences` for this sprint, unless the human explicitly approves adding a DataStore dependency.
- Field keys: `agent_name`, `office_name`, `phone`, `website`, `business_card`, `signature`.

## UX Impact
- Screens affected: Existing manual composer entry point plus new My Details settings screen.
- RTL/Hebrew checks: Field labels and buttons must be readable in RTL, including mixed phone/URL text.
- Empty/fallback states: Empty values are allowed and should reload as empty strings, not crash.
- Error states: Save should not crash on long text, Hebrew text, phone numbers, URLs, or pasted business-card text.

## Rollback Plan
Revert `MainActivity.kt`, delete `app/src/main/java/com/followupnadlan/profile/MyDetailsProfile.kt`, delete `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`, and update only the Sprint 3 task docs to record the rollback. Do not touch manifest, Gradle files, context docs, Sprint 1 docs, or Sprint 2 docs.

## Review Checklist
- AndroidManifest is clean.
- Gradle files are unchanged.
- No new permissions or package queries.
- No AccessibilityService.
- No auto-send WhatsApp.
- wa.me / ACTION_VIEW remains user-driven.
- Existing manual composer still works.
- Local storage only; no backend/API/cloud sync.
- No CRM, contact reading, call log reading, templates, properties, log/history, export, Snooze, or WhatsApp detection.
- My Details saves and reloads all six requested fields.
- Manual smoke is not marked PASS without real-phone evidence.

## Agent Handoff
- Planning model: GPT-5.5 High.
- Execution model: Codex 5.3 High.
- Required first read: `.agents/skills/followup-nadlan-product-constitution/SKILL.md`
- Execution mode: one sprint only, after explicit human approval.
- Expansion rule: no scope expansion without human approval.
- Approval gate: Stop here until the human approves this `PLAN.md`.

## Potential Gotchas
- Android background restrictions: Not touched; do not add background work.
- notification permission denial: Not touched; do not add notification behavior.
- READ_CALL_LOG denial fallback: Existing manual composer must keep working without call permissions.
- OEM battery killing FGS: Not touched; do not add Foreground Service work.
- duplicate reminders: Not touched; do not add reminders or Snooze.
- invalid phone formatting for wa.me: My Details `phone` can be plain saved text in Sprint 3; do not wire it into WhatsApp behavior yet unless explicitly approved.
- RTL text and mixed Hebrew/phone-number layout: Phone, website, and business-card text may be LTR inside RTL fields; check visual readability on a real phone.
- Room migration risk: Avoided in Sprint 3 by using `SharedPreferences`.
- direct-APK update/install friction: Not touched; no distribution changes.
