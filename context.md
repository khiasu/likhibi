# LIKHIBI — Project Context & Tracking Document

> **Role**: Senior Software Architect & NLP Research Engineer  
> **Project**: Likhibi: Computational Resource Curation, Contextual Language Modeling, and Prototype Neural Translation for Nagamese Creole  
> **Type**: B.Tech CSE Project (7th Sem Mini Project / 8th Sem Major Project)  
> **Author / Student**: Khiasuthong T (Reg No: 2306107010)  
> **Project Coordinators**: Mr. Nzanthung Odyuo & Mr. Nokshangthemba  
> **Current Status**: **Mini Project Phase COMPLETED** (100%) | **Major Project Phase INITIATING**

---

## 1. Project Mandate & Mini/Major Split

LIKHIBI is a research-first Natural Language Processing (NLP) initiative dedicated to building foundational computational language resources for **Nagamese**, a low-resource creole language spoken by ~30,000 native and ~500,000 L2 speakers across Nagaland, Northeast India.

```
+========================================================================================+
|                                    LIKHIBI ROADMAP                                     |
+========================================================================================+
|                                                                                        |
|  [PHASE 1: MINI PROJECT (7th Sem - COMPLETED)]                                         |
|  ├── 1. 21,000-Word Validated Digital Lexicon (nagamese_lexicon.json)                  |
|  ├── 2. Monolingual (185k tokens) & Aligned Parallel Scripture Corpus (6,965 pairs)    |
|  ├── 3. Statistical N-Gram Language Model (Trigrams, Bigrams, Unigrams; PPL: 45.59)    |
|  ├── 4. Sub-millisecond Character-Level Trie Prefix Tree Index (trie_index.json)       |
|  └── 5. Production Android Demonstration Platform (Likhibi IME Keyboard)              |
|                                                                                        |
|  [PHASE 2: MAJOR PROJECT (8th Sem - INITIATION)]                                       |
|  ├── 1. Seq2Seq / Transformer Neural Machine Translation (NMT: English <-> Nagamese)  |
|  ├── 2. Automated BLEU & chrF++ Translation Evaluation Benchmarks                      |
|  ├── 3. On-Device Quantized Translation Runtime (TFLite / ONNX Integration)             |
|  ├── 4. Production Packaging, Google Play Store Release & HuggingFace Publishing       |
|  └── 5. Final B.Tech Dissertation, Conference Publication & Documentation              |
+========================================================================================+
```

---

## 2. Mini Project Deliverables & Completed Specifications

### ✅ Deliverable 1: Validated Digital Lexicon (`nagamese_lexicon.json`)
* **Size**: **21,000 entries** (20,000 Nagamese Creole/Native + 790 English Code-Switched Loanwords + 210 Hindi/Assamese Borrowings).
* **Schema**: `id`, `lemma`, `phonetic_ipa`, `pos_category`, `english_definition`, `etymology_origin`, `orthographic_variants`, `frequency_count`, `is_validated`.
* **Validation**: Full automated word-by-word structural verification scan with **0 invalid entries** (`validation_report.json`).

### ✅ Deliverable 2: Computational Language Corpora
* **Monolingual Corpus** (`datasets/raw/vocabulary/monolingual_nagamese.txt`): 6,965 sentences, 185,945 tokens extracted across 26 Nagamese New Testament books.
* **Parallel Corpus** (`datasets/processed/parallel_corpus/bible_parallel_corpus.tsv`): 6,965 verse-aligned English–Nagamese bilingual sentence pairs (1.60 MB).

### ✅ Deliverable 3: Contextual Prediction Engine
* **Statistical Language Model**:
  * **Trigram Model** (`trigrams.json`): 45,024 3-gram contexts for full 2-word context awareness.
  * **Bigram Model** (`bigrams.json`): 44,642 word-to-word transition mappings.
  * **Unigram Model** (`unigrams.json`): 23,300 unique lexical vocabulary tokens with empirical corpus counts.
  * Add-$k$ smoothing ($k = 0.01$) with backoff (**Perplexity score: 45.59**).
