package com.followupnadlan.sharing

/** Whether the card opened straight in the chosen app or via the system chooser fallback. */
enum class TargetOpening { DIRECT, FALLBACK_CHOOSER }

/** Outcome of preparing and launching the contact-card share, mapped to UI by the caller. */
sealed interface ContactCardShareResult {
    /** WhatsApp / chooser opened with the card attached. Fields are non-identifying (for logs). */
    data class Opened(val preferredPackage: String, val targetOpening: TargetOpening) : ContactCardShareResult

    /** The saved card is missing required fields; nothing was opened. */
    data object IncompleteProfile : ContactCardShareResult

    /** Something failed; [userMessage] is a friendly Hebrew message for the UI. */
    data class Failed(val userMessage: String) : ContactCardShareResult
}
