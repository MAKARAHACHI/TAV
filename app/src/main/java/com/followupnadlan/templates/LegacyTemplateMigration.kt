package com.followupnadlan.templates

/**
 * One-time migration from the old Nadlan/business templates to the accessibility
 * defaults.
 *
 * Older installs may have:
 *  - a [selectedTemplateId] pointing at a removed legacy template id, and/or
 *  - saved bodies that still hold the old business copy.
 *
 * Migration rules:
 *  - A legacy selected id is remapped to the accessibility default (עדין).
 *  - A saved body that exactly matches a known legacy default text is dropped,
 *    so the new accessibility default text is shown instead.
 *  - A saved body that the user customized is left untouched. We never
 *    overwrite the user's own message.
 */
object LegacyTemplateMigration {
    /** Ids of templates that existed only in the old Nadlan/business app. */
    val LEGACY_TEMPLATE_IDS = setOf(
        "buyer_property_details",
        "seller_valuation",
        "missed_call",
        "missed_call_auto_response"
    )

    /**
     * Exact old default bodies. Matching is whitespace-insensitive, but no longer
     * substring-based, so user edits containing an old phrase are not erased.
     */
    private val LEGACY_DEFAULT_BODIES = listOf(
        """
            שלום, תודה שפניתם ל{{businessName}}.
            אנחנו כרגע בשטח או בעבודה ולכן לא תמיד יכולים לענות מיד.
            קיבלנו את פנייתכם ונחזור אליכם בהקדם.
        """.trimIndent(),
        "שלום, שמחתי לדבר איתך לגבי הדירה. מצרף פרטים.",
        "שלום, שמחתי לדבר איתך לגבי הנכס. מצרף פרטים.",
        "ראיתי שפספסת את השיחה שלך.",
        "ראיתי שפספסתי את השיחה שלך."
    )

    /** Remaps a stored selected id to the accessibility default when it is legacy. */
    fun migrateSelectedTemplateId(storedId: String?): String =
        if (storedId == null || storedId in LEGACY_TEMPLATE_IDS) {
            SprintOneTemplates.DEFAULT_ID
        } else {
            storedId
        }

    /** True when the stored id refers to a removed legacy template. */
    fun isLegacySelectedId(storedId: String?): Boolean =
        storedId != null && storedId in LEGACY_TEMPLATE_IDS

    /**
     * @return true if the saved body is a known legacy default that should be dropped.
     */
    fun isLegacyDefaultBody(savedBody: String?): Boolean {
        if (savedBody.isNullOrBlank()) return false
        val normalized = normalizeBody(savedBody)
        return LEGACY_DEFAULT_BODIES.any { normalizeBody(it) == normalized }
    }

    private fun normalizeBody(body: String): String =
        body.replace(Regex("\\s+"), " ").trim()
}
