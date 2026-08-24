# Likhibi: Computational Resource Curation, Contextual Language Modeling, and Prototype Neural Translation for Nagamese Creole

---

## 📌 Executive Summary

**LIKHIBI** is a research-first Natural Language Processing (NLP) framework designed to build foundational computational language resources for **Nagamese**, an indigenous creole language spoken by over 500,000 people across Nagaland, India. 

Because Nagamese has historically lacked standardized digital datasets, computational lexicons, and input method infrastructure, LIKHIBI addresses these fundamental low-resource NLP challenges through a rigorous two-phase academic methodology:

1. **Phase 1 (Mini Project — COMPLETED)**:
   - Curation and automated validation of a **21,000-entry structured digital lexicon**.
   - Construction of **Monolingual (185,945 tokens)** and **Aligned Parallel Scripture Corpora (6,965 verse pairs)**.
   - Development of a **Contextual Prediction Engine** combining Statistical Trigrams (45,024 3-grams), Bigrams, and a Character-Level Trie Prefix Index (0.77 MB).
   - Deployment of a production-grade **Android Demonstration IME** (Likhibi Keyboard) with 100% offline local inference (<5ms latency, <5MB RAM).

2. **Phase 2 (Major Project — INITIATING)**:
   - Development and fine-tuning of an **English ↔ Nagamese Neural Machine Translation (NMT)** model baseline.
   - Empirical evaluation using **BLEU, chrF++, and METEOR** metrics.
   - On-device quantized NMT inference engine embedded into the mobile IME keyboard.
   - Open-source dataset release on HuggingFace and final research publication.

---

## 📁 Repository Architecture

```
.
├── context.md                             # Context tracking & execution log
├── README.md                              # Main Research Project Overview (THIS FILE)
├── documentation.md                       # Complete Technical & Architectural Reference
│
├── docs/                                  # Project & Research Specifications
│   ├── MINI_PROJECT_TRACK.md              # B.Tech 7th Sem Review-I tracking
│   ├── PRESENTATION_PREP_GUIDE.md         # Viva & presentation Q&A guide
│   ├── ARCHITECTURE.md                    # System architecture & decoupling rules
│   ├── NLP_PIPELINE.md                    # Data acquisition & modeling pipeline
│   ├── DATASETS.md                        # Dataset schemas & specifications
│   └── ROADMAP.md                         # 6-Month timeline & milestones
│
├── datasets/                              # Language Resources & Datasets
│   ├── raw/                               # Raw New Testament PDFs & scraped text
│   ├── processed/                         # Validated 21k Lexicon & Parallel Corpus
│   │   ├── lexical_database/              # nagamese_lexicon.json (21,000 entries)
│   │   ├── parallel_corpus/               # bible_parallel_corpus.tsv (6,965 pairs)
│   │   └── prediction_models/             # trie_index.json, trigrams, bigrams, unigrams
│   └── evaluations/                       # Evaluation suites & perplexity reports
│
├── nlp_research/                          # NLP Core Pipeline (Python 3.10+)
│   ├── preprocessing/                     # Tokenization, cleaning, PDF & web extractors
│   ├── lexical_db/                        # Lexicon schema, builder & validation suite
│   ├── corpus/                            # Sentence alignment & corpus manager
│   ├── prediction/                        # Statistical N-gram + Trie prefix builder
│   ├── translation/                       # English-Nagamese NMT Prototype
│   └── evaluation/                        # BLEU, Perplexity & Lexical Coverage
│
└── app/                                   # Android Demonstration Platform (Kotlin)
    └── src/main/
        ├── assets/models/                 # Offline payloads (trie, unigram, bigram, trigram)
        └── java/com/likhibi/
            ├── android/                   # IME Service, Custom Keyboard View, Settings
            ├── nlp/                       # On-Device Prediction & Translation Interfaces
            └── models/                    # Domain data classes
```

---

## 📊 Key Research Metrics & Benchmarks

| Metric / Resource | Value / Result | Description |
| :--- | :--- | :--- |
| **Validated Lexical Database** | **21,000 words** | 20,000 Native/Creole + 790 English + 210 Hindi/Assamese Borrowings |
| **Lexicon Validation Accuracy** | **100% (0 errors)** | Automated structural and orthographic validation check |
| **Monolingual Corpus Tokens** | **185,945 tokens** | Extracted from 26 Nagamese New Testament books (6,965 sentences) |
| **Parallel Translation Pairs** | **6,965 verse pairs** | Aligned English (KJV/WEB) to Nagamese Scripture |
| **Statistical Trigram Contexts** | **45,024 3-grams** | Contextual 2-word backoff language model |
| **N-Gram Model Perplexity** | **45.59** | Evaluated on held-out test split with Add-$k$ smoothing ($k=0.01$) |
| **Trie Index Payload Size** | **0.77 MB** | Compact character prefix tree for sub-millisecond mobile lookup |
| **On-Device IME Keystroke Latency** | **< 5 ms** | 100% offline, zero-network dependency |
| **On-Device IME Memory Footprint** | **< 5 MB RAM** | Highly optimized for budget and low-spec Android devices |

---

## 🛠️ Build & Installation Instructions

### Android Application
1. Connect an Android device with USB debugging enabled.
2. Build and install via Gradle:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Python NLP Research Pipeline
1. Setup Python 3.10+ virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # Or `venv\Scripts\activate` on Windows
   pip install -r nlp_research/requirements.txt
   ```
2. Train and export language models:
   ```bash
   python nlp_research/prediction/train_and_export.py
   ```

---
