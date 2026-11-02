package com.dhtoan.dokey.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.dhtoan.dokey.DokeySettings
import com.dhtoan.dokey.engine.Abbreviations
import com.dhtoan.dokey.engine.Dictionary
import com.dhtoan.dokey.engine.SwipeDecoder
import com.dhtoan.dokey.engine.TypingMethod
import com.dhtoan.dokey.engine.VietEngine
import com.dhtoan.dokey.engine.VietText
import com.dhtoan.dokey.settings.SettingsActivity
import java.io.File

class DokeyIme : InputMethodService(), KeyboardView.Listener, PanelView.Listener {

    private lateinit var settings: DokeySettings
    private val engine = VietEngine()
    private var view: KeyboardView? = null
    private var panel: PanelView? = null
    private var panelKind: PanelView.Kind? = null

    @Volatile private var dictionary: Dictionary? = null
    @Volatile private var decoder: SwipeDecoder? = null
    private var abbreviations = Abbreviations("")
    private var abbreviationSource: String? = null
    /** Dictionary key of the syllable before the cursor, for predictions; null after punctuation. */
    private var previousKey: String? = null
    /** What the suggestion bar shows, and whether tapping one replaces the word being typed. */
    private var suggestions: List<String> = emptyList()
    private var swipeCandidates: List<String>? = null

