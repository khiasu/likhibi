package com.likhibi.keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val typefaceMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val typefaceBold = Typeface.create("sans-serif-medium", Typeface.BOLD)

    interface OnKeyActionListener {
        fun onKey(code: Int)
        fun onText(text: String)
    }

    var listener: OnKeyActionListener? = null
    private var isShifted = false
    var isCapsLock = false
    var enterKeyLabel = "↵"
    private var keyboardMode = Mode.QWERTY
    private var currentTheme = "theme_midnight"
    private var lastAppliedTheme: String? = null
    private var cachedThemeColors: ThemeColors? = null
    private var cachedWallpaper: BitmapDrawable? = null
    private val drawableCache = mutableMapOf<String, android.graphics.drawable.Drawable.ConstantState>()

    private val prefs = context.getSharedPreferences("likhibi_keyboard_prefs", Context.MODE_PRIVATE)

    private var soundPool: SoundPool? = null
    private var soundClick: Int = 0
    private var soundSpace: Int = 0
    private var deleteRunnable: Runnable? = null
    private val deleteHandler = Handler(Looper.getMainLooper())

    // Active screen popup preview
    private var activePopup: PopupWindow? = null

    // Tracking view state (whether showing normal keys, clipboard, or theme switcher)
    private var viewState = ViewState.KEYS

    enum class Mode {
        QWERTY, SYMBOLS, EXTRA_SYMBOLS, EMOJI
    }

    enum class ViewState {
        KEYS, CLIPBOARD, THEME_SWITCHER
    }

    enum class HapticType {
        KEY_STANDARD, KEY_SPACE, KEY_MODIFIER, KEY_ACCENT
    }

    // Theme Config class
    data class ThemeColors(
        val bgColor: Int,
        val bgGradient: List<String>? = null,
        val keyBgNormal: Int,
        val keyBgNormalPressed: Int,
        val keyBgModifier: Int,
        val keyBgModifierPressed: Int,
        val keyBgAccent: Int,
        val keyBgAccentPressed: Int,
        val keyTextColorNormal: Int,
        val keyTextColorModifier: Int,
        val keyTextColorAccent: Int,
        val isGlass: Boolean,
        val hasShadow: Boolean,
        val suggestionBarBg: Int = Color.parseColor("#1A1C24"),
        val suggestionTextNormal: Int = Color.parseColor("#A0A5B5"),
        val suggestionTextAccent: Int = Color.parseColor("#00E5FF"),
        val toolbarToggleColor: Int = Color.parseColor("#00E5FF"),
        val dividerColor: Int = Color.parseColor("#2E313D"),
        val clipboardCardBg: Int = Color.parseColor("#212330"),
        val clipboardCardPressed: Int = Color.parseColor("#323547"),
        val clipboardEmptyText: Int = Color.parseColor("#808495"),
        val deleteColor: Int = Color.parseColor("#FF5252"),
        val accentColor: Int = Color.parseColor("#00E5FF")
    )

    data class KeyInfo(
        val label: String,
        val code: Int,
        val weight: Float = 1.0f,
        val isModifier: Boolean = false,
        val isAccent: Boolean = false
    )

    var themeChangeListener: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        currentTheme = prefs.getString("selected_theme", "theme_midnight") ?: "theme_midnight"
        applyTheme()
        initSoundPool()
    }

    private fun initSoundPool() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
        try {
            soundClick = soundPool?.load(context, R.raw.key_click, 1) ?: 0
            soundSpace = soundPool?.load(context, R.raw.key_space, 1) ?: 0
        } catch (e: Exception) { /* ignore missing resources */ }
    }

    private fun getSelectedTypeface(): Typeface {
        val fontName = prefs.getString("selected_font", "sans-serif") ?: "sans-serif"
        return Typeface.create(fontName, Typeface.NORMAL)
    }

    fun setShifted(shifted: Boolean) {
        if (isShifted != shifted) {
            isShifted = shifted
            if (keyboardMode == Mode.QWERTY && viewState == ViewState.KEYS) {
                buildKeyboard()
            }
        }
    }

    fun isShifted(): Boolean = isShifted

    fun switchMode(mode: Mode) {
        if (keyboardMode != mode || viewState != ViewState.KEYS) {
            keyboardMode = mode
            viewState = ViewState.KEYS
            buildKeyboard()
        }
    }

    /**
     * Batch resets the keyboard state to avoid multiple redundant UI rebuilds
     */
    fun resetState(mode: Mode, shifted: Boolean) {
        val modeChanged = (this.keyboardMode != mode)
        val shiftChanged = (this.isShifted != shifted)
        val stateChanged = (this.viewState != ViewState.KEYS)

        this.keyboardMode = mode
        this.isShifted = shifted
        this.viewState = ViewState.KEYS

        if (modeChanged || shiftChanged || stateChanged) {
            applyTheme(forceRebuild = true)
        } else {
            applyTheme(forceRebuild = false)
        }
    }

    fun getMode(): Mode = keyboardMode

    fun getViewState(): ViewState = viewState

    /**
     * Resolves theme colors based on current theme selection
     */
    private fun getThemeColors(): ThemeColors {
        val theme = prefs.getString("selected_theme", "theme_oled_black") ?: "theme_oled_black"
        return when (theme) {
            "theme_classic_light" -> ThemeColors(
                bgColor = Color.parseColor("#F5F5F0"), // Creme
                keyBgNormal = Color.parseColor("#FFFFFF"),
                keyBgNormalPressed = Color.parseColor("#E8E8E3"),
                keyBgModifier = Color.parseColor("#EBEBE6"),
                keyBgModifierPressed = Color.parseColor("#DFDFD9"),
                keyBgAccent = Color.parseColor("#D32F2F"), // Red accent
                keyBgAccentPressed = Color.parseColor("#B71C1C"),
                keyTextColorNormal = Color.parseColor("#212121"),
                keyTextColorModifier = Color.parseColor("#424242"),
                keyTextColorAccent = Color.WHITE,
                isGlass = false,
                hasShadow = true,
                suggestionBarBg = Color.parseColor("#F5F5F0"),
                suggestionTextNormal = Color.parseColor("#757575"),
                suggestionTextAccent = Color.parseColor("#D32F2F"),
                toolbarToggleColor = Color.parseColor("#D32F2F"),
                dividerColor = Color.parseColor("#E0E0E0"),
                clipboardCardBg = Color.parseColor("#FFFFFF"),
                clipboardCardPressed = Color.parseColor("#F5F5F0"),
                clipboardEmptyText = Color.parseColor("#9E9E9E"),
                deleteColor = Color.parseColor("#D32F2F"),
                accentColor = Color.parseColor("#D32F2F")
            )
            "theme_oled_black" -> ThemeColors(
                bgColor = Color.parseColor("#000000"),
                keyBgNormal = Color.parseColor("#121212"),
                keyBgNormalPressed = Color.parseColor("#242424"),
                keyBgModifier = Color.parseColor("#0A0A0A"),
                keyBgModifierPressed = Color.parseColor("#1C1C1C"),
                keyBgAccent = Color.parseColor("#E0E0E0"),
                keyBgAccentPressed = Color.parseColor("#FFFFFF"),
                keyTextColorNormal = Color.WHITE,
                keyTextColorModifier = Color.parseColor("#AAAAAA"),
                keyTextColorAccent = Color.BLACK,
                isGlass = false,
                hasShadow = false,
                suggestionBarBg = Color.parseColor("#000000"),
                suggestionTextNormal = Color.parseColor("#888888"),
                suggestionTextAccent = Color.parseColor("#FFFFFF"),
                toolbarToggleColor = Color.parseColor("#FFFFFF"),
                dividerColor = Color.parseColor("#1A1A1A"),
                clipboardCardBg = Color.parseColor("#0A0A0A"),
                clipboardCardPressed = Color.parseColor("#1F1F1F"),
                clipboardEmptyText = Color.parseColor("#555555"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#FFFFFF")
            )
            "theme_slate_grey" -> ThemeColors(
                bgColor = Color.parseColor("#263238"),
                keyBgNormal = Color.parseColor("#37474F"),
                keyBgNormalPressed = Color.parseColor("#455A64"),
                keyBgModifier = Color.parseColor("#1C272C"),
                keyBgModifierPressed = Color.parseColor("#2D3E46"),
                keyBgAccent = Color.parseColor("#80CBC4"),
                keyBgAccentPressed = Color.parseColor("#4DB6AC"),
                keyTextColorNormal = Color.parseColor("#ECEFF1"),
                keyTextColorModifier = Color.parseColor("#CFD8DC"),
                keyTextColorAccent = Color.parseColor("#263238"),
                isGlass = false,
                hasShadow = true,
                suggestionBarBg = Color.parseColor("#263238"),
                suggestionTextNormal = Color.parseColor("#90A4AE"),
                suggestionTextAccent = Color.parseColor("#80CBC4"),
                toolbarToggleColor = Color.parseColor("#80CBC4"),
                dividerColor = Color.parseColor("#1C272C"),
                clipboardCardBg = Color.parseColor("#37474F"),
                clipboardCardPressed = Color.parseColor("#455A64"),
                clipboardEmptyText = Color.parseColor("#78909C"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#80CBC4")
            )
            "theme_navy_blue" -> ThemeColors(
                bgColor = Color.parseColor("#0D1B2A"),
                keyBgNormal = Color.parseColor("#1B263B"),
                keyBgNormalPressed = Color.parseColor("#415A77"),
                keyBgModifier = Color.parseColor("#08121E"),
                keyBgModifierPressed = Color.parseColor("#152233"),
                keyBgAccent = Color.parseColor("#E0E1DD"),
                keyBgAccentPressed = Color.parseColor("#FFFFFF"),
                keyTextColorNormal = Color.parseColor("#E0E1DD"),
                keyTextColorModifier = Color.parseColor("#778DA9"),
                keyTextColorAccent = Color.parseColor("#0D1B2A"),
                isGlass = false,
                hasShadow = true,
                suggestionBarBg = Color.parseColor("#0D1B2A"),
                suggestionTextNormal = Color.parseColor("#778DA9"),
                suggestionTextAccent = Color.parseColor("#E0E1DD"),
                toolbarToggleColor = Color.parseColor("#E0E1DD"),
                dividerColor = Color.parseColor("#08121E"),
                clipboardCardBg = Color.parseColor("#1B263B"),
                clipboardCardPressed = Color.parseColor("#415A77"),
                clipboardEmptyText = Color.parseColor("#778DA9"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#E0E1DD")
            )
            "theme_earth_tone" -> ThemeColors(
                bgColor = Color.parseColor("#3E2723"),
                keyBgNormal = Color.parseColor("#4E342E"),
                keyBgNormalPressed = Color.parseColor("#5D4037"),
                keyBgModifier = Color.parseColor("#301E1A"),
                keyBgModifierPressed = Color.parseColor("#402823"),
                keyBgAccent = Color.parseColor("#FFCC80"),
                keyBgAccentPressed = Color.parseColor("#FFB74D"),
                keyTextColorNormal = Color.parseColor("#EFEBE9"),
                keyTextColorModifier = Color.parseColor("#BCAAA4"),
                keyTextColorAccent = Color.parseColor("#3E2723"),
                isGlass = false,
                hasShadow = true,
                suggestionBarBg = Color.parseColor("#3E2723"),
                suggestionTextNormal = Color.parseColor("#8D6E63"),
                suggestionTextAccent = Color.parseColor("#FFCC80"),
                toolbarToggleColor = Color.parseColor("#FFCC80"),
                dividerColor = Color.parseColor("#301E1A"),
                clipboardCardBg = Color.parseColor("#4E342E"),
                clipboardCardPressed = Color.parseColor("#5D4037"),
                clipboardEmptyText = Color.parseColor("#8D6E63"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#FFCC80")
            )
            "theme_custom_wallpaper" -> ThemeColors(
                bgColor = Color.TRANSPARENT,
                keyBgNormal = Color.argb(35, 255, 255, 255),
                keyBgNormalPressed = Color.argb(85, 255, 255, 255),
                keyBgModifier = Color.argb(15, 255, 255, 255),
                keyBgModifierPressed = Color.argb(55, 255, 255, 255),
                keyBgAccent = Color.argb(120, 0, 229, 255),
                keyBgAccentPressed = Color.argb(200, 0, 229, 255),
                keyTextColorNormal = Color.WHITE,
                keyTextColorModifier = Color.WHITE,
                keyTextColorAccent = Color.BLACK,
                isGlass = true,
                hasShadow = false,
                suggestionBarBg = Color.argb(140, 18, 19, 26),
                suggestionTextNormal = Color.argb(200, 255, 255, 255),
                suggestionTextAccent = Color.parseColor("#00E5FF"),
                toolbarToggleColor = Color.parseColor("#00E5FF"),
                dividerColor = Color.argb(40, 255, 255, 255),
                clipboardCardBg = Color.argb(40, 255, 255, 255),
                clipboardCardPressed = Color.argb(80, 255, 255, 255),
                clipboardEmptyText = Color.argb(140, 255, 255, 255),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#00E5FF")
            )
            else -> ThemeColors(
                bgColor = Color.parseColor("#12131A"),
                keyBgNormal = Color.parseColor("#252736"),
                keyBgNormalPressed = Color.parseColor("#373A50"),
                keyBgModifier = Color.parseColor("#1B1C26"),
                keyBgModifierPressed = Color.parseColor("#2D2F40"),
                keyBgAccent = Color.parseColor("#00E5FF"),
                keyBgAccentPressed = Color.parseColor("#00B0FF"),
                keyTextColorNormal = Color.WHITE,
                keyTextColorModifier = Color.parseColor("#E0E0E0"),
                keyTextColorAccent = Color.BLACK,
                isGlass = false,
                hasShadow = true,
                suggestionBarBg = Color.parseColor("#1A1C24"),
                suggestionTextNormal = Color.parseColor("#A0A5B5"),
                suggestionTextAccent = Color.parseColor("#00E5FF"),
                toolbarToggleColor = Color.parseColor("#00E5FF"),
                dividerColor = Color.parseColor("#2E313D"),
                clipboardCardBg = Color.parseColor("#212330"),
                clipboardCardPressed = Color.parseColor("#323547"),
                clipboardEmptyText = Color.parseColor("#808495"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#00E5FF")
            )
        }
    }

    /**
     * Applies the current visual theme background to the keyboard view
     */
    fun applyTheme(forceRebuild: Boolean = false) {
        val selectedTheme = prefs.getString("selected_theme", "theme_midnight") ?: "theme_midnight"
        
        // If theme hasn't changed and we aren't forcing a rebuild, skip heavy operations
        if (!forceRebuild && selectedTheme == lastAppliedTheme && cachedThemeColors != null) {
            return
        }
        
        // Clear drawable cache on theme change
        if (selectedTheme != lastAppliedTheme) {
            drawableCache.clear()
        }

        currentTheme = selectedTheme
        lastAppliedTheme = currentTheme
        cachedThemeColors = getThemeColors()
        val colors = cachedThemeColors!!

        val density = resources.displayMetrics.density
        val padding = (6 * density).toInt()
        setPadding(padding, padding, padding, padding)

        when {
            currentTheme == "theme_custom_wallpaper" -> {
                if (cachedWallpaper != null) {
                    background = cachedWallpaper
                } else {
                    val wallpaperFile = File(context.filesDir, "custom_wallpaper.jpg")
                    if (wallpaperFile.exists()) {
                        try {
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 2 // Avoid OOM
                            }
                            val bitmap = BitmapFactory.decodeFile(wallpaperFile.absolutePath, options)
                            if (bitmap != null) {
                                cachedWallpaper = BitmapDrawable(resources, bitmap)
                                background = cachedWallpaper
                            } else {
                                // Fallback gradient
                                background = GradientDrawable(
                                    GradientDrawable.Orientation.TL_BR,
                                    intArrayOf(Color.parseColor("#1F1C2C"), Color.parseColor("#928DAB"))
                                )
                            }
                        } catch (e: Exception) {
                            background = GradientDrawable(
                                GradientDrawable.Orientation.TL_BR,
                                intArrayOf(Color.parseColor("#1F1C2C"), Color.parseColor("#928DAB"))
                            )
                        }
                    } else {
                        // Default beautiful sunset fallback for custom wallpaper
                        background = GradientDrawable(
                            GradientDrawable.Orientation.TL_BR,
                            intArrayOf(Color.parseColor("#1F1C2C"), Color.parseColor("#928DAB"))
                        )
                    }
                }
            }
            colors.bgGradient != null -> {
                cachedWallpaper = null
                val parsedColors = colors.bgGradient.map { Color.parseColor(it) }.toIntArray()
                background = GradientDrawable(GradientDrawable.Orientation.TL_BR, parsedColors)
            }
            else -> {
                cachedWallpaper = null
                setBackgroundColor(colors.bgColor)
            }
        }

        // Rebuild active view state
        when (viewState) {
            ViewState.KEYS -> buildKeyboard()
            ViewState.CLIPBOARD -> showClipboard()
            ViewState.THEME_SWITCHER -> showThemeSwitcher()
        }
    }

    fun buildKeyboard() {
        removeAllViews()
        viewState = ViewState.KEYS
        val rows = getRowsForCurrentState()
        val colors = cachedThemeColors ?: getThemeColors().also { cachedThemeColors = it }

        val density = resources.displayMetrics.density
        // Increased height slightly (54dp) for ultra-premium layout spacing
        val rowHeightPx = (54 * density).toInt()

        for ((index, rowKeys) in rows.withIndex()) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    rowHeightPx
                )
                gravity = Gravity.CENTER
            }

            val isRow2QWERTY = (keyboardMode == Mode.QWERTY && index == 1)
            if (isRow2QWERTY) {
                rowLayout.addView(createSpacer(0.5f))
            }

            for (key in rowKeys) {
                rowLayout.addView(createKeyView(key, colors))
            }

            if (isRow2QWERTY) {
                rowLayout.addView(createSpacer(0.5f))
            }

            addView(rowLayout)
        }
    }

    private fun getRowsForCurrentState(): List<List<KeyInfo>> {
        return when (keyboardMode) {
            Mode.QWERTY -> getQwertyRows()
            Mode.SYMBOLS -> getSymbolsRows()
            Mode.EXTRA_SYMBOLS -> getExtraSymbolsRows()
            Mode.EMOJI -> getEmojiRows()
        }
    }

    private fun getQwertyRows(): List<List<KeyInfo>> {
        val r1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val r2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val r3 = listOf("z", "x", "c", "v", "b", "n", "m")

        return listOf(
            r1.map { KeyInfo(formatText(it), it[0].code) },
            r2.map { KeyInfo(formatText(it), it[0].code) },
            listOf(
                KeyInfo(if (isShifted) "⬆" else "⇧", -1, weight = 1.35f, isModifier = true),
                *r3.map { KeyInfo(formatText(it), it[0].code) }.toTypedArray(),
                KeyInfo("⌫", -5, weight = 1.35f, isModifier = true)
            ),
            listOf(
                KeyInfo("?123", -2, weight = 1.3f, isModifier = true),
                KeyInfo("😊", -10, weight = 1.0f, isModifier = true),
                KeyInfo("likhibi", 32, weight = 4.4f), // Spacebar branding
                KeyInfo(".", 46, weight = 1.0f),
                KeyInfo(enterKeyLabel, 10, weight = 1.3f, isAccent = true)
            )
        )
    }

    private fun getSymbolsRows(): List<List<KeyInfo>> {
        val r1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val r2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
        val r3 = listOf(".", ",", "?", "!", "'")

        return listOf(
            r1.map { KeyInfo(it, it[0].code) },
            r2.map { KeyInfo(it, it[0].code) },
            listOf(
                KeyInfo("=\\<", -3, weight = 1.35f, isModifier = true),
                *r3.map { KeyInfo(it, it[0].code) }.toTypedArray(),
                KeyInfo("⌫", -5, weight = 1.35f, isModifier = true)
            ),
            listOf(
                KeyInfo("ABC", -2, weight = 1.3f, isModifier = true),
                KeyInfo("😊", -10, weight = 1.0f, isModifier = true),
                KeyInfo("space", 32, weight = 4.4f),
                KeyInfo(".", 46, weight = 1.0f),
                KeyInfo(enterKeyLabel, 10, weight = 1.3f, isAccent = true)
            )
        )
    }

    private fun getExtraSymbolsRows(): List<List<KeyInfo>> {
        val r1 = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
        val r2 = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
        val r3 = listOf(".", ",", "?", "!", "'")

        return listOf(
            r1.map { KeyInfo(it, it[0].code) },
            r2.map { KeyInfo(it, it[0].code) },
            listOf(
                KeyInfo("?123", -4, weight = 1.35f, isModifier = true),
                *r3.map { KeyInfo(it, it[0].code) }.toTypedArray(),
                KeyInfo("⌫", -5, weight = 1.35f, isModifier = true)
            ),
            listOf(
                KeyInfo("ABC", -2, weight = 1.3f, isModifier = true),
                KeyInfo("😊", -10, weight = 1.0f, isModifier = true),
                KeyInfo("space", 32, weight = 4.4f),
                KeyInfo(".", 46, weight = 1.0f),
                KeyInfo(enterKeyLabel, 10, weight = 1.3f, isAccent = true)
            )
        )
    }

    private fun getEmojiRows(): List<List<KeyInfo>> {
        val r1 = listOf("😂", "❤️", "👍", "😊", "🙏", "🔥", "😭", "😍")
        val r2 = listOf("😘", "🎉", "✨", "🤣", "💀", "🥺", "👀", "🥳")
        val r3 = listOf("😔", "😮", "😎", "👏", "💖", "👌", "💯", "🤝")

        return listOf(
            r1.map { KeyInfo(it, 0) },
            r2.map { KeyInfo(it, 0) },
            r3.map { KeyInfo(it, 0) },
            listOf(
                KeyInfo("ABC", -2, weight = 1.6f, isModifier = true),
                KeyInfo("space", 32, weight = 4.8f),
                KeyInfo("⌫", -5, weight = 1.6f, isModifier = true)
            )
        )
    }

    private fun formatText(text: String): String {
        return if (isShifted) text.uppercase() else text.lowercase()
    }

    private fun createKeyView(key: KeyInfo, colors: ThemeColors): View {
        val density = resources.displayMetrics.density

        val keyTextView = TextView(context).apply {
            text = if (isShifted && key.label.length == 1 && key.label[0].isLetter()) key.label.uppercase() else key.label
            gravity = Gravity.CENTER
            val selectedFont = getSelectedTypeface()
            typeface = if (key.isAccent) Typeface.create(selectedFont, Typeface.BOLD) else selectedFont

            // Text sizing & colors
            if (key.isAccent) {
                setTextColor(colors.keyTextColorAccent)
                textSize = 21f
            } else if (key.isModifier) {
                setTextColor(colors.keyTextColorModifier)
                textSize = 17f
            } else {
                setTextColor(colors.keyTextColorNormal)
                textSize = 20f
                if (key.code == 32) {
                    textSize = 14f
                    // space text color matching active styling
                    setTextColor(if (colors.isGlass) Color.argb(180, 255, 255, 255) else Color.parseColor("#7A8090"))
                }
            }

            // Normal and Pressed key styles
            val bgNormalColor = when {
                key.isAccent -> colors.keyBgAccent
                key.isModifier -> {
                    if (key.code == -1 && (isShifted || isCapsLock)) {
                        // Cyan highlight for Shift on OnePlus/Midnight, light glass on gradients
                        if (currentTheme == "theme_oneplus" || currentTheme == "theme_midnight") Color.parseColor("#00E5FF") else colors.keyBgAccent
                    } else colors.keyBgModifier
                }
                else -> colors.keyBgNormal
            }

            val bgPressedColor = when {
                key.isAccent -> colors.keyBgAccentPressed
                key.isModifier -> {
                    if (key.code == -1 && (isShifted || isCapsLock)) {
                        if (currentTheme == "theme_oneplus" || currentTheme == "theme_midnight") Color.parseColor("#00B0FF") else colors.keyBgAccentPressed
                    } else colors.keyBgModifierPressed
                }
                else -> colors.keyBgNormalPressed
            }

            // Calculate matching shadow color for 3D keycaps
            val shadowColor = when {
                key.isAccent -> Color.parseColor("#008FA3")
                key.isModifier -> Color.parseColor("#0F1015")
                else -> Color.parseColor("#181922")
            }

            // Apply premium 3D or Glassmorphic drawable
            background = getCachedKeyDrawable(
                normalColor = bgNormalColor,
                pressedColor = bgPressedColor,
                shadowColor = shadowColor,
                hasShadow = colors.hasShadow,
                radius = 8f * density
            )

            // Spacing layout setup with OnePlus & iOS margins
            val params = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, key.weight)
            params.setMargins(
                (3.5f * density).toInt(),
                (4.5f * density).toInt(),
                (3.5f * density).toInt(),
                (4.5f * density).toInt()
            )
            layoutParams = params

            // Snappy physics-based key press spring scaling & haptics
            var startX = 0f
            var isDraggingSpace = false

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        isDraggingSpace = false
                        v.animate().cancel()
                        // Visually translates down slightly to simulate physical click
                        v.animate().scaleX(0.92f).scaleY(0.92f).translationY(1.5f * density).setDuration(25).start()

                        val haptic = when {
                            key.isAccent -> HapticType.KEY_ACCENT
                            key.code == 32 -> HapticType.KEY_SPACE
                            key.isModifier -> HapticType.KEY_MODIFIER
                            else -> HapticType.KEY_STANDARD
                        }
                        performPremiumHaptic(haptic)

                        // Key popup letter bubble (iOS / Gboard style)
                        val shouldShowPopup = !key.isModifier && key.code != 32 && key.code != 10 && keyboardMode != Mode.EMOJI
                        if (shouldShowPopup) {
                            showKeyPreview(v, key.label)
                        }

                        if (key.code == -5) {
                            deleteRunnable = object : Runnable {
                                override fun run() {
                                    listener?.onKey(key.code)
                                    performPremiumHaptic(HapticType.KEY_MODIFIER)
                                    deleteHandler.postDelayed(this, 50L)
                                }
                            }
                            deleteHandler.postDelayed(deleteRunnable!!, 350L)
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (key.code == 32) {
                            val diffX = event.rawX - startX
                            if (Math.abs(diffX) > 25 * density) {
                                isDraggingSpace = true
                                if (diffX > 0) {
                                    listener?.onKey(Int.MAX_VALUE) // special code for right
                                } else {
                                    listener?.onKey(Int.MIN_VALUE) // special code for left
                                }
                                startX = event.rawX
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().cancel()
                        // Spring back bounce
                        v.animate().scaleX(1.0f).scaleY(1.0f).translationY(0f)
                            .setDuration(220)
                            .setInterpolator(OvershootInterpolator(2.4f))
                            .start()

                        dismissKeyPreview()
                        
                        if (key.code == -5) {
                            deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
                            deleteRunnable = null
                        }
                        if (event.action == MotionEvent.ACTION_UP && !isDraggingSpace) {
                            v.performClick()
                        }
                    }
                }
                true
            }

            setOnClickListener {
                if (keyboardMode == Mode.EMOJI && !key.isModifier && key.code != 32) {
                    listener?.onText(key.label)
                } else {
                    listener?.onKey(key.code)
                }
            }
        }

        return keyTextView
    }

    private fun createSpacer(weight: Float): View {
        return View(context).apply {
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
        }
    }

    /**
     * Caches drawables to prevent redundant object allocation for 30+ keys
     */
    private fun getCachedKeyDrawable(
        normalColor: Int,
        pressedColor: Int,
        shadowColor: Int,
        hasShadow: Boolean,
        radius: Float
    ): RippleDrawable {
        val key = "$normalColor-$pressedColor-$shadowColor-$hasShadow-$radius"
        val state = drawableCache.getOrPut(key) {
            createPremiumKeyDrawable(normalColor, pressedColor, shadowColor, hasShadow, radius).constantState!!
        }
        return state.newDrawable() as RippleDrawable
    }

    /**
     * Programmatic drawable generator: creates a gorgeous 3D keycap or frosted glass card
     */
    private fun createPremiumKeyDrawable(
        normalColor: Int,
        pressedColor: Int,
        shadowColor: Int,
        hasShadow: Boolean,
        radius: Float
    ): RippleDrawable {
        val contentDrawable = if (hasShadow) {
            val shadow = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(shadowColor)
            }
            val keycap = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(normalColor)
            }
            val layers = arrayOf(shadow, keycap)
            LayerDrawable(layers).apply {
                setLayerInset(1, 0, 0, 0, (2.5f * resources.displayMetrics.density).toInt())
            }
        } else {
            // Frosted glassmorphism background
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(normalColor)
                setStroke(1, Color.argb(45, 255, 255, 255))
            }
        }

        val maskDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.WHITE)
        }

        return RippleDrawable(
            ColorStateList.valueOf(pressedColor),
            contentDrawable,
            maskDrawable
        )
    }

    /**
     * Shows a gorgeous iOS/Gboard-style popup preview bubble floating above the touched key.
     */
    private fun showKeyPreview(keyView: View, label: String) {
        dismissKeyPreview()

        val density = resources.displayMetrics.density
        val colors = cachedThemeColors ?: getThemeColors()
        val popupWidth = (56 * density).toInt()
        val popupHeight = (64 * density).toInt()

        val popupView = TextView(context).apply {
            text = label
            textSize = 28f
            setTextColor(colors.keyTextColorNormal)
            gravity = Gravity.CENTER
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)

            val previewBg = colors.clipboardCardBg
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(previewBg)
                setStroke((1.5f * density).toInt(), colors.accentColor)
            }
            elevation = 8f * density
        }

        activePopup = PopupWindow(popupView, popupWidth, popupHeight).apply {
            isClippingEnabled = false
        }

        val location = IntArray(2)
        keyView.getLocationInWindow(location)

        val x = location[0] + (keyView.width - popupWidth) / 2
        val y = location[1] - popupHeight - (10 * density).toInt()

        try {
            activePopup?.showAtLocation(keyView, Gravity.NO_GRAVITY, x, y)
        } catch (e: Exception) {
        }
    }

    private fun dismissKeyPreview() {
        activePopup?.dismiss()
        activePopup = null
    }

    /**
     * Custom tactile feedback profiles dynamically scaled by user haptic strength setting
     */
    private fun performPremiumHaptic(type: HapticType) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        val strengthSetting = prefs.getInt("haptic_strength", 10) // default 10ms letter tap
        if (strengthSetting <= 0) return

        val duration = when (type) {
            HapticType.KEY_STANDARD -> strengthSetting.toLong()
            HapticType.KEY_SPACE -> (strengthSetting * 1.5).toLong()
            HapticType.KEY_MODIFIER -> (strengthSetting * 1.2).toLong()
            HapticType.KEY_ACCENT -> (strengthSetting * 2.0).toLong()
        }

        val amplitude = when (type) {
            HapticType.KEY_STANDARD -> 180
            HapticType.KEY_SPACE -> 140
            HapticType.KEY_MODIFIER -> 220
            HapticType.KEY_ACCENT -> 255
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))

        val soundEnabled = prefs.getBoolean("sound_enabled", true)
        if (soundEnabled && soundPool != null) {
            try {
                val baseVolume = prefs.getFloat("sound_volume", 0.5f)
                val volume = baseVolume * 0.4f // Scaled down for a softer thud
                val rate = when (type) {
                    HapticType.KEY_SPACE -> 0.7f // Deepest sound for space
                    HapticType.KEY_MODIFIER -> 0.75f
                    HapticType.KEY_ACCENT -> 0.85f
                    else -> 0.8f // Baseline soft tap
                }
                if (type == HapticType.KEY_SPACE) {
                    soundPool?.play(soundSpace, volume, volume, 1, 0, rate)
                } else {
                    soundPool?.play(soundClick, volume, volume, 1, 0, rate)
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Displays a gorgeous in-keyboard scrolling Clipboard Manager Shelf.
     * Tapping a clip inserts it. Swipe-like delete buttons clear items.
     */
    fun showClipboard() {
        removeAllViews()
        viewState = ViewState.CLIPBOARD
        val density = resources.displayMetrics.density
        val colors = cachedThemeColors ?: getThemeColors()

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt())
        }

        val title = TextView(context).apply {
            text = "Clipboard"
            setTextColor(colors.keyTextColorNormal)
            textSize = 15f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val clearBtn = TextView(context).apply {
            text = "Clear All"
            setTextColor(colors.deleteColor)
            textSize = 13f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = createPremiumKeyDrawable(
                Color.argb(20, Color.red(colors.deleteColor), Color.green(colors.deleteColor), Color.blue(colors.deleteColor)),
                Color.argb(50, Color.red(colors.deleteColor), Color.green(colors.deleteColor), Color.blue(colors.deleteColor)),
                0, false, 6f * density
            )
            setOnClickListener {
                prefs.edit().putString("clipboard_history", "").apply()
                showClipboard()
            }
        }
        header.addView(clearBtn)

        header.addView(View(context).apply { layoutParams = LayoutParams((12 * density).toInt(), 1) })

        val closeBtn = TextView(context).apply {
            text = "Close"
            setTextColor(colors.accentColor)
            textSize = 13f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = createPremiumKeyDrawable(
                Color.argb(20, Color.red(colors.accentColor), Color.green(colors.accentColor), Color.blue(colors.accentColor)),
                Color.argb(50, Color.red(colors.accentColor), Color.green(colors.accentColor), Color.blue(colors.accentColor)),
                0, false, 6f * density
            )
            setOnClickListener {
                viewState = ViewState.KEYS
                buildKeyboard()
            }
        }
        header.addView(closeBtn)
        addView(header)

        addView(View(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                setMargins(0, 0, 0, (6 * density).toInt())
            }
            setBackgroundColor(colors.dividerColor)
        })

        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (170 * density).toInt())
        }

        val listLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding((8 * density).toInt(), 0, (8 * density).toInt(), (8 * density).toInt())
        }

        val saved = prefs.getString("clipboard_history", "") ?: ""
        val clips = if (saved.isEmpty()) emptyList() else saved.split("[LIKHIBI_SPLIT]").filter { it.isNotEmpty() }

        if (clips.isEmpty()) {
            val emptyTxt = TextView(context).apply {
                text = "Clipboard is empty. Copied text will appear here."
                setTextColor(colors.clipboardEmptyText)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, (40 * density).toInt(), 0, 0)
            }
            listLayout.addView(emptyTxt)
        } else {
            for (clip in clips.reversed()) {
                val itemRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((12 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
                    layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, (4 * density).toInt(), 0, (4 * density).toInt())
                    }
                    background = createPremiumKeyDrawable(
                        colors.clipboardCardBg,
                        colors.clipboardCardPressed,
                        0,
                        false,
                        10f * density
                    )
                }

                val clipText = TextView(context).apply {
                    text = clip
                    setTextColor(colors.keyTextColorNormal)
                    textSize = 14f
                    maxLines = 2
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
                itemRow.addView(clipText)

                val deleteIcon = TextView(context).apply {
                    text = "×"
                    setTextColor(colors.deleteColor)
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
                    setOnClickListener {
                        val newList = clips.toMutableList().apply { remove(clip) }
                        prefs.edit().putString("clipboard_history", newList.joinToString("[LIKHIBI_SPLIT]")).apply()
                        showClipboard()
                    }
                }
                itemRow.addView(deleteIcon)

                itemRow.setOnClickListener {
                    listener?.onText(clip)
                    viewState = ViewState.KEYS
                    buildKeyboard()
                }

                listLayout.addView(itemRow)
            }
        }

        scrollView.addView(listLayout)
        addView(scrollView)
    }

    /**
     * Displays a gorgeous inline live Theme Picker Shelf.
     * Tapping any theme applies it instantly in the keyboard and saves it.
     */
    fun showThemeSwitcher() {
        removeAllViews()
        viewState = ViewState.THEME_SWITCHER
        val density = resources.displayMetrics.density
        val colors = cachedThemeColors ?: getThemeColors()

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt())
        }

        val title = TextView(context).apply {
            text = "Select Keyboard Theme"
            setTextColor(colors.keyTextColorNormal)
            textSize = 15f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val closeBtn = TextView(context).apply {
            text = "✕ Close"
            setTextColor(colors.accentColor)
            textSize = 13f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
            background = createPremiumKeyDrawable(
                Color.argb(20, Color.red(colors.accentColor), Color.green(colors.accentColor), Color.blue(colors.accentColor)),
                Color.argb(50, Color.red(colors.accentColor), Color.green(colors.accentColor), Color.blue(colors.accentColor)),
                0, false, 6f * density
            )
            setOnClickListener {
                viewState = ViewState.KEYS
                buildKeyboard()
            }
        }
        header.addView(closeBtn)
        addView(header)

        addView(View(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                setMargins(0, 0, 0, (12 * density).toInt())
            }
            setBackgroundColor(colors.dividerColor)
        })

        val scrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (160 * density).toInt())
            isHorizontalScrollBarEnabled = false
        }

        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
        }

        val themePresets = listOf(
            Triple("theme_classic_light", "Likhibi's Light", listOf("#F5F5F0", "#D32F2F")),
            Triple("theme_oled_black", "Likhibi's Dark", listOf("#000000", "#121212")),
            Triple("theme_slate_grey", "Slate Grey", listOf("#263238", "#80CBC4")),
            Triple("theme_navy_blue", "Navy Blue", listOf("#0D1B2A", "#E0E1DD")),
            Triple("theme_earth_tone", "Earth Tone", listOf("#3E2723", "#FFCC80")),
            Triple("theme_custom_wallpaper", "Custom Photo", listOf("#1F1C2C", "#928DAB"))
        )

        for ((id, name, colorsList) in themePresets) {
            val itemCard = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding((12 * density).toInt(), (14 * density).toInt(), (12 * density).toInt(), (14 * density).toInt())
                layoutParams = LayoutParams((110 * density).toInt(), (130 * density).toInt()).apply {
                    setMargins((6 * density).toInt(), 0, (6 * density).toInt(), 0)
                }

                val cardBgColor = if (currentTheme == id) colors.clipboardCardPressed else colors.clipboardCardBg
                background = createPremiumKeyDrawable(
                    cardBgColor,
                    colors.clipboardCardPressed,
                    if (currentTheme == id) colors.accentColor else Color.parseColor("#0E0F14"),
                    true,
                    14f * density
                )
            }

            val dot = View(context).apply {
                layoutParams = LayoutParams((38 * density).toInt(), (38 * density).toInt()).apply {
                    setMargins(0, 0, 0, (12 * density).toInt())
                }
                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    colorsList.map { Color.parseColor(it) }.toIntArray()
                ).apply {
                    shape = GradientDrawable.OVAL
                    if (currentTheme == id) {
                        setStroke((2 * density).toInt(), Color.WHITE)
                    }
                }
            }
            itemCard.addView(dot)

            val nameTxt = TextView(context).apply {
                text = name
                setTextColor(if (currentTheme == id) colors.accentColor else colors.keyTextColorNormal)
                textSize = 12f
                gravity = Gravity.CENTER
                typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            }
            itemCard.addView(nameTxt)

            itemCard.setOnClickListener {
                prefs.edit().putString("selected_theme", id).apply()
                cachedThemeColors = null
                themeChangeListener?.invoke()
                viewState = ViewState.KEYS
                applyTheme(forceRebuild = true)
            }

            container.addView(itemCard)
        }

        scrollView.addView(container)
        addView(scrollView)
    }
}
