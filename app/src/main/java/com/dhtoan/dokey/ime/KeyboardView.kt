package com.dhtoan.dokey.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import kotlin.math.hypot

enum class Shift { OFF, ONCE, LOCKED }

enum class EnterKind { NEWLINE, SEARCH, SEND, GO, NEXT, DONE }

enum class ToolbarAction { LANGUAGE, ONE_HAND, SETTINGS, HIDE, SWAP_SIDE, EXPAND }

data class KeyboardState(
    val layout: Layout = Layouts.alpha(false),
    val shift: Shift = Shift.OFF,
    val vietnamese: Boolean = true,
    val methodLabel: String = "Telex",
    val enter: EnterKind = EnterKind.NEWLINE,
    val theme: KeyboardTheme = Themes.LIGHT,
    val keyHeightDp: Float = 52f,
    val borders: Boolean = true,
    val preview: Boolean = true,
    /** "off", "left" or "right". */
    val oneHand: String = "off",
    val keyStyle: KeyStyle = KeyStyle.FLAT,
    /** Background picture for the photo theme, drawn centre-cropped under [photoDim]. */
    val photo: Bitmap? = null,
    /** 0–100: how much the photo is darkened so the keys stay readable. */
    val photoDim: Int = 35,
)

enum class KeyStyle(val id: String) {
    FLAT("flat"),
    /** Keys with a darker bottom edge, as if they stand out from the board. */
    RAISED("raised"),
    /** Fully rounded keys. */
    ROUND("round"),
    /** Only a thin outline around each key. */
    OUTLINE("outline");

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: FLAT
    }
}

