package com.followupnadlan.accessibility

/**
 * The single ⚠️ line on Home — shown *only* when something is actually broken.
 *
 * "שקט = עובד": when everything works, Home says nothing at all. No ✓, no "פעיל", no status
 * block. But silence about a real fault is a lie (§2) — if the app cannot keep its promise, the
 * user must be told, in the language of the consequence for their client rather than the
 * mechanism that failed.
 */
enum class HomeWarning {
    /** Service off or a required permission missing: nobody is being answered. */
    NOT_ANSWERING,

    /** Working, but the message goes out unsigned — the client can't tell who wrote. */
    EMPTY_PROFILE
}

data class HomeWarningState(
    val serviceEnabled: Boolean,
    val phoneStatePermissionGranted: Boolean,
    val callLogPermissionGranted: Boolean,
    val profileEmpty: Boolean
)

object HomeWarningLogic {
    /**
     * At most one warning, most severe first: "your clients are getting nothing" outranks
     * "they're getting something unsigned". Two warnings at once would turn Home into the status
     * board this design deliberately removed.
     */
    fun warningFor(state: HomeWarningState): HomeWarning? = when {
        !state.serviceEnabled ||
            !state.phoneStatePermissionGranted ||
            !state.callLogPermissionGranted -> HomeWarning.NOT_ANSWERING
        state.profileEmpty -> HomeWarning.EMPTY_PROFILE
        else -> null
    }

    /** Result-language, from the client's point of view — never the failing component's name. */
    fun message(warning: HomeWarning): String = when (warning) {
        HomeWarning.NOT_ANSWERING -> "כרגע לקוחות לא מקבלים ממך מענה — הפעל"
        HomeWarning.EMPTY_PROFILE -> "הוסף את הפרטים שלך — הלקוח לא יודע מי שלח"
    }
}
