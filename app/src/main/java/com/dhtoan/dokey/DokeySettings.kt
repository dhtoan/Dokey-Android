package com.dhtoan.dokey

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import com.dhtoan.dokey.engine.Abbreviations
import com.dhtoan.dokey.engine.TypingMethod
import com.dhtoan.dokey.ime.Themes

/** Preferences shared by the keyboard service and the settings screen. */
class DokeySettings(context: Context) {
    val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    val method: TypingMethod
        get() = if (prefs.getString(METHOD, "telex") == "vni") TypingMethod.VNI else TypingMethod.TELEX

    var vietnamese: Boolean
        get() = prefs.getBoolean(VIETNAMESE, true)
        set(v) = prefs.edit { putBoolean(VIETNAMESE, v) }

    val modernTone get() = prefs.getBoolean(MODERN_TONE, true)
    val spellCheck get() = prefs.getBoolean(SPELL_CHECK, true)
    val autoCapitalize get() = prefs.getBoolean(AUTO_CAP, true)
    val doubleSpacePeriod get() = prefs.getBoolean(DOUBLE_SPACE, true)

    val theme: String get() = prefs.getString(THEME, defaultTheme) ?: defaultTheme
    val keyHeight: String get() = prefs.getString(KEY_HEIGHT, "medium") ?: "medium"
    val keyBorders get() = prefs.getBoolean(KEY_BORDERS, true)
    val keyPreview get() = prefs.getBoolean(KEY_PREVIEW, true)
    val keyStyle: String get() = prefs.getString(KEY_STYLE, "flat") ?: "flat"
    val photoDim get() = prefs.getInt(PHOTO_DIM, 35)
    /** Changes whenever a new background photo is saved, so the keyboard reloads it. */
    val photoVersion get() = prefs.getLong(PHOTO_VERSION, 0L)

    /** On by default for VNI, which types tones with digits. */
    val numberRow get() = prefs.getBoolean(NUMBER_ROW, method == TypingMethod.VNI)

    var oneHand: String
        get() = prefs.getString(ONE_HAND, "off") ?: "off"
        set(v) = prefs.edit { putString(ONE_HAND, v) }

    val suggestions get() = prefs.getBoolean(SUGGESTIONS, true)
    val swipeTyping get() = prefs.getBoolean(SWIPE, true)
    val toneGestures get() = prefs.getBoolean(TONE_GESTURES, true)
    val abbreviations get() = prefs.getBoolean(ABBREVIATIONS, true)
    val abbreviationText: String get() = prefs.getString(ABBREVIATION_TEXT, null) ?: Abbreviations.DEFAULT
    val clipboardHistory get() = prefs.getBoolean(CLIPBOARD_HISTORY, true)

    val vibrate get() = prefs.getBoolean(VIBRATE, true)
    val vibrateMs get() = prefs.getInt(VIBRATE_MS, 12)
    val sound get() = prefs.getBoolean(SOUND, false)

    companion object {
        const val NAME = "dokey"
        const val METHOD = "method"
        const val VIETNAMESE = "vietnamese"
        const val MODERN_TONE = "modern_tone"
        const val SPELL_CHECK = "spell_check"
        const val AUTO_CAP = "auto_cap"
        const val DOUBLE_SPACE = "double_space_period"
        const val THEME = "theme"
        const val KEY_HEIGHT = "key_height"
        const val KEY_BORDERS = "key_borders"
        const val KEY_PREVIEW = "key_preview"
        const val KEY_STYLE = "key_style"
        const val PHOTO_DIM = "photo_dim"
        const val PHOTO_VERSION = "photo_version"
        /** The background photo, downscaled, in the app's private files. */
        const val PHOTO_FILE = "keyboard_photo.jpg"
        const val NUMBER_ROW = "number_row"
        const val ONE_HAND = "one_hand"
        const val SUGGESTIONS = "suggestions"
        const val SWIPE = "swipe_typing"
        const val TONE_GESTURES = "tone_gestures"
        const val ABBREVIATIONS = "abbreviations"
        const val ABBREVIATION_TEXT = "abbreviation_text"
        const val CLIPBOARD_HISTORY = "clipboard_history"
        /** Typing data that is not a setting (clipboard, recent emoji), kept apart from the settings. */
        const val STATE_NAME = "dokey_state"
        const val VIBRATE = "vibrate"
        const val VIBRATE_MS = "vibrate_ms"
        const val SOUND = "sound"

        val defaultTheme = if (Build.VERSION.SDK_INT >= 31) Themes.MATERIAL_YOU else Themes.SYSTEM
    }
}