/** The keyboard, drawn on a Canvas: a toolbar strip on top and rows of keys below. */
@SuppressLint("ViewConstructor")
class KeyboardView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        /** A key went down (for haptics and sound). */
        fun onPress(key: Key)
        fun onKey(key: Key)
        fun onAlt(text: String)
        fun onCursorMove(steps: Int)
        fun onSpaceLongPress()
        fun onToolbar(action: ToolbarAction)
    }

    private class KeyRect(val key: Key, val rect: RectF, val hit: RectF)

    private enum class Mode { NORMAL, ALTS, CURSOR, CONSUMED, TOOLBAR }

    var state = KeyboardState()
        set(value) {
            val resize = value.layout.rows.size != field.layout.rows.size || value.keyHeightDp != field.keyHeightDp
            field = value
            rectsDirty = true
            if (resize) requestLayout()
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val toolbarH = dp(44f)
    private val hGap = dp(5f)
    private val vGap = dp(8f)
    private val sidePad = dp(4f)
    private val bottomPad = dp(6f)
    private val radius = dp(8f)
    private var navInset = 0

    private val keys = ArrayList<KeyRect>()
    private val toolbarItems = ArrayList<Pair<ToolbarAction, RectF>>()
    private var rectsDirty = true

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val photoSrc = Rect()
    private val photoDst = Rect()
    private var bgShader: LinearGradient? = null
    private var shaderKey = ""

    // Touch state: one active pointer; a second finger finishes the first (fast two-thumb typing).
    private val handler = Handler(Looper.getMainLooper())
    private var activeId = -1
    private var active: KeyRect? = null
    private var mode = Mode.NORMAL
    private var toolbarHit: ToolbarAction? = null
    private var downX = 0f
    private var cursorX = 0f
    private var alts: List<String> = emptyList()
    private val altRect = RectF()
    private var altCell = 0f
    private var altSel = 0

    private val longPress = Runnable { onLongPress() }
    private val repeatDelete = object : Runnable {
        override fun run() {
            active?.let { listener.onKey(it.key) }
            handler.postDelayed(this, REPEAT_MS)
        }
    }

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            if (bottom != navInset) {
                navInset = bottom
                requestLayout()
            }
            insets
        }
    }

    private val keyH get() = dp(state.keyHeightDp)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val rows = state.layout.rows.size
        val height = toolbarH + rows * (keyH + vGap) + bottomPad + navInset
        setMeasuredDimension(width, height.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        rectsDirty = true
    }

    /** The horizontal span of the keys; narrower in one-handed mode. */
    private fun keyArea(): Pair<Float, Float> {
        val w = width.toFloat()
        val panel = w * ONE_HAND_PANEL
        return when (state.oneHand) {
            "left" -> sidePad to w - panel
            "right" -> panel to w - sidePad
            else -> sidePad to w - sidePad
        }
    }

    private fun computeRects() {
        rectsDirty = false
        keys.clear()
        val (left, right) = keyArea()
        val unit = (right - left) / state.layout.columns
        var y = toolbarH + vGap / 2
        for (row in state.layout.rows) {
            var x = left + row.indent * unit
            val start = keys.size
            for (key in row.keys) {
                val w = key.width * unit
                keys += KeyRect(
                    key,
                    RectF(x + hGap / 2, y, x + w - hGap / 2, y + keyH),
                    RectF(x, y - vGap / 2, x + w, y + keyH + vGap / 2),
                )
                x += w
            }
            if (keys.size > start) {
                keys[start].hit.left = left
                keys[keys.size - 1].hit.right = right
            }
            y += keyH + vGap
        }

        toolbarItems.clear()
        val w = width.toFloat()
        val item = dp(48f)
        toolbarItems += ToolbarAction.LANGUAGE to RectF(dp(8f), 0f, dp(8f) + dp(132f), toolbarH)
        var x = w - dp(4f)
        for (a in listOf(ToolbarAction.HIDE, ToolbarAction.SETTINGS, ToolbarAction.ONE_HAND)) {
            toolbarItems += a to RectF(x - item, 0f, x, toolbarH)
            x -= item
        }
        if (state.oneHand != "off") {
            val panelLeft = if (state.oneHand == "left") w - w * ONE_HAND_PANEL else 0f
            val panelRight = panelLeft + w * ONE_HAND_PANEL
            val mid = toolbarH + (height - toolbarH - navInset) / 2f
            toolbarItems += ToolbarAction.SWAP_SIDE to RectF(panelLeft, mid - dp(56f), panelRight, mid)
            toolbarItems += ToolbarAction.EXPAND to RectF(panelLeft, mid, panelRight, mid + dp(56f))
        }
    }

    // ---- Drawing ----

    override fun onDraw(canvas: Canvas) {
        if (rectsDirty) computeRects()
        val t = state.theme
        drawBackground(canvas, t)
        drawToolbar(canvas, t)
        for (k in keys) drawKey(canvas, k, t)
        val a = active
        if (mode == Mode.ALTS) {
            drawAlts(canvas, t)
        } else if (a != null && mode == Mode.NORMAL && state.preview && a.key.isChar) {
            drawPreview(canvas, a, t)
        }
    }

    private fun drawBackground(canvas: Canvas, t: KeyboardTheme) {
        val photo = state.photo
        if (photo != null) {
            // Centre-crop the picture to the keyboard's shape.
            val scale = maxOf(width.toFloat() / photo.width, height.toFloat() / photo.height)
            val w = (width / scale).toInt()
            val h = (height / scale).toInt()
            val left = (photo.width - w) / 2
            val top = (photo.height - h) / 2
            photoSrc.set(left, top, left + w, top + h)
            photoDst.set(0, 0, width, height)
            canvas.drawBitmap(photo, photoSrc, photoDst, photoPaint)
            canvas.drawColor(Color.argb(state.photoDim * 255 / 100, 0, 0, 0))
            return
        }
        if (t.bgTop == t.bgBottom) {
            canvas.drawColor(t.bgTop)
            return
        }
        val key = "${t.id}:${t.bgTop}:${t.bgBottom}:$height"
        if (key != shaderKey) {
            bgShader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), t.bgTop, t.bgBottom, Shader.TileMode.CLAMP)
            shaderKey = key
        }
        fill.shader = bgShader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        fill.shader = null
    }

    private fun drawToolbar(canvas: Canvas, t: KeyboardTheme) {
        stroke.color = t.textDim
        for ((action, r) in toolbarItems) {
            val pressed = mode == Mode.TOOLBAR && toolbarHit == action
            val cx = r.centerX()
            val cy = r.centerY()
            val s = dp(22f)
            when (action) {
                ToolbarAction.LANGUAGE -> {
                    val pill = RectF(r.left, cy - dp(15f), r.right, cy + dp(15f))
                    fill.color = if (state.vietnamese) t.accent else t.keyFn
                    if (pressed) fill.alpha = 200
                    canvas.drawRoundRect(pill, pill.height() / 2, pill.height() / 2, fill)
                    fill.alpha = 255
                    text.color = if (state.vietnamese) t.onAccent else t.text
                    text.textSize = dp(14f)
                    text.typeface = Typeface.DEFAULT_BOLD
                    val label = if (state.vietnamese) "VI · ${state.methodLabel}" else "EN · English"
                    canvas.drawText(label, cx, baseline(cy), text)
                }
                else -> {
                    if (pressed) {
                        fill.color = t.keyPressed
                        canvas.drawCircle(cx, cy, dp(19f), fill)
                    }
                    when (action) {
                        ToolbarAction.HIDE -> Icons.chevronDown(canvas, cx, cy, s, stroke)
                        ToolbarAction.SETTINGS -> Icons.gear(canvas, cx, cy, s, stroke)
                        ToolbarAction.ONE_HAND -> {
                            stroke.color = if (state.oneHand != "off") t.accent else t.textDim
                            Icons.oneHand(canvas, cx, cy, s, stroke)
                            stroke.color = t.textDim
                        }
                        ToolbarAction.SWAP_SIDE -> Icons.swapSides(canvas, cx, cy, s, stroke)
                        ToolbarAction.EXPAND -> Icons.expand(canvas, cx, cy, s, stroke)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun baseline(cy: Float) = cy - (text.descent() + text.ascent()) / 2

    private fun isFunctionKey(k: Key) = k.code < 0

    private fun drawKey(canvas: Canvas, kr: KeyRect, t: KeyboardTheme) {
        val k = kr.key
        val r = kr.rect
        val pressed = active === kr && mode != Mode.TOOLBAR
        val isEnter = k.code == Codes.ENTER
        val bg = when {
            isEnter -> t.accent
            pressed -> t.keyPressed
            isFunctionKey(k) -> t.keyFn
            else -> t.key
        }
        drawKeyShape(canvas, r, bg, state.borders || isEnter || pressed, isEnter || pressed, t)
        val fg = if (isEnter) t.onAccent else t.text
        val cx = r.centerX()
        val cy = r.centerY()
        val icon = minOf(r.height(), r.width()) * 0.46f
        stroke.color = fg
        when (k.code) {
            Codes.SHIFT -> Icons.shift(canvas, cx, cy, icon, stroke, state.shift != Shift.OFF, state.shift == Shift.LOCKED)
            Codes.DELETE -> Icons.backspace(canvas, cx, cy, icon, stroke)
            Codes.ENTER -> when (state.enter) {
                EnterKind.SEARCH -> Icons.search(canvas, cx, cy, icon, stroke)
                EnterKind.SEND -> Icons.send(canvas, cx, cy, icon, stroke)
                EnterKind.GO, EnterKind.NEXT -> Icons.arrowRight(canvas, cx, cy, icon, stroke)
                EnterKind.DONE -> Icons.check(canvas, cx, cy, icon, stroke)
                EnterKind.NEWLINE -> Icons.enter(canvas, cx, cy, icon, stroke)
            }
            Codes.SPACE -> {
                text.color = t.textDim
                text.textSize = dp(13f)
                text.typeface = Typeface.DEFAULT
                if (state.layout.columns == 10) {
                    val label = if (state.vietnamese) "Tiếng Việt" else "English"
                    canvas.drawText(label, cx, baseline(cy), text)
                }
            }
            Codes.LANGUAGE -> {
                text.color = if (state.vietnamese) t.accent else t.text
                text.textSize = dp(15f)
                text.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(if (state.vietnamese) "VI" else "EN", cx, baseline(cy), text)
            }
            else -> {
                val label = displayLabel(k)
                text.color = fg
                text.typeface = Typeface.DEFAULT
                text.textSize = when {
                    k.code < 0 -> dp(15f)
                    label.length > 1 -> dp(16f)
                    else -> dp(22f)
                }
                canvas.drawText(label, cx, baseline(cy), text)
                val hint = k.alts.firstOrNull()
                if (k.isLetter && hint != null && hint.length == 1 && hint[0].isDigit()) {
                    text.color = t.textDim
                    text.textSize = dp(10f)
                    canvas.drawText(hint, r.right - dp(8f), r.top + dp(13f), text)
                }
            }
        }
    }

    /** Draws the key cap in the current [KeyStyle]. [filled] is false for borderless keys. */
    private fun drawKeyShape(canvas: Canvas, r: RectF, bg: Int, filled: Boolean, solid: Boolean, t: KeyboardTheme) {
        val style = state.keyStyle
        val rad = if (style == KeyStyle.ROUND) minOf(r.width(), r.height()) / 2 else radius
        when {
            style == KeyStyle.OUTLINE && !solid -> {
                outline.color = t.text
                outline.alpha = 90
                canvas.drawRoundRect(r, rad, rad, outline)
            }
            !filled -> Unit
            style == KeyStyle.RAISED -> {
                fill.color = shade(bg, t.dark)
                canvas.drawRoundRect(r.left, r.top + dp(2f), r.right, r.bottom + dp(2f), rad, rad, fill)
                fill.color = bg
                canvas.drawRoundRect(r, rad, rad, fill)
            }
            else -> {
                fill.color = bg
                canvas.drawRoundRect(r, rad, rad, fill)
            }
        }
    }

    /** The colour of a raised key's bottom edge: the key colour, darker. */
    private fun shade(c: Int, dark: Boolean): Int {
        val f = if (dark) 0.45f else 0.72f
        val a = maxOf(Color.alpha(c), 120)
        return Color.argb(a, (Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())
    }

    private fun displayLabel(k: Key) =
        if (k.isLetter && state.shift != Shift.OFF) k.label.uppercase() else k.label

    private fun drawPreview(canvas: Canvas, kr: KeyRect, t: KeyboardTheme) {
        val r = kr.rect
        val w = r.width() + dp(12f)
        val h = keyH * 1.1f
        var top = r.top - h - dp(4f)
        if (top < 0f) top = 0f
        val left = (r.centerX() - w / 2).coerceIn(0f, width - w)
        val box = RectF(left, top, left + w, top + h)
        fill.color = t.popup
        fill.setShadowLayer(dp(6f), 0f, dp(2f), 0x40000000)
        canvas.drawRoundRect(box, radius * 1.4f, radius * 1.4f, fill)
        fill.clearShadowLayer()
        text.color = t.text
        text.textSize = dp(30f)
        text.typeface = Typeface.DEFAULT
        canvas.drawText(displayLabel(kr.key), box.centerX(), baseline(box.centerY()), text)
    }

    private fun drawAlts(canvas: Canvas, t: KeyboardTheme) {
        fill.color = t.popup
        fill.setShadowLayer(dp(8f), 0f, dp(2f), 0x50000000)
        canvas.drawRoundRect(altRect, radius * 1.4f, radius * 1.4f, fill)
        fill.clearShadowLayer()
        text.textSize = dp(22f)
        text.typeface = Typeface.DEFAULT
        alts.forEachIndexed { i, s ->
            val l = altRect.left + i * altCell
            val cell = RectF(l + dp(3f), altRect.top + dp(3f), l + altCell - dp(3f), altRect.bottom - dp(3f))
            if (i == altSel) {
                fill.color = t.accent
                canvas.drawRoundRect(cell, radius, radius, fill)
            }
            text.color = if (i == altSel) t.onAccent else t.text
            canvas.drawText(s, cell.centerX(), baseline(cell.centerY()), text)
        }
    }

    // ---- Touch ----

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                if (activeId != -1) finish(commit = true)
                start(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> {
                val i = e.findPointerIndex(activeId)
                if (i >= 0) move(e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (e.getPointerId(e.actionIndex) == activeId) finish(commit = true)
            }
            MotionEvent.ACTION_CANCEL -> finish(commit = false)
        }
        return true
    }

    private fun toolbarAt(x: Float, y: Float) = toolbarItems.firstOrNull { it.second.contains(x, y) }?.first

    private fun keyAt(x: Float, y: Float): KeyRect? {
        keys.firstOrNull { it.hit.contains(x, y) }?.let { return it }
        if (y < toolbarH) return null
        val (left, right) = keyArea()
        if (x < left || x > right) return null
        return keys.minByOrNull { hypot(it.rect.centerX() - x, it.rect.centerY() - y) }
    }

    private fun start(id: Int, x: Float, y: Float) {
        if (rectsDirty) computeRects()
        activeId = id
        downX = x
        cursorX = x
        mode = Mode.NORMAL
        val tool = toolbarAt(x, y)
        val k = if (tool == null) keyAt(x, y) else null
        if (k == null) {
            toolbarHit = tool
            mode = Mode.TOOLBAR
            invalidate()
            return
        }
        active = k
        listener.onPress(k.key)
        when {
            k.key.code == Codes.DELETE -> {
                mode = Mode.CONSUMED
                listener.onKey(k.key)
                handler.postDelayed(repeatDelete, REPEAT_START_MS)
            }
            k.key.alts.isNotEmpty() || k.key.code == Codes.SPACE -> handler.postDelayed(longPress, LONG_PRESS_MS)
        }
        invalidate()
    }

    private fun move(x: Float, y: Float) {
        val a = active ?: return
        when (mode) {
            Mode.ALTS -> {
                val i = ((x - altRect.left) / altCell).toInt().coerceIn(0, alts.size - 1)
                if (i != altSel) {
                    altSel = i
                    invalidate()
                }
            }
            Mode.CURSOR -> {
                val step = dp(10f)
                val n = ((x - cursorX) / step).toInt()
                if (n != 0) {
                    listener.onCursorMove(n)
                    cursorX += n * step
                }
            }
            Mode.NORMAL -> {
                if (a.key.code == Codes.SPACE) {
                    if (abs(x - downX) > dp(16f)) {
                        handler.removeCallbacks(longPress)
                        mode = Mode.CURSOR
                        cursorX = x
                    }
                    return
                }
                if (!a.hit.contains(x, y) && a.key.isChar) {
                    val n = keyAt(x, y)
                    if (n != null && n !== a && n.key.isChar) {
                        handler.removeCallbacks(longPress)
                        active = n
                        if (n.key.alts.isNotEmpty()) handler.postDelayed(longPress, LONG_PRESS_MS)
                        invalidate()
                    }
                }
            }
            else -> Unit
        }
    }

    private fun onLongPress() {
        val a = active ?: return
        if (a.key.code == Codes.SPACE) {
            mode = Mode.CONSUMED
            invalidate()
            listener.onSpaceLongPress()
            return
        }
        alts = a.key.alts.map { if (state.shift != Shift.OFF && it[0].isLetter()) it.uppercase() else it }
        altCell = maxOf(a.rect.width(), dp(38f))
        val total = altCell * alts.size
        val left = (a.rect.centerX() - altCell / 2).coerceAtMost(width - sidePad - total).coerceAtLeast(sidePad)
        var top = a.rect.top - keyH - dp(10f)
        if (top < 0f) top = 0f
        altRect.set(left, top, left + total, top + keyH + dp(4f))
        altSel = 0
        mode = Mode.ALTS
        listener.onPress(a.key)
        invalidate()
    }

    private fun finish(commit: Boolean) {
        handler.removeCallbacks(longPress)
        handler.removeCallbacks(repeatDelete)
        val a = active
        val m = mode
        val tool = toolbarHit
        val alt = alts.getOrNull(altSel)
        activeId = -1
        active = null
        mode = Mode.NORMAL
        toolbarHit = null
        alts = emptyList()
        invalidate()
        if (!commit) return
        when (m) {
            Mode.NORMAL -> a?.let { listener.onKey(it.key) }
            Mode.ALTS -> alt?.let { listener.onAlt(it) }
            Mode.TOOLBAR -> tool?.let { listener.onToolbar(it) }
            else -> Unit
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val LONG_PRESS_MS = 350L
        private const val REPEAT_START_MS = 400L
        private const val REPEAT_MS = 50L
        private const val ONE_HAND_PANEL = 0.17f
    }
}
