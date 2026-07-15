package com.followupnadlan.accessibility

import androidx.compose.ui.text.input.KeyboardType
import com.followupnadlan.followuplog.FollowUpActionType
import com.followupnadlan.whatsapp.PhoneNumberNormalizer
import com.followupnadlan.whatsapp.WhatsAppLinkBuilder

data class HomeQuickWhatsAppNumberState(
    val text: String,
    val lastMissedPhone: String?
)

sealed class HomeQuickWhatsAppOpenPlan {
    data class OpenComposer(
        val normalizedPhone: String,
        val link: String,
        val message: String,
        val successLogAction: FollowUpActionType = FollowUpActionType.MANUAL_WHATSAPP_COMPOSER_OPENED,
        val failureLogAction: FollowUpActionType = FollowUpActionType.WHATSAPP_REPLY_FAILED
    ) : HomeQuickWhatsAppOpenPlan()

    data object InvalidNumber : HomeQuickWhatsAppOpenPlan()
}

object HomeQuickWhatsAppNumberLogic {
    fun applyLatestMissedCaller(
        currentText: String,
        currentLastMissedPhone: String?,
        latestMissedPhone: String?,
        isActivelyEditing: Boolean
    ): HomeQuickWhatsAppNumberState {
        if (latestMissedPhone.isNullOrBlank()) {
            return HomeQuickWhatsAppNumberState(
                text = currentText,
                lastMissedPhone = currentLastMissedPhone
            )
        }

        if (isActivelyEditing) {
            return HomeQuickWhatsAppNumberState(
                text = currentText,
                lastMissedPhone = currentLastMissedPhone
            )
        }

        if (latestMissedPhone != currentLastMissedPhone) {
            return HomeQuickWhatsAppNumberState(
                text = latestMissedPhone,
                lastMissedPhone = latestMissedPhone
            )
        }

        return HomeQuickWhatsAppNumberState(
            text = currentText,
            lastMissedPhone = currentLastMissedPhone
        )
    }
}

object HomeQuickWhatsAppOpenPlanner {
    val blockedLogActions = setOf(
        FollowUpActionType.WHATSAPP_AUTO_SENT,
        FollowUpActionType.AUTO_SMS_SENT,
        FollowUpActionType.FALLBACK_SMS_SENT
    )

    fun plan(phoneInput: String, message: String): HomeQuickWhatsAppOpenPlan {
        val normalizedPhone = PhoneNumberNormalizer.normalizeForWhatsApp(phoneInput)
            ?: return HomeQuickWhatsAppOpenPlan.InvalidNumber

        return HomeQuickWhatsAppOpenPlan.OpenComposer(
            normalizedPhone = normalizedPhone,
            link = WhatsAppLinkBuilder.build(normalizedPhone, message),
            message = message
        )
    }
}

object HomeQuickWhatsAppUiSpec {
    const val TITLE = "מספר לפתיחת WhatsApp"
    const val BUTTON_TEXT = "פתח WhatsApp"
    const val INVALID_NUMBER = "בדוק/י שהמספר תקין"
    const val WHATSAPP_FAILURE = "לא הצלחנו לפתוח WhatsApp למספר הזה"
    val helperText: String? = null
    val phoneKeyboardType: KeyboardType = KeyboardType.Phone
}
