package com.likhibi.keyboard

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
    private var btnToggle: TextView? = null
    private var suggestionsLayout: LinearLayout? = null
    private var toolsLayout: LinearLayout? = null
    private var btnToolClip: TextView? = null
    private var btnToolTheme: TextView? = null
    private var btnToolSettings: TextView? = null
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
                val word = (it as TextView).text?.toString()?.trim().orEmpty()
                if (word.isNotEmpty()) {
                    acceptSuggestion(word)
                }
            }
        }

        val prefs = getSharedPreferences("likhibi_keyboard_prefs", Context.MODE_PRIVATE)
        lastTheme = prefs.getString("selected_theme", "theme_midnight") ?: "theme_midnight"
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

        btnToggle?.setOnClickListener {
            toggleToolbar()
        }

        btnToolClip?.setOnClickListener {
            keyboardView?.showClipboard()
        }

        btnToolTheme?.setOnClickListener {
            keyboardView?.showThemeSwitcher()
        }

        btnToolSettings?.setOnClickListener {
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

        registerClipboardListener()

        return view
    }

    /**
     * Applies theme-specific colors to the suggestion bar, toolbar, and tool buttons
     */
    private fun applySuggestionBarTheme() {
        val prefs = getSharedPreferences("likhibi_keyboard_prefs", MODE_PRIVATE)
        val theme = prefs.getString("selected_theme", "theme_midnight") ?: "theme_midnight"

        data class BarColors(
            val barBg: Int, val textNormal: Int, val textAccent: Int,
            val toggleColor: Int, val divider: Int, val toolText: Int
        )

        val c = when (theme) {
            "theme_oled" -> BarColors(
                Color.BLACK, Color.parseColor("#888888"), Color.WHITE,
                Color.WHITE, Color.parseColor("#1A1A1A"), Color.parseColor("#CCCCCC")
            )
            "theme_oneplus" -> BarColors(
                Color.parseColor("#0D0E14"), Color.parseColor("#A0A5B5"), Color.parseColor("#00E5FF"),
                Color.parseColor("#00E5FF"), Color.parseColor("#1E202B"), Color.WHITE
            )
            "theme_aurora" -> BarColors(
                Color.argb(60, 58, 28, 113), Color.argb(200, 255, 255, 255), Color.parseColor("#FFAF7B"),
                Color.parseColor("#FFAF7B"), Color.argb(40, 255, 255, 255), Color.WHITE
            )
            "theme_ocean" -> BarColors(
                Color.argb(60, 2, 170, 176), Color.argb(200, 255, 255, 255), Color.parseColor("#00CDAC"),
                Color.parseColor("#00CDAC"), Color.argb(40, 255, 255, 255), Color.WHITE
            )
            "theme_custom_wallpaper" -> BarColors(
                Color.argb(140, 18, 19, 26), Color.argb(200, 255, 255, 255), Color.parseColor("#00E5FF"),
                Color.parseColor("#00E5FF"), Color.argb(40, 255, 255, 255), Color.WHITE
            )
            else -> BarColors( // Midnight
                Color.parseColor("#1A1C24"), Color.parseColor("#A0A5B5"), Color.parseColor("#00E5FF"),
                Color.parseColor("#00E5FF"), Color.parseColor("#2E313D"), Color.WHITE
            )
        }

        suggestionBar?.setBackgroundColor(c.barBg)
        imeRootView?.setBackgroundColor(c.barBg)
        btnToggle?.setTextColor(c.toggleColor)
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

        // Tool button text colors
        btnToolClip?.setTextColor(c.toolText)
        btnToolTheme?.setTextColor(c.toolText)
        btnToolSettings?.setTextColor(c.toolText)
    }

    private fun toggleToolbar() {
        if (toolsLayout?.visibility == View.VISIBLE) {
            // Close tools, show suggestions
            toolsLayout?.visibility = View.GONE
            suggestionsLayout?.visibility = View.VISIBLE
            btnToggle?.text = "❖"
            // Revert keyboard shelf if showing clipboard or theme swappers
            if (keyboardView?.getViewState() != CustomKeyboardView.ViewState.KEYS) {
                keyboardView?.switchMode(CustomKeyboardView.Mode.QWERTY)
            }
        } else {
            // Show tools, hide suggestions
            suggestionsLayout?.visibility = View.GONE
            toolsLayout?.visibility = View.VISIBLE
            btnToggle?.text = "✕"
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
        val currentTheme = prefs.getString("selected_theme", "theme_midnight") ?: "theme_midnight"
        val currentFont = prefs.getString("selected_font", "sans-serif") ?: "sans-serif"
        
        if (currentTheme != lastTheme || currentFont != lastFont) {
            setInputView(onCreateInputView())
        }

        super.onStartInput(attribute, restarting)
        currentComposing = StringBuilder()
        suggestionsEnabledForField = attribute?.let { !isPasswordField(it) } ?: true
        updateSuggestionsForCurrentState()
        suggestionBar?.visibility = if (suggestionsEnabledForField) View.VISIBLE else View.GONE
        
        // Auto reset toolbar state back to suggestions on launch
        toolsLayout?.visibility = View.GONE
        suggestionsLayout?.visibility = View.VISIBLE
        btnToggle?.text = "❖"

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
            10 -> handleEnter(ic)
            32 -> handleSpace(ic)
            else -> handleCharacter(ic, code)
        }
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
        val ch = primaryCode.toChar()
        currentComposing.append(ch)
        ic.setComposingText(currentComposing, 1)
        updateSuggestionsForCurrentState()
        
        // Auto-shift back to lowercase after typing a letter
        val kv = keyboardView ?: return
        if (kv.isShifted()) {
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
        ic.commitText("\n", 1)

        // Dynamically learn the bigram from the last 2 committed words
        val last2 = getLastWords(2)
        if (last2.size == 2) {
            offlineEngine.learnBigram(last2[0], last2[1])
        }

        updateSuggestionsForCurrentState()
    }

    private fun handleShift() {
        val kv = keyboardView ?: return
        kv.setShifted(!kv.isShifted())
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
                btnToggle?.text = "❖"
                if (keyboardView?.getViewState() != CustomKeyboardView.ViewState.KEYS) {
                    keyboardView?.switchMode(CustomKeyboardView.Mode.QWERTY)
                }
            }

            val localMatches = offlineEngine.getPrefixMatches(composing)
            if (localMatches.isNotEmpty()) {
                updateSuggestionBar(localMatches)
            } else {
                updateSuggestionBar(listOf(composing, "", ""))
            }
        } else {
            val context = getLastWords(5)
            if (context.isEmpty()) {
                updateSuggestionBar(offlineEngine.getPopularWords())
                return
            }

            val contextKey = context.joinToString(" ")
            
            val cached = offlineEngine.getCachedSuggestions(contextKey)
            if (cached != null) {
                updateSuggestionBar(cached)
            } else {
                val lastWord = context.last()
                val localPredictions = offlineEngine.getLocalNextWordPredictions(lastWord)
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
