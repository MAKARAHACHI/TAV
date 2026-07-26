package com.followupnadlan.templates

import com.followupnadlan.profile.ContactCard
import com.followupnadlan.profile.SignatureLine

/**
 * Builds the follow-up message sent after a conversation: the body the user wrote, optionally
 * closed by the business card as a text line.
 *
 * Shared by the ended journey's preview and by the follow-up notification, so what the user sees
 * on the screen and what the client receives are produced by the same code — the artifact-not-
 * description rule enforced at the source rather than by two implementations agreeing.
 *
 * The card is text, never a vCard: WhatsApp blocks file sharing to numbers that aren't saved, and
 * most clients are unsaved, so a one-tap vCard would be a promise that fails for the common case.
 *
 * Pure logic, no Android — see EndedMessageComposerTest.
 */
object EndedMessageComposer {
    fun compose(body: String, card: ContactCard, attachCard: Boolean): String =
        if (attachCard) SignatureLine.append(body, card) else body.trim()
}
