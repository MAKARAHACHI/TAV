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

/**
 * Optional capabilities the app can gain but does NOT need to keep its core promise. Kept separate
 * from [FollowUpPermission] (the required three) so a manual-only user is never told they are
 * "missing" something essential — §2 honesty. [ACCESSIBILITY] only unlocks automatic missed sending.
 */
enum class FollowUpOptionalCapability {
    ACCESSIBILITY
}

data class PermissionSnapshot(
    val phoneStateGranted: Boolean,
    val callLogGranted: Boolean,
    val contactsGranted: Boolean,
    /**
     * Whether our Accessibility service is enabled. Read in the Android layer from
     * `WhatsAppAutoSendController.isAccessibilityServiceEnabled()` and fed in here as a plain boolean.
     * OPTIONAL — deliberately NOT part of [PermissionStatusLogic.canAnswerCalls].
     */
    val accessibilityEnabled: Boolean = false
) {
    fun isGranted(permission: FollowUpPermission): Boolean = when (permission) {
        FollowUpPermission.PHONE_STATE -> phoneStateGranted
        FollowUpPermission.CALL_LOG -> callLogGranted
        FollowUpPermission.CONTACTS -> contactsGranted
    }

    fun isGranted(capability: FollowUpOptionalCapability): Boolean = when (capability) {
        FollowUpOptionalCapability.ACCESSIBILITY -> accessibilityEnabled
    }
}

object PermissionStatusLogic {
    /** All permissions the app asks for, in the order they matter. */
    val all: List<FollowUpPermission> = FollowUpPermission.entries

    /** Optional capabilities, shown separately from the required permissions. */
    val optional: List<FollowUpOptionalCapability> = FollowUpOptionalCapability.entries

    fun grantedCount(snapshot: PermissionSnapshot): Int = all.count(snapshot::isGranted)

    /** The live summary shown on the ⚙️ row, e.g. "2 מ-3 פעילות". Counts REQUIRED permissions only. */
    fun summary(snapshot: PermissionSnapshot): String =
        "${grantedCount(snapshot)} מ-${all.size} פעילות"

    /** What the user gets from this permission — the reason to grant it, not its identifier. */
    fun outcome(permission: FollowUpPermission): String = when (permission) {
        FollowUpPermission.PHONE_STATE -> "זיהוי שיחות — כדי לדעת שהתקשרו אליך"
        FollowUpPermission.CALL_LOG -> "יומן שיחות — כדי לדעת שלא ענית"
        FollowUpPermission.CONTACTS -> "אנשי קשר — כדי להבדיל בין לקוח למישהו שאתה מכיר"
    }

    /** The title of an optional capability row. */
    fun title(capability: FollowUpOptionalCapability): String = when (capability) {
        FollowUpOptionalCapability.ACCESSIBILITY -> "שליחה אוטומטית (נגישות)"
    }

    /** What the optional capability unlocks — plainly, never its Android name. */
    fun outcome(capability: FollowUpOptionalCapability): String = when (capability) {
        FollowUpOptionalCapability.ACCESSIBILITY -> "כדי לשלוח הודעות לבד בשיחות שלא ענית"
    }

    /**
     * Whether the two permissions the missed-call promise depends on are in place. Contacts is
     * excluded deliberately: without it the app still answers, it just cannot tell saved from
     * unsaved numbers. Accessibility is excluded deliberately too: it is OPTIONAL and only affects
     * automatic sending, never the app's ability to answer calls.
     */
    fun canAnswerCalls(snapshot: PermissionSnapshot): Boolean =
        snapshot.phoneStateGranted && snapshot.callLogGranted
}
