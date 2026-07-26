package com.followupnadlan.setup

/**
 * The live permission picture behind the ⚙️ screen's "הרשאות — 2 מ-3 פעילות" row.
 *
 * Each permission is described by *what the user loses without it*, never by its Android name:
 * "יומן שיחות — כדי לדעת שלא ענית", not "READ_CALL_LOG". This is the one screen where system
 * language is acceptable, but the result still has to be the thing the user can act on.
 *
 * This is also the engine behind the ⚠️ line on Home and the malfunction notification — one
 * source of truth for "can the app currently keep its promise?".
 *
 * Pure logic, no Android — see PermissionStatusTest.
 */
enum class FollowUpPermission {
    PHONE_STATE,
    CALL_LOG,
    CONTACTS
}

data class PermissionSnapshot(
    val phoneStateGranted: Boolean,
    val callLogGranted: Boolean,
    val contactsGranted: Boolean
) {
    fun isGranted(permission: FollowUpPermission): Boolean = when (permission) {
        FollowUpPermission.PHONE_STATE -> phoneStateGranted
        FollowUpPermission.CALL_LOG -> callLogGranted
        FollowUpPermission.CONTACTS -> contactsGranted
    }
}

object PermissionStatusLogic {
    /** All permissions the app asks for, in the order they matter. */
    val all: List<FollowUpPermission> = FollowUpPermission.entries

    fun grantedCount(snapshot: PermissionSnapshot): Int = all.count(snapshot::isGranted)

    /** The live summary shown on the ⚙️ row, e.g. "2 מ-3 פעילות". */
    fun summary(snapshot: PermissionSnapshot): String =
        "${grantedCount(snapshot)} מ-${all.size} פעילות"

    /** What the user gets from this permission — the reason to grant it, not its identifier. */
    fun outcome(permission: FollowUpPermission): String = when (permission) {
        FollowUpPermission.PHONE_STATE -> "זיהוי שיחות — כדי לדעת שהתקשרו אליך"
        FollowUpPermission.CALL_LOG -> "יומן שיחות — כדי לדעת שלא ענית"
        FollowUpPermission.CONTACTS -> "אנשי קשר — כדי להבדיל בין לקוח למישהו שאתה מכיר"
    }

    /**
     * Whether the two permissions the missed-call promise depends on are in place. Contacts is
     * excluded deliberately: without it the app still answers, it just cannot tell saved from
     * unsaved numbers.
     */
    fun canAnswerCalls(snapshot: PermissionSnapshot): Boolean =
        snapshot.phoneStateGranted && snapshot.callLogGranted
}
