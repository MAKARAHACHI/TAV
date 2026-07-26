package com.followupnadlan.postcall

import android.content.Context
import com.followupnadlan.accessibility.EndedCardSettings
import com.followupnadlan.missedcall.MissedCallAutoResponseSettings
import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.MyDetailsStore
import com.followupnadlan.templates.EndedMessageComposer
import com.followupnadlan.templates.MessageComposition
import com.followupnadlan.templates.TemplateRole
import com.followupnadlan.templates.TemplateRoleSelector
import com.followupnadlan.templates.TemplateStore

/**
 * Assembles the follow-up message exactly as the ended journey previews it: the body the user
 * wrote, closed by the business card when "מצורף" is on.
 *
 * Both the screen and the notification go through [EndedMessageComposer], so what the user
 * approved on the screen is literally what the notification offers to send.
 */
object EndedFollowUpMessage {
    fun build(context: Context): String {
        val settings = MissedCallAutoResponseSettings(context)
        val template = TemplateRoleSelector.forRole(
            templates = TemplateStore(context).loadTemplates(),
            role = TemplateRole.CALL_ENDED,
            selectedIdForRole = settings.selectedEndedTemplateId
        ) ?: return ""

        return EndedMessageComposer.compose(
            body = MessageComposition.build(template),
            card = ContactCard.fromProfile(MyDetailsStore(context).load()),
            attachCard = EndedCardSettings(context).cardAttached
        )
    }
}
