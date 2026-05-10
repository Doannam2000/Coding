package com.nantcompany.clipy.onboarding

import android.content.Context

class OnboardingStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("clipy_prefs", Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    private companion object {
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
