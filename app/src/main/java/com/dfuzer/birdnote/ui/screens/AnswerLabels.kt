package com.dfuzer.birdnote.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.ChordQuality
import com.dfuzer.birdnote.domain.IntervalName

@Composable
fun IntervalName.label(): String = stringResource(labelRes())

@Composable
fun ChordQuality.label(): String = stringResource(labelRes())

@Composable
fun AppLanguage.label(): String = stringResource(labelRes())

@StringRes
private fun IntervalName.labelRes(): Int = when (this) {
    IntervalName.SECONDE -> R.string.interval_second
    IntervalName.TIERCE -> R.string.interval_third
    IntervalName.QUARTE -> R.string.interval_fourth
    IntervalName.QUINTE -> R.string.interval_fifth
    IntervalName.SIXTE -> R.string.interval_sixth
    IntervalName.SEPTIEME -> R.string.interval_seventh
    IntervalName.OCTAVE -> R.string.interval_octave
    IntervalName.NEUVIEME -> R.string.interval_ninth
}

@StringRes
private fun ChordQuality.labelRes(): Int = when (this) {
    ChordQuality.MAJOR -> R.string.chord_major
    ChordQuality.MINOR -> R.string.chord_minor
    ChordQuality.AUGMENTED -> R.string.chord_augmented
    ChordQuality.DIMINISHED -> R.string.chord_diminished
    ChordQuality.DOMINANT_7 -> R.string.chord_dominant_7
    ChordQuality.MAJOR_7 -> R.string.chord_major_7
    ChordQuality.MINOR_7 -> R.string.chord_minor_7
    ChordQuality.HALF_DIMINISHED_7 -> R.string.chord_half_diminished_7
    ChordQuality.DIMINISHED_7 -> R.string.chord_diminished_7
}

@StringRes
internal fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.ENGLISH -> R.string.language_name_en
    AppLanguage.SPANISH_LATIN_AMERICA -> R.string.language_name_es
    AppLanguage.PORTUGUESE_BRAZIL -> R.string.language_name_pt
    AppLanguage.INDONESIAN -> R.string.language_name_id
    AppLanguage.GERMAN -> R.string.language_name_de
    AppLanguage.ITALIAN -> R.string.language_name_it
    AppLanguage.JAPANESE -> R.string.language_name_ja
    AppLanguage.FRENCH -> R.string.language_name_fr
    AppLanguage.ARABIC -> R.string.language_name_ar
    AppLanguage.HINDI -> R.string.language_name_hi
    AppLanguage.RUSSIAN -> R.string.language_name_ru
    AppLanguage.TURKISH -> R.string.language_name_tr
}
