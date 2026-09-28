package com.herohiman.tournant.cost

import android.content.Context
import androidx.preference.PreferenceManager
import com.herohiman.tournant.Constants.Companion.PREF_PRIVACY_MODE

object CostPrivacyManager {

	private var inMemoryOverride: Boolean? = null

	fun isPrivacyModeEnabled(context: Context? = null): Boolean {
		inMemoryOverride?.let { return it }
		if (context == null) return false
		val prefs = PreferenceManager.getDefaultSharedPreferences(context)
		return prefs.getBoolean(PREF_PRIVACY_MODE, false)
	}

	fun setPrivacyModeEnabled(context: Context? = null, enabled: Boolean) {
		inMemoryOverride = enabled
		if (context != null) {
			val prefs = PreferenceManager.getDefaultSharedPreferences(context)
			prefs.edit().putBoolean(PREF_PRIVACY_MODE, enabled).apply()
		}
	}

	fun togglePrivacyMode(context: Context? = null): Boolean {
		val currentState = isPrivacyModeEnabled(context)
		val newState = !currentState
		setPrivacyModeEnabled(context, newState)
		return newState
	}

	fun setInMemoryOverride(enabled: Boolean?) {
		inMemoryOverride = enabled
	}

	fun clearInMemoryOverride() {
		inMemoryOverride = null
	}
}
