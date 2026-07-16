package com.followupnadlan.templates

/**
 * Picks the template to send for a given call scenario ([TemplateRole]). Pure logic, no
 * Android — see TemplateRoleSelectorTest.
 *
 * Resolution order (never returns an empty message when any card exists):
 *  1. the user's chosen default for this role, if it still exists and matches the role
 *  2. any card of this role (e.g. the user deleted their chosen default)
 *  3. any card at all (e.g. an existing user has no card of this role yet) — sends a
 *     completed-call card on a missed call rather than nothing.
 */
object TemplateRoleSelector {
    fun forRole(
        templates: List<MessageTemplate>,
        role: TemplateRole,
        selectedIdForRole: String
    ): MessageTemplate? =
        templates.firstOrNull { it.id == selectedIdForRole && it.role == role }
            ?: templates.firstOrNull { it.role == role }
            ?: templates.firstOrNull()
}
