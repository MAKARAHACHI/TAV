package com.followupnadlan.templates

/**
 * Which call scenario a template is written for. The missed-call engine and the follow-up
 * prompt pick a template by role so the wording matches the situation (a "missed call"
 * message reads differently from a "call ended" one). Only the wording differs — both go
 * through the same send path.
 */
enum class TemplateRole {
    /** Incoming call that was not answered. */
    MISSED_CALL,

    /** Call that ended normally (answered incoming or outgoing). */
    CALL_ENDED
}

data class MessageTemplate(
    val id: String,
    val title: String,
    val body: String,
    /** Optional link to a digital business card; appended to the sent message when non-blank. */
    val cardLink: String = "",
    /** Optional link to a website; appended to the sent message when non-blank. */
    val websiteLink: String = "",
    /**
     * The call scenario this card is written for. Defaults to [TemplateRole.CALL_ENDED] so
     * older stored cards (encoded before this field existed) keep their current behavior.
     */
    val role: TemplateRole = TemplateRole.CALL_ENDED
)