    private lateinit var clipboard: ClipboardStore
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClip() }

    private var layoutId = "alpha"
    private var shift = Shift.OFF
    private var lastShiftTap = 0L
    /** Fields where Vietnamese transforms would get in the way (passwords, e-mail, URLs, numbers). */
    private var plainField = false
    private var lastWasSpace = false
    private var photo: Bitmap? = null
    private var photoVersion = -1L

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applySettings() }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    private val audio: AudioManager? by lazy { getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    override fun onCreate() {
        super.onCreate()
        settings = DokeySettings(this)
        settings.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        clipboard = ClipboardStore(statePrefs)
        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.addPrimaryClipChangedListener(clipListener)
        Thread {
            val d = Dictionary.bundled()
            decoder = SwipeDecoder(d)
            dictionary = d
        }.start()
    }

    override fun onDestroy() {
        settings.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    private val statePrefs by lazy { getSharedPreferences(DokeySettings.STATE_NAME, Context.MODE_PRIVATE) }

    override fun onCreateInputView(): View {
        val v = KeyboardView(this, this)
        val p = PanelView(this, this).apply { visibility = View.GONE }
        view = v
        panel = p
        applySettings()
        return FrameLayout(this).apply {
            addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(p, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        engine.reset()
        lastWasSpace = false
        previousKey = null
        swipeCandidates = null
        closePanel()
        captureClip()
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        plainField = when (cls) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> true
            InputType.TYPE_CLASS_TEXT -> variation in PLAIN_TEXT_VARIATIONS
            else -> false
        }
        layoutId = if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE ||
            cls == InputType.TYPE_CLASS_DATETIME
        ) "numpad" else "alpha"
        shift = Shift.OFF
        updateAutoShift()
        applySettings()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        currentInputConnection?.let { commitWord(it) }
        super.onFinishInputView(finishingInput)
    }

    override fun onFinishInput() {
        engine.reset()
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        // The cursor moved away from the word being typed (tap elsewhere, app changed the text).
        if (!engine.isEmpty && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            engine.reset()
            previousKey = null
            swipeCandidates = null
            currentInputConnection?.finishComposingText()
            refreshSuggestions()
        }
    }

    /** Pushes preferences and the current state into the engine and the view. */
    private fun applySettings() {
        engine.method = settings.method
        engine.modernTone = settings.modernTone
        engine.spellCheck = settings.spellCheck
        val source = if (settings.abbreviations) settings.abbreviationText else ""
        if (source != abbreviationSource) {
            abbreviationSource = source
            abbreviations = Abbreviations(source)
        }
        if (::clipboard.isInitialized && !settings.clipboardHistory && clipboard.all.isNotEmpty()) clipboard.clear()
        val v = view ?: return
        val layout = when (layoutId) {
            "symbols" -> Layouts.SYMBOLS
            "more_symbols" -> Layouts.MORE_SYMBOLS
            "numpad" -> Layouts.NUMPAD
            else -> Layouts.alpha(settings.numberRow)
        }
        v.state = KeyboardState(
            layout = layout,
            shift = shift,
            vietnamese = settings.vietnamese,
            methodLabel = if (settings.method == TypingMethod.VNI) "VNI" else "Telex",
            enter = enterKind(),
            theme = Themes.resolve(this, settings.theme),
            keyHeightDp = when (settings.keyHeight) {
                "small" -> 46f
                "large" -> 58f
                else -> 52f
            },
            borders = settings.keyBorders,
            preview = settings.keyPreview,
            oneHand = settings.oneHand,
            keyStyle = KeyStyle.of(settings.keyStyle),
            photo = if (settings.theme == Themes.PHOTO) loadPhoto() else null,
            photoDim = settings.photoDim,
            suggestions = suggestions,
            swipeTyping = settings.swipeTyping && useEngine && layoutId == "alpha",
            toneGestures = settings.toneGestures && useEngine,
        )
    }

    /** Recomputes the suggestion bar for the word being typed, or predictions after a word. */
    private fun refreshSuggestions() {
        val dict = dictionary
        val modern = settings.modernTone
        suggestions = when {
            !useEngine || layoutId != "alpha" -> emptyList()
            !engine.isEmpty -> {
                val typed = engine.composing
                val out = ArrayList<String>()
                abbreviations.expand(typed)?.let { out.add(it) }
                swipeCandidates?.let { c -> c.filter { it !in out }.forEach { out.add(it) } }
                if (settings.suggestions && dict != null && swipeCandidates == null) {
                    dict.suggest(typed, previousKey, 3, modern).filter { it !in out }.forEach { out.add(it) }
                }
                if (out.isNotEmpty() && typed !in out) out.add(minOf(1, out.size), typed)
                out.take(3)
            }
            settings.suggestions && dict != null && previousKey != null ->
                dict.predict(previousKey!!, 3, modern)
            else -> emptyList()
        }
        view?.let { it.state = it.state.copy(suggestions = suggestions) }
    }

    private fun keyOfLastSyllable(text: String?): String? {
        val last = text?.trim()?.split(' ')?.lastOrNull() ?: return null
        return dictionary?.keyOf(last)
    }

    private fun captureClip() {
        if (!::clipboard.isInitialized || !settings.clipboardHistory) return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = try {
            cm.primaryClip
        } catch (e: SecurityException) {
            null
        } ?: return
        if (Build.VERSION.SDK_INT >= 24 &&
            clip.description?.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true
        ) return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
        if (clipboard.all.firstOrNull() != text) clipboard.add(text)
    }

    /** The saved background photo, decoded once per new picture. */
    private fun loadPhoto(): Bitmap? {
        val version = settings.photoVersion
        if (version != photoVersion) {
            photoVersion = version
            val file = File(filesDir, DokeySettings.PHOTO_FILE)
            photo = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
        }
        return photo
    }

    private fun refreshView() = applySettings()

    private fun enterKind(): EnterKind {
        val info = currentInputEditorInfo ?: return EnterKind.NEWLINE
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return EnterKind.NEWLINE
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> EnterKind.SEARCH
            EditorInfo.IME_ACTION_SEND -> EnterKind.SEND
            EditorInfo.IME_ACTION_GO -> EnterKind.GO
            EditorInfo.IME_ACTION_NEXT -> EnterKind.NEXT
            EditorInfo.IME_ACTION_DONE -> EnterKind.DONE
            else -> EnterKind.NEWLINE
        }
    }

    private val useEngine get() = settings.vietnamese && !plainField

    // ---- KeyboardView.Listener ----

    override fun onPress(key: Key) {
        if (settings.vibrate) {
            val ms = settings.vibrateMs.toLong()
            val vib = vibrator
            if (vib != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(ms)
                }
            }
        }
        if (settings.sound) {
            val fx = when (key.code) {
                Codes.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                Codes.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                Codes.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio?.playSoundEffect(fx, -1f)
        }
    }

    override fun onKey(key: Key) {
        val ic = currentInputConnection ?: return
        val wasSpace = lastWasSpace
        lastWasSpace = false
        when (key.code) {
            Codes.SHIFT -> handleShift()
            Codes.DELETE -> handleDelete(ic)
            Codes.SYMBOLS -> setLayout("symbols")
            Codes.MORE_SYMBOLS -> setLayout("more_symbols")
            Codes.ALPHA -> setLayout("alpha")
            Codes.LANGUAGE -> toggleLanguage(ic)
            Codes.ENTER -> handleEnter(ic)
            Codes.SPACE -> handleSpace(ic, wasSpace)
            else -> typeChar(key.code.toChar(), ic)
        }
    }

    override fun onAlt(text: String) {
        val ic = currentInputConnection ?: return
        lastWasSpace = false
        if (text.length == 1) typeChar(text[0], ic, applyShift = false) else {
            commitWord(ic)
            ic.commitText(text, 1)
        }
    }

    override fun onCursorMove(steps: Int) {
        val ic = currentInputConnection ?: return
        commitWord(ic)
        previousKey = null
        val code = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(steps)) { sendDownUpKeyEvents(code) }
    }

    override fun onSpaceLongPress() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    override fun onToolbar(action: ToolbarAction) {
        when (action) {
            ToolbarAction.LANGUAGE -> currentInputConnection?.let { toggleLanguage(it) }
            ToolbarAction.ONE_HAND -> settings.oneHand = if (settings.oneHand == "off") "right" else "off"
            ToolbarAction.SWAP_SIDE -> settings.oneHand = if (settings.oneHand == "left") "right" else "left"
            ToolbarAction.EXPAND -> settings.oneHand = "off"
            ToolbarAction.SETTINGS -> {
                currentInputConnection?.let { commitWord(it) }
                startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ToolbarAction.HIDE -> requestHideSelf(0)
            ToolbarAction.EMOJI -> openPanel(PanelView.Kind.EMOJI)
            ToolbarAction.CLIPBOARD -> openPanel(PanelView.Kind.CLIPBOARD)
        }
    }

    override fun onSuggestion(index: Int) {
        val ic = currentInputConnection ?: return
        val word = suggestions.getOrNull(index) ?: return
        if (!engine.isEmpty) {
            ic.setComposingText(word, 1)
            ic.finishComposingText()
            engine.reset()
        } else {
            ic.commitText(word, 1)
        }
        swipeCandidates = null
        ic.commitText(" ", 1)
        lastWasSpace = true
        previousKey = keyOfLastSyllable(word)
        if (shift == Shift.ONCE) shift = Shift.OFF
        updateAutoShift()
        refreshSuggestions()
    }

    override fun onSwipe(keys: String) {
        val ic = currentInputConnection ?: return
        val d = decoder ?: return
        val committed = commitWord(ic)
        val prev = keyOfLastSyllable(committed) ?: previousKey
        val candidates = d.decode(keys, prev, 4, settings.modernTone).map {
            when (shift) {
                Shift.LOCKED -> it.uppercase()
                Shift.ONCE -> it.replaceFirstChar { c -> c.uppercaseChar() }
                Shift.OFF -> it
            }
        }
        if (candidates.isEmpty()) return
        val before = ic.getTextBeforeCursor(1, 0)
        if (!before.isNullOrEmpty() && !before[0].isWhitespace()) ic.commitText(" ", 1)
        previousKey = prev
        engine.setWord(candidates[0])
        ic.setComposingText(engine.composing, 1)
        swipeCandidates = candidates
        lastWasSpace = false
        if (shift == Shift.ONCE) {
            shift = Shift.OFF
            refreshView()
        }
        refreshSuggestions()
    }

    override fun onToneGesture(tone: Int) {
        val ic = currentInputConnection ?: return
        val modern = settings.modernTone
        if (!engine.isEmpty) {
            if (engine.applyTone(tone)) ic.setComposingText(engine.composing, 1)
            swipeCandidates = null
            refreshSuggestions()
            return
        }
        // No word being typed: put the tone on the word just before the cursor.
        val before = ic.getTextBeforeCursor(40, 0)?.toString() ?: return
        val m = Regex("(\\p{L}+)(\\s?)$").find(before) ?: return
        val word = m.groupValues[1]
        val toned = VietText.withTone(word, tone, modern)
        if (toned == word) return
        ic.beginBatchEdit()
        ic.deleteSurroundingText(m.value.length, 0)
        ic.commitText(toned + m.groupValues[2], 1)
        ic.endBatchEdit()
    }

    // ---- Emoji and clipboard ----

    private fun openPanel(kind: PanelView.Kind) {
        currentInputConnection?.let { commitWord(it) }
        captureClip()
        val p = panel ?: return
        panelKind = kind
        p.show(kind, view?.state?.theme ?: Themes.LIGHT, recentEmoji(), clipboard.all)
        p.visibility = View.VISIBLE
        view?.visibility = View.INVISIBLE
    }

    private fun closePanel() {
        panelKind = null
        panel?.visibility = View.GONE
        view?.visibility = View.VISIBLE
    }

    private fun recentEmoji(): List<String> =
        statePrefs.getString(RECENT_EMOJI, "").orEmpty().split(' ').filter { it.isNotEmpty() }

    override fun onPanelText(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        previousKey = null
        if (panelKind == PanelView.Kind.EMOJI) {
            val recent = (listOf(text) + recentEmoji().filter { it != text }).take(32)
            statePrefs.edit().putString(RECENT_EMOJI, recent.joinToString(" ")).apply()
        }
    }

    override fun onPanelDelete() {
        currentInputConnection?.let { handleDelete(it) }
    }

    override fun onPanelClose() {
        closePanel()
        refreshSuggestions()
    }

    override fun onClipboardRemove(text: String) {
        clipboard.remove(text)
        panelKind?.let { openPanel(it) }
    }

    override fun onClipboardClear() {
        clipboard.clear()
        panelKind?.let { openPanel(it) }
    }

    // ---- Key handling ----

    private fun typeChar(c0: Char, ic: InputConnection, applyShift: Boolean = true) {
        val c = if (applyShift && shift != Shift.OFF && layoutId == "alpha") c0.uppercaseChar() else c0
        swipeCandidates = null
        if (useEngine && engine.type(c)) {
            ic.setComposingText(engine.composing, 1)
        } else {
            commitWord(ic)
            ic.commitText(c.toString(), 1)
            previousKey = null
        }
        if (shift == Shift.ONCE && c.isLetter()) {
            shift = Shift.OFF
            refreshView()
        }
        if (!c.isLetterOrDigit()) updateAutoShift()
        refreshSuggestions()
    }

    /** Commits the word being typed and returns it (null if there was none). */
    private fun commitWord(ic: InputConnection): String? {
        if (engine.isEmpty) return null
        val word = engine.composing
        ic.finishComposingText()
        engine.reset()
        swipeCandidates = null
        return word
    }

    private fun handleSpace(ic: InputConnection, wasSpace: Boolean) {
        var expanded: String? = null
        if (!engine.isEmpty && useEngine) {
            expanded = abbreviations.expand(engine.composing)?.also { ic.setComposingText(it, 1) }
        }
        val committed = commitWord(ic)
        previousKey = keyOfLastSyllable(expanded ?: committed)
        val before = ic.getTextBeforeCursor(2, 0)
        if (wasSpace && settings.doubleSpacePeriod && before != null && before.length == 2 &&
            before[1] == ' ' && before[0].isLetterOrDigit()
        ) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
        } else {
            ic.commitText(" ", 1)
            lastWasSpace = true
        }
        if (layoutId == "symbols" || layoutId == "more_symbols") setLayout("alpha")
        updateAutoShift()
        refreshSuggestions()
    }

    private fun handleDelete(ic: InputConnection) {
        swipeCandidates = null
        if (engine.backspace()) {
            ic.setComposingText(engine.composing, 1)
            if (engine.isEmpty) ic.finishComposingText()
            refreshSuggestions()
            return
        }
        previousKey = null
        val selected = ic.getSelectedText(0)
        when {
            !selected.isNullOrEmpty() -> ic.commitText("", 1)
            currentInputEditorInfo?.inputType == InputType.TYPE_NULL -> sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            Build.VERSION.SDK_INT >= 24 -> ic.deleteSurroundingTextInCodePoints(1, 0)
            else -> ic.deleteSurroundingText(1, 0)
        }
        updateAutoShift()
        refreshSuggestions()
    }

    private fun handleEnter(ic: InputConnection) {
        commitWord(ic)
        previousKey = null
        val info = currentInputEditorInfo
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        val noAction = info == null || (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 ||
            action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED
        when {
            !noAction -> ic.performEditorAction(action)
            info?.inputType == InputType.TYPE_NULL -> sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            else -> ic.commitText("\n", 1)
        }
        updateAutoShift()
        refreshSuggestions()
    }

    private fun handleShift() {
        val now = SystemClock.uptimeMillis()
        shift = when {
            shift == Shift.LOCKED -> Shift.OFF
            shift == Shift.ONCE && now - lastShiftTap < DOUBLE_TAP_MS -> Shift.LOCKED
            shift == Shift.ONCE -> Shift.OFF
            else -> Shift.ONCE
        }
        lastShiftTap = now
        refreshView()
    }

    private fun toggleLanguage(ic: InputConnection) {
        commitWord(ic)
        previousKey = null
        suggestions = emptyList()
        settings.vietnamese = !settings.vietnamese
    }

    private fun setLayout(id: String) {
        layoutId = id
        if (id == "alpha") updateAutoShift() else refreshView()
    }

    /** Capitalises the next letter at the start of a sentence, when the field asks for it. */
    private fun updateAutoShift() {
        if (shift == Shift.LOCKED) return
        val ic = currentInputConnection
        val info = currentInputEditorInfo
        val caps = settings.autoCapitalize && ic != null && info != null && layoutId == "alpha" &&
            !plainField && ic.getCursorCapsMode(info.inputType) != 0
        val next = if (caps) Shift.ONCE else Shift.OFF
        if (next != shift) {
            shift = next
            refreshView()
        }
    }

    companion object {
        private const val DOUBLE_TAP_MS = 350L
        private const val RECENT_EMOJI = "recent_emoji"
        private val PLAIN_TEXT_VARIATIONS = setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
        )
    }
}
