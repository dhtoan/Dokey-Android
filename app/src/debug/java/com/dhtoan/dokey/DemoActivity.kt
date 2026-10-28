package com.dhtoan.dokey

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.edit

/**
 * A text field with the keyboard open, for CI screenshots. Extras set preferences first:
 * `theme`, `method` (telex/vni), `one_hand` (off/left/right), `number_row` (true/false), `text`.
 */
class DemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = DokeySettings(this).prefs
        prefs.edit {
            putString(DokeySettings.THEME, intent.getStringExtra("theme") ?: DokeySettings.defaultTheme)
            putString(DokeySettings.METHOD, intent.getStringExtra("method") ?: "telex")
            putString(DokeySettings.ONE_HAND, intent.getStringExtra("one_hand") ?: "off")
            putBoolean(DokeySettings.VIETNAMESE, true)
            intent.getStringExtra("number_row")?.let { putBoolean(DokeySettings.NUMBER_ROW, it == "true") }
                ?: remove(DokeySettings.NUMBER_ROW)
        }
        val pad = (24 * resources.displayMetrics.density).toInt()
        val title = TextView(this).apply {
            text = "Dokey"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        }
        val field = EditText(this).apply {
            setText(intent.getStringExtra("text") ?: "Xin chào! Gõ tiếng Việt thật nhanh với Dokey ")
            textSize = 20f
            setSelection(text.length)
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(pad, pad * 2, pad, pad)
                addView(title)
                addView(field)
            },
        )
        field.requestFocus()
    }
}
