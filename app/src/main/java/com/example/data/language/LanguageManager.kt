package com.example.data.language

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Supported Language Model for Hajri Card.
 *
 * Designed to be modular and scalable so additional Indian or global languages
 * (such as Bengali, Gujarati, Punjabi, Malayalam, Odia, Urdu, Assamese)
 * can be added effortlessly by simply adding an entry to [SUPPORTED_LANGUAGES]
 * and the corresponding strings.xml resource folder.
 */
data class AppLanguage(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val flagHint: String = ""
)

object LanguageManager {
    private const val PREFS_NAME = "hajri_language_prefs"
    private const val KEY_LANGUAGE_CODE = "selected_language_code"
    const val DEFAULT_LANGUAGE_CODE = "en"

    /**
     * Currently supported languages in Hajri Card.
     */
    val SUPPORTED_LANGUAGES: List<AppLanguage> = listOf(
        AppLanguage(code = "en", englishName = "English", nativeName = "English"),
        AppLanguage(code = "hi", englishName = "Hindi", nativeName = "हिन्दी"),
        AppLanguage(code = "mr", englishName = "Marathi", nativeName = "मराठी"),
        AppLanguage(code = "ta", englishName = "Tamil", nativeName = "தமிழ்"),
        AppLanguage(code = "te", englishName = "Telugu", nativeName = "తెలుగు"),
        AppLanguage(code = "kn", englishName = "Kannada", nativeName = "ಕನ್ನಡ")
    )

    private val _currentLanguage = MutableStateFlow(SUPPORTED_LANGUAGES.first())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private var isInitialized = false

    /**
     * Initialize LanguageManager on Application / Activity startup.
     * Loads persisted language or detects supported device locale.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        val savedLang = getSavedLanguage(context)
        _currentLanguage.value = savedLang
        applyLocaleToApp(context, savedLang.code)
        isInitialized = true
    }

    /**
     * Retrieves saved language from SharedPreferences.
     * If no language is saved, checks if device's default locale matches any supported language.
     */
    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = getPrefs(context)
        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, null)

        if (savedCode != null) {
            val matched = SUPPORTED_LANGUAGES.find { it.code.equals(savedCode, ignoreCase = true) }
            if (matched != null) return matched
        }

        // Check device default language
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        val systemLanguageCode = systemLocale?.language?.lowercase(Locale.ROOT) ?: DEFAULT_LANGUAGE_CODE
        return SUPPORTED_LANGUAGES.find { it.code == systemLanguageCode }
            ?: SUPPORTED_LANGUAGES.first { it.code == DEFAULT_LANGUAGE_CODE }
    }

    /**
     * Updates the app language, persists the preference, and notifies collectors.
     */
    fun setLanguage(context: Context, languageCode: String): AppLanguage {
        val selected = SUPPORTED_LANGUAGES.find { it.code.equals(languageCode, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.first()

        getPrefs(context).edit()
            .putString(KEY_LANGUAGE_CODE, selected.code)
            .apply()

        _currentLanguage.value = selected
        applyLocaleToApp(context, selected.code)
        return selected
    }

    /**
     * Creates a localized Context configured for the given language code.
     */
    fun getLocalizedContext(baseContext: Context, languageCode: String = _currentLanguage.value.code): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(baseContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale)
        }

        return baseContext.createConfigurationContext(config)
    }

    private fun applyLocaleToApp(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
