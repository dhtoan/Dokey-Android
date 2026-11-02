package com.dhtoan.dokey.ime

import android.content.SharedPreferences
import androidx.core.content.edit

/** Recently copied texts (newest first), kept on the phone only. */
class ClipboardStore(private val prefs: SharedPreferences) {
    private val items = ArrayList<String>()

    init {
        prefs.getString(KEY, null)?.split(SEP)?.filter { it.isNotEmpty() }?.let { items.addAll(it) }
    }

    val all: List<String> get() = items

    fun add(text: String) {
        val t = text.take(MAX_LENGTH)
        if (t.isBlank()) return
        items.remove(t)
        items.add(0, t)
        while (items.size > MAX_ITEMS) items.removeAt(items.size - 1)
        save()
    }

    fun remove(text: String) {
        if (items.remove(text)) save()
    }

    fun clear() {
        items.clear()
        save()
    }

    private fun save() = prefs.edit { putString(KEY, items.joinToString(SEP)) }

    companion object {
        private const val KEY = "clipboard_items"
        private const val SEP = "\u0001"
        private const val MAX_ITEMS = 25
        private const val MAX_LENGTH = 5000
    }
}
