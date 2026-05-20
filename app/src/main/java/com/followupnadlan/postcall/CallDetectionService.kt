package com.followupnadlan.postcall

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.followupnadlan.MainActivity
import com.followupnadlan.R
import com.followupnadlan.notifications.FollowUpNotificationHelper

class CallDetectionService : Service() {
    private lateinit var monitor: CallStateMonitor
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    override fun onCreate() {
        super.onCreate()
        val preferences = CallDetectionPreferences(applicationContext)
        monitor = CallStateMonitor(
            minCallDurationSeconds = preferences.minCallDurationSeconds.toLong(),
            onCallEnded = {
                FollowUpNotificationHelper(applicationContext).showFollowUpNotification(
                    phone = "",
                    leadName = "",
                    templateId = ""
                )
            }
        )
        startStatusNotification()
        registerCallListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterCallListener()
        super.onDestroy()
    }

    private fun registerCallListener() {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        val telephonyManager = getSystemService(TelephonyManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handlePlatformState(state)
                }
            }
            telephonyCallback = callback
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated by Android in favor of TelephonyCallback on API 31+.")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handlePlatformState(state)
                }
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
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

    private fun handlePlatformState(state: Int) {
        val callState = when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> CallStateMonitor.CallState.OFFHOOK
            TelephonyManager.CALL_STATE_RINGING -> CallStateMonitor.CallState.RINGING
            else -> CallStateMonitor.CallState.IDLE
        }
        monitor.onStateChanged(callState, System.currentTimeMillis())
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

    private companion object {
        const val STATUS_CHANNEL_ID = "call_detection_status"
        const val STATUS_NOTIFICATION_ID = 9001
        const val STATUS_REQUEST_CODE = 9001
        const val STATUS_CHANNEL_NAME = "זיהוי שיחות"
        const val STATUS_CHANNEL_DESCRIPTION = "התראת סטטוס לזיהוי סיום שיחה"
        const val STATUS_TITLE = "זיהוי שיחות פעיל"
        const val STATUS_BODY = "האפליקציה תציג התראת פולואפ אחרי שיחה."
    }
}
