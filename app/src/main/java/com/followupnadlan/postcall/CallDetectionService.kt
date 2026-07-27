package com.followupnadlan.postcall

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.followupnadlan.MainActivity
import com.followupnadlan.R
import com.followupnadlan.accessibility.AllowedRecipientsStore
import com.followupnadlan.accessibility.EndedScopeSettings
import com.followupnadlan.accessibility.FollowUpCooldownSettings
import com.followupnadlan.accessibility.LocalMomentResolver
import com.followupnadlan.accessibility.WorkingHoursDecider
import com.followupnadlan.accessibility.FollowUpPromptModeLogic
import com.followupnadlan.accessibility.WorkingHoursSettings
import com.followupnadlan.missedcall.ContactVerifier
import com.followupnadlan.missedcall.MissedCallAutoResponseHandler
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.notifications.EndedSuggestionNotificationHelper
import com.followupnadlan.notifications.FollowUpFailureNotificationHelper
import com.followupnadlan.notifications.FollowUpNotificationHelper

class CallDetectionService : Service() {
    private lateinit var monitor: CallStateMonitor
    private lateinit var diagnostics: CallDetectionDiagnostics
    private val mainHandler = Handler(Looper.getMainLooper())
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    override fun onCreate() {
        super.onCreate()
        diagnostics = CallDetectionDiagnostics(applicationContext)
        monitor = CallStateMonitor(
            onCallEnded = {
                postFollowUpNotificationAfterCallEnd()
            }
        )
        startStatusNotification()
        diagnostics.setServiceActive(true)
        // Running again — clear any "FollowUp הופסק" left over from the previous stop.
        runCatching { FollowUpFailureNotificationHelper(applicationContext).cancelServiceStopped() }
        registerCallListener()
        runRecentMissedCallBackfill()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        unregisterCallListener()
        diagnostics.setServiceActive(false)
        notifyIfStoppedUnexpectedly()
        super.onDestroy()
    }

    /**
     * The service died while the user still expects it to be running — killed by the OEM battery
     * manager, or a permission revoked. They may not open this app for weeks, so the ⚠️ line on
     * Home cannot carry this alone: staying silent would let them believe clients are being
     * answered when nobody is (§2).
     *
     * A deliberate switch-off is not a fault, so it is not reported.
     */
    private fun notifyIfStoppedUnexpectedly() {
        val context = applicationContext
        val userExpectsItRunning = CallDetectionPreferences(context).isEnabled()
        if (!userExpectsItRunning) return

        runCatching { FollowUpFailureNotificationHelper(context).showServiceStopped() }
    }

    /**
     * Offers a follow-up after a conversation — but only when the call actually earns one.
     * Previously every ended call produced a notification; the eligibility rules (answered, long
     * enough, in scope, reachable number, not in cooldown) now live in [EndedSuggestionDecider].
     */
    private fun postFollowUpNotificationAfterCallEnd() {
        mainHandler.postDelayed(
            {
                runCatching { offerFollowUpForLatestCall() }
            },
            CALL_LOG_READ_DELAY_MILLIS
        )
    }

    private fun offerFollowUpForLatestCall() {
        val context = applicationContext
        val latestCall = CallLogReader(context).readLatestCall() ?: return
        val phone = latestCall.phoneNumber.orEmpty()
        val now = System.currentTimeMillis()

        val suggestionStore = EndedSuggestionStore(context)
        val contactVerifier = ContactVerifier(context)
        // The scope / cooldown / working-hours gate is identical for both after-call moments, so
        // the verified ENDED decider owns it for both. Part A adds NO_ANSWER *alongside* — this
        // decider's rules are unchanged.
        val cooldowns = FollowUpCooldownSettings(context)
        val decision = EndedSuggestionDecider.decide(
            EndedSuggestionInput(
                callDurationSeconds = latestCall.durationSeconds,
                phoneNumber = phone,
                isSavedContact = contactVerifier.isSavedContact(phone),
                contactsPermissionGranted = contactVerifier.hasContactsPermission(),
                scope = EndedScopeSettings(context).scope,
                allowedNumbers = AllowedRecipientsStore(context).load().map { it.number },
                lastSuggestedAtEpochMs = suggestionStore.lastSuggestedAt(phone),
                lastAnyNotificationAtEpochMs = suggestionStore.lastAnyNotificationAt(),
                sameNumberCooldownMillis = cooldowns.sameNumberCooldownMillis,
                globalQuietMillis = cooldowns.globalQuietMillis,
                nowEpochMs = now,
                withinWorkingHours = WorkingHoursDecider.isWithinWorkingHours(
                    LocalMomentResolver.resolve(now),
                    WorkingHoursSettings(context).snapshot()
                )
            )
        )
        if (decision != EndedSuggestionDecision.SUGGEST) return

        val settings = MissedCallAutoResponseSettings(context)
        // Which after-call moment is this? An outgoing call with zero duration never connected.
        when (EndedMomentClassifier.classify(latestCall.type, latestCall.durationSeconds)) {
            EndedMoment.NO_ANSWER_OUTGOING -> offerNoAnswer(context, phone, now, suggestionStore, settings)
            EndedMoment.ENDED -> offerEnded(context, phone, now, latestCall, suggestionStore, settings)
        }
    }

