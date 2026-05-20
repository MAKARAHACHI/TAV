# PLAN: Sprint 7 Manual Smoke Fixes

**Status**: Human-approved by current `/goal`
**Generated**: 2026-05-19
**Layer**: Usability repair
**Risk**: Low

## Goal Statement
Fix only the three Sprint 6 manual-smoke UX issues: Hebrew tag labels, cursor-aware tag insertion, and preserved WhatsApp send-screen name/phone state when moving between local screens.

## Product Guardrail Check
- FollowUp Nadlan constitution read: Yes
- User-controlled WhatsApp send preserved: Yes
- Local-first MVP preserved: Yes
- Backend/API avoided: Yes
- Manifest and Gradle changes avoided: Yes
- CRM/contact-management scope avoided: Yes

Decision: Proceed.

## Non-Goals
- Do not modify Manifest, Gradle, dependencies, backend, database, Room, migrations, network behavior, analytics, scheduling, CRM, contact management, contact reading, or WhatsApp automation.
- Do not add new supported placeholder keys.
- Do not change tag rendering semantics.
- Do not claim manual phone smoke PASS without real Android phone evidence.

## Files Expected To Change
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTags.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagsTest.kt`
- `tasks/sprint-7-manual-smoke-fixes/PLAN.md`
- `tasks/sprint-7-manual-smoke-fixes/MANUAL_SMOKE_TEST.md`
- `tasks/sprint-7-manual-smoke-fixes/EXECUTION_LOG.md`
- `tasks/sprint-7-manual-smoke-fixes/REVIEW.md`

## Sprint 7 Tasks
1. Replace visible tag button text with Hebrew labels while keeping internal placeholder keys unchanged.
2. Insert tags at the current template-editor cursor or replace the selected range, then move the cursor after the inserted tag.
3. Preserve local WhatsApp send-screen client phone/name state when navigating away and back.
4. Add focused unit coverage for supported tag keys, Hebrew labels, and insertion behavior.
5. Validate with `.\gradlew.bat test assembleDebug`, Manifest/Gradle diff review, forbidden-scope grep, and truthful manual smoke status.

## Testing Strategy
- Unit tests: supported tag keys unchanged, Hebrew labels present, insertion at cursor, selected range replacement, cursor after insertion.
- Regression: existing renderer tests continue to prove tag rendering behavior is unchanged.
- Build: `.\gradlew.bat test assembleDebug`.
- Manual phone smoke: NOT RUN unless tested on a real Android phone.

## Rollback Plan
Revert the Sprint 7 app/test files and remove this task folder. Do not touch prior sprint task folders, Manifest, Gradle, context files, or local template persistence.
