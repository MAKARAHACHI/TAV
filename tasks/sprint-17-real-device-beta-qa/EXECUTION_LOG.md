# EXECUTION LOG: Sprint 17 Real Device QA Hardening and Beta Release Candidate

## Sprint 17: Real-device beta QA hardening
Status: Completed
Started: 2026-06-21
Completed: 2026-06-21

### Changes made
- `tasks/sprint-17-real-device-beta-qa/WORKTREE_REVIEW.md`: documented current dirty tree and sprint file ownership.
- `app/src/main/java/com/followupnadlan/MainActivity.kt`: added minimal Hebrew readiness status inside the existing missed-call response settings card.
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAccessibilityService.kt`: logs failed Accessibility auto-send when a visible WhatsApp window has no reliable send button or the click fails.
- `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt`: added regression coverage for safe skip when SMS permission/manual fallback are unavailable and no auto-click attempt without user-enabled automation.
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`: added regression coverage that manual WhatsApp reply labels never claim sent.
- `tasks/sprint-17-real-device-beta-qa/README.md`: documented scope, readiness status, and skipped fake missed-call test trigger.
- `tasks/sprint-17-real-device-beta-qa/MANUAL_QA_CHECKLIST.md`: added real-device beta QA checklist.
- `tasks/sprint-17-real-device-beta-qa/BETA_RC.md`: added controlled beta release-candidate note.

### Validation run
- Command: `.\gradlew.bat test`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 12s`; 51 actionable tasks, 14 executed, 37 up-to-date.

- Command: `.\gradlew.bat assembleDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 3s`; debug APK assembled.

- Command: `.\gradlew.bat lintDebug`
- Result: PASS
- Evidence: `BUILD SUCCESSFUL in 11s`; lint report written to `app/build/reports/lint-results-debug.html`.

### Manual QA
- Check: Real missed-call WhatsApp/SMS matrix
- Result: NOT RUN
- Notes: Requires physical Android device, SIM, WhatsApp install states, permission toggles, and OEM/background checks.

### Deviations from plan
- Developer/test fake missed-call trigger was skipped. A production-visible manual trigger would be a new feature and would risk bypassing the real call-log/package/permission/cooldown path that beta QA must prove.

### Blockers
- None for local code validation.

### Next recommended action
- Perform real-device manual QA before beta distribution.
