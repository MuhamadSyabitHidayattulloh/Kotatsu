package eu.kanade.tachiyomi.source

import android.content.SharedPreferences

interface ConfigurableSource : Source {
	fun setupPreferenceScreen(screen: Any) {}
}

interface PreferencesSource {
	val preferences: SharedPreferences
}