* **Trie Prefix Index** (`trie_index.json`): Precompiled 21,000-lemma character prefix tree for sub-millisecond offline lookup (0.77 MB).
* **Phonetic Dialect Normalization**: Sound cluster resilience (`s` $\leftrightarrow$ `sh`, `o` $\leftrightarrow$ `u`, `ee` $\leftrightarrow$ `i`, `b` $\leftrightarrow$ `v`).
* **Contextual Dual-Ranking**: Combines Trie dictionary frequency with N-gram context transition probability and user history.

### ✅ Deliverable 4: Android Demonstration Platform (Likhibi IME)
* **Architecture**: 100% offline, native Kotlin input method service with <5 MB RAM footprint and <5 ms keystroke latency.
* **UI/UX Aesthetics**:
  * 6 Flagship Themes: **Midnight Glass**, **Pure Minimal**, **Liquid Glass**, **Material You**, **Naga Heritage** (Dynamic Day/Night with Creme `#F9F6F0` and Smoky Black `#121214`), and **Custom Studio**.
  * Custom Studio controls: Wallpaper selector, dimming slider, keycap opacity, corner radius, 3D elevation switch, and 7 accent glow chips.
  * Mathematical 10.0-unit Gboard layout alignment, Number row toggle, and zero-latency instant `ACTION_DOWN` typing pipeline.
  * Multiline-aware newline / Enter return behavior in WhatsApp, Telegram, Notes, and Messages.

---

## 3. Major Project Phase (Continuation Roadmap)

1. **Neural Machine Translation (NMT)**:
   - Build and train encoder-decoder Transformer / Seq2Seq models on `bible_parallel_corpus.tsv`.
   - Implement Byte-Pair Encoding (BPE) subword tokenization for Nagamese morphological agglutination.
2. **Translation Benchmark & Metric Evaluation**:
   - Establish baseline BLEU, chrF++, and METEOR benchmarks on held-out test splits.
3. **On-Device Inferencing**:
   - Quantize NMT models into TFLite / ONNX format for on-device translation within the keyboard shelf.
4. **Public Deployment & Publication**:
   - Deploy signed release APK to Google Play Store and publish corpus to HuggingFace.
   - Author final research thesis and submit for academic publication.

---

## 4. Repository Layout & Decoupling Guide

```
f:\likhibi-main\
├── context.md                             # Context tracking & execution log
├── README.md                              # Main Research Project Overview
├── documentation.md                       # Comprehensive Technical Reference
│
├── docs/                                  # Project & Research Specifications
│   ├── MINI_PROJECT_TRACK.md              # B.Tech 7th Sem Review-I tracking
│   ├── PRESENTATION_PREP_GUIDE.md         # Viva & presentation Q&A guide
│   ├── ARCHITECTURE.md                    # System architecture & decoupling rules
│   ├── NLP_PIPELINE.md                    # Data acquisition & modeling pipeline
│   ├── DATASETS.md                        # Dataset schemas & specs
│   └── ROADMAP.md                         # 6-Month timeline & milestones
│
├── datasets/                              # Datasets & Language Resources
│   ├── raw/                               # Raw New Testament PDFs & scraped text
│   ├── processed/                         # Validated 21k Lexicon & Parallel Corpus
│   └── evaluations/                       # Evaluation suites & perplexity logs
│
├── nlp_research/                          # NLP Core Pipeline & Modeling (Python)
│   ├── preprocessing/                     # Tokenizer, Cleaner, PDF & Web scrapers
│   ├── lexical_db/                        # 21k Lexicon schema, builder & validator
│   ├── corpus/                            # Sentence aligner & parallel corpus manager
│   ├── prediction/                        # Trigram, Bigram, Trie builder & export
│   ├── translation/                       # English-Nagamese NMT prototype
│   └── evaluation/                        # BLEU, Perplexity & Lexical coverage
│
├── app/                                   # Android Demonstration Platform (Kotlin)
│   └── src/main/
│       ├── assets/models/                 # Offline payloads (trie, unigram, bigram, trigram)
│       └── java/com/likhibi/
│           ├── android/                   # IME Service, Custom Keyboard View, Settings
│           ├── nlp/                       # On-Device Prediction & Translation Interfaces
│           └── models/                    # Domain data classes
│
└── tools/                                 # Maintenance & build utilities
```