    /** The existing ENDED moment (answered calls, unanswered incoming). Behaviour unchanged. */
    private fun offerEnded(
        context: Context,
        phone: String,
        now: Long,
        latestCall: LatestCallLogEntry,
        suggestionStore: EndedSuggestionStore,
        settings: MissedCallAutoResponseSettings
    ) {
        // Per-moment enable (Part C2). The service only runs while the master isEnabled is on, so
        // the master gate is already applied by the service lifecycle.
        if (!settings.endedMomentEnabled) return

        val message = EndedFollowUpMessage.build(context)
        if (message.isBlank()) return

        // A name only when the number is genuinely saved; otherwise the number is the identity.
        val displayName = ContactNameResolver(context).resolveFirstName(phone).orEmpty()
        EndedSuggestionNotificationHelper(context).showSuggestion(
            phone = phone,
            displayName = displayName,
            message = message,
            wasAnswered = latestCall.type != FollowUpCallType.Missed && latestCall.durationSeconds > 0L
        )
        suggestionStore.markSuggested(phone, now)
    }

    /**
     * Part A — the "לא ענו" moment: I called a client who did not pick up. MANUAL approve only, so
     * it opens the follow-up prompt (edit-before-send) rather than the one-tap ended send. The
     * message is the active NO_ANSWER_OUTGOING variant (text + signature, no vCard file-attach).
     */
    private fun offerNoAnswer(
        context: Context,
        phone: String,
        now: Long,
        suggestionStore: EndedSuggestionStore,
        settings: MissedCallAutoResponseSettings
    ) {
        if (!settings.noAnswerMomentEnabled) return

        val message = NoAnswerFollowUpMessage.build(context)
        if (message.isBlank()) return

        val displayName = ContactNameResolver(context).resolveFirstName(phone).orEmpty()
        FollowUpNotificationHelper(context).showFollowUpNotification(
            phone = phone,
            leadName = displayName,
            templateId = settings.selectedNoAnswerTemplateId,
            callType = FollowUpPromptModeLogic.CALL_TYPE_NO_ANSWER
        )
        suggestionStore.markSuggested(phone, now)
    }

    private fun registerCallListener() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            diagnostics.recordEvent(CallDetectionDiagnostics.PERMISSION_MISSING_READ_PHONE_STATE)
            stopSelf()
            return
        }
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            diagnostics.recordEvent(CallDetectionDiagnostics.PERMISSION_MISSING_READ_CALL_LOG)
        }

        val telephonyManager = getSystemService(TelephonyManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handlePlatformState(state, null)
                }
            }
            telephonyCallback = callback
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated by Android in favor of TelephonyCallback on API 31+.")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handlePlatformState(state, phoneNumber)
                }
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
        diagnostics.recordEvent(CallDetectionDiagnostics.CALL_RECEIVER_REGISTERED)
    }

    private fun unregisterCallListener() {
        val telephonyManager = getSystemService(TelephonyManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let(telephonyManager::unregisterTelephonyCallback)
            telephonyCallback = null
        } else {
            phoneStateListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
            phoneStateListener = null
        }
    }

    private fun handlePlatformState(state: Int, phoneNumber: String?) {
        val callState = when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> CallStateMonitor.CallState.OFFHOOK
            TelephonyManager.CALL_STATE_RINGING -> CallStateMonitor.CallState.RINGING
            else -> CallStateMonitor.CallState.IDLE
        }
        monitor.onStateChanged(callState, System.currentTimeMillis())
        PhoneStateEventProcessor(applicationContext, onConfirmedMissedCall = ::handleMissedIncomingCall)
            .handleState(callState, phoneNumber)
    }

    private fun handleMissedIncomingCall(phoneNumber: String?) =
        MissedCallAutoResponseHandler(applicationContext).handleConfirmedMissedIncomingCandidate(phoneNumber)

    private fun runRecentMissedCallBackfill() {
        MissedCallBackfill(applicationContext).processRecentMissedCalls { phone ->
            MissedCallAutoResponseHandler(applicationContext).handleConfirmedMissedIncomingCandidate(phone)
        }
    }

    private fun startStatusNotification() {
        createStatusChannel()
        val notification = Notification.Builder(applicationContext, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(STATUS_TITLE)
            .setContentText(STATUS_BODY)
            .setContentIntent(createOpenAppIntent())
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                STATUS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(STATUS_NOTIFICATION_ID, notification)
        }
    }

    private fun createStatusChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            STATUS_CHANNEL_ID,
            STATUS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = STATUS_CHANNEL_DESCRIPTION
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createOpenAppIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        return PendingIntent.getActivity(
            applicationContext,
            STATUS_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val ACTION_STOP = "com.followupnadlan.postcall.STOP_CALL_DETECTION"
        const val STATUS_CHANNEL_ID = "call_detection_status"
        private const val STATUS_NOTIFICATION_ID = 9001
        private const val STATUS_REQUEST_CODE = 9001
        private const val CALL_LOG_READ_DELAY_MILLIS = 1_000L
        private const val STATUS_CHANNEL_NAME = "זיהוי שיחות"
        private const val STATUS_CHANNEL_DESCRIPTION = "סטטוס לזיהוי שיחות שלא נענו"
        private const val STATUS_TITLE = "אני זמין/ה בכתב פעיל"
        private const val STATUS_BODY = "עונה בכתב לשיחות שלא נענו"

        fun start(context: Context) {
            val intent = Intent(context, CallDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallDetectionService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}

private fun FollowUpCallType.toNotificationExtra(): String = when (this) {
    FollowUpCallType.Incoming -> "incoming"
    FollowUpCallType.Outgoing -> "outgoing"
    FollowUpCallType.Missed -> "missed"
}
