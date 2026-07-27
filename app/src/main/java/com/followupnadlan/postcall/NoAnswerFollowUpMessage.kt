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
 * Assembles the follow-up message for the "לא ענו" moment — an outgoing call the client did not
 * answer (Part A). Built exactly like [EndedFollowUpMessage] so it flows through the same send path
 * (WhatsApp text + signature, no vCard file-attach): the active NO_ANSWER_OUTGOING variant, closed
 * by the business card when "מצורף" is on.
 */
object NoAnswerFollowUpMessage {
    fun build(context: Context): String {
        val settings = MissedCallAutoResponseSettings(context)
        val template = TemplateRoleSelector.forRole(
            templates = TemplateStore(context).loadTemplates(),
            role = TemplateRole.NO_ANSWER_OUTGOING,
            selectedIdForRole = settings.selectedNoAnswerTemplateId
        ) ?: return ""

        return EndedMessageComposer.compose(
            body = MessageComposition.build(template),
            card = ContactCard.fromProfile(MyDetailsStore(context).load()),
            attachCard = EndedCardSettings(context).cardAttached
        )
    }
}
