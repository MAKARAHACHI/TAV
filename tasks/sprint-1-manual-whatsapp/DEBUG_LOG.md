# DEBUG LOG: Sprint 1 Reset Message Button

## Symptom
On a real Android phone, the Sprint 1 manual WhatsApp screen works except the reset message button.

## Expected behavior
After a user selects a template and edits the message, tapping the reset message button restores the message field to the currently selected template default. The phone number and selected template remain unchanged, and the wa.me preview updates from the same message state.

## Reproduction steps
1. Open the app on Android.
2. Select a Hebrew template.
3. Edit the message field.
4. Tap the reset message button.
5. Observe whether the message returns to the selected template default and the wa.me preview changes.

## Evidence collected
- Inspected `app/src/main/java/com/followupnadlan/MainActivity.kt`.
- The message field is controlled by Compose `message` state.
- The wa.me preview is derived from `message`.
- The WhatsApp open action builds its link from `message`.
- No Android permissions or manifest changes were needed.

## Hypotheses
1. Reset updates a different value than the editable message state - not confirmed; source showed reset wrote to `message`.
2. Reset default source can drift from template selection default - mitigated by routing initial value, template selection, and reset through `defaultMessageFor(selectedTemplate)`.
3. The reset button target is unstable or cramped on device - plausible for a two-button row without explicit width constraints; mitigated by making the action row full-width and assigning equal button weights.

## Root cause
Best current explanation: the reset action was functionally close, but the control path was implicit and the button layout had no stable width constraints on a real device. The fix makes reset use the same selected-template default helper as template selection and gives both action buttons stable equal-width touch targets.

## Minimal fix
- Added `defaultMessageFor(template)` as the single source for Sprint 1 template default message text.
- Updated initial message state, template selection, and reset to use that helper.
- Made the action row full-width with equal button weights.

## Validation
- `.\gradlew.bat test assembleDebug`: PASS, `BUILD SUCCESSFUL in 4s`, 64 actionable tasks.
- Permission/scope grep for forbidden permissions and Sprint 2 surfaces: PASS, no matches.
- 2026-05-17 real Android phone smoke test: PASS; reset message button now works correctly and no auto-send behavior exists.

## Regression risk
Low. The change is limited to `MainActivity.kt`, does not alter phone state, selected template state, link generation, manifest permissions, dependencies, or WhatsApp opening behavior.
