package com.likhibi.android

import com.likhibi.keyboard.R
import com.likhibi.keyboard.BuildConfig
import com.likhibi.nlp.engine.NagameseOfflineEngine
import com.likhibi.nlp.engine.GeminiClient

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LikhibiImeService : InputMethodService() {
    private val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var geminiJob: Job? = null
    private var lastTheme = ""
    private var lastFont = ""

    private var keyboardView: CustomKeyboardView? = null
    private var imeRootView: View? = null
    private var suggestionBar: LinearLayout? = null
    private var suggestionViews: List<TextView> = emptyList()

    // Toolbar elements
    private var btnToggle: android.widget.ImageView? = null
    private var iconToolClip: android.widget.ImageView? = null
    private var iconToolTheme: android.widget.ImageView? = null
    private var iconToolSettings: android.widget.ImageView? = null
    private var textToolClip: TextView? = null
    private var textToolTheme: TextView? = null
    private var textToolSettings: TextView? = null
    private var suggestionsLayout: LinearLayout? = null
    private var toolsLayout: LinearLayout? = null
    private var btnToolClip: View? = null
    private var btnToolTheme: View? = null
    private var btnToolSettings: View? = null
    private var toolbarDivider: View? = null
    private var barDivider: View? = null

    // Clipboard elements
    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureCurrentClip() }
    private var isClipListenerRegistered = false

    private var geminiClient: GeminiClient? = null
    private lateinit var offlineEngine: NagameseOfflineEngine

    private var suggestionsEnabledForField: Boolean = true
    private var currentComposing: StringBuilder = StringBuilder()

    companion object {
        const val KEYCODE_DELETE = -5
        const val KEYCODE_SHIFT = -1
        const val KEYCODE_SYMBOL_SWITCH = -2
        const val KEYCODE_SYMBOL_EXTRA = -3
        const val KEYCODE_SYMBOL_BACK = -4
        const val KEYCODE_EMOJI_SWITCH = -10
        const val KEYCODE_SWITCH_IME = -11
    }

    override fun onCreate() {
        super.onCreate()
        geminiClient = GeminiClient(BuildConfig.GEMINI_API_KEY)
        offlineEngine = NagameseOfflineEngine(this)
        
        // Pre-warm the dictionary on a background thread to prevent first-tap lag
        serviceScope.launch(Dispatchers.Default) {
            offlineEngine.getPrefixMatches("a")
        }
    }

    override fun onCreateInputView(): View {
        val view = LayoutInflater.from(this).inflate(R.layout.ime_view, null)
        imeRootView = view
        suggestionBar = view.findViewById(R.id.suggestion_bar)
        val s1 = view.findViewById<TextView>(R.id.suggestion_1)
        val s2 = view.findViewById<TextView>(R.id.suggestion_2)
        val s3 = view.findViewById<TextView>(R.id.suggestion_3)
        suggestionViews = listOf(s1, s2, s3)

        for (tv in suggestionViews) {
            tv.setOnClickListener {
                keyboardView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                val word = (it as TextView).text?.toString()?.trim().orEmpty()
                if (word.isNotEmpty()) {
                    acceptSuggestion(word)
                }
            }
        }

        val prefs = getSharedPreferences("likhibi_keyboard_prefs", Context.MODE_PRIVATE)
        lastTheme = prefs.getString("selected_theme", "theme_midnight_glass") ?: "theme_midnight_glass"
        lastFont = prefs.getString("selected_font", "sans-serif") ?: "sans-serif"

        // Initialize and bind toolbar controls
        btnToggle = view.findViewById(R.id.btn_toolbar_toggle)
        suggestionsLayout = view.findViewById(R.id.suggestions_container)
        toolsLayout = view.findViewById(R.id.tools_container)
        toolbarDivider = view.findViewById(R.id.toolbar_divider)
        barDivider = view.findViewById(R.id.bar_divider)
        btnToolClip = view.findViewById(R.id.btn_tool_clip)
        btnToolTheme = view.findViewById(R.id.btn_tool_theme)
        btnToolSettings = view.findViewById(R.id.btn_tool_settings)
        iconToolClip = view.findViewById(R.id.icon_tool_clip)
        iconToolTheme = view.findViewById(R.id.icon_tool_theme)
        iconToolSettings = view.findViewById(R.id.icon_tool_settings)
        textToolClip = view.findViewById(R.id.text_tool_clip)
        textToolTheme = view.findViewById(R.id.text_tool_theme)
        textToolSettings = view.findViewById(R.id.text_tool_settings)
        iconToolClip = view.findViewById(R.id.icon_tool_clip)
        iconToolTheme = view.findViewById(R.id.icon_tool_theme)
        iconToolSettings = view.findViewById(R.id.icon_tool_settings)
        textToolClip = view.findViewById(R.id.text_tool_clip)
        textToolTheme = view.findViewById(R.id.text_tool_theme)
        textToolSettings = view.findViewById(R.id.text_tool_settings)

        btnToggle?.setOnClickListener {
            keyboardView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            toggleToolbar()
        }

        btnToolClip?.setOnClickListener {
            keyboardView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            keyboardView?.showClipboard()
        }

        btnToolTheme?.setOnClickListener {
            keyboardView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            keyboardView?.showThemeSwitcher()
        }

        btnToolSettings?.setOnClickListener {
            keyboardView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            val intent = Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        keyboardView = view.findViewById<CustomKeyboardView>(R.id.keyboard_view).also {
            it.listener = object : CustomKeyboardView.OnKeyActionListener {
                override fun onKey(code: Int) {
                    handleCustomKey(code)
                }

                override fun onText(text: String) {
                    handleText(text)
                }
            }
        }

        // Apply active theme immediately on view creation
        keyboardView?.applyTheme()
        applySuggestionBarTheme()

        keyboardView?.themeChangeListener = {
            applySuggestionBarTheme()
        }
        keyboardView?.wallpaperPickerListener = {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("ACTION_PICK_WALLPAPER", true)
            }
            startActivity(intent)
        }

        registerClipboardListener()

        return view
    }

    /**
     * Applies theme-specific colors to the suggestion bar, toolbar, and tool buttons
     */
    private fun applySuggestionBarTheme() {
        val prefs = getSharedPreferences("likhibi_keyboard_prefs", MODE_PRIVATE)
        val theme = prefs.getString("selected_theme", "theme_midnight_glass") ?: "theme_midnight_glass"

        data class BarColors(
            val barBg: Int, val textNormal: Int, val textAccent: Int,
            val toggleColor: Int, val divider: Int, val toolText: Int
        )

        val c = when (theme) {
            "theme_midnight_glass" -> BarColors(
                Color.parseColor("#0A0F1E"), Color.parseColor("#8899B8"), Color.parseColor("#00E5FF"),
                Color.parseColor("#00E5FF"), Color.parseColor("#1A2040"), Color.parseColor("#8899B8")
            )
            "theme_pure_minimal" -> BarColors(
                Color.parseColor("#F7F7F5"), Color.parseColor("#888888"), Color.parseColor("#2A2A2A"),
                Color.parseColor("#2A2A2A"), Color.parseColor("#E8E8E6"), Color.parseColor("#555555")
            )
            "theme_liquid_glass" -> BarColors(
                Color.parseColor("#080D1A"), Color.parseColor("#8E9EB8"), Color.parseColor("#00D2FF"),
                Color.parseColor("#00D2FF"), Color.argb(40, 0, 210, 255), Color.parseColor("#8E9EB8")
            )
            "theme_material_you" -> BarColors(
                Color.parseColor("#FEF7FF"), Color.parseColor("#49454F"), Color.parseColor("#6750A4"),
                Color.parseColor("#6750A4"), Color.parseColor("#E7E0EC"), Color.parseColor("#49454F")
            )
            "theme_naga_heritage" -> {
                val isNight = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNight) {
                    BarColors(
                        Color.parseColor("#121214"), Color.parseColor("#A8A096"), Color.parseColor("#E53935"),
                        Color.parseColor("#E53935"), Color.parseColor("#241A1C"), Color.parseColor("#A8A096")
                    )
                } else {
                    BarColors(
                        Color.parseColor("#F9F6F0"), Color.parseColor("#5A544A"), Color.parseColor("#D32F2F"),
                        Color.parseColor("#D32F2F"), Color.parseColor("#E5DED2"), Color.parseColor("#5A544A")
                    )
                }
            }
            "theme_custom" -> {
                val accentColorValue = prefs.getInt("custom_accent_color", Color.parseColor("#00E5FF"))
                val overlayOpacity = prefs.getInt("custom_overlay_opacity", 140).coerceIn(0, 220)
                BarColors(
                    Color.argb(overlayOpacity, 10, 10, 18), Color.argb(200, 255, 255, 255), accentColorValue,
                    accentColorValue, Color.argb(40, 255, 255, 255), Color.WHITE
                )
            }
            else -> BarColors(
                Color.parseColor("#0A0F1E"), Color.parseColor("#8899B8"), Color.parseColor("#00E5FF"),
                Color.parseColor("#00E5FF"), Color.parseColor("#1A2040"), Color.parseColor("#8899B8")
            )
        }

        suggestionBar?.setBackgroundColor(c.barBg)
        imeRootView?.setBackgroundColor(c.barBg)
        // btnToggle logo is pristine background-less
        toolbarDivider?.setBackgroundColor(c.divider)
        barDivider?.setBackgroundColor(c.divider)

        // Suggestion text colors
        suggestionViews.getOrNull(0)?.setTextColor(c.textNormal)
        suggestionViews.getOrNull(1)?.setTextColor(c.textAccent)
        suggestionViews.getOrNull(2)?.setTextColor(c.textNormal)

        // Suggestion separator dividers
        suggestionsLayout?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child !is TextView && child.layoutParams.width == 1) {
                    child.setBackgroundColor(c.divider)
                }
            }
        }

        // Tool button vector tint & text colors
        textToolClip?.setTextColor(c.toolText)
        textToolTheme?.setTextColor(c.toolText)
        textToolSettings?.setTextColor(c.toolText)
        iconToolClip?.setColorFilter(c.toolText)
        iconToolTheme?.setColorFilter(c.toolText)
        iconToolSettings?.setColorFilter(c.toolText)
    }

    private fun toggleToolbar() {
        if (toolsLayout?.visibility == View.VISIBLE) {
            // Close tools, show suggestions
            toolsLayout?.visibility = View.GONE
            suggestionsLayout?.visibility = View.VISIBLE
            btnToggle?.setImageResource(R.mipmap.ic_launcher)
            btnToggle?.rotation = 0f
            // Revert keyboard shelf if showing clipboard or theme swappers
            if (keyboardView?.getViewState() != CustomKeyboardView.ViewState.KEYS) {
                keyboardView?.switchMode(CustomKeyboardView.Mode.QWERTY)
            }
        } else {
            // Show tools, hide suggestions
            suggestionsLayout?.visibility = View.GONE
            toolsLayout?.visibility = View.VISIBLE
            btnToggle?.setImageResource(R.drawable.ic_close_clean)
        }
    }

    private fun registerClipboardListener() {
        if (isClipListenerRegistered) return
        try {
            if (clipboardManager == null) {
                clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            }
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
            isClipListenerRegistered = true
        } catch (e: Exception) {
            Log.e("LikhibiIME", "Failed to register clipboard listener: ${e.message}")
        }
    }

    private fun unregisterClipboardListener() {
        if (!isClipListenerRegistered) return
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipListener)
            isClipListenerRegistered = false
        } catch (e: Exception) {
            Log.e("LikhibiIME", "Failed to unregister clipboard listener: ${e.message}")
        }
    }

    /**
     * Intercepts copied text while keyboard is active and saves to SharedPreferences
     */
    private fun captureCurrentClip() {
        try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    saveToClipboardHistory(text)
                }
            }
        } catch (e: Exception) {
            Log.e("LikhibiIME", "Failed to capture clipboard text: ${e.message}")
        }
    }

    private fun saveToClipboardHistory(text: String) {
        val prefs = getSharedPreferences("likhibi_keyboard_prefs", MODE_PRIVATE)
        val saved = prefs.getString("clipboard_history", "") ?: ""
        val clips = if (saved.isEmpty()) mutableListOf() else saved.split("[LIKHIBI_SPLIT]").filter { it.isNotEmpty() }.toMutableList()

        // Push new item to the top by removing past duplicates and inserting
        clips.remove(text)
        clips.add(text)

        // Limit clipboard history to last 15 items
        val trimmed = if (clips.size > 15) clips.takeLast(15) else clips

        prefs.edit().putString("clipboard_history", trimmed.joinToString("[LIKHIBI_SPLIT]")).apply()

        // Live refresh the clipboard layout if currently showing on screen
        if (keyboardView?.getViewState() == CustomKeyboardView.ViewState.CLIPBOARD) {
            keyboardView?.showClipboard()
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        val prefs = getSharedPreferences("likhibi_keyboard_prefs", Context.MODE_PRIVATE)
        val currentTheme = prefs.getString("selected_theme", "theme_midnight_glass") ?: "theme_midnight_glass"
        val currentFont = prefs.getString("selected_font", "sans-serif") ?: "sans-serif"
        
        if (currentTheme != lastTheme || currentFont != lastFont) {
            setInputView(onCreateInputView())
        }

        super.onStartInput(attribute, restarting)

        val imeOptions = attribute?.imeOptions ?: 0
        val inputType = attribute?.inputType ?: 0
        val enterAction = imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        val enterLabel = if (noEnterAction || isMultiLine) {
            "↵"
        } else {
            when (enterAction) {
                EditorInfo.IME_ACTION_SEARCH -> "🔍"
                EditorInfo.IME_ACTION_GO -> "➔"
                EditorInfo.IME_ACTION_SEND -> "➤"
                EditorInfo.IME_ACTION_NEXT -> "↵"
                EditorInfo.IME_ACTION_DONE -> "✓"
                else -> "↵"
            }
        }
        keyboardView?.enterKeyLabel = enterLabel
        currentComposing = StringBuilder()
        suggestionsEnabledForField = attribute?.let { !isPasswordField(it) } ?: true
        updateSuggestionsForCurrentState()
        suggestionBar?.visibility = if (suggestionsEnabledForField) View.VISIBLE else View.GONE
        
        // Auto reset toolbar state back to suggestions on launch
        toolsLayout?.visibility = View.GONE
        suggestionsLayout?.visibility = View.VISIBLE
        btnToggle?.setImageResource(R.mipmap.ic_launcher)

        // Batch reset visual state to avoid redundant UI rebuilds and ANRs
        keyboardView?.resetState(CustomKeyboardView.Mode.QWERTY, false)
        applySuggestionBarTheme()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        geminiJob?.cancel()
        currentComposing = StringBuilder()
        updateSuggestionBar(offlineEngine.getPopularWords())
    }

    override fun onDestroy() {
        super.onDestroy()
        geminiJob?.cancel()
        unregisterClipboardListener()
        serviceScope.coroutineContext[Job]?.cancel()
    }

    private fun handleCustomKey(code: Int) {
        val ic = currentInputConnection ?: return

        when (code) {
            KEYCODE_DELETE -> handleBackspace(ic)
            KEYCODE_SHIFT -> handleShift()
            KEYCODE_SYMBOL_SWITCH -> handleSymbolSwitch()
            KEYCODE_SYMBOL_EXTRA -> handleSymbolExtra()
            KEYCODE_SYMBOL_BACK -> handleSymbolBack()
            KEYCODE_EMOJI_SWITCH -> handleEmojiSwitch()
            KEYCODE_SWITCH_IME -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showInputMethodPicker()
            }
            10 -> handleEnter(ic)
            32 -> handleSpace(ic)
            Int.MAX_VALUE -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_RIGHT))
            Int.MIN_VALUE -> ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_DPAD_LEFT))
            else -> handleCharacter(ic, code)
        }
    }

    private var lastShiftTime = 0L

    private fun handleShift() {
        val now = System.currentTimeMillis()
        if (now - lastShiftTime < 300) {
            keyboardView?.isCapsLock = true
            keyboardView?.setShifted(true)
        } else {
            keyboardView?.isCapsLock = false
            keyboardView?.setShifted(!(keyboardView?.isShifted() ?: false))
        }
        lastShiftTime = now
    }

    private fun handleText(text: String) {
        val ic = currentInputConnection ?: return
        
        // Commit current composition first if active
        val composing = currentComposing.toString().trim()
        if (composing.isNotEmpty()) {
            offlineEngine.learnWord(composing)
            ic.commitText(currentComposing.toString(), 1)
            ic.finishComposingText()
            currentComposing = StringBuilder()
        }
        ic.commitText(text, 1)

        // Dynamically learn the bigram from the last 2 committed words
        val last2 = getLastWords(2)
        if (last2.size == 2) {
            offlineEngine.learnBigram(last2[0], last2[1])
        }
    }

    private fun handleCharacter(ic: InputConnection, primaryCode: Int) {
        var ch = primaryCode.toChar().toString()
        if (keyboardView?.isShifted() == true) {
            ch = ch.uppercase()
        }
        currentComposing.append(ch)
        ic.setComposingText(currentComposing, 1)
        updateSuggestionsForCurrentState()
        
        // Auto-shift back to lowercase after typing a letter
        val kv = keyboardView ?: return
        if (kv.isShifted() && !kv.isCapsLock) {
            kv.setShifted(false)
        }
    }

    private fun handleBackspace(ic: InputConnection) {
        if (currentComposing.isNotEmpty()) {
            currentComposing.deleteAt(currentComposing.length - 1)
            if (currentComposing.isEmpty()) {
                ic.finishComposingText()
            } else {
                ic.setComposingText(currentComposing, 1)
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
        updateSuggestionsForCurrentState()
    }

    private fun handleSpace(ic: InputConnection) {
        val composing = currentComposing.toString().trim()
        if (composing.isNotEmpty()) {
            offlineEngine.learnWord(composing)
            ic.commitText(currentComposing.toString(), 1)
            ic.finishComposingText()
            currentComposing = StringBuilder()
        }
        ic.commitText(" ", 1)

        // Dynamically learn the bigram from the last 2 committed words
        val last2 = getLastWords(2)
        if (last2.size == 2) {
            offlineEngine.learnBigram(last2[0], last2[1])
        }

        updateSuggestionsForCurrentState()
    }

    private fun handleEnter(ic: InputConnection) {
        val composing = currentComposing.toString().trim()
        if (composing.isNotEmpty()) {
            offlineEngine.learnWord(composing)
            ic.commitText(currentComposing.toString(), 1)
            ic.finishComposingText()
            currentComposing = StringBuilder()
        }

        val info = currentInputEditorInfo
        val imeOptions = info?.imeOptions ?: 0
        val inputType = info?.inputType ?: 0
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        if (!noEnterAction && !isMultiLine && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            // Newline for multiline chat & text fields (WhatsApp, Telegram, Notes, Messages)
            val committed = ic.commitText("\n", 1)
            if (!committed) {
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            }
        }

        // Dynamically learn the bigram from the last 2 committed words
        val last2 = getLastWords(2)
        if (last2.size == 2) {
            offlineEngine.learnBigram(last2[0], last2[1])
        }

        updateSuggestionsForCurrentState()
    }


    private fun handleSymbolSwitch() {
        val kv = keyboardView ?: return
        if (kv.getMode() != CustomKeyboardView.Mode.QWERTY) {
            kv.switchMode(CustomKeyboardView.Mode.QWERTY)
        } else {
            kv.switchMode(CustomKeyboardView.Mode.SYMBOLS)
        }
    }

    private fun handleSymbolExtra() {
        keyboardView?.switchMode(CustomKeyboardView.Mode.EXTRA_SYMBOLS)
    }

    private fun handleSymbolBack() {
        keyboardView?.switchMode(CustomKeyboardView.Mode.SYMBOLS)
    }

    private fun handleEmojiSwitch() {
        val kv = keyboardView ?: return
        if (kv.getMode() == CustomKeyboardView.Mode.EMOJI) {
            kv.switchMode(CustomKeyboardView.Mode.QWERTY)
        } else {
            kv.switchMode(CustomKeyboardView.Mode.EMOJI)
        }
    }

    private fun acceptSuggestion(word: String) {
        val ic = currentInputConnection ?: return

        val composing = currentComposing.toString().trim()
        if (composing.isNotEmpty()) {
            offlineEngine.learnWord(composing)
        }

        ic.commitText(word, 1)
        ic.finishComposingText()
        ic.commitText(" ", 1)
        currentComposing = StringBuilder()

        offlineEngine.learnWord(word)

        val last2 = getLastWords(2)
        if (last2.size == 2) {
            offlineEngine.learnBigram(last2[0], last2[1])
        }

        updateSuggestionsForCurrentState()
    }

    private fun updateSuggestionsForCurrentState() {
        if (!suggestionsEnabledForField) return

        val composing = currentComposing.toString().trim()
        if (composing.isNotEmpty()) {
            // Auto revert toolbar back to Suggestions Mode when user starts active typing
            if (toolsLayout?.visibility == View.VISIBLE) {
                toolsLayout?.visibility = View.GONE
                suggestionsLayout?.visibility = View.VISIBLE
                btnToggle?.setImageResource(R.mipmap.ic_launcher)
                if (keyboardView?.getViewState() != CustomKeyboardView.ViewState.KEYS) {
                    keyboardView?.switchMode(CustomKeyboardView.Mode.QWERTY)
                }
            }

            val context = getLastWords(3)
            val localMatches = offlineEngine.getPrefixMatches(context, composing)
            if (localMatches.isNotEmpty()) {
                updateSuggestionBar(localMatches)
            } else {
                updateSuggestionBar(listOf(composing, "", ""))
            }
        } else {
            val context = getLastWords(3)
            if (context.isEmpty()) {
                updateSuggestionBar(offlineEngine.getPopularWords())
                return
            }

            val contextKey = context.joinToString(" ")
            
            val cached = offlineEngine.getCachedSuggestions(contextKey)
            if (cached != null) {
                updateSuggestionBar(cached)
            } else {
                val localPredictions = offlineEngine.getLocalNextWordPredictions(context)
                updateSuggestionBar(localPredictions)

                queryGeminiBackground(context, contextKey)
            }
        }
    }

    private fun queryGeminiBackground(contextWords: List<String>, contextKey: String) {
        geminiJob?.cancel()
        geminiJob = serviceScope.launch {
            delay(80)

            val suggestions = withContext(Dispatchers.IO) {
                runCatching {
                    geminiClient?.suggestNextWords(contextWords).orEmpty()
                }.onFailure {
                    Log.e("LikhibiIME", "Gemini API background suggestion query failed: ${it.message}")
                }.getOrDefault(emptyList())
            }

            if (suggestions.isNotEmpty()) {
                offlineEngine.cacheSuggestions(contextKey, suggestions)

                val currentContext = getLastWords(5)
                if (currentContext.joinToString(" ") == contextKey) {
                    updateSuggestionBar(suggestions)
                }
            }
        }
    }

    private fun getLastWords(maxWords: Int): List<String> {
        val ic = currentInputConnection ?: return emptyList()
        val before = ic.getTextBeforeCursor(60, 0)?.toString().orEmpty()
        val combined = (before + currentComposing.toString()).trim()
        if (combined.isEmpty()) return emptyList()
        val tokens = combined
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        return tokens.takeLast(maxWords)
    }

    private fun updateSuggestionBar(words: List<String>) {
        if (!suggestionsEnabledForField) return
        val padded = (words + listOf("", "", "")).take(3)
        for (i in 0..2) {
            suggestionViews.getOrNull(i)?.text = padded[i]
        }
    }

    private fun isPasswordField(info: EditorInfo): Boolean {
        val type = info.inputType
        val variation = type and InputType.TYPE_MASK_VARIATION
        val klass = type and InputType.TYPE_MASK_CLASS
        val textPassword = klass == InputType.TYPE_CLASS_TEXT && (
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            )
        val numberPassword = klass == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        return textPassword || numberPassword
    }
}
