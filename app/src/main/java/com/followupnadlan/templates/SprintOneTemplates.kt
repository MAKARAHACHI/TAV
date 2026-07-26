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

    /** Fresh-install default for completed calls. */
    const val DEFAULT_ID = ENDED_ID

    /** Fresh-install default for missed calls. */
    const val DEFAULT_MISSED_ID = MISSED_ID

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

    val all = listOf(ended, missed)
}
