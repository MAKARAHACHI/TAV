package com.followupnadlan.templates

/**
 * One-time migration from the old Nadlan/business templates to the accessibility
 * defaults.
 *
 * Older installs may have:
 *  - a [selectedTemplateId] pointing at a removed legacy template id, and/or
 *  - saved bodies (per-template) that still hold the old business copy.
 *
 * Migration rules:
 *  - A legacy selected id is remapped to the accessibility default (עדין).
 *  - A saved body that still matches a known legacy default text is dropped, so
 *    the new accessibility default text is shown instead.
 *  - A saved body that the user customized (does NOT match a legacy default) is
 *    left untouched — we never overwrite the user's own message.
 *
 * Pure logic only; the SharedPreferences plumbing lives in the stores that call this.
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
     * Normalized snippets of the old default bodies. A saved body is considered a
     * non-customized legacy default if it contains any of these, so we can safely
     * replace it. Matching is whitespace-insensitive and trimmed.
     */
    private val LEGACY_DEFAULT_MARKERS = listOf(
        "תודה שפניתם ל",
        "שמחתי לדבר איתך לגבי הדירה",
        "שמחתי לדבר איתך לגבי הנכס",
        "ראיתי שפספסתי את השיחה שלך"
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
     * Decides whether a saved body should be discarded (returning the accessibility
     * default) or kept (user customization).
     *
     * @return true if the saved body is a legacy default that should be dropped.
     */
    fun isLegacyDefaultBody(savedBody: String?): Boolean {
        if (savedBody.isNullOrBlank()) return false
        val normalized = savedBody.replace(Regex("\\s+"), " ").trim()
        return LEGACY_DEFAULT_MARKERS.any { normalized.contains(it) }
    }
}
