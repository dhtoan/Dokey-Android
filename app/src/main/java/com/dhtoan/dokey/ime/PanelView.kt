package com.dhtoan.dokey.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** The emoji and clipboard panels, shown in place of the keys. */
@SuppressLint("ViewConstructor")
class PanelView(context: Context, private val listener: Listener) : LinearLayout(context) {

    interface Listener {
        fun onPanelText(text: String)
        fun onPanelDelete()
        fun onPanelClose()
        fun onClipboardRemove(text: String)
        fun onClipboardClear()
    }

    enum class Kind { EMOJI, CLIPBOARD }

    private val density = resources.displayMetrics.density
    private fun dp(v: Int) = (v * density).toInt()

    private val header = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val body = ScrollView(context)
    private var theme: KeyboardTheme = Themes.LIGHT
    private var category = -1
    private var recents: List<String> = emptyList()
    private var clips: List<String> = emptyList()
    private var kind = Kind.EMOJI

    init {
        orientation = VERTICAL
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(44)))
        addView(body, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun show(kind: Kind, theme: KeyboardTheme, recents: List<String>, clips: List<String>) {
        this.kind = kind
        this.theme = theme
        this.recents = recents
        this.clips = clips
        if (category < 0) category = if (recents.isEmpty()) 1 else 0
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(theme.bgTop, theme.bgBottom))
        rebuild()
    }

    private fun rebuild() {
        header.removeAllViews()
        header.addView(button("ABC", bold = true) { listener.onPanelClose() })
        if (kind == Kind.EMOJI) buildEmoji() else buildClipboard()
        body.scrollTo(0, 0)
    }

    private fun button(label: String, bold: Boolean = false, onClick: () -> Unit) = TextView(context).apply {
        text = label
        setTextColor(theme.text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(14), 0, dp(14), 0)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        setOnClickListener { onClick() }
    }

    private fun buildEmoji() {
        val tabs = LinearLayout(context).apply { orientation = HORIZONTAL }
        val names = listOf("🕘") + Emojis.CATEGORIES.map { it.icon }
        names.forEachIndexed { i, icon ->
            tabs.addView(TextView(context).apply {
                text = icon
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                gravity = Gravity.CENTER
                setPadding(dp(10), 0, dp(10), 0)
                alpha = if (i == category) 1f else 0.5f
                if (i == 0 && icon == "🕘") contentDescription = "Gần đây"
                setOnClickListener {
                    category = i
                    rebuild()
                }
            }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
        header.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabs)
        }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        header.addView(button("⌫") { listener.onPanelDelete() })

        val items = if (category == 0) recents else Emojis.CATEGORIES[category - 1].items
        val grid = GridLayout(context).apply { columnCount = COLUMNS }
        for (e in items) {
            grid.addView(TextView(context).apply {
                text = e
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                gravity = Gravity.CENTER
                setOnClickListener { listener.onPanelText(e) }
            }, GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f),
            ).apply {
                width = 0
                height = dp(48)
            })
        }
        body.removeAllViews()
        if (items.isEmpty()) body.addView(message("Chưa có emoji nào dùng gần đây.")) else body.addView(grid)
    }

    private fun buildClipboard() {
        header.addView(TextView(context).apply {
            text = "Bộ nhớ tạm"
            setTextColor(theme.textDim)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER_VERTICAL
        }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        if (clips.isNotEmpty()) header.addView(button("Xoá hết") { listener.onClipboardClear() })

        body.removeAllViews()
        if (clips.isEmpty()) {
            body.addView(message("Những gì bạn sao chép sẽ hiện ở đây. Chạm để dán, giữ để xoá một mục."))
            return
        }
        val list = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        for (clip in clips) {
            list.addView(TextView(context).apply {
                text = clip
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(theme.text)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setPadding(dp(14), dp(10), dp(14), dp(10))
                background = GradientDrawable().apply {
                    setColor(theme.key)
                    cornerRadius = dp(10).toFloat()
                }
                setOnClickListener { listener.onPanelText(clip) }
                setOnLongClickListener {
                    listener.onClipboardRemove(clip)
                    true
                }
            }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
        }
        body.addView(list)
    }

    private fun message(s: String): View = TextView(context).apply {
        text = s
        setTextColor(theme.textDim)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        setPadding(dp(24), dp(32), dp(24), dp(24))
    }

    companion object {
        private const val COLUMNS = 8
    }
}
