# LIKHIBI — Architectural Specification & Decoupling Guide

## 1. High-Level Architectural Diagram

```
+-------------------------------------------------------------------------------+
|                       OFFLINE RESEARCH PIPELINE (Python)                      |
|  - Raw Text Cleaning (nlp_research/preprocessing/)                            |
|  - Lexicon Construction (nlp_research/lexical_db/)                            |
|  - Parallel Corpus Alignment (nlp_research/corpus/)                           |
|  - N-Gram & Trie Model Compilation (nlp_research/prediction/)                 |
|  - Neural Machine Translation (nlp_research/translation/)                     |
+---------------------------------------+---------------------------------------+
                                        |
                                        v  (Exports Serialized JSON Payloads)
+---------------------------------------+---------------------------------------+
|                       ON-DEVICE ASSET STORAGE (assets/models/)                |
|  - trie_index.json (0.77 MB)          - trigrams.json (1.74 MB)               |
|  - unigrams.json (0.30 MB)            - bigrams.json (0.27 MB)                |
+---------------------------------------+---------------------------------------+
                                        |
                                        v  (Instantiates In-Memory Singletons)
+---------------------------------------+---------------------------------------+
|                       ANDROID DEMONSTRATION PLATFORM (app/)                   |
|  - LikhibiImeService (InputMethodService)                                     |
|  - CustomKeyboardView (Programmatic 10.0-unit Canvas View)                   |
|  - TriePredictor + NgramPredictor (Offline Local Inferencing)                |
|  - NagameseOfflineEngine (Dual-Ranking & Personalization)                     |
+-------------------------------------------------------------------------------+
```

---

## 2. Decoupling Principles

1. **No Training on Mobile**: The Android application is strictly an **inference and demonstration platform**. No dataset generation, corpus scraping, or model training occurs on the Android device.
2. **Zero Network Latency**: Core typing prediction operates 100% offline using precompiled binary asset files stored in `app/src/main/assets/models/`.
3. **Hardware Independence**: The UI layout utilizes density-independent programmatic views (`CustomKeyboardView.kt`) with exact 10.0-unit Gboard weight distributions, ensuring identical visual behavior across all Android screen sizes and densities.
