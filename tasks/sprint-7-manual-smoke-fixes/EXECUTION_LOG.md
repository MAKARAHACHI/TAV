# EXECUTION LOG: Sprint 7 Manual Smoke Fixes

## Sprint 7: Manual Smoke Fixes
Status: Completed
Started: 2026-05-19
Completed: 2026-05-19

### Changes made
- `app/src/main/java/com/followupnadlan/templates/TemplateTags.kt`: added the supported tag list with Hebrew display labels and unchanged placeholder keys, plus pure cursor-aware insertion logic.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: changed template tag buttons to display Hebrew labels while inserting internal keys; changed the template editor to use cursor/selection-aware `TextFieldValue`; hoisted WhatsApp send-screen client phone/name state so it survives local screen navigation.
- `app/src/test/java/com/followupnadlan/templates/TemplateTagsTest.kt`: added focused tests for unchanged keys, Hebrew labels, insert-at-cursor, replace-selected-text, and cursor-after-insert behavior.
- `tasks/sprint-7-manual-smoke-fixes/PLAN.md`: created approved Sprint 7 plan.
- `tasks/sprint-7-manual-smoke-fixes/MANUAL_SMOKE_TEST.md`: created real-phone smoke checklist with NOT RUN status.
- `tasks/sprint-7-manual-smoke-fixes/REVIEW.md`: recorded source/build review and remaining real-phone smoke gap.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS after rerun with approved access
- Evidence: Initial sandbox run failed on Gradle wrapper lock access at `C:\Users\danie\.gradle\wrapper\...\gradle-8.9-bin.zip.lck`; approved-access rerun exited successfully.
- Command: `.\gradlew.bat test assembleDebug`
- Result: PASS after rerun with approved access
- Evidence: Initial sandbox run failed on the same Gradle wrapper lock access; approved-access rerun ended with `BUILD SUCCESSFUL in 4s`, `64 actionable tasks: 3 executed, 61 up-to-date`.
- Command: `git diff -- app\src\main\AndroidManifest.xml app\build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties gradle\wrapper`
- Result: PASS
- Evidence: Empty output; no Manifest or Gradle diff.
- Command: `git diff -- app build.gradle.kts settings.gradle.kts gradle.properties | rg -n "^\+.*(backend|database|network|Room|dependencies|CRM|analytics|scheduling|contact-management|contact management|contact database|READ_CONTACTS|uses-permission|BIND_ACCESSIBILITY_SERVICE|SYSTEM_ALERT_WINDOW|QUERY_ALL_PACKAGES|WRITE_CALL_LOG|SEND_SMS|READ_SMS|AccessibilityService|auto-send|autosend)"`
- Result: PASS
- Evidence: No matches. Command exited 1 because `rg` found no forbidden added lines; PowerShell also printed an LF/CRLF working-copy warning for `MainActivity.kt`.

### Manual QA
- Check: Real Android phone smoke
- Result: PASS
- Notes: User reported Sprint 7 manual smoke PASS on 2026-05-19. Verified manually: Hebrew template tag labels, cursor-position tag insertion, selected-text replacement during tag insertion, and preserved WhatsApp send-screen phone/name state after navigating forward/back.

### Deviations from plan
- None.

### Blockers
- None.

### Next recommended action
- Sprint 7 manual-smoke fix scope is closed from the reported real-phone QA evidence.
