package com.example.birdnote.domain

import java.util.Locale

enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    SPANISH_LATIN_AMERICA("es-419"),
    PORTUGUESE_BRAZIL("pt-BR"),
    INDONESIAN("id"),
    GERMAN("de"),
    ITALIAN("it"),
    JAPANESE("ja"),
    FRENCH("fr"),
}

data class DeviceLocale(
    val language: String,
    val region: String,
)

/**
 * A saved choice wins over the phone. Otherwise a supported device locale is
 * used, and anything else falls back to French.
 */
fun resolveAppLanguage(storedTag: String?, device: DeviceLocale): AppLanguage =
    storedAppLanguage(storedTag) ?: device.toAppLanguage() ?: AppLanguage.FRENCH

fun storedAppLanguage(value: String?): AppLanguage? =
    AppLanguage.entries.firstOrNull { it.tag.equals(value, ignoreCase = true) }

private fun DeviceLocale.toAppLanguage(): AppLanguage? = when {
    language.equals("en", ignoreCase = true) -> AppLanguage.ENGLISH
    language.equals("de", ignoreCase = true) -> AppLanguage.GERMAN
    language.equals("it", ignoreCase = true) -> AppLanguage.ITALIAN
    language.equals("ja", ignoreCase = true) -> AppLanguage.JAPANESE
    language.equals("fr", ignoreCase = true) -> AppLanguage.FRENCH
    language.equals("id", ignoreCase = true) || language.equals("in", ignoreCase = true) ->
        AppLanguage.INDONESIAN
    language.equals("es", ignoreCase = true) &&
        region.uppercase(Locale.ROOT) in LATIN_AMERICAN_SPANISH ->
        AppLanguage.SPANISH_LATIN_AMERICA
    language.equals("pt", ignoreCase = true) && region.equals("BR", ignoreCase = true) ->
        AppLanguage.PORTUGUESE_BRAZIL
    else -> null
}

/** Regions whose parent locale is es-419, plus the 419 region itself. */
private val LATIN_AMERICAN_SPANISH = setOf(
    "419",
    "AR",
    "BO",
    "BR",
    "BZ",
    "CL",
    "CO",
    "CR",
    "CU",
    "DO",
    "EC",
    "GT",
    "HN",
    "MX",
    "NI",
    "PA",
    "PE",
    "PR",
    "PY",
    "SV",
    "US",
    "UY",
    "VE",
)
