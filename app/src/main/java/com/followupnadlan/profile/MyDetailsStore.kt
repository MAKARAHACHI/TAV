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
        signature = preferences.getString(KEY_SIGNATURE, "").orEmpty(),
        property1Name = preferences.getString(KEY_PROPERTY_1_NAME, "").orEmpty(),
        property1Link = preferences.getString(KEY_PROPERTY_1_LINK, "").orEmpty(),
        property2Name = preferences.getString(KEY_PROPERTY_2_NAME, "").orEmpty(),
        property2Link = preferences.getString(KEY_PROPERTY_2_LINK, "").orEmpty(),
        property3Name = preferences.getString(KEY_PROPERTY_3_NAME, "").orEmpty(),
        property3Link = preferences.getString(KEY_PROPERTY_3_LINK, "").orEmpty(),
        activePropertyIndex = preferences.getInt(KEY_ACTIVE_PROPERTY_INDEX, 1).coerceIn(1, 3)
    )

    fun save(profile: MyDetailsProfile) {
        preferences.edit()
            .putString(KEY_AGENT_NAME, profile.agentName)
            .putString(KEY_OFFICE_NAME, profile.officeName)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_WEBSITE, profile.website)
            .putString(KEY_BUSINESS_CARD, profile.businessCard)
            .putString(KEY_SIGNATURE, profile.signature)
            .putString(KEY_PROPERTY_1_NAME, profile.property1Name)
            .putString(KEY_PROPERTY_1_LINK, profile.property1Link)
            .putString(KEY_PROPERTY_2_NAME, profile.property2Name)
            .putString(KEY_PROPERTY_2_LINK, profile.property2Link)
            .putString(KEY_PROPERTY_3_NAME, profile.property3Name)
            .putString(KEY_PROPERTY_3_LINK, profile.property3Link)
            .putInt(KEY_ACTIVE_PROPERTY_INDEX, profile.activePropertyIndex.coerceIn(1, 3))
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
        const val KEY_PROPERTY_1_NAME = "property_1_name"
        const val KEY_PROPERTY_1_LINK = "property_1_link"
        const val KEY_PROPERTY_2_NAME = "property_2_name"
        const val KEY_PROPERTY_2_LINK = "property_2_link"
        const val KEY_PROPERTY_3_NAME = "property_3_name"
        const val KEY_PROPERTY_3_LINK = "property_3_link"
        const val KEY_ACTIVE_PROPERTY_INDEX = "active_property_index"
    }
}
