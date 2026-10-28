package com.dhtoan.dokey.ime

import android.content.Context
import android.content.res.Configuration
import android.os.Build

/** Colours of the keyboard. All values are ARGB ints. */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val bgTop: Int,
    val bgBottom: Int,
    val key: Int,
    val keyFn: Int,
    val keyPressed: Int,
    val text: Int,
    val textDim: Int,
    val accent: Int,
    val onAccent: Int,
    val popup: Int,
    val dark: Boolean,
)

object Themes {
    const val MATERIAL_YOU = "material_you"
    const val SYSTEM = "system"

    private fun c(v: Long) = v.toInt()

    val LIGHT = KeyboardTheme(
        "light", "Sáng", c(0xFFECEFF4), c(0xFFE6E9EF), c(0xFFFFFFFF), c(0xFFD5DBE5), c(0xFFC3CAD6),
        c(0xFF1B1D22), c(0xFF697080), c(0xFF1E6FE8), c(0xFFFFFFFF), c(0xFFFFFFFF), false,
    )
    val DARK = KeyboardTheme(
        "dark", "Tối", c(0xFF1A1B1F), c(0xFF16171A), c(0xFF2E3036), c(0xFF24262B), c(0xFF464951),
        c(0xFFE8EAED), c(0xFF9AA0A6), c(0xFF8AB4F8), c(0xFF0B1D3A), c(0xFF3A3D44), true,
    )

    /** Fixed themes, in the order shown in settings. */
    val PRESETS: List<KeyboardTheme> = listOf(
        LIGHT,
        DARK,
        KeyboardTheme(
            "amoled", "Đen AMOLED", c(0xFF000000), c(0xFF000000), c(0xFF141414), c(0xFF0B0B0B), c(0xFF2C2C2C),
            c(0xFFEDEDED), c(0xFF8A8A8A), c(0xFF4F8CFF), c(0xFFFFFFFF), c(0xFF1F1F1F), true,
        ),
        KeyboardTheme(
            "ocean", "Đại dương", c(0xFF0F3057), c(0xFF00587A), c(0x33FFFFFF), c(0x1FFFFFFF), c(0x55FFFFFF),
            c(0xFFFFFFFF), c(0xB3E0F2FF), c(0xFF4FD1C5), c(0xFF06302D), c(0xFF1B4F72), true,
        ),
        KeyboardTheme(
            "sunset", "Hoàng hôn", c(0xFFFF7E5F), c(0xFF7B4397), c(0x33FFFFFF), c(0x22FFFFFF), c(0x55FFFFFF),
            c(0xFFFFFFFF), c(0xCCFFF1E6), c(0xFFFFD166), c(0xFF4A2A00), c(0xFF8E4A86), true,
        ),
        KeyboardTheme(
            "sakura", "Hoa anh đào", c(0xFFFFF0F5), c(0xFFFDE2EC), c(0xFFFFFFFF), c(0xFFF8CFDD), c(0xFFF2B5C8),
            c(0xFF4A2433), c(0xFF9C6B7C), c(0xFFE75A8C), c(0xFFFFFFFF), c(0xFFFFFFFF), false,
        ),
        KeyboardTheme(
            "forest", "Rừng thông", c(0xFF13261D), c(0xFF0D1C15), c(0xFF22392D), c(0xFF1A2E23), c(0xFF35543F),
            c(0xFFE3F1E8), c(0xFF94B8A2), c(0xFF7BD389), c(0xFF0B2913), c(0xFF2B4636), true,
        ),
        KeyboardTheme(
            "lavender", "Oải hương", c(0xFFF3EEFC), c(0xFFE9E0F8), c(0xFFFFFFFF), c(0xFFDCCFF3), c(0xFFC9B6EC),
            c(0xFF2E2340), c(0xFF7A6C93), c(0xFF7C4DFF), c(0xFFFFFFFF), c(0xFFFFFFFF), false,
        ),
        KeyboardTheme(
            "coffee", "Cà phê sữa", c(0xFFEFE6DA), c(0xFFE4D6C3), c(0xFFFBF7F1), c(0xFFD8C6AE), c(0xFFC7AF91),
            c(0xFF3B2A1E), c(0xFF8A715C), c(0xFF8B5A3C), c(0xFFFFFFFF), c(0xFFFBF7F1), false,
        ),
        KeyboardTheme(
            "midnight", "Đêm sao", c(0xFF141E30), c(0xFF243B55), c(0x26FFFFFF), c(0x18FFFFFF), c(0x4DFFFFFF),
            c(0xFFF1F4FF), c(0xB3C7D2F0), c(0xFFB39DFF), c(0xFF1C1240), c(0xFF2C3E5C), true,
        ),
    )

    fun isNight(context: Context) =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun resolve(context: Context, id: String): KeyboardTheme = when (id) {
        MATERIAL_YOU -> if (Build.VERSION.SDK_INT >= 31) materialYou(context) else resolve(context, SYSTEM)
        SYSTEM -> if (isNight(context)) DARK else LIGHT
        else -> PRESETS.firstOrNull { it.id == id } ?: resolve(context, SYSTEM)
    }

    /** Colours taken from the system palette generated from the wallpaper (Android 12+). */
    private fun materialYou(context: Context): KeyboardTheme {
        fun r(id: Int) = context.getColor(id)
        return if (isNight(context)) {
            KeyboardTheme(
                MATERIAL_YOU, "Material You",
                r(android.R.color.system_neutral1_900), r(android.R.color.system_neutral1_900),
                r(android.R.color.system_neutral2_800), r(android.R.color.system_neutral2_700),
                r(android.R.color.system_neutral2_600), r(android.R.color.system_neutral1_50),
                r(android.R.color.system_neutral2_300), r(android.R.color.system_accent1_200),
                r(android.R.color.system_accent1_800), r(android.R.color.system_neutral2_700), true,
            )
        } else {
            KeyboardTheme(
                MATERIAL_YOU, "Material You",
                r(android.R.color.system_neutral2_100), r(android.R.color.system_neutral2_100),
                r(android.R.color.system_neutral1_10), r(android.R.color.system_accent2_100),
                r(android.R.color.system_neutral2_200), r(android.R.color.system_neutral1_900),
                r(android.R.color.system_neutral2_600), r(android.R.color.system_accent1_600),
                r(android.R.color.system_accent1_0), r(android.R.color.system_neutral1_10), false,
            )
        }
    }
}
