package com.followupnadlan.templates

/**
 * Default missed-call message templates for the "אני זמין/ה בכתב" accessibility app.
 *
 * These are the only user-visible default messages. They contain no business /
 * real-estate copy and no template tags — the text is sent as-is. Legacy
 * Nadlan/business templates were removed; see [LegacyTemplateMigration] for how
 * older installs are migrated to these.
 */
object SprintOneTemplates {
    const val OPEN_ID = "accessibility_open"
    const val GENTLE_ID = "accessibility_gentle"
    const val PRIVATE_ID = "accessibility_private"
    const val MISSED_ID = "accessibility_missed"

    /** Fresh-install default for completed calls. */
    const val DEFAULT_ID = GENTLE_ID

    /** Fresh-install default for missed calls. */
    const val DEFAULT_MISSED_ID = MISSED_ID

    val open = MessageTemplate(
        id = OPEN_ID,
        title = "גלוי",
        body = """
            שלום, אני חירש/ת או כבד/ת שמיעה ולא תמיד יכול/ה לענות לשיחה קולית.
            אפשר לכתוב לי כאן ב־WhatsApp או ב־SMS ואחזור אליך בכתב.
        """.trimIndent()
    )

    val gentle = MessageTemplate(
        id = GENTLE_ID,
        title = "עדין",
        body = """
            שלום, קשה לי לענות לשיחות קוליות.
            אפשר בבקשה לכתוב לי כאן ב־WhatsApp או ב־SMS?
        """.trimIndent()
    )

    val private = MessageTemplate(
        id = PRIVATE_ID,
        title = "פרטי",
        body = """
            שלום, אני מעדיף/ה תקשורת בכתב.
            אפשר לכתוב לי כאן ואחזור אליך בהודעה.
        """.trimIndent()
    )

    val missed = MessageTemplate(
        id = MISSED_ID,
        title = "שיחה שלא נענתה",
        role = TemplateRole.MISSED_CALL,
        body = """
            שלום, ראיתי שחיפשת אותי ולא הספקתי לענות.
            אפשר לכתוב לי כאן בקצרה במה מדובר ואחזור אליך בהקדם.
        """.trimIndent()
    )

    val all = listOf(open, gentle, private, missed)
}
