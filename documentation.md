# LIKHIBI — Technical Documentation & Architectural Reference

## 1. Project Vision & Research Scope

LIKHIBI is a research-first Natural Language Processing (NLP) framework designed to build foundational language resources for **Nagamese**, a low-resource creole spoken in Nagaland, India.

### Key Objectives
1. **Lexical Database**: ~20,000 validated lexical entries (lemmas, IPA phonetics, POS, English glosses, etymology, variants).
2. **Parallel Corpus**: Sentence-aligned English–Nagamese bilingual dataset.
3. **Word Prediction**: N-gram statistical language models combined with a compact Trie prefix matcher.
4. **Machine Translation**: Prototype English ↔ Nagamese Neural Machine Translation (NMT).
5. **Android Demonstration IME**: A custom mobile input method engine serving as an evaluation platform.

---

## 2. Decoupled Architecture

```
                                    +-----------------------+
                                    |   Research Pipeline   |
                                    |    (nlp_research/)    |
                                    +-----------+-----------+
                                                |
                                                v  Compiles Binary Models & Lexicon
                                    +-----------+-----------+
                                    |   On-Device Storage   |
                                    |    (datasets/ & res)  |
                                    +-----------+-----------+
                                                |
                                                v  Exposes via Interfaces
                                    +-----------+-----------+
                                    |   Android IME Engine  |
                                    |        (app/)         |
                                    +-----------------------+
```

---

## 3. Package & Subsystem Specifications

### 3.1 Android Host Platform (`app/src/main/java/com/likhibi/android/`)
- **`LikhibiImeService.kt`**: Main `InputMethodService` entry point handling input connections, cursor buffering, layout switching, and sensory feedback.
- **`CustomKeyboardView.kt`**: High-performance programmatic canvas view rendering the keyboard grid without heavy XML layouts.
- **`SettingsActivity.kt`**: Edge-to-edge configuration interface for themes, sound volume, haptic intensity, and active layout selection.

### 3.2 On-Device NLP Interfaces & Engines (`app/src/main/java/com/likhibi/nlp/`)
- **`PredictionEngine.kt`**: Contract interface for contextual word predictions and auto-completion.
- **`TranslationEngine.kt`**: Contract interface for English–Nagamese text translation.
- **`LexicalRepository.kt`**: Abstraction over the structured Nagamese lexical database.
- **`engine/TriePredictor.kt`**: [Placeholder Stub] On-device Trie prefix matching engine.
- **`engine/NgramPredictor.kt`**: [Placeholder Stub] On-device statistical N-gram language model engine.
- **`engine/GeminiClient.kt`**: Remote LLM client for API fallback during experimentation.
- **`engine/NagameseOfflineEngine.kt`**: Legacy prototype prediction engine (retained for backward compatibility during migration).

### 3.3 Domain Data Models (`app/src/main/java/com/likhibi/models/`)
- **`LexicalEntry.kt`**: Data model representing a Nagamese dictionary item.
- **`PredictionResult.kt`**: Data structure holding predicted words, confidence scores, and source metadata.
- **`TranslationResult.kt`**: Data structure holding MT source/target strings and metrics.

### 3.4 Python Research Pipeline (`nlp_research/`)
- **`preprocessing/`**: `cleaner.py` (normalization/variants) & `tokenizer.py` (segmentation/POS tagging).
- **`lexical_db/`**: `schema.py` (entry dataclass) & `builder.py` (DB construction & validation tool).
- **`corpus/`**: `aligner.py` (sentence alignment & quality filtering).
- **`prediction/`**: `trie_builder.py` (Trie index builder) & `ngram_model.py` (N-gram LM trainer).
- **`translation/`**: `dataset_loader.py` (bilingual loader) & `nmt_prototype.py` (Seq2Seq / Transformer trainer).
- **`evaluation/`**: `evaluator.py` (BLEU, Perplexity, Lexical Coverage) & `benchmark.py` (Benchmark runner).

---

## 4. Build & Environment Instructions

### Android App
- Package: `com.likhibi`
- Build Tool: Gradle with Kotlin DSL (`build.gradle.kts`)
- Target SDK: Android 34 / Min SDK 24
- Debug Build: `./gradlew assembleDebug`

### Research Pipeline
- Python Version: Python 3.10+
- Requirements: Defined in `nlp_research/requirements.txt`
