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
import com.followupnadlan.missedcall.MissedCallAutoResponseHandler
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
        val preferences = CallDetectionPreferences(applicationContext)
        monitor = CallStateMonitor(
            minCallDurationSeconds = preferences.minCallDurationSeconds.toLong(),
            onCallEnded = {
                postFollowUpNotificationAfterCallEnd()
            }
        )
        startStatusNotification()
        diagnostics.setServiceActive(true)
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
        super.onDestroy()
    }

    private fun postFollowUpNotificationAfterCallEnd() {
        mainHandler.postDelayed(
            {
                val latestCall = CallLogReader(applicationContext).readLatestCall()
                val contactFirstName = latestCall?.phoneNumber?.let {
                    ContactNameResolver(applicationContext).resolveFirstName(it)
                }
                FollowUpNotificationHelper(applicationContext).showFollowUpNotification(
                    phone = latestCall?.phoneNumber.orEmpty(),
                    leadName = contactFirstName.orEmpty(),
                    templateId = "",
                    callDurationSeconds = latestCall?.durationSeconds,
                    callTimestampMillis = latestCall?.timestampMillis,
                    callType = latestCall?.type?.toNotificationExtra()
                )
            },
            CALL_LOG_READ_DELAY_MILLIS
        )
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
        private const val STATUS_BODY = "זיהוי שיחות שלא נענו פעיל ברקע."

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
