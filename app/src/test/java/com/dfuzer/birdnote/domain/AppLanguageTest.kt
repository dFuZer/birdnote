package com.dfuzer.birdnote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun savedChoiceOverridesTheDeviceLocale() {
        assertEquals(
            AppLanguage.GERMAN,
            resolveAppLanguage("de", DeviceLocale("en", "US")),
        )
        assertEquals(
            AppLanguage.FRENCH,
            resolveAppLanguage("fr", DeviceLocale("en", "US")),
        )
        assertEquals(
            AppLanguage.SPANISH_LATIN_AMERICA,
            resolveAppLanguage("es-419", DeviceLocale("fr", "FR")),
        )
        assertEquals(
            AppLanguage.ARABIC,
            resolveAppLanguage("ar", DeviceLocale("en", "US")),
        )
    }

    @Test
    fun savedChoiceMatchesTagsIgnoringCase() {
        assertEquals(AppLanguage.PORTUGUESE_BRAZIL, storedAppLanguage("pt-br"))
        assertEquals(AppLanguage.ENGLISH, storedAppLanguage("EN"))
        assertEquals(AppLanguage.ARABIC, storedAppLanguage("AR"))
        assertEquals(AppLanguage.HINDI, storedAppLanguage("hi"))
        assertEquals(AppLanguage.RUSSIAN, storedAppLanguage("ru"))
        assertEquals(AppLanguage.TURKISH, storedAppLanguage("tr"))
        assertNull(storedAppLanguage(null))
        assertNull(storedAppLanguage(""))
        assertNull(storedAppLanguage("sv"))
        assertNull(storedAppLanguage("ENGLISH"))
    }

    @Test
    fun supportedDeviceLocalesResolveBeforeAnyChoice() {
        assertEquals(AppLanguage.ENGLISH, resolveAppLanguage(null, DeviceLocale("en", "US")))
        assertEquals(AppLanguage.ENGLISH, resolveAppLanguage(null, DeviceLocale("en", "GB")))
        assertEquals(AppLanguage.GERMAN, resolveAppLanguage(null, DeviceLocale("de", "AT")))
        assertEquals(AppLanguage.ITALIAN, resolveAppLanguage(null, DeviceLocale("it", "IT")))
        assertEquals(AppLanguage.JAPANESE, resolveAppLanguage(null, DeviceLocale("ja", "JP")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("fr", "CA")))
        assertEquals(AppLanguage.INDONESIAN, resolveAppLanguage(null, DeviceLocale("id", "ID")))
        assertEquals(AppLanguage.INDONESIAN, resolveAppLanguage(null, DeviceLocale("in", "")))
        assertEquals(
            AppLanguage.SPANISH_LATIN_AMERICA,
            resolveAppLanguage(null, DeviceLocale("es", "419")),
        )
        assertEquals(
            AppLanguage.SPANISH_LATIN_AMERICA,
            resolveAppLanguage(null, DeviceLocale("es", "MX")),
        )
        assertEquals(
            AppLanguage.SPANISH_LATIN_AMERICA,
            resolveAppLanguage(null, DeviceLocale("es", "AR")),
        )
        assertEquals(
            AppLanguage.PORTUGUESE_BRAZIL,
            resolveAppLanguage(null, DeviceLocale("pt", "BR")),
        )
        assertEquals(AppLanguage.ARABIC, resolveAppLanguage(null, DeviceLocale("ar", "SA")))
        assertEquals(AppLanguage.ARABIC, resolveAppLanguage(null, DeviceLocale("ar", "EG")))
        assertEquals(AppLanguage.ARABIC, resolveAppLanguage(null, DeviceLocale("ar", "")))
        assertEquals(AppLanguage.HINDI, resolveAppLanguage(null, DeviceLocale("hi", "IN")))
        assertEquals(AppLanguage.RUSSIAN, resolveAppLanguage(null, DeviceLocale("ru", "RU")))
        assertEquals(AppLanguage.RUSSIAN, resolveAppLanguage(null, DeviceLocale("ru", "")))
        assertEquals(AppLanguage.TURKISH, resolveAppLanguage(null, DeviceLocale("tr", "TR")))
    }

    @Test
    fun unsupportedDeviceLocalesFallBackToFrench() {
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("sv", "SE")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("zh", "CN")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("es", "ES")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("es", "")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("pt", "PT")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("pt", "")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage(null, DeviceLocale("", "")))
    }

    @Test
    fun invalidSavedChoiceFallsBackToTheDeviceThenFrench() {
        assertEquals(AppLanguage.ITALIAN, resolveAppLanguage("nope", DeviceLocale("it", "IT")))
        assertEquals(AppLanguage.FRENCH, resolveAppLanguage("nope", DeviceLocale("sv", "SE")))
        assertEquals(AppLanguage.JAPANESE, resolveAppLanguage("", DeviceLocale("ja", "JP")))
    }
}
