package com.followupnadlan.setup

import android.content.Context

class SetupPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isSetupCompleted(): Boolean = preferences.getBoolean(KEY_SETUP_WIZARD_COMPLETED, false)

    fun setSetupCompleted(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SETUP_WIZARD_COMPLETED, value)
            .apply()
    }

    fun isSelfTestPassed(): Boolean = preferences.getBoolean(KEY_SELF_TEST_PASSED, false)

    fun setSelfTestPassed(value: Boolean) {
        preferences.edit()
            .putBoolean(KEY_SELF_TEST_PASSED, value)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "setup_preferences"
        const val KEY_SETUP_WIZARD_COMPLETED = "setup_wizard_completed"
        const val KEY_SELF_TEST_PASSED = "self_test_passed"
    }
}
