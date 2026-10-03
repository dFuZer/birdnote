package com.dfuzer.birdnote

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.DeviceLocale
import com.dfuzer.birdnote.domain.resolveAppLanguage
import java.util.Locale

internal const val KEY_APP_LANGUAGE = "app_language"

fun Context.withResolvedLocale(): Context {
    val storedTag = getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_APP_LANGUAGE, null)
    return withLocale(resolveAppLanguage(storedTag, systemDeviceLocale()))
}

fun systemDeviceLocale(): DeviceLocale {
    val locales = Resources.getSystem().configuration.locales
    if (locales.isEmpty) return DeviceLocale("", "")
    val locale = locales[0]
    return DeviceLocale(locale.language, locale.country)
}

fun Context.findActivity(): Activity {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    error("Settings must be hosted by an activity")
}

private fun Context.withLocale(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return createConfigurationContext(config)
}
