# DEBUG LOG: Background Missed-Call Detection Reliability

## Symptom
Missed-call detection is unreliable and appears not to work when the app is closed or backgrounded.

## Expected behavior
When the bridge is enabled, the app should keep a lightweight foreground call detector active, observe cellular `RINGING`, `OFFHOOK`, and `IDLE`, confirm missed incoming calls, and pass only confirmed missed-call candidates into the existing `MissedCallAutoResponseHandler`.

## Reproduction steps
Current investigation is source-audit only; no device was attached in this planning pass.

Recommended device reproduction:
1. Install debug APK fresh.
2. Grant notifications, `READ_PHONE_STATE`, and `READ_CALL_LOG`.
3. Enable the bridge.
4. Confirm persistent notification `אני זמין/ה בכתב פעיל`.
5. Place an incoming cellular call and do not answer.
6. Confirm diagnostic log has `PHONE_STATE_RECEIVED_RINGING`, `PHONE_STATE_RECEIVED_IDLE`, `MISSED_CALL_CONFIRMED`, and `MISSED_CALL_HANDLER_STARTED`.
7. Repeat with app in foreground, app backgrounded, app swiped away, and after reboot if boot restart is added.

## Evidence collected
- Branch: `feature/accessibility-design-compose-final`.
- Worktree: dirty before this task; inherited changes span `MainActivity.kt`, accessibility UI/stores, missed-call handler/decision/status, notifications, templates, and tests.
- Manifest declares `READ_PHONE_STATE`, `READ_CALL_LOG`, `READ_CONTACTS`, `SEND_SMS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`, and `MANAGE_OWN_CALLS`.
- Manifest declares `.postcall.CallDetectionService` with `foregroundServiceType="phoneCall"`.
- No manifest-registered receiver for `PHONE_STATE` was found.
- `CallDetectionService` registers `TelephonyCallback.CallStateListener` on API 31+ and `PhoneStateListener.LISTEN_CALL_STATE` below API 31.
- `CallDetectionService.onStartCommand()` returns `START_NOT_STICKY`.
- `CallDetectionService.registerCallListener()` stops itself if `READ_PHONE_STATE` is missing.
- `CallDetectionService` ignores the `phoneNumber` parameter on the older `PhoneStateListener` path and the modern callback does not expose a number.
- `CallStateMonitor` tracks `incomingRang` and `answeredDuringCurrentCall` only in memory.
- `MissedCallAutoResponseHandler.handleMissedIncomingCandidate()` rereads latest CallLog and routes through `MissedCallAutoResponseDecision`.
- `AccessibilityApp` bridge toggle sets `MissedCallAutoResponseSettings.isEnabled` but no inspected production code starts or stops `CallDetectionService`.
- `CallDetectionPreferences.isEnabled()` is used by self-test, but no inspected production code sets it from the current bridge UI.
- `HomeServiceStatus.rows()` returns `גישור פעיל` whenever `bridgeEnabled=true`, without permission or service readiness inputs.

## Hypotheses
1. The foreground detector is not running when the user thinks the bridge is active.
   - Evidence for: Bridge UI toggles `MissedCallAutoResponseSettings`, while no inspected caller starts `CallDetectionService` or sets `CallDetectionPreferences`.
   - Evidence against: None found in current source search.
2. The detector loses missed-call state when the process dies between `RINGING` and `IDLE`.
   - Evidence for: `CallStateMonitor` pending state is memory-only.
   - Evidence against: None; current tests cover in-memory transitions only.
3. The app cannot reliably get the incoming number from phone-state callbacks.
   - Evidence for: Android 12+ `TelephonyCallback.CallStateListener` returns only state; current older listener ignores `phoneNumber`; current handler relies on CallLog.
   - Evidence against: `READ_CALL_LOG` path can recover recent latest call when granted and timely.
4. The UI readiness status is misleading.
   - Evidence for: Home service status does not accept permission/service readiness inputs and directly displays active from toggle state.
   - Evidence against: Setup self-test separately checks permissions, but it is not wired into Home bridge status.

## Root cause
Current best explanation: the release blocker is a lifecycle/readiness mismatch, not the pure decision engine. The app has a foreground call-detection service implementation, but the current bridge UI does not start/stop it or synchronize with `CallDetectionPreferences`; there is no manifest phone-state receiver; pending missed-call state is not persisted; and Home can claim `גישור פעיל` without required call-detection permissions or an active listener.

## Minimal fix
1. Wire bridge enable/disable to `CallDetectionPreferences` and foreground `CallDetectionService` lifecycle.
2. Add truthful readiness/status for `READ_PHONE_STATE`, `READ_CALL_LOG`, and service/listener state.
3. Add a manifest phone-state receiver if supported by target API behavior, plus foreground service monitoring when bridge is enabled.
4. Persist pending incoming call state in SharedPreferences.
5. Add conservative recent CallLog missed-call backfill on app/service start.
6. Add local QA diagnostics and regression tests.

## Validation
Planned validation:
- `.\gradlew.bat test`
- `.\gradlew.bat assembleDebug`
- `.\gradlew.bat lintDebug`
- `git diff --check`
- `git status`
- Manual real-device adb/logcat QA using tags/events from `CallDetectionDiagnostics`.

## Regression risk
- Manifest receiver and foreground service changes are lifecycle-sensitive.
- Permission request timing can annoy users if not scoped to setup/readiness.
- CallLog backfill can duplicate or process old calls unless dedup and recency checks are strict.
- Samsung and other OEM battery policies can still kill background monitoring even after this fix.
- Inherited Accessibility/SMS branch behavior is high-risk and must not be expanded by this sprint.
