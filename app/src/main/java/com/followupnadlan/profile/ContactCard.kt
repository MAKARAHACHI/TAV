package com.followupnadlan.profile

/**
 * The single contact-card identity the business owner shares as a vCard. Pure value object —
 * no Android — so it stays trivially testable. It reuses the existing [MyDetailsProfile] fields
 * (agent name / office / phone) rather than introducing a second source of truth.
 */
data class ContactCard(
    val fullName: String,
    val org: String,
    val phone: String,
    /** Optional; shown on the card and in the vCard only when filled. */
    val website: String = ""
) {
    /** A card can be shared only once it has at least a name and a phone. */
    val isComplete: Boolean
        get() = fullName.isNotBlank() && phone.isNotBlank()

    companion object {
        fun fromProfile(profile: MyDetailsProfile): ContactCard = ContactCard(
            fullName = profile.agentName.trim(),
            org = profile.officeName.trim(),
            phone = profile.phone.trim(),
            website = profile.website.trim()
        )
    }
}
