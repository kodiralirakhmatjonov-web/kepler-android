package com.iumrah.beta.core.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val Context.iumrahPreferences by preferencesDataStore(name = "iumrah_settings")

enum class AppAppearance(val wireValue: String) { SYSTEM("system"), LIGHT("light"), DARK("dark") }

enum class AppLanguage(val code: String, val localeTag: String) {
    RUSSIAN("ru", "ru-RU"),
    ENGLISH("en", "en-US"),
    UZBEK("uz", "uz-Latn-UZ"),
    UZBEK_CYRILLIC("uz-cyrl", "uz-Cyrl-UZ");

    val locale: Locale get() = Locale.forLanguageTag(localeTag)

    companion object {
        fun fromCode(value: String?): AppLanguage = entries.firstOrNull { it.code == value } ?: UZBEK
    }
}

data class AppSettingsState(
    val appearance: AppAppearance = AppAppearance.SYSTEM,
    val language: AppLanguage = AppLanguage.UZBEK,
    val firstName: String = "",
    val lastName: String = "",
    val telegram: String = "",
    val whatsapp: String = "",
    val hasCompletedOnboarding: Boolean = false,
    val isLoaded: Boolean = false,
) {
    val displayName: String
        get() = listOf(firstName.trim(), lastName.trim()).filter { it.isNotBlank() }.joinToString(" ")

    val hasBookingIdentity: Boolean
        get() = firstName.isNotBlank() && lastName.isNotBlank() && (telegram.isNotBlank() || whatsapp.isNotBlank())
}

class AppSettingsStore(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(AppSettingsState())
    val state: StateFlow<AppSettingsState> = _state

    init {
        scope.launch { restore() }
    }

    suspend fun restore() {
        val values = context.iumrahPreferences.data.first()
        _state.value = AppSettingsState(
            appearance = values[Keys.APPEARANCE]?.let { raw -> AppAppearance.entries.firstOrNull { it.wireValue == raw } } ?: AppAppearance.SYSTEM,
            language = AppLanguage.fromCode(values[Keys.LANGUAGE]),
            firstName = values[Keys.FIRST_NAME].orEmpty(),
            lastName = values[Keys.LAST_NAME].orEmpty(),
            telegram = values[Keys.TELEGRAM].orEmpty(),
            whatsapp = values[Keys.WHATSAPP].orEmpty(),
            hasCompletedOnboarding = values[Keys.ONBOARDING] == "true",
            isLoaded = true,
        )
    }

    fun setAppearance(value: AppAppearance) = persist(Keys.APPEARANCE, value.wireValue) { copy(appearance = value) }
    fun setLanguage(value: AppLanguage) = persist(Keys.LANGUAGE, value.code) { copy(language = value) }
    fun completeOnboarding() = persist(Keys.ONBOARDING, "true") { copy(hasCompletedOnboarding = true) }

    fun updateProfile(firstName: String, lastName: String, telegram: String, whatsapp: String) {
        _state.update { it.copy(firstName = firstName, lastName = lastName, telegram = telegram, whatsapp = whatsapp) }
        scope.launch {
            context.iumrahPreferences.edit {
                it[Keys.FIRST_NAME] = firstName
                it[Keys.LAST_NAME] = lastName
                it[Keys.TELEGRAM] = telegram
                it[Keys.WHATSAPP] = whatsapp
            }
        }
    }

    private fun persist(key: Preferences.Key<String>, value: String, reducer: AppSettingsState.() -> AppSettingsState) {
        _state.update { it.reducer() }
        scope.launch { context.iumrahPreferences.edit { it[key] = value } }
    }

    private object Keys {
        val APPEARANCE = stringPreferencesKey("iumrah.appearance")
        val LANGUAGE = stringPreferencesKey("iumrah.language")
        val FIRST_NAME = stringPreferencesKey("iumrah.profile.firstName")
        val LAST_NAME = stringPreferencesKey("iumrah.profile.lastName")
        val TELEGRAM = stringPreferencesKey("iumrah.profile.telegram")
        val WHATSAPP = stringPreferencesKey("iumrah.profile.whatsapp")
        val ONBOARDING = stringPreferencesKey("iumrah.hasCompletedOnboarding.cinematic.v4")
    }
}
