# EXECUTION LOG: Sprint 18A Debug-only Missed Call Simulator for Real Device QA

## Sprint 18A: Debug-only missed-call simulator
Status: Completed
Started: 2026-06-21
Completed: 2026-06-21

### Changes made
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallCandidate.kt`: added a shared missed-call candidate event shape used by both real and debug-triggered flows.
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt`: refactored the handler to accept a supplied candidate event while preserving the existing decision, cooldown, WhatsApp, SMS, and fallback logic.
- `app/src/main/java/com/followupnadlan/missedcall/DebugMissedCallSimulator.kt`: added a clearly named debug-only simulator guard around synthetic missed-call creation with `source=debug_missed_call_simulator`.
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAutoSendController.kt`: preserved the event `source` through delayed Accessibility auto-send success/failure logs.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: added a debug-only Hebrew QA section with test-number input and trigger button, visible only when `BuildConfig.DEBUG` is true.
- `app/src/test/java/com/followupnadlan/missedcall/DebugMissedCallSimulatorTest.kt`: added JVM coverage for delegation, blank-number rejection, release guard, and duplicate cooldown behavior.
- `app/build.gradle.kts`: enabled generated `BuildConfig` so the debug-only guard can use `BuildConfig.DEBUG` explicitly.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL`; debug and release unit-test compilation passed, including the new simulator tests.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL`; debug APK assembled.

- Command: `.\gradlew.bat lintDebug`
- Result: PASS
- Evidence: lint reports generated at `app/build/reports/lint-results-debug.html`, `app/build/reports/lint-results-debug.txt`, and `app/build/reports/lint-results-debug.xml`.

### Manual QA
- Check: Trigger the debug missed-call simulator on a real debug build and verify the same missed-call response flow opens WhatsApp or falls back according to current settings.
- Result: NOT RUN
- Notes: Requires a physical Android device and app installation from a debug APK.

### Deviations from plan
- No separate PLAN.md was created because the current `/goal` request included explicit implementation acceptance criteria and explicit validation commands, matching the existing Sprint 16 execution pattern.
- `app/build.gradle.kts` was updated to enable generated `BuildConfig`, which was required to satisfy the explicit `BuildConfig.DEBUG` guard requirement.

### Blockers
- None.

### Next recommended action
- Review
