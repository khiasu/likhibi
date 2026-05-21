# Likhibi Keyboard - Complete Technical Documentation

## 1. Project Vision
Likhibi is a premium Android Input Method Editor (IME) created with a focus on speed, modern design, and advanced linguistic support for Nagamese. Unlike standard Android keyboards that rely heavily on bloated XML layouts, Likhibi utilizes a dynamic, programmatic rendering engine that ensures fluid 60FPS typing, low latency, and highly customizable aesthetics.

## 2. Core Architecture

### 2.1 The Entry Point: `LikhibiImeService.kt`
This class extends Android's `InputMethodService` and acts as the bridge between the Android OS and the Likhibi typing engine.
- **Input Connection Lifecycle**: It intercepts standard Android input events (`onStartInputView`, `onFinishInput`) to properly handle cursor positions and text contexts.
- **Context Buffering Strategy**: To maximize typing snappiness, the engine strictly fetches only the last 60 characters of text context. This minimizes Inter-Process Communication (IPC) overhead, eliminating the lag commonly found in custom keyboards when typing long paragraphs.
- **State Management**: It handles the toggle between the QWERTY keyboard view, the Suggestion Bar, and the internal Clipboard manager.
- **Dynamic Re-inflation**: When the user changes themes or fonts in the settings app, `LikhibiImeService` intercepts the change on resumption and triggers a full teardown and rebuild (`setInputView(onCreateInputView())`) to apply changes instantaneously.

### 2.2 The UI Engine: `CustomKeyboardView.kt`
Instead of relying on XML for 30+ keys, the entire keyboard grid is generated programmatically.
- **Programmatic Drawables**: Keys are styled using in-memory `RippleDrawable`, `GradientDrawable`, and `ColorStateList` objects.
- **Caching Mechanism**: Generating complex glassmorphic/3D drawables is expensive. `CustomKeyboardView` uses a `HashMap` to cache the `ConstantState` of drawables based on their color/shadow signature, preventing GC (Garbage Collection) stuttering.
- **Multi-Touch Event Handling**: Custom `setOnTouchListener` implementations on individual keys provide absolute control over the `ACTION_DOWN` and `ACTION_UP` events. This facilitates unique features like:
  - **Spring Physics Animation**: Keys physically compress and bounce back using overshoot interpolators.
  - **Hold-to-Delete**: A repeating `Runnable` on the `Handler` loop allows the backspace key to rapid-fire delete characters.
- **Key Preview Popups**: High-contrast, theme-aware popups render exactly above the finger to confirm key-presses, fully matching the user's custom font.

### 2.3 The Intelligence Layer: `NagameseOfflineEngine.kt`
Likhibi includes a specialized, on-device prediction engine.
- **In-Memory Trie/HashMap**: The engine loads thousands of high-frequency Nagamese words and bi-grams instantly.
- **Zero-Latency**: Because the prediction logic relies on local RAM instead of an SQLite database or an external API, suggestions appear on the top bar in under 2 milliseconds.

### 2.4 The Settings App: `SettingsActivity.kt`
The frontend settings app utilizes a sleek, edge-to-edge UI with a modern collapsing toolbar.
- **Shared Preferences**: Acts as the single source of truth for the theme engine, font styling, sound volumes, and haptic strength.
- **Real-time Previews**: As users adjust their Haptic slider, the device fires an immediate tactile sample so they can dial in the perfect click feel.

## 3. Sensory Feedback Integration

### 3.1 Auditory (`SoundPool`)
- Standard `MediaPlayer` classes introduce audio latency. Likhibi uses `SoundPool` to preload uncompressed `.wav` files into RAM.
- **Pitch Modulation**: The engine modulates the playback rate dynamically. The Spacebar plays at `0.9x` for a deeper thud, while the Shift/Enter keys play at `1.4x` and `1.15x` for a sharper, distinct "click" sound.

### 3.2 Haptics (`Vibrator`)
- Vibrations are not uniform. The amplitude and duration of haptic feedback scale based on the user's preferred millisecond length, combined with a modifier:
  - Standard keys = Baseline vibration.
  - Spacebar = 1.5x duration, lower amplitude.
  - Modifiers (Shift/Enter) = Higher amplitude (sharper tap).

## 4. Building and Distributing
To compile the application:
1. Ensure `JAVA_HOME` is set to the Android Studio JDK.
2. Run `./gradlew assembleDebug` for testing or `./gradlew assembleRelease` to generate a production-ready, signed APK.
3. The resulting `.apk` can be installed on any modern Android device.

## 5. Future Roadmap
The architecture is primed for Phase 2:
- **Deep Linguistic Research**: Expanding the `NagameseOfflineEngine` to understand sentence semantics and grammar rules.
- **Meaning Analysis**: Providing on-the-fly translations or deeper text-meaning lookups.
