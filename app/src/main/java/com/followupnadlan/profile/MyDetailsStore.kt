package com.followupnadlan.profile

import android.content.Context

class MyDetailsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): MyDetailsProfile = MyDetailsProfile(
        agentName = preferences.getString(KEY_AGENT_NAME, "").orEmpty(),
        officeName = preferences.getString(KEY_OFFICE_NAME, "").orEmpty(),
        phone = preferences.getString(KEY_PHONE, "").orEmpty(),
        website = preferences.getString(KEY_WEBSITE, "").orEmpty(),
        businessCard = preferences.getString(KEY_BUSINESS_CARD, "").orEmpty(),
        signature = preferences.getString(KEY_SIGNATURE, "").orEmpty()
    )

    fun save(profile: MyDetailsProfile) {
        preferences.edit()
            .putString(KEY_AGENT_NAME, profile.agentName)
            .putString(KEY_OFFICE_NAME, profile.officeName)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_WEBSITE, profile.website)
            .putString(KEY_BUSINESS_CARD, profile.businessCard)
            .putString(KEY_SIGNATURE, profile.signature)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "my_details_profile"
        const val KEY_AGENT_NAME = "agent_name"
        const val KEY_OFFICE_NAME = "office_name"
        const val KEY_PHONE = "phone"
        const val KEY_WEBSITE = "website"
        const val KEY_BUSINESS_CARD = "business_card"
        const val KEY_SIGNATURE = "signature"
    }
}
