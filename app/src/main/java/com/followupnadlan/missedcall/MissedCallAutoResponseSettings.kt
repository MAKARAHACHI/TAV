package com.followupnadlan.missedcall

import android.content.Context
import com.followupnadlan.templates.LegacyTemplateMigration
import com.followupnadlan.templates.SprintOneTemplates

class MissedCallAutoResponseSettings(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    /**
     * Default card for completed calls. Kept under the original key so an existing user's
     * chosen template stays their "call ended" default with no migration step.
     */
    var selectedEndedTemplateId: String
        // Remap a stored legacy Nadlan/business template id to the accessibility default.
        get() = LegacyTemplateMigration.migrateSelectedTemplateId(preferences.getString(KEY_TEMPLATE_ID, null))
        set(value) {
            preferences.edit().putString(KEY_TEMPLATE_ID, value).commit()
        }

    /** Default card for missed calls. Falls back to the fresh-install missed default. */
    var selectedMissedTemplateId: String
        get() = preferences.getString(KEY_MISSED_TEMPLATE_ID, null) ?: SprintOneTemplates.DEFAULT_MISSED_ID
        set(value) {
            preferences.edit().putString(KEY_MISSED_TEMPLATE_ID, value).commit()
        }

    var primaryChannel: MissedCallResponsePrimaryChannel
        get() = preferences.getString(KEY_PRIMARY_CHANNEL, null)
            ?.let { runCatching { MissedCallResponsePrimaryChannel.valueOf(it) }.getOrNull() }
            ?: MissedCallResponsePrimaryChannel.WHATSAPP_FIRST
        set(value) {
            preferences.edit().putString(KEY_PRIMARY_CHANNEL, value.name).apply()
        }

    var whatsappMode: MissedCallWhatsAppMode
        get() = preferences.getString(KEY_WHATSAPP_MODE, null)
            ?.let { runCatching { MissedCallWhatsAppMode.valueOf(it) }.getOrNull() }
            ?: MissedCallWhatsAppMode.PREPARED_MANUAL
        set(value) {
            preferences.edit().putString(KEY_WHATSAPP_MODE, value.name).apply()
        }

    var whatsappAutomationEnabled: Boolean
        get() = preferences.getBoolean(KEY_WHATSAPP_AUTOMATION_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_WHATSAPP_AUTOMATION_ENABLED, value).apply()
        }

    var preferredWhatsAppPackage: String
        get() = preferences.getString(KEY_PREFERRED_WHATSAPP_PACKAGE, null).orEmpty()
        set(value) {
            preferences.edit().putString(KEY_PREFERRED_WHATSAPP_PACKAGE, value).apply()
        }

    var smsFallbackEnabled: Boolean
        get() = preferences.getBoolean(KEY_SMS_FALLBACK_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_SMS_FALLBACK_ENABLED, value).apply()
        }

    var manualSmsFallbackEnabled: Boolean
        get() = preferences.getBoolean(KEY_MANUAL_SMS_FALLBACK_ENABLED, true)
        set(value) {
            preferences.edit().putBoolean(KEY_MANUAL_SMS_FALLBACK_ENABLED, value).apply()
        }

    var cooldownMillis: Long
        get() = preferences.getLong(KEY_COOLDOWN_MILLIS, MissedCallAutoResponseDecision.DEFAULT_COOLDOWN_MILLIS)
        set(value) {
            preferences.edit()
                .putLong(KEY_COOLDOWN_MILLIS, value.coerceAtLeast(MIN_COOLDOWN_MILLIS))
                .apply()
        }

    companion object {
        val DEFAULT_TEMPLATE_ID = SprintOneTemplates.DEFAULT_ID
        const val SOURCE = "missed_call_auto_response"
        private const val PREFERENCES_NAME = "missed_call_auto_response_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_TEMPLATE_ID = "template_id"
        private const val KEY_MISSED_TEMPLATE_ID = "missed_template_id"
        private const val KEY_PRIMARY_CHANNEL = "primary_channel"
        private const val KEY_WHATSAPP_MODE = "whatsapp_mode"
        private const val KEY_WHATSAPP_AUTOMATION_ENABLED = "whatsapp_automation_enabled"
        private const val KEY_PREFERRED_WHATSAPP_PACKAGE = "preferred_whatsapp_package"
        private const val KEY_SMS_FALLBACK_ENABLED = "sms_fallback_enabled"
        private const val KEY_MANUAL_SMS_FALLBACK_ENABLED = "manual_sms_fallback_enabled"
        private const val KEY_COOLDOWN_MILLIS = "cooldown_millis"
        private const val MIN_COOLDOWN_MILLIS = 60_000L
    }
}
