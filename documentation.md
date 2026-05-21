# Likhibi Keyboard - Complete Technical Documentation & Context

## 1. Project Vision
Likhibi Keyboard is an AI-assisted Android input method (IME) specifically designed for the **Nagamese language** (a creole spoken in Nagaland, India). It combines a high-performance offline engine with deep Gemini AI integration for context-aware predictions. Unlike standard Android keyboards that rely heavily on bloated XML layouts, Likhibi utilizes a dynamic, programmatic rendering engine that ensures fluid 60FPS typing, low latency, and highly customizable aesthetics.

## 2. Core Architecture

### 2.1 The Entry Point: `LikhibiImeService.kt`
This class extends Android's `InputMethodService` and acts as the bridge between the Android OS and the Likhibi typing engine.
- **Input Connection Lifecycle**: It intercepts standard Android input events (`onStartInputView`, `onFinishInput`) to properly handle cursor positions and text contexts.
- **Context Buffering Strategy**: To maximize typing snappiness, the engine strictly fetches only the last 60 characters of text context. This minimizes Inter-Process Communication (IPC) overhead.
- **State Management**: It handles the toggle between the QWERTY keyboard view, the Suggestion Bar, and the internal Clipboard manager.
- **Dynamic Re-inflation**: When the user changes themes or fonts, it intercepts the change on resumption and triggers a full teardown and rebuild.

### 2.2 The UI Engine: `CustomKeyboardView.kt`
Instead of relying on XML for 30+ keys, the entire keyboard grid is generated programmatically.
- **Programmatic Drawables**: Keys are styled using in-memory `RippleDrawable`, `GradientDrawable`, and `ColorStateList` objects.
- **Caching Mechanism**: Generating complex glassmorphic/3D drawables is expensive. `CustomKeyboardView` uses a `HashMap` to cache the `ConstantState` of drawables based on their color/shadow signature. Custom wallpaper bitmaps are also decoded once and cached.
- **Multi-Touch Event Handling**: Custom `setOnTouchListener` implementations facilitate unique features like spring physics animation, spacebar cursor drag, and hold-to-delete.
- **Key Preview Popups**: High-contrast, theme-aware popups render exactly above the finger to confirm key-presses.

### 2.3 The Intelligence Layer
- **`NagameseOfflineEngine.kt`**: Contains a pre-compiled dictionary of 430+ Nagamese words and thousands of bigram transitions. Uses `lazy` initialization and background pre-warming to keep the main thread free. Zero-latency predictions.
- **`GeminiClient.kt`**: Connects to Google's Gemini API (currently using `gemini-1.5-flash`) for advanced next-word predictions based on the last 5 words typed. Handles rate limiting gracefully by falling back to the Offline Engine.

### 2.4 The Settings App: `SettingsActivity.kt`
The frontend settings app utilizes a sleek, edge-to-edge UI.
- **Shared Preferences**: Acts as the single source of truth for the theme engine, font styling, sound volumes, and haptic strength.
- **Real-time Previews**: Real-time slider interactions. Features dynamic image picking for custom wallpaper themes and Android system settings intents.

## 3. Sensory Feedback Integration

### 3.1 Auditory (`SoundPool`)
- Standard `MediaPlayer` classes introduce audio latency. Likhibi uses `SoundPool` to preload uncompressed `.wav` files into RAM.
- **Custom Sound Profile**: Generates an ultra-soft synthesized "thud" mapped directly to user-defined `sound_volume` preferences. Spacebar produces a uniquely deeper thud.

### 3.2 Haptics (`Vibrator`)
- The amplitude and duration of haptic feedback scale based on the user's preferred millisecond length, combined with modifiers (e.g. Spacebar = 1.5x duration).

## 4. Key Files & Structure
- `app/src/main/java/com/likhibi/keyboard/`: Contains all logic.
- `app/src/main/res/layout/ime_view.xml`: Defines the toolbar and the container for `CustomKeyboardView`.
- `app/src/main/res/xml/method.xml` & `qwerty.xml`: Keyboard metadata and resource-based definitions.
- **Package Name**: `com.likhibi.keyboard`
- **Main Activity**: `SettingsActivity` (Used for enabling IME and selecting themes).
- **Required Permissions**: `INTERNET` (Gemini), `VIBRATE` (Haptics).

## 5. Building and Distributing
To compile the application:
1. Ensure `JAVA_HOME` is set to the Android Studio JDK.
2. Run `./gradlew assembleDebug` for testing or `./gradlew assembleRelease` to generate a production-ready, signed APK.
3. The resulting `.apk` can be installed on any modern Android device.

## 6. Future Roadmap
- **Deep Linguistic Research**: Expanding the `NagameseOfflineEngine` to understand sentence semantics and grammar rules.
- **Meaning Analysis**: Providing on-the-fly translations or deeper text-meaning lookups.
