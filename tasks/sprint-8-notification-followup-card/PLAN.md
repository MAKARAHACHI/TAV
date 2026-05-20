# PLAN: Sprint 8 Notification Follow-Up Pop-Out Card

Status: Human-approved by current `/goal` request
Date: 2026-05-19

## Goal
Add a narrow notification-based entry point for the existing FollowUp Nadlan send flow. The notification is a smoke sprint: it proves a Hebrew notification/card can be triggered, tapped, and routed back into the existing manual WhatsApp composer without adding call detection, CRM, scheduling, backend, database, or WhatsApp automation scope.

## Product Fit
- Reduces immediate follow-up friction for a busy real-estate agent.
- Keeps WhatsApp sending user-driven.
- Uses a notification tap to open the app; no background Activity launch.
- Keeps the MVP local-first and simple.
- Falls back cleanly when notification permission is denied or phone/name are missing.

## Non-Goals
- No backend, server, database, Room, analytics, CRM, scheduling engine, contact-management, or networking.
- No automatic WhatsApp sending.
- No `SYSTEM_ALERT_WINDOW`.
- No full-screen notification takeover.
- No real call-state listener in this sprint.
- No redesign of My Details or the main send flow.

## Files To Read First
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `app/src/main/java/com/followupnadlan/profile/MyDetailsStore.kt`
- `app/src/main/java/com/followupnadlan/templates/TemplateStore.kt`

## Allowed Changes
- `app/src/main/java/com/followupnadlan/MainActivity.kt`
- `app/src/main/java/com/followupnadlan/notifications/FollowUpNotificationHelper.kt`
- `app/src/main/AndroidManifest.xml` for `POST_NOTIFICATIONS`
- `tasks/sprint-8-notification-followup-card/*`

## Implementation Plan
1. Add a small `FollowUpNotificationHelper`.
   - Create a notification channel for Android 8+.
   - Build a Hebrew notification with title `להוציא פולואפ?`.
   - Use a stable request code and immutable/update PendingIntent flags.
   - Put current phone, lead name, and selected template id into the PendingIntent extras.
2. Add a manual smoke-test trigger in the existing send screen.
   - Keep the trigger small and clearly removable later.
   - Do not add call detection in this sprint.
3. Add Android 13+ permission handling.
   - Request `POST_NOTIFICATIONS` only when needed.
   - If denied, show a clear local status and keep the manual send screen usable.
4. Route notification taps to the existing composer.
   - Restore phone/name/template id from extras where available.
   - If values are missing, open the composer with empty fields and no crash.
5. Validate.
   - `.\gradlew.bat test`
   - `.\gradlew.bat assembleDebug`
   - Manifest/Gradle diff review
   - Forbidden-scope grep

## Stop Condition
If real call detection requires risky permissions, foreground service work, broad architecture, or anything beyond this notification smoke path, do not implement it. Document call detection as deferred.

## Acceptance Criteria
- Gradle unit tests pass.
- `assembleDebug` passes.
- Notification permission path does not crash.
- Manual trigger can show a Hebrew notification.
- Tapping the notification opens the existing follow-up/send screen.
- Phone/name/template are restored where available.
- Missing phone/name opens cleanly with empty fields.
- WhatsApp send/share/copy flow remains unchanged.
- My Details remains unchanged except for reused profile/template data.
- Manifest/Gradle diff is minimal and explained.
- No forbidden app-code scope is introduced.
