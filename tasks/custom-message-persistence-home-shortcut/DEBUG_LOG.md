# DEBUG LOG: Custom Message Persistence

## Symptom
Editing the accessibility default message appears to save, but leaving/reopening the message screen or relaunching can show the previous/default body.

## Expected behavior
The selected template's custom body must override the built-in body for UI preview, message editing, WhatsApp preparation, and SMS fallback.

## Reproduction steps
1. Open the default message screen.
2. Select one of the accessibility templates.
3. Edit the body and save.
4. Leave and reopen the screen or app.
5. Confirm the edited body remains selected and is used by send flows.

## Evidence collected
- `TemplatesScreen` loaded templates with `remember(templateStore) { templateStore.loadTemplates() }`.
- Saving wrote to `TemplateStore`, but the remembered template list did not refresh in the active composition.
- `TemplateStore.saveTemplate` used `SharedPreferences.apply()`.
- Runtime missed-call handling already resolves the selected template through `TemplateStore.loadTemplates()` before both WhatsApp and SMS branches.

## Hypotheses
1. Stale Compose template state causes saved edits to be hidden by the old in-memory template list - supported by `remember(templateStore)` around `loadTemplates()`.
2. Asynchronous SharedPreferences writes can lose the newest body if the app process is closed immediately after save - possible because `apply()` is asynchronous.
3. Legacy migration can remove user edits that happen to match legacy marker snippets - possible because old matching used broad contains markers.

## Root cause
Confirmed: template saving did not update the active template state in `AccessibilityApp`, and the core message body write used asynchronous persistence. Migration matching was also broader than the product rule requires.

## Minimal fix
Keep templates as top-level Compose state, refresh that state after saves, use committed writes for the core saved template body/selected template, and tighten legacy default matching.

## Validation
- `.\gradlew.bat test`: PASS
- `.\gradlew.bat assembleDebug`: PASS
- `.\gradlew.bat lintDebug`: PASS
- `git diff --check`: PASS with CRLF normalization warnings only
- Source review: WhatsApp and SMS branches still consume the single resolved `message` from `TemplateStore`.

## Regression risk
Low to medium. Template storage and Home UI are core, but no schema, permissions, call detection, recipient rules, or send actions are changed.
