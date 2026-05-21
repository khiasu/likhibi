# Project Context: Likhibi Keyboard

## Overview
Likhibi Keyboard is an AI-assisted Android input method (IME) specifically designed for the **Nagamese language** (a creole spoken in Nagaland, India). It combines a high-performance offline engine with deep Gemini AI integration for context-aware predictions.

## Core Architecture
- **LikhibiImeService.kt**: The main `InputMethodService`. Manages the lifecycle, input connection, toolbar toggling, and coordinates between the UI and the prediction engines.
- **CustomKeyboardView.kt**: A performance-optimized, programmatically drawn keyboard. It handles themes (including custom wallpapers), haptics, key previews, and specialized "shelves" for Clipboard and Theme switching.
- **NagameseOfflineEngine.kt**: Handles instant local suggestions. It contains a pre-compiled dictionary of 430+ Nagamese words and thousands of bigram transitions. Uses `lazy` initialization and background pre-warming to keep the main thread free.
- **GeminiClient.kt**: Connects to Google's Gemini API (currently using `gemini-1.5-flash`) for advanced next-word predictions based on the last 5 words typed.

## Critical Optimizations (Performance & Stability)
The following fixes were implemented to resolve "App Not Responding" (ANR) and "Keeps Stopping" errors:

1.  **Main Thread Offloading**:
    *   Nagamese dictionary and bigrams use `by lazy` to prevent lag during service startup.
    *   Initial dictionary "warming" happens on a background thread in `onCreate`.
2.  **Resource Caching**:
    *   **Drawable Cache**: Keyboard key backgrounds (Shadows/Ripples) are cached via `ConstantState` in `CustomKeyboardView` to prevent thousands of object allocations during typing.
    *   **Wallpaper Cache**: Custom wallpaper bitmaps are decoded once and stored as `BitmapDrawable`.
    *   **Theme Cache**: `ThemeColors` are parsed once per theme change.
3.  **UI Efficiency**:
    *   `resetState()`: Consolidates Shift, Mode, and Theme updates into a single UI rebuild.
    *   `applyTheme()`: Skip logic prevents redundant view hierarchy reconstruction if the theme hasn't changed.
4.  **Crash Fixes**:
    *   Added `android:theme="@style/Theme.LikhibiKeyboard"` to the service in `AndroidManifest.xml` to ensure system attributes (like `selectableItemBackgroundBorderless`) resolve correctly.
    *   Switched to `gemini-1.5-flash` endpoint and removed `thinkingConfig` for broader API compatibility.

## Key Files
- `app/src/main/java/com/likhibi/keyboard/`: Contains all logic.
- `app/src/main/res/layout/ime_view.xml`: Defines the toolbar and the container for `CustomKeyboardView`.
- `app/src/main/res/xml/method.xml` & `qwerty.xml`: Keyboard metadata and resource-based definitions.

## Build & Test Info
- **Package Name**: `com.likhibi.keyboard`
- **Build Task**: `:app:assembleDebug`
- **Main Activity**: `SettingsActivity` (Used for enabling IME and selecting themes).
- **Required Permissions**: `INTERNET` (Gemini), `VIBRATE` (Haptics).

## Known Constraints
- **Gemini Quota**: Free tier has a 20 requests/day limit per project (Error 429). The app handles this gracefully by falling back to the Offline Engine.
- **UI State**: The keyboard uses an `enum ViewState` to manage transitions between the QWERTY keys, Clipboard history, and the Theme picker carousel.
