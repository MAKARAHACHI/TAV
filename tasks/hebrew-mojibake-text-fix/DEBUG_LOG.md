# DEBUG LOG: Hebrew Mojibake Text Fix

## Symptom
App UI text showed gibberish such as `×‘×™×ª` instead of Hebrew.

## Expected behavior
FollowUp Nadlan MVP UI remains Hebrew-only and RTL.

## Reproduction steps
Open the accessibility app after the recent missed-call reliability sprint and inspect Home, Settings, templates, and recipient screens.

## Evidence collected
- `AccessibilityApp.kt` contained 92 corrupted lines with UTF-8 Hebrew decoded as Windows-1252 and then saved back as UTF-8.
- Repository-wide Kotlin scan for `×|Â|â|Ö¾` found the corruption in `AccessibilityApp.kt`.
- Other checked Kotlin files already contained valid Hebrew strings.

## Hypotheses
1. Runtime locale/font issue - rejected because the corrupted bytes were literal source text.
2. Source encoding mojibake - confirmed by strings such as `×‘×™×ª`, `×”×’×“×¨×•×ª`, and `×©×œ×— ×‘Ö¾WhatsApp` in source.

## Root cause
During prior edits, `AccessibilityApp.kt` Hebrew strings were transcoded incorrectly: UTF-8 Hebrew bytes were interpreted as Windows-1252 text.

## Minimal fix
Mechanically recovered only corrupted lines in `AccessibilityApp.kt` back to UTF-8 Hebrew. No UI behavior, permissions, sending logic, or detector logic was changed.

## Validation
- Mojibake scan: no `×`, `Â`, `â`, or `Ö¾` markers remain in Kotlin sources/tests.
- `.\gradlew.bat test`: PASS
- `.\gradlew.bat assembleDebug`: PASS
- `.\gradlew.bat lintDebug`: PASS
- `git diff --check`: PASS with CRLF warnings only

## Regression risk
Low. The change is text recovery in one Compose file. Risk is limited to any string missed by the marker scan; current scan is clean.
