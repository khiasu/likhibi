# LIKHIBI — Technical Documentation & Architectural Reference

## 1. System Architecture & Decoupling Philosophy

LIKHIBI enforces strict architectural decoupling between the **Offline NLP Research Pipeline (Python)** and the **On-Device Demonstration Keyboard (Kotlin/Android)**.

```
+-------------------------------------------------------------------------------+
|                            RESEARCH PIPELINE (Python)                         |
|  [Raw Data Extraction] -> [Lexicon Builder] -> [N-Gram & Trie Model Trainers] |
+---------------------------------------+---------------------------------------+
                                        |
                                        v (Compiles Compact Serialized Payloads)
+---------------------------------------+---------------------------------------+
|                       ON-DEVICE ASSET STORAGE (assets/models/)                |
|  - trie_index.json (0.77 MB)          - trigrams.json (1.74 MB)               |
|  - unigrams.json (0.30 MB)            - bigrams.json (0.27 MB)                |
+---------------------------------------+---------------------------------------+
                                        |
                                        v (Loaded into Memory via Interfaces)
+---------------------------------------+---------------------------------------+
|                          ANDROID IME ENGINE (app/)                            |
|  [TriePredictor] + [NgramPredictor] -> [NagameseOfflineEngine] -> [Keyboard]  |
+-------------------------------------------------------------------------------+
```

---

## 2. NLP Research Subsystems (`nlp_research/`)

### 2.1 Preprocessing & Corpus Ingestion (`nlp_research/preprocessing/`)
* **`pdf_extractor.py`**: Extracts plain text, removes formatting noise, and extracts chapter/verse markers across 26 Nagamese New Testament PDF books.
* **`tokenizer.py`**: Performs rule-based tokenization, lowercase normalization, punctuation stripping, and sentence boundary detection.
* **`cleaner.py`**: Normalizes common orthographic spelling variations and filters non-Nagamese artifacts.
* **`fetch_web_corpus.py`**: Scrapes authentic contemporary Nagamese text from online sources (`xobdo.org`, news articles, forums).

### 2.2 Lexical Database Engine (`nlp_research/lexical_db/`)
* **`schema.py`**: Defines the `LexicalEntry` structure conforming to computational lexicography standards.
* **`builder.py`**: Merges corpus frequency counts, IPA phonetic transcriptions, grammatical POS categories, English glosses, and etymological origins.
* **`validate_lexicon.py`**: Automated schema and integrity validator ensuring 100% of 21,000 entries adhere strictly to data constraints.

### 2.3 Contextual Prediction Pipeline (`nlp_research/prediction/`)
* **`ngram_model.py`**: Implements unigram, bigram, and trigram maximum likelihood estimation with Add-$k$ smoothing ($k=0.01$) and Jelinek-Mercer/Stupid Backoff interpolation.
* **`trie_builder.py`**: Builds character-level Trie prefix trees from the 21,000 lexicon entries and serializes them into compact JSON representations.
* **`prediction_engine.py`**: Hybrid predictor merging context probability with dictionary frequency.
* **`train_and_export.py`**: End-to-end batch script that trains models and automatically exports production assets.

### 2.4 Machine Translation Prototype (`nlp_research/translation/`)
* **`dataset_loader.py`**: Parses and batches parallel sentences from `bible_parallel_corpus.tsv` for training.
* **`nmt_prototype.py`**: Encoder-decoder Transformer / Seq2Seq neural machine translation baseline for English $\leftrightarrow$ Nagamese translation.

### 2.5 Evaluation Suite (`nlp_research/evaluation/`)
* **`evaluator.py`**: Calculates Perplexity (PPL), BLEU scores, chrF++, and vocabulary coverage metrics.
* **`benchmark.py`**: Benchmarking harness evaluating latency and accuracy against held-out test sets.

---

## 3. Android IME Platform (`app/src/main/java/com/likhibi/`)

### 3.1 Input Method Core (`com.likhibi.android`)
* **`LikhibiImeService.kt`**: Subclasses `InputMethodService`. Manages `InputConnection`, cursor tracking, multi-word context buffering (`getLastWords(3)`), multiline Enter handling, toolbar switching, and suggestion chip routing.
* **`CustomKeyboardView.kt`**: High-performance programmatic view rendering the 10.0-unit Gboard-aligned key layout, Number row toggle, zero-latency instant `ACTION_DOWN` touch events, and native ripple feedback.
* **`SettingsActivity.kt`**: Material Design preferences hub managing the 6 flagship themes, Custom Studio controls (opacity, rounding, shadows, accent swatches), haptics, sounds, and active fonts.

### 3.2 On-Device NLP Predictors (`com.likhibi.nlp.engine`)
* **`TriePredictor.kt`**: Parses `trie_index.json` into an in-memory memory-efficient Trie tree supporting sub-millisecond prefix auto-completion and phonetic sound clustering (`s` $\leftrightarrow$ `sh`, `o` $\leftrightarrow$ `u`, `ee` $\leftrightarrow$ `i`, `b` $\leftrightarrow$ `v`).
* **`NgramPredictor.kt`**: Loads `trigrams.json`, `bigrams.json`, and `unigrams.json` to compute real-time contextual transition probabilities with backoff.
* **`NagameseOfflineEngine.kt`**: Dual-ranking engine that combines Trie frequency + N-gram context bonus + persistent user personalization (`likhibi_user_dict_prefs`).

---

## 4. Dataset Specifications

1. **`datasets/processed/lexical_database/nagamese_lexicon.json`**:
   * Total Entries: **21,000**
   * Fields: `id`, `lemma`, `phonetic_ipa`, `pos_category`, `english_definition`, `etymology_origin`, `orthographic_variants`, `frequency_count`, `is_validated`.
2. **`datasets/processed/parallel_corpus/bible_parallel_corpus.tsv`**:
   * Total Aligned Sentences: **6,965 verse pairs**
   * Columns: `id`, `book_name`, `chapter`, `verse`, `english_text`, `nagamese_text`.
3. **`datasets/raw/vocabulary/monolingual_nagamese.txt`**:
   * Total Sentences: **6,965**
   * Total Tokens: **185,945**
