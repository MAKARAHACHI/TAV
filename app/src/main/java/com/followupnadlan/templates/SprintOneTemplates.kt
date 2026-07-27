package com.followupnadlan.templates

/**
 * Default message templates for the follow-up bridge — one per call scenario.
 *
 * These are the only user-visible defaults: a completed-call ("סיום שיחה") card and a
 * missed-call ("שיחה שלא נענתה") card. The copy is profession-neutral (fits a lawyer,
 * accountant, consultant, therapist…) and carries no name/signature, since the message is
 * sent as-is with no template tags. Links (digital card / website) live in the template's
 * own fields and are appended automatically when filled — see [MessageComposition].
 *
 * Legacy Nadlan/business templates were removed; see [LegacyTemplateMigration] for how
 * older installs are migrated to these.
 */
object SprintOneTemplates {
    const val ENDED_ID = "followup_ended"
    const val MISSED_ID = "followup_missed"
    const val NO_ANSWER_ID = "followup_no_answer"

    /** Fresh-install default for completed calls. */
    const val DEFAULT_ID = ENDED_ID

    /** Fresh-install default for missed calls. */
    const val DEFAULT_MISSED_ID = MISSED_ID

    /** Fresh-install default for outgoing calls the client did not answer. */
    const val DEFAULT_NO_ANSWER_ID = NO_ANSWER_ID

    val ended = MessageTemplate(
        id = ENDED_ID,
        title = "סיום שיחה",
        role = TemplateRole.CALL_ENDED,
        body = "תודה על השיחה! שמח שדיברנו. מצרף את הפרטים שלי:"
    )

    val missed = MessageTemplate(
        id = MISSED_ID,
        title = "שיחה שלא נענתה",
        role = TemplateRole.MISSED_CALL,
        body = "תודה שהתקשרת, אני כרגע לא פנוי. אחזור אליך ברגע שאתפנה."
    )

    /** Outgoing call I made, client did not pick up (Part A default text). */
    val noAnswer = MessageTemplate(
        id = NO_ANSWER_ID,
        title = "לא ענו",
        role = TemplateRole.NO_ANSWER_OUTGOING,
        body = "ניסיתי להתקשר ולא ענית, אשמח שתחזור אליי 🙏"
    )

    val all = listOf(ended, missed, noAnswer)
}
