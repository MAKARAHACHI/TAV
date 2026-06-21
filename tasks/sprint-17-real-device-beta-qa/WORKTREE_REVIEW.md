# Sprint 17 Worktree Review

Generated: 2026-06-21

## Command Run

```powershell
git status --short --branch
git diff --name-status
```

## Current Branch State

`main...origin/main`

The worktree is dirty. Sprint 14, Sprint 15, and Sprint 16 changes are still uncommitted relative to `origin/main`; Sprint 17 must preserve them and avoid reverting unrelated files.

## Files Modified By Sprint 14

Evidence: `tasks/sprint-14-lead-pipeline/PLAN.md`, Sprint 14 execution/review docs, current package ownership, and current diff.

- `app/src/main/java/com/followupnadlan/data/followup/FollowUpTaskDao.kt`
- `app/src/main/java/com/followupnadlan/data/lead/LeadDao.kt`
- `app/src/main/java/com/followupnadlan/pipeline/LeadPipeline.kt` (untracked)
- `app/src/test/java/com/followupnadlan/pipeline/LeadPipelineTest.kt` (untracked)
- `app/src/main/java/com/followupnadlan/snooze/ReminderWorker.kt`
- `context/DATA_CONTRACTS.md`
- `context/FOLLOW_UP_CARD.md`
- `context/SNOOZE_REMINDERS.md`
- `tasks/sprint-14-lead-pipeline/` (untracked task folder)

Likely shared with later sprints:

- `app/src/main/java/com/followupnadlan/MainActivity.kt`

## Files Modified By Sprint 15

Evidence: `tasks/sprint-15-missed-call-auto-response/PLAN.md`, `EXECUTION_LOG.md`, `REVIEW.md`, and current diff.

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallDetectionService.kt`
- `app/src/main/java/com/followupnadlan/postcall/CallStateMonitor.kt`
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- `app/src/main/java/com/followupnadlan/setup/SetupStatus.kt`
- `app/src/main/java/com/followupnadlan/templates/SprintOneTemplates.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTagRenderer.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateTags.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseSettings.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallCooldownStore.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/ManualSmsReplyActivity.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/SmsSender.kt` (untracked)
- `app/src/main/java/com/followupnadlan/notifications/MissedCallManualReplyNotificationHelper.kt` (untracked)
- `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt` (untracked)
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`
- `app/src/test/java/com/followupnadlan/postcall/CallStateMonitorTest.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateStoreTest.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagRendererTest.kt`
- `app/src/test/java/com/followupnadlan/templates/TemplateTagsTest.kt`
- `context/POST_CALL_ENGINE.md`
- `tasks/sprint-15-missed-call-auto-response/` (untracked task folder)

## Files Modified By Sprint 16

Evidence: `tasks/sprint-16-whatsapp-first-missed-call-response/README.md`, `EXECUTION_LOG.md`, `REVIEW.md`, and current package ownership.

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/whatsapp_auto_send_accessibility_service.xml` (untracked)
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/setup/SelfTestChecker.kt`
- `app/src/main/java/com/followupnadlan/setup/SetupStatus.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogEntry.kt`
- `app/src/main/java/com/followupnadlan/followuplog/FollowUpLogStore.kt`
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecision.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/MissedCallAutoResponseHandler.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAccessibilityService.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAutoSendController.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppPackageResolver.kt` (untracked)
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppReplySender.kt` (untracked)
- `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt` (untracked)
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`
- `tasks/sprint-16-whatsapp-first-missed-call-response/` (untracked task folder)

## Files Touched In Sprint 17

- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/missedcall/WhatsAppAccessibilityService.kt`
- `app/src/test/java/com/followupnadlan/missedcall/MissedCallAutoResponseDecisionTest.kt`
- `app/src/test/java/com/followupnadlan/followuplog/FollowUpLogStoreTest.kt`
- `tasks/sprint-17-real-device-beta-qa/PLAN.md`
- `tasks/sprint-17-real-device-beta-qa/WORKTREE_REVIEW.md`
- `tasks/sprint-17-real-device-beta-qa/README.md`
- `tasks/sprint-17-real-device-beta-qa/MANUAL_QA_CHECKLIST.md`
- `tasks/sprint-17-real-device-beta-qa/BETA_RC.md`
- `tasks/sprint-17-real-device-beta-qa/EXECUTION_LOG.md`
- `tasks/sprint-17-real-device-beta-qa/REVIEW.md`

## Untracked Files That Should Be Committed Later

These appear to be legitimate sprint artifacts and should be committed with their owning sprint after review:

- `app/src/main/java/com/followupnadlan/missedcall/`
- `app/src/main/java/com/followupnadlan/notifications/MissedCallManualReplyNotificationHelper.kt`
- `app/src/main/java/com/followupnadlan/pipeline/`
- `app/src/main/res/xml/`
- `app/src/test/java/com/followupnadlan/missedcall/`
- `app/src/test/java/com/followupnadlan/pipeline/`
- `tasks/sprint-14-lead-pipeline/`
- `tasks/sprint-15-missed-call-auto-response/`
- `tasks/sprint-16-whatsapp-first-missed-call-response/`
- `tasks/sprint-17-real-device-beta-qa/`

## Suspicious Or Unrelated Files

- No unrelated app/code file was identified outside the Sprint 14-17 scope.
- `AndroidManifest.xml` is high-risk because it contains the Sprint 15 `SEND_SMS` exception and Sprint 16 Accessibility service, but those match prior sprint docs and were not introduced by Sprint 17.
- PowerShell reported line-ending warnings for modified files. This is not treated as suspicious by itself.
