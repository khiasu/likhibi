package com.likhibi.android

import com.likhibi.keyboard.R
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
    private var currentTheme = "theme_midnight_glass"
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
        val accentColor: Int = Color.parseColor("#00E5FF"),
        val cornerRadiusDp: Float = 8f,
        val borderWidthDp: Float = 0f,
        val borderColor: Int = Color.TRANSPARENT
    )

    data class KeyInfo(
        val label: String,
        val code: Int,
        val weight: Float = 1.0f,
        val isModifier: Boolean = false,
        val isAccent: Boolean = false
    )

    var themeChangeListener: (() -> Unit)? = null
    var wallpaperPickerListener: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        currentTheme = prefs.getString("selected_theme", "theme_midnight_glass") ?: "theme_midnight_glass"
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
        val theme = prefs.getString("selected_theme", "theme_midnight_glass") ?: "theme_midnight_glass"
        return when (theme) {

            "theme_midnight_glass" -> ThemeColors(
                bgColor = Color.parseColor("#080C18"),
                keyBgNormal = Color.argb(140, 25, 35, 60),
                keyBgNormalPressed = Color.argb(200, 40, 55, 90),
                keyBgModifier = Color.argb(100, 18, 25, 45),
                keyBgModifierPressed = Color.argb(170, 35, 48, 80),
                keyBgAccent = Color.parseColor("#00E5FF"),
                keyBgAccentPressed = Color.parseColor("#00B8D4"),
                keyTextColorNormal = Color.parseColor("#E4E8F0"),
                keyTextColorModifier = Color.parseColor("#7D8CAA"),
                keyTextColorAccent = Color.parseColor("#040810"),
                isGlass = true,
                hasShadow = false,
                cornerRadiusDp = 10f,
                borderWidthDp = 0.75f,
                borderColor = Color.argb(40, 80, 140, 255),
                suggestionBarBg = Color.parseColor("#0A0F1E"),
                suggestionTextNormal = Color.parseColor("#8899B8"),
                suggestionTextAccent = Color.parseColor("#00E5FF"),
                toolbarToggleColor = Color.parseColor("#00E5FF"),
                dividerColor = Color.parseColor("#1A2040"),
                clipboardCardBg = Color.argb(140, 20, 30, 55),
                clipboardCardPressed = Color.argb(200, 35, 50, 85),
                clipboardEmptyText = Color.parseColor("#556688"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#00E5FF")
            )
            "theme_pure_minimal" -> ThemeColors(
                bgColor = Color.parseColor("#F7F7F5"),
                keyBgNormal = Color.parseColor("#FFFFFF"),
                keyBgNormalPressed = Color.parseColor("#ECECEA"),
                keyBgModifier = Color.parseColor("#EEEEED"),
                keyBgModifierPressed = Color.parseColor("#E0E0DE"),
                keyBgAccent = Color.parseColor("#303030"),
                keyBgAccentPressed = Color.parseColor("#1A1A1A"),
                keyTextColorNormal = Color.parseColor("#2A2A2A"),
                keyTextColorModifier = Color.parseColor("#8A8A8A"),
                keyTextColorAccent = Color.parseColor("#FFFFFF"),
                isGlass = false,
                hasShadow = false,
                cornerRadiusDp = 6f,
                borderWidthDp = 0.5f,
                borderColor = Color.parseColor("#E0E0DE"),
                suggestionBarBg = Color.parseColor("#F7F7F5"),
                suggestionTextNormal = Color.parseColor("#888888"),
                suggestionTextAccent = Color.parseColor("#2A2A2A"),
                toolbarToggleColor = Color.parseColor("#2A2A2A"),
                dividerColor = Color.parseColor("#E8E8E6"),
                clipboardCardBg = Color.parseColor("#FFFFFF"),
                clipboardCardPressed = Color.parseColor("#F0F0EE"),
                clipboardEmptyText = Color.parseColor("#AAAAAA"),
                deleteColor = Color.parseColor("#E53935"),
                accentColor = Color.parseColor("#2A2A2A")
            )
            "theme_liquid_glass" -> ThemeColors(
                bgColor = Color.parseColor("#0B0F1C"),
                keyBgNormal = Color.argb(45, 255, 255, 255),
                keyBgNormalPressed = Color.argb(95, 255, 255, 255),
                keyBgModifier = Color.argb(25, 255, 255, 255),
                keyBgModifierPressed = Color.argb(65, 255, 255, 255),
                keyBgAccent = Color.parseColor("#00D2FF"),
                keyBgAccentPressed = Color.parseColor("#00A3FF"),
                keyTextColorNormal = Color.WHITE,
                keyTextColorModifier = Color.parseColor("#BACBE2"),
                keyTextColorAccent = Color.parseColor("#040C18"),
                isGlass = true,
                hasShadow = false,
                cornerRadiusDp = 14f,
                borderWidthDp = 1f,
                borderColor = Color.argb(85, 255, 255, 255),
                suggestionBarBg = Color.parseColor("#080D1A"),
                suggestionTextNormal = Color.parseColor("#8E9EB8"),
                suggestionTextAccent = Color.parseColor("#00D2FF"),
                toolbarToggleColor = Color.parseColor("#00D2FF"),
                dividerColor = Color.argb(40, 0, 210, 255),
                clipboardCardBg = Color.argb(45, 255, 255, 255),
                clipboardCardPressed = Color.argb(95, 255, 255, 255),
                clipboardEmptyText = Color.parseColor("#7A8CA8"),
                deleteColor = Color.parseColor("#FF453A"),
                accentColor = Color.parseColor("#00D2FF")
            )
            "theme_material_you" -> ThemeColors(
                bgColor = Color.parseColor("#FEF7FF"),
                keyBgNormal = Color.parseColor("#FFFFFF"),
                keyBgNormalPressed = Color.parseColor("#E8DEF8"),
                keyBgModifier = Color.parseColor("#E8DEF8"),
                keyBgModifierPressed = Color.parseColor("#D0BCFF"),
                keyBgAccent = Color.parseColor("#6750A4"),
                keyBgAccentPressed = Color.parseColor("#4F378B"),
                keyTextColorNormal = Color.parseColor("#1D1B20"),
                keyTextColorModifier = Color.parseColor("#49454F"),
                keyTextColorAccent = Color.parseColor("#FFFFFF"),
                isGlass = false,
                hasShadow = true,
                cornerRadiusDp = 16f,
                borderWidthDp = 0f,
                borderColor = Color.TRANSPARENT,
                suggestionBarBg = Color.parseColor("#FEF7FF"),
                suggestionTextNormal = Color.parseColor("#49454F"),
                suggestionTextAccent = Color.parseColor("#6750A4"),
                toolbarToggleColor = Color.parseColor("#6750A4"),
                dividerColor = Color.parseColor("#E7E0EC"),
                clipboardCardBg = Color.parseColor("#F3EDF7"),
                clipboardCardPressed = Color.parseColor("#E8DEF8"),
                clipboardEmptyText = Color.parseColor("#79747E"),
                deleteColor = Color.parseColor("#B3261E"),
                accentColor = Color.parseColor("#6750A4")
            )
            "theme_naga_heritage" -> {
                val isNight = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNight) {
                    ThemeColors(
                        bgColor = Color.parseColor("#121214"),
                        keyBgNormal = Color.parseColor("#1E1E22"),
                        keyBgNormalPressed = Color.parseColor("#323238"),
                        keyBgModifier = Color.parseColor("#161619"),
                        keyBgModifierPressed = Color.parseColor("#26262B"),
                        keyBgAccent = Color.parseColor("#D32F2F"),
                        keyBgAccentPressed = Color.parseColor("#B71C1C"),
                        keyTextColorNormal = Color.parseColor("#F5EFE6"),
                        keyTextColorModifier = Color.parseColor("#A8A096"),
                        keyTextColorAccent = Color.WHITE,
                        isGlass = false,
                        hasShadow = true,
                        cornerRadiusDp = 8f,
                        borderWidthDp = 0.75f,
                        borderColor = Color.parseColor("#2E181A"),
                        suggestionBarBg = Color.parseColor("#121214"),
                        suggestionTextNormal = Color.parseColor("#A8A096"),
                        suggestionTextAccent = Color.parseColor("#E53935"),
                        toolbarToggleColor = Color.parseColor("#E53935"),
                        dividerColor = Color.parseColor("#241A1C"),
                        clipboardCardBg = Color.parseColor("#1E1E22"),
                        clipboardCardPressed = Color.parseColor("#2C2C32"),
                        clipboardEmptyText = Color.parseColor("#7A7268"),
                        deleteColor = Color.parseColor("#E53935"),
                        accentColor = Color.parseColor("#E53935")
                    )
                } else {
                    ThemeColors(
                        bgColor = Color.parseColor("#F9F6F0"),
                        keyBgNormal = Color.parseColor("#FFFFFF"),
                        keyBgNormalPressed = Color.parseColor("#ECE6DC"),
                        keyBgModifier = Color.parseColor("#EFEAE0"),
                        keyBgModifierPressed = Color.parseColor("#E0D8CC"),
                        keyBgAccent = Color.parseColor("#D32F2F"),
                        keyBgAccentPressed = Color.parseColor("#B71C1C"),
                        keyTextColorNormal = Color.parseColor("#1A1A1E"),
                        keyTextColorModifier = Color.parseColor("#5A544A"),
                        keyTextColorAccent = Color.WHITE,
                        isGlass = false,
                        hasShadow = true,
                        cornerRadiusDp = 8f,
                        borderWidthDp = 0.5f,
                        borderColor = Color.parseColor("#E0D6C8"),
                        suggestionBarBg = Color.parseColor("#F9F6F0"),
                        suggestionTextNormal = Color.parseColor("#5A544A"),
                        suggestionTextAccent = Color.parseColor("#D32F2F"),
                        toolbarToggleColor = Color.parseColor("#D32F2F"),
                        dividerColor = Color.parseColor("#E5DED2"),
                        clipboardCardBg = Color.parseColor("#FFFFFF"),
                        clipboardCardPressed = Color.parseColor("#F0EAE0"),
                        clipboardEmptyText = Color.parseColor("#8A8072"),
                        deleteColor = Color.parseColor("#D32F2F"),
                        accentColor = Color.parseColor("#D32F2F")
                    )
                }
            }
            "theme_custom" -> {
                val keyOpacity = prefs.getInt("custom_key_opacity", 50).coerceIn(0, 255)
                val overlayOpacity = prefs.getInt("custom_overlay_opacity", 140).coerceIn(0, 255)
                val cornerRadius = prefs.getFloat("custom_corner_radius", 10f).coerceIn(0f, 22f)
                val borderWidth = prefs.getFloat("custom_border_width", 0.75f).coerceIn(0f, 3f)
                val borderOpacity = prefs.getInt("custom_border_opacity", 60).coerceIn(0, 255)
                val hasShadow = prefs.getBoolean("custom_key_has_shadow", false)
                val keyStyle = prefs.getString("custom_key_style", "glass") ?: "glass"
                val textMode = prefs.getString("custom_text_color_mode", "white") ?: "white"
                val accentColorValue = prefs.getInt("custom_accent_color", Color.parseColor("#00E5FF"))

                val keyBgNormalColor = when (keyStyle) {
                    "dark" -> Color.argb(keyOpacity.coerceAtLeast(160), 22, 24, 32)
                    "light" -> Color.argb(keyOpacity.coerceAtLeast(180), 245, 245, 248)
                    else -> Color.argb(keyOpacity, 255, 255, 255) // glass
                }
                val keyBgPressedColor = when (keyStyle) {
                    "dark" -> Color.argb((keyOpacity + 50).coerceAtMost(255), 45, 50, 65)
                    "light" -> Color.argb(255, 220, 220, 225)
                    else -> Color.argb((keyOpacity + 60).coerceAtMost(255), 255, 255, 255)
                }
                val keyBgModifierColor = when (keyStyle) {
                    "dark" -> Color.argb(keyOpacity.coerceAtLeast(160), 16, 18, 24)
                    "light" -> Color.argb(keyOpacity.coerceAtLeast(180), 232, 232, 236)
                    else -> Color.argb((keyOpacity - 20).coerceAtLeast(10), 255, 255, 255)
                }
                val keyBgModifierPressedColor = when (keyStyle) {
                    "dark" -> Color.argb((keyOpacity + 40).coerceAtMost(255), 35, 40, 52)
                    "light" -> Color.argb(255, 210, 210, 216)
                    else -> Color.argb((keyOpacity + 40).coerceAtMost(255), 255, 255, 255)
                }

                val textColor = when (textMode) {
                    "black" -> Color.parseColor("#141416")
                    "ivory" -> Color.parseColor("#F5EFE6")
                    "accent" -> accentColorValue
                    else -> Color.WHITE
                }
                val modTextColor = when (textMode) {
                    "black" -> Color.parseColor("#666666")
                    "ivory" -> Color.parseColor("#A8A096")
                    "accent" -> Color.argb(200, Color.red(accentColorValue), Color.green(accentColorValue), Color.blue(accentColorValue))
                    else -> Color.argb(220, 255, 255, 255)
                }

                ThemeColors(
                    bgColor = Color.TRANSPARENT,
                    keyBgNormal = keyBgNormalColor,
                    keyBgNormalPressed = keyBgPressedColor,
                    keyBgModifier = keyBgModifierColor,
                    keyBgModifierPressed = keyBgModifierPressedColor,
                    keyBgAccent = accentColorValue,
                    keyBgAccentPressed = Color.argb(230, Color.red(accentColorValue), Color.green(accentColorValue), Color.blue(accentColorValue)),
                    keyTextColorNormal = textColor,
                    keyTextColorModifier = modTextColor,
                    keyTextColorAccent = if (keyStyle == "light" || textMode == "black") Color.WHITE else Color.BLACK,
                    isGlass = (keyStyle == "glass"),
                    hasShadow = hasShadow,
                    cornerRadiusDp = cornerRadius,
                    borderWidthDp = borderWidth,
                    borderColor = Color.argb(borderOpacity, 255, 255, 255),
                    suggestionBarBg = Color.argb(overlayOpacity, 10, 10, 18),
                    suggestionTextNormal = modTextColor,
                    suggestionTextAccent = accentColorValue,
                    toolbarToggleColor = accentColorValue,
                    dividerColor = Color.argb(40, 255, 255, 255),
                    clipboardCardBg = Color.argb(keyOpacity.coerceAtLeast(40), 255, 255, 255),
                    clipboardCardPressed = Color.argb((keyOpacity + 50).coerceAtMost(255), 255, 255, 255),
                    clipboardEmptyText = Color.argb(140, 255, 255, 255),
                    deleteColor = Color.parseColor("#FF5252"),
                    accentColor = accentColorValue
                )
            }
            else -> ThemeColors(
                bgColor = Color.parseColor("#080C18"),
                keyBgNormal = Color.argb(140, 25, 35, 60),
                keyBgNormalPressed = Color.argb(200, 40, 55, 90),
                keyBgModifier = Color.argb(100, 18, 25, 45),
                keyBgModifierPressed = Color.argb(170, 35, 48, 80),
                keyBgAccent = Color.parseColor("#00E5FF"),
                keyBgAccentPressed = Color.parseColor("#00B8D4"),
                keyTextColorNormal = Color.parseColor("#E4E8F0"),
                keyTextColorModifier = Color.parseColor("#7D8CAA"),
                keyTextColorAccent = Color.parseColor("#040810"),
                isGlass = true,
                hasShadow = false,
                cornerRadiusDp = 10f,
                borderWidthDp = 0.75f,
                borderColor = Color.argb(40, 80, 140, 255),
                suggestionBarBg = Color.parseColor("#0A0F1E"),
                suggestionTextNormal = Color.parseColor("#8899B8"),
                suggestionTextAccent = Color.parseColor("#00E5FF"),
                toolbarToggleColor = Color.parseColor("#00E5FF"),
                dividerColor = Color.parseColor("#1A2040"),
                clipboardCardBg = Color.argb(140, 20, 30, 55),
                clipboardCardPressed = Color.argb(200, 35, 50, 85),
                clipboardEmptyText = Color.parseColor("#556688"),
                deleteColor = Color.parseColor("#FF5252"),
                accentColor = Color.parseColor("#00E5FF")
            )
        }
    }

    /**
     * Applies the current visual theme background to the keyboard view
     */
    fun applyTheme(forceRebuild: Boolean = false) {
        val selectedTheme = prefs.getString("selected_theme", "theme_midnight_glass") ?: "theme_midnight_glass"
        
        // If theme hasn't changed and we aren't forcing a rebuild, skip heavy operations
        if (!forceRebuild && selectedTheme == lastAppliedTheme && cachedThemeColors != null) {
            return
        }
        
        // Clear drawable and wallpaper cache on theme change or forced rebuild
        if (forceRebuild || selectedTheme != lastAppliedTheme) {
            drawableCache.clear()
            if (forceRebuild) cachedWallpaper = null
        }

        currentTheme = selectedTheme
        lastAppliedTheme = currentTheme
        cachedThemeColors = getThemeColors()
        val colors = cachedThemeColors!!

        val density = resources.displayMetrics.density
        setPadding((3 * density).toInt(), (2 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())

        when {
            currentTheme == "theme_custom" -> {
                val overlayDim = prefs.getInt("custom_overlay_opacity", 140)
                val dimColor = Color.argb(overlayDim.coerceIn(0, 220), 10, 10, 18)
                val wallpaperFile = File(context.filesDir, "custom_wallpaper.jpg")
                if (wallpaperFile.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(wallpaperFile.absolutePath)
                        if (bitmap != null) {
                            background = CenterCropWallpaperDrawable(bitmap, dimColor)
                        } else {
                            background = GradientDrawable(
                                GradientDrawable.Orientation.TL_BR,
                                intArrayOf(Color.parseColor("#12121A"), Color.parseColor("#1F1C2C"))
                            )
                        }
                    } catch (e: Exception) {
                        background = GradientDrawable(
                            GradientDrawable.Orientation.TL_BR,
                            intArrayOf(Color.parseColor("#12121A"), Color.parseColor("#1F1C2C"))
                        )
                    }
                } else {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(Color.parseColor("#12121A"), Color.parseColor("#1F1C2C"))
                    )
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
        val showNumberRow = prefs.getBoolean("show_number_row", false)
        val rowHeightPx = if (showNumberRow && keyboardMode == Mode.QWERTY) (44 * density).toInt() else (48 * density).toInt()

        val isAtoLRowIndex = if (showNumberRow) 2 else 1

        for ((index, rowKeys) in rows.withIndex()) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    rowHeightPx
                )
                gravity = Gravity.CENTER
            }

            val isRowAtoL = (keyboardMode == Mode.QWERTY && index == isAtoLRowIndex)
            if (isRowAtoL) {
                rowLayout.addView(createSpacer(0.5f))
            }

            for (key in rowKeys) {
                rowLayout.addView(createKeyView(key, colors))
            }

            if (isRowAtoL) {
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
        val showNumberRow = prefs.getBoolean("show_number_row", false)
        val r0 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val r1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
        val r2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        val r3 = listOf("z", "x", "c", "v", "b", "n", "m")

        val result = mutableListOf<List<KeyInfo>>()
        // Row 1 (Optional Number Row): 10 keys x 1.0f = 10.0f
        if (showNumberRow) {
            result.add(r0.map { KeyInfo(it, it[0].code, weight = 1.0f) })
        }
        // Row 2 (q..p): 10 keys x 1.0f = 10.0f
        result.add(r1.map { KeyInfo(formatText(it), it[0].code, weight = 1.0f) })
        // Row 3 (a..l): 0.5f spacer + 9 keys x 1.0f + 0.5f spacer = 10.0f
        result.add(r2.map { KeyInfo(formatText(it), it[0].code, weight = 1.0f) })
        // Row 4 (z..m): Shift (1.5f) + 7 keys x 1.0f + Backspace (1.5f) = 10.0f
        result.add(listOf(
            KeyInfo(if (isShifted) "⬆" else "⇧", -1, weight = 1.5f, isModifier = true),
            *r3.map { KeyInfo(formatText(it), it[0].code, weight = 1.0f) }.toTypedArray(),
            KeyInfo("⌫", -5, weight = 1.5f, isModifier = true)
        ))
        // Row 5 (Bottom Bar): ?123 (1.4f) + , (1.0f) + 😊 (1.0f) + Space (4.2f) + . (1.0f) + Enter (1.4f) = 10.0f
        result.add(listOf(
            KeyInfo("?123", -2, weight = 1.4f, isModifier = true),
            KeyInfo(",", 44, weight = 1.0f),
            KeyInfo("😊", -10, weight = 1.0f, isModifier = true),
            KeyInfo("likhibi", 32, weight = 4.2f),
            KeyInfo(".", 46, weight = 1.0f),
            KeyInfo(enterKeyLabel, 10, weight = 1.4f, isAccent = true)
        ))
        return result
    }

    private fun getSymbolsRows(): List<List<KeyInfo>> {
        val r1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        val r2 = listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
        val r3 = listOf("*", "\"", "'", ":", ";", "!", "?")

        return listOf(
            r1.map { KeyInfo(it, it[0].code, weight = 1.0f) },
            r2.map { KeyInfo(it, it[0].code, weight = 1.0f) },
            listOf(
                KeyInfo("=\\<", -3, weight = 1.5f, isModifier = true),
                *r3.map { KeyInfo(it, it[0].code, weight = 1.0f) }.toTypedArray(),
                KeyInfo("⌫", -5, weight = 1.5f, isModifier = true)
            ),
            listOf(
                KeyInfo("ABC", -2, weight = 1.4f, isModifier = true),
                KeyInfo(",", 44, weight = 1.0f),
                KeyInfo("😊", -10, weight = 1.0f, isModifier = true),
                KeyInfo("space", 32, weight = 4.2f),
                KeyInfo(".", 46, weight = 1.0f),
                KeyInfo(enterKeyLabel, 10, weight = 1.4f, isAccent = true)
            )
        )
    }

    private fun getExtraSymbolsRows(): List<List<KeyInfo>> {
        val r1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
        val r2 = listOf("£", "¥", "€", "¢", "^", "°", "=", "{", "}", "\\")
        val r3 = listOf("%", "_", "<", ">", "[", "]", "§")

        return listOf(
            r1.map { KeyInfo(it, it[0].code, weight = 1.0f) },
            r2.map { KeyInfo(it, it[0].code, weight = 1.0f) },
            listOf(
                KeyInfo("?123", -4, weight = 1.5f, isModifier = true),
                *r3.map { KeyInfo(it, it[0].code, weight = 1.0f) }.toTypedArray(),
                KeyInfo("⌫", -5, weight = 1.5f, isModifier = true)
            ),
            listOf(
                KeyInfo("ABC", -2, weight = 1.4f, isModifier = true),
                KeyInfo(",", 44, weight = 1.0f),
                KeyInfo("😊", -10, weight = 1.0f, isModifier = true),
                KeyInfo("space", 32, weight = 4.2f),
                KeyInfo(".", 46, weight = 1.0f),
                KeyInfo(enterKeyLabel, 10, weight = 1.4f, isAccent = true)
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
                    setTextColor(if (colors.isGlass) Color.argb(180, 255, 255, 255) else colors.suggestionTextNormal)
                }
            }

            // Normal and Pressed key styles
            val bgNormalColor = when {
                key.isAccent -> colors.keyBgAccent
                key.isModifier -> {
                    if (key.code == -1 && (isShifted || isCapsLock)) colors.accentColor
                    else colors.keyBgModifier
                }
                else -> colors.keyBgNormal
            }

            val bgPressedColor = when {
                key.isAccent -> colors.keyBgAccentPressed
                key.isModifier -> {
                    if (key.code == -1 && (isShifted || isCapsLock)) colors.keyBgAccentPressed
                    else colors.keyBgModifierPressed
                }
                else -> colors.keyBgNormalPressed
            }

            // Dynamic shadow color — darkens key bg for natural 3D depth on any theme
            val shadowColor = when {
                key.isAccent -> darkenColor(colors.keyBgAccent, 0.35f)
                key.isModifier -> darkenColor(colors.keyBgModifier, 0.4f)
                else -> darkenColor(colors.keyBgNormal, 0.35f)
            }

            // Apply premium 3D or Glassmorphic drawable
            background = getCachedKeyDrawable(
                normalColor = bgNormalColor,
                pressedColor = bgPressedColor,
                shadowColor = shadowColor,
                hasShadow = colors.hasShadow,
                radius = colors.cornerRadiusDp * density,
                borderWidthDp = colors.borderWidthDp,
                borderColor = colors.borderColor
            )

            // Exact Gboard key margins and spacing
            val params = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, key.weight)
            params.setMargins(
                (2.5f * density).toInt(),
                (2.5f * density).toInt(),
                (2.5f * density).toInt(),
                (2.5f * density).toInt()
            )
            layoutParams = params

            // Blazing-fast touch listener with instant ACTION_DOWN registration & zero UI jank
            var startX = 0f
            var isDraggingSpace = false
            var spaceLongPressTriggered = false
            var spaceLongPressRunnable: Runnable? = null
            val spaceHandler = Handler(Looper.getMainLooper())

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.rawX
                        isDraggingSpace = false
                        spaceLongPressTriggered = false
                        v.isPressed = true

                        // Haptic feedback (instant)
                        val haptic = when {
                            key.isAccent -> HapticType.KEY_ACCENT
                            key.code == 32 -> HapticType.KEY_SPACE
                            key.isModifier -> HapticType.KEY_MODIFIER
                            else -> HapticType.KEY_STANDARD
                        }
                        performPremiumHaptic(haptic)

                        // Instant key dispatch on ACTION_DOWN for zero-latency fluid typing
                        if (key.code != 32) { // Space is handled on down or drag
                            if (keyboardMode == Mode.EMOJI && !key.isModifier) {
                                listener?.onText(key.label)
                            } else {
                                listener?.onKey(key.code)
                            }
                        } else {
                            // Spacebar: commit space on down
                            listener?.onKey(32)
                            spaceLongPressRunnable = Runnable {
                                spaceLongPressTriggered = true
                                performPremiumHaptic(HapticType.KEY_ACCENT)
                                listener?.onKey(-11) // Switch input method / keyboard
                            }
                            spaceHandler.postDelayed(spaceLongPressRunnable!!, 450L)
                        }

                        // Continuous backspace repeat
                        if (key.code == -5) {
                            deleteRunnable = object : Runnable {
                                override fun run() {
                                    listener?.onKey(key.code)
                                    performPremiumHaptic(HapticType.KEY_MODIFIER)
                                    deleteHandler.postDelayed(this, 50L)
                                }
                            }
                            deleteHandler.postDelayed(deleteRunnable!!, 320L)
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (key.code == 32) {
                            val diffX = event.rawX - startX
                            if (Math.abs(diffX) > 18 * density) {
                                isDraggingSpace = true
                                spaceLongPressRunnable?.let { spaceHandler.removeCallbacks(it) }
                                if (diffX > 0) {
                                    listener?.onKey(Int.MAX_VALUE) // D-pad right
                                } else {
                                    listener?.onKey(Int.MIN_VALUE) // D-pad left
                                }
                                startX = event.rawX
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        spaceLongPressRunnable?.let { spaceHandler.removeCallbacks(it) }
                        spaceLongPressRunnable = null

                        if (key.code == -5) {
                            deleteRunnable?.let { deleteHandler.removeCallbacks(it) }
                            deleteRunnable = null
                        }
                    }
                }
                true
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
        radius: Float,
        borderWidthDp: Float = 0f,
        borderColor: Int = Color.TRANSPARENT
    ): RippleDrawable {
        val key = "$normalColor-$pressedColor-$shadowColor-$hasShadow-$radius-$borderWidthDp-$borderColor"
        val state = drawableCache.getOrPut(key) {
            createPremiumKeyDrawable(normalColor, pressedColor, shadowColor, hasShadow, radius, borderWidthDp, borderColor).constantState!!
        }
        return state.newDrawable() as RippleDrawable
    }

    /**
     * Utility: darken a color by a factor (0.0 = no change, 1.0 = fully black)
     */
    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * (1f - factor)).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * (1f - factor)).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(color).coerceAtLeast(80), r, g, b)
    }

    private fun createPremiumKeyDrawable(
        normalColor: Int,
        pressedColor: Int,
        shadowColor: Int,
        hasShadow: Boolean,
        radius: Float,
        borderWidthDp: Float = 0f,
        borderColor: Int = Color.TRANSPARENT
    ): RippleDrawable {
        val density = resources.displayMetrics.density
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
                setLayerInset(1, 0, 0, 0, (2.5f * density).toInt())
            }
        } else {
            // Frosted glassmorphism / minimal flat key background
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(normalColor)
                val bwPx = if (borderWidthDp > 0f) (borderWidthDp * density).toInt().coerceAtLeast(1) else 0
                if (bwPx > 0 && borderColor != Color.TRANSPARENT) {
                    setStroke(bwPx, borderColor)
                }
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

    private fun showKeyPreview(keyView: View, label: String) {
        // Native ripple & instant keycaps provide clean, 120fps fluid feedback without popup blocking
    }

    private fun dismissKeyPreview() {
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
            setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
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

        val saved = prefs.getString("clipboard_history", "") ?: ""
        val clips = if (saved.isEmpty()) emptyList() else saved.split("[LIKHIBI_SPLIT]").filter { it.isNotEmpty() }

        if (clips.isNotEmpty()) {
            val clearBtn = android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_trash_clean)
                setColorFilter(colors.deleteColor)
                layoutParams = LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                    setMargins(0, 0, (8 * density).toInt(), 0)
                }
                setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
                background = createPremiumKeyDrawable(
                    Color.argb(20, Color.red(colors.deleteColor), Color.green(colors.deleteColor), Color.blue(colors.deleteColor)),
                    Color.argb(50, Color.red(colors.deleteColor), Color.green(colors.deleteColor), Color.blue(colors.deleteColor)),
                    0, false, 8f * density
                )
                setOnClickListener {
                    prefs.edit().putString("clipboard_history", "").apply()
                    showClipboard()
                }
            }
            header.addView(clearBtn)
        }

        val closeBtn = android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_close_clean)
            setColorFilter(colors.keyTextColorModifier)
            layoutParams = LayoutParams((32 * density).toInt(), (32 * density).toInt())
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            background = createPremiumKeyDrawable(
                Color.argb(20, 255, 255, 255),
                Color.argb(50, 255, 255, 255),
                0, false, 8f * density
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
            setPadding((10 * density).toInt(), 0, (10 * density).toInt(), (8 * density).toInt())
        }

        if (clips.isEmpty()) {
            val emptyLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, (36 * density).toInt(), 0, 0)
            }
            val emptyIcon = android.widget.ImageView(context).apply {
                setImageResource(R.drawable.ic_tool_clipboard)
                setColorFilter(colors.clipboardEmptyText)
                layoutParams = LayoutParams((32 * density).toInt(), (32 * density).toInt()).apply {
                    setMargins(0, 0, 0, (8 * density).toInt())
                }
                alpha = 0.5f
            }
            emptyLayout.addView(emptyIcon)
            val emptyTxt = TextView(context).apply {
                text = "Clipboard is empty\nCopied text will appear here"
                setTextColor(colors.clipboardEmptyText)
                textSize = 13f
                gravity = Gravity.CENTER
            }
            emptyLayout.addView(emptyTxt)
            listLayout.addView(emptyLayout)
        } else {
            for (clip in clips.reversed()) {
                val itemRow = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding((12 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
                    layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        setMargins(0, (3 * density).toInt(), 0, (3 * density).toInt())
                    }
                    background = createPremiumKeyDrawable(
                        colors.clipboardCardBg,
                        colors.clipboardCardPressed,
                        0,
                        false,
                        10f * density,
                        colors.borderWidthDp,
                        colors.borderColor
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

                val deleteIcon = android.widget.ImageView(context).apply {
                    setImageResource(R.drawable.ic_close_clean)
                    setColorFilter(colors.clipboardEmptyText)
                    layoutParams = LayoutParams((24 * density).toInt(), (24 * density).toInt())
                    setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt())
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
            setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt())
        }

        val title = TextView(context).apply {
            text = "Themes"
            setTextColor(colors.keyTextColorNormal)
            textSize = 16f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        val wallpaperBtn = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding((10 * density).toInt(), (5 * density).toInt(), (10 * density).toInt(), (5 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (32 * density).toInt()).apply {
                setMargins(0, 0, (8 * density).toInt(), 0)
            }
            background = createPremiumKeyDrawable(
                Color.argb(25, Color.red(colors.accentColor), Color.green(colors.accentColor), Color.blue(colors.accentColor)),
                Color.argb(60, Color.red(colors.accentColor), Color.green(colors.accentColor), Color.blue(colors.accentColor)),
                0, false, 16f * density
            )
            setOnClickListener {
                wallpaperPickerListener?.invoke()
            }
        }
        val wpIcon = android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_wallpaper_clean)
            setColorFilter(colors.accentColor)
            layoutParams = LinearLayout.LayoutParams((16 * density).toInt(), (16 * density).toInt()).apply {
                setMargins(0, 0, (5 * density).toInt(), 0)
            }
        }
        wallpaperBtn.addView(wpIcon)
        val wpText = TextView(context).apply {
            text = "Wallpaper"
            setTextColor(colors.accentColor)
            textSize = 12f
            typeface = Typeface.create(getSelectedTypeface(), Typeface.BOLD)
        }
        wallpaperBtn.addView(wpText)
        header.addView(wallpaperBtn)

        val closeBtn = android.widget.ImageView(context).apply {
            setImageResource(R.drawable.ic_close_clean)
            setColorFilter(colors.keyTextColorModifier)
            layoutParams = LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt())
            setPadding((6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt(), (6 * density).toInt())
            background = createPremiumKeyDrawable(
                Color.argb(20, 255, 255, 255),
                Color.argb(50, 255, 255, 255),
                0, false, 8f * density
            )
            setOnClickListener {
                viewState = ViewState.KEYS
                buildKeyboard()
            }
        }
        header.addView(closeBtn)
        addView(header)

        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                setMargins(0, 0, 0, (8 * density).toInt())
            }
            setBackgroundColor(colors.dividerColor)
        })

        val scrollView = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (165 * density).toInt())
            isHorizontalScrollBarEnabled = false
        }

        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
        }

        val themePresets = listOf(
            Triple("theme_midnight_glass", "Midnight Glass", Triple("#080C18", "#1E2740", "#00E5FF")),
            Triple("theme_pure_minimal", "Pure Minimal", Triple("#F7F7F5", "#FFFFFF", "#303030")),
            Triple("theme_liquid_glass", "Liquid Glass", Triple("#0B0F1C", "#253550", "#00D2FF")),
            Triple("theme_material_you", "Material You", Triple("#FEF7FF", "#FFFFFF", "#6750A4")),
            Triple("theme_naga_heritage", "Naga Heritage", Triple("#121214", "#1E1E22", "#D32F2F")),
            Triple("theme_custom", "Custom Photo", Triple("#12121A", "#2A2A38", "#00E5FF"))
        )

        for ((id, name, palette) in themePresets) {
            val isSelected = (currentTheme == id)
            val cardBg = Color.parseColor(palette.first)
            val keyColor = Color.parseColor(palette.second)
            val accentClr = Color.parseColor(palette.third)

            val itemCard = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
                layoutParams = LinearLayout.LayoutParams((114 * density).toInt(), (145 * density).toInt()).apply {
                    setMargins((6 * density).toInt(), 0, (6 * density).toInt(), 0)
                }

                background = createPremiumKeyDrawable(
                    if (isSelected) Color.argb(45, Color.red(accentClr), Color.green(accentClr), Color.blue(accentClr)) else colors.clipboardCardBg,
                    colors.clipboardCardPressed,
                    0,
                    false,
                    14f * density,
                    if (isSelected) 1.5f else 0.5f,
                    if (isSelected) accentClr else Color.argb(25, 255, 255, 255)
                )
            }

            // Mini Keyboard Preview Box (with adaptive contrast border)
            val isLightTheme = (id == "theme_pure_minimal" || id == "theme_material_you")
            val previewBox = LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LinearLayout.LayoutParams((98 * density).toInt(), (66 * density).toInt()).apply {
                    setMargins(0, (2 * density).toInt(), 0, (10 * density).toInt())
                }
                setPadding((5 * density).toInt(), (6 * density).toInt(), (5 * density).toInt(), (6 * density).toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 8f * density
                    setColor(cardBg)
                    setStroke(
                        (1f * density).toInt(),
                        if (isLightTheme) Color.parseColor("#CCCCCC") else Color.argb(60, 255, 255, 255)
                    )
                }
            }

            // Preview Row 1
            val pRow1 = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (12 * density).toInt()).apply {
                    setMargins(0, 0, 0, (4 * density).toInt())
                }
            }
            for (k in 0..5) {
                pRow1.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                        setMargins((1 * density).toInt(), 0, (1 * density).toInt(), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 3f * density
                        setColor(keyColor)
                    }
                })
            }
            previewBox.addView(pRow1)

            // Preview Row 2
            val pRow2 = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (12 * density).toInt()).apply {
                    setMargins(0, 0, 0, (4 * density).toInt())
                }
            }
            for (k in 0..4) {
                pRow2.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                        setMargins((1 * density).toInt(), 0, (1 * density).toInt(), 0)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 3f * density
                        setColor(keyColor)
                    }
                })
            }
            previewBox.addView(pRow2)

            // Preview Row 3 (Bottom with Accent Enter Key)
            val pRow3 = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (12 * density).toInt())
            }
            pRow3.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.2f).apply {
                    setMargins((1 * density).toInt(), 0, (1 * density).toInt(), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 3f * density
                    setColor(keyColor)
                }
            })
            pRow3.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3f).apply {
                    setMargins((1 * density).toInt(), 0, (1 * density).toInt(), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 3f * density
                    setColor(keyColor)
                }
            })
            pRow3.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f).apply {
                    setMargins((1 * density).toInt(), 0, (1 * density).toInt(), 0)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 3f * density
                    setColor(accentClr)
                }
            })
            previewBox.addView(pRow3)

            itemCard.addView(previewBox)

            // Theme Name & Check Badge
            val nameRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val nameTxt = TextView(context).apply {
                text = name
                setTextColor(if (isSelected) accentClr else colors.keyTextColorNormal)
                textSize = 12f
                gravity = Gravity.CENTER
                typeface = Typeface.create(getSelectedTypeface(), if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            }
            nameRow.addView(nameTxt)

            if (isSelected) {
                val checkIcon = android.widget.ImageView(context).apply {
                    setImageResource(R.drawable.ic_check_clean)
                    setColorFilter(accentClr)
                    layoutParams = LinearLayout.LayoutParams((14 * density).toInt(), (14 * density).toInt()).apply {
                        setMargins((4 * density).toInt(), 0, 0, 0)
                    }
                }
                nameRow.addView(checkIcon)
            }

            itemCard.addView(nameRow)

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

/**
 * CenterCropWallpaperDrawable scales and crops a bitmap to fill the view bounds
 * without distortion and without reporting intrinsic width/height, ensuring
 * that the keyboard height is never inflated or expanded by the image size.
 */
class CenterCropWallpaperDrawable(
    private val bitmap: android.graphics.Bitmap,
    private val overlayColor: Int
) : android.graphics.drawable.Drawable() {
    private val drawMatrix = android.graphics.Matrix()
    private val bitmapPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = android.graphics.Paint().apply {
        color = overlayColor
        style = android.graphics.Paint.Style.FILL
    }

    override fun draw(canvas: android.graphics.Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f || bitmap.isRecycled) return

        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val scale = Math.max(w / bw, h / bh)
        val dx = (w - bw * scale) * 0.5f
        val dy = (h - bh * scale) * 0.5f

        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(dx, dy)

        canvas.save()
        canvas.clipRect(bounds)
        canvas.drawBitmap(bitmap, drawMatrix, bitmapPaint)
        if (android.graphics.Color.alpha(overlayColor) > 0) {
            canvas.drawRect(bounds, overlayPaint)
        }
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        bitmapPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        bitmapPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

    // CRITICAL: Returning -1 ensures LinearLayout wrap_content is NEVER expanded by the bitmap dimensions!
    override fun getIntrinsicWidth(): Int = -1
    override fun getIntrinsicHeight(): Int = -1
}
