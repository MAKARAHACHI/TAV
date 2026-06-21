# DEBUG LOG: Sprint 18A Debug Missed-Call WhatsApp Test Misreads as Broken

## Symptom
The debug-only "בדיקת שיחה שלא נענתה" flow reports that the test event was triggered, but on device the user can see no WhatsApp action and assumes the unanswered-call test is broken.

Current device report after that fix: WhatsApp opens with the prepared message, but the automatic send path does not press Send.

## Expected behavior
The debug simulator should still go through the real missed-call pipeline, but the QA surface should report the truthful expected outcome so a blocked path is distinguishable from a broken trigger.

## Reproduction steps
1. Install a debug build.
2. Open the manual screen and use the debug missed-call simulator.
3. Trigger the same phone number twice, or trigger while the feature is disabled / template missing.
4. Observe that the UI reports a generic success message even when the pipeline immediately skips the action.

## Evidence collected
- `MainActivity.kt` always mapped `TRIGGERED` to a generic "event sent" status.
- `MissedCallAutoResponseHandler.kt` already contained multiple legitimate skip paths: disabled, duplicate cooldown, no template, no number, no permission.
- The simulator correctly delegates to the existing handler; the issue is the missing truthful QA feedback.
- `WhatsAppAccessibilityService.kt` marked the pending auto-send as failed immediately when `rootInActiveWindow` or the send button was not yet ready on the first Accessibility event.
- The same service only accepted a directly clickable send node, so it was less tolerant when the labeled node and the clickable ancestor were different nodes.

## Hypotheses
1. The simulator callback is not firing.
Evidence against: `DebugMissedCallSimulator` constructs a candidate and calls the supplied handler callback directly.
2. The real missed-call pipeline is firing, but a gate like cooldown or disabled feature is stopping WhatsApp.
Evidence for: `MissedCallAutoResponseDecision` contains those gates, and the debug UI was not surfacing them.
3. The Accessibility auto-send service clears the pending action too early before the WhatsApp compose screen is fully ready.
Evidence for: the service previously called `markFailed` on the first event where the root or send button was unavailable.

## Root cause
There were two issues:
1. The debug UI treated "candidate accepted by simulator" as "WhatsApp test worked", even though the real pipeline could immediately skip or reroute the event.
2. In the auto-send mode, the Accessibility service could fail too early during WhatsApp screen loading, clearing the pending send before the send button became clickable.

## Minimal fix
1. Add a no-side-effect preview in `MissedCallAutoResponseHandler` and map the predicted pipeline action to a truthful debug-only status message before/while triggering the real simulator event.
2. Keep the pending auto-send alive during a short grace window while WhatsApp is loading, then fail only after that grace window, and tolerate a clickable ancestor around the labeled send node.

## Validation
- Added unit coverage for the new debug status formatter.
- Added unit coverage for the auto-send grace-window decision.
- Build/test validation run after patching.

## Regression risk
Low to medium. The debug-only messaging change is low risk. The Accessibility auto-send change only affects the existing opt-in auto-send path and delays failure to allow WhatsApp UI loading.
