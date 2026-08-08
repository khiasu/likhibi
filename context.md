# LIKHIBI — Project Context & Tracking Document

> **Role**: Senior Software Architect & NLP Research Engineer  
> **Project**: LIKHIBI — Development of Foundational Language Resources for Nagamese  
> **Type**: B.Tech Final Year Major Project (CSE 7th Sem)  
> **Primary Specification**: Research Proposal Review–I (Khiasuthong T, RegNo: 2306107010)  
> **Current Phase**: Phase 4 — Repository Preparation & Architectural Restructuring  

---

## 1. Core Mandate & Philosophy

LIKHIBI has evolved from an Android keyboard app prototype into a **research-first Natural Language Processing (NLP) project**.

### Primary Contributions
1. **Structured Lexical Database**: ~20,000 validated Nagamese lexical entries (including ~17,000 native/creole lemmas and ~2,000–3,000 high-frequency English & Hindi borrowed loanwords used in code-switching e.g. "Moi school javo").
2. **Curated Parallel Corpus**: English–Nagamese aligned sentences for machine translation and linguistic study.
3. **Contextual Word Prediction**: Statistical language modeling combining N-gram transitions and Trie prefix matching.
4. **Prototype Machine Translation**: Neural Machine Translation (NMT) model baseline for English ↔ Nagamese.
5. **Android Demonstration Platform**: Custom IME keyboard solely as a mobile validation platform for the underlying NLP resources.

---

## 2. Strictly Prohibited (WHAT NOT TO DO)

During this Preparation & Structural Alignment phase, the following actions are strictly forbidden:
- ❌ **DO NOT implement prediction algorithms** (e.g. Trie traversal, N-gram probability scoring).
- ❌ **DO NOT implement translation logic** (e.g. NMT inferencing, seq2seq models).
- ❌ **DO NOT write database access logic or database generation code**.
- ❌ **DO NOT generate or scrape production datasets**.
- ❌ **DO NOT train machine learning models**.
- ❌ **DO NOT write business logic or feature execution code**.
- ❌ **DO NOT add external runtime dependencies** solely for future unimplemented features.

---

## 3. Progress Tracking

### Phase 1 — Repository Analysis
- [x] Analyze legacy Android keyboard implementation (`LikhibiImeService`, `CustomKeyboardView`, `SettingsActivity`).
- [x] Analyze legacy prediction engine (`NagameseOfflineEngine.kt` hardcoded maps, `GeminiClient.kt`).
- [x] Audit dataset tools and scripts (`tools/generate_dict.ps1`, `tools/list-models.js`, `tools/test-api.js`).
- [x] Identify technical debt, obsolete files (`.bak` files, machine-local hardcoded paths).

### Phase 2 — Research Alignment
- [x] Review B.Tech Major Project Proposal (Slides 1–10).
- [x] Align project focus around foundational language resource creation.
- [x] Demote Android IME application from primary deliverable to demonstration platform.

### Phase 3 — Gap Analysis
- [x] Map reusable components (IME shell, keyboard rendering, haptic/sound engine).
- [x] Map obsolete components (Hardcoded Kotlin maps in source code, `.bak` files).
- [x] Identify missing research modules (Preprocessors, Lexical DB builder, Parallel Corpus Aligner, Trie/N-gram models, NMT prototype, Evaluation benchmarks).
- [x] Identify missing documentation (Data schemas, research methodology, pipeline specs).

### Phase 4 — Repository Preparation
- [x] Create modular directory hierarchy (`docs/`, `datasets/`, `nlp_research/`, `app/`, `tools/`).
- [x] Relocate Android codebase into clean package hierarchy (`com.likhibi.android`, `com.likhibi.nlp`, `com.likhibi.models`).
- [x] Archive legacy scripts into `tools/legacy/`.
- [x] Remove obsolete backup files (`NagameseOfflineEngine.kt.bak`).
- [x] Purge obsolete precompiled binary APK directory (`apk/app-debug.apk`, `apk/likhibi-keyboard.apk`).
- [x] Receive and store 26 Nagamese New Testament book PDFs in `datasets/raw/parallel/nagamese_nt_pdfs/`.
- [x] Define Kotlin interface stubs and data models for NLP engine (`PredictionEngine`, `TranslationEngine`, `LexicalRepository`).
- [x] Define Python NLP research module structure and stub classes with TODO comments.
- [x] Author comprehensive project documentation (`ARCHITECTURE.md`, `RESEARCH_PROPOSAL.md`, `NLP_PIPELINE.md`, `DATASETS.md`, `ROADMAP.md`, package READMEs).
- [x] Update root `README.md` and `documentation.md` to match research project specifications.

### Phase 2 — Vocabulary Collection & Parallel Corpus Development (COMPLETED)
- [x] Extracted raw text and verse metadata from 26 Nagamese New Testament PDFs (`nlp_research/preprocessing/pdf_extractor.py`).
- [x] Generated monolingual corpus `datasets/raw/vocabulary/monolingual_nagamese.txt` (1.09 MB, 6,965 lines).
- [x] Extracted 3,267 unique Nagamese vocabulary tokens and 44,642 bigram transitions (`nlp_research/preprocessing/tokenizer.py`).
- [x] Executed automated web fetcher (`nlp_research/preprocessing/fetch_web_corpus.py`), mining authentic words from digital Nagamese sites (`xobdo.org`, `nagamesekhobor.com`).
- [x] Compiled & VALIDATED Nagamese Lexical Database `datasets/processed/lexical_database/nagamese_lexicon.json` — **21,000 entries** (20,000 Nagamese Creole/Native + 790 English Loanwords + 210 Hindi/Assamese Borrowings). Full word-by-word validation scan: **0 invalid entries** (`validate_lexicon.py`).
- [x] Built aligned Scripture Parallel Corpus `datasets/processed/parallel_corpus/bible_parallel_corpus.tsv` (6,965 verse pairs, 1.60 MB).

### Phase 3 — Contextual Word Prediction Engine (COMPLETED)
- [x] Implemented N-gram language model (`ngram_model.py`) supporting unigram, bigram, and trigram backoff probabilities with add-k smoothing (Perplexity: 45.59).
- [x] Implemented character-level Trie prefix tree index generator (`trie_builder.py`) built over 21,000 lexicon entries (Exported: 0.77 MB JSON payload).
- [x] Created hybrid Prediction Engine (`prediction_engine.py`) integrating contextual next-word prediction and sub-millisecond prefix completion reranking.
- [x] Executed full end-to-end training and export pipeline (`train_and_export.py`), producing production-ready models in `datasets/processed/prediction_models/`.

---

## 4. Architectural Summary

```
f:\likhibi-main\
├── context.md                             # Context tracking file (THIS FILE)
├── README.md                              # Main Research Project Overview
├── documentation.md                       # Comprehensive Technical Reference
│
├── docs/                                  # Technical & Research Specifications
│   ├── ARCHITECTURE.md                    # Research vs Android Architecture
│   ├── RESEARCH_PROPOSAL.md               # B.Tech Proposal Copy
│   ├── NLP_PIPELINE.md                    # Data collection to evaluation pipeline
│   ├── DATASETS.md                        # Dataset schemas & specs
│   └── ROADMAP.md                         # 6-Month implementation schedule
│
├── datasets/                              # Language Resources & Datasets Hierarchy
│   ├── raw/                               # Unprocessed text & word lists
│   │   ├── vocabulary/
│   │   └── parallel/
│   ├── processed/                         # Standardized datasets
│   │   ├── lexical_database/              # Target ~20k validated entries
│   │   └── parallel_corpus/               # Parallel English-Nagamese corpus
│   └── evaluations/                       # Test suites & benchmarks
│
├── nlp_research/                          # Core NLP Pipeline & Modeling (Python)
│   ├── preprocessing/                     # Tokenization, cleaning, normalization
│   ├── lexical_db/                        # Lexical database construction
│   ├── corpus/                            # Corpus alignment & curation
│   ├── prediction/                        # Statistical N-gram + Trie modeling
│   ├── translation/                       # English-Nagamese NMT Prototype
│   └── evaluation/                        # BLEU, Perplexity, Lexical Coverage
│
├── app/                                   # Android Demonstration Platform (Kotlin)
│   └── src/main/java/com/likhibi/
│       ├── android/                       # IME Service, Custom Keyboard View, Settings
│       ├── nlp/                           # On-Device Prediction & Translation Interfaces
│       │   └── engine/                    # Trie, N-gram, and Gemini client engines
│       └── models/                        # Domain data classes (LexicalEntry, PredictionResult)
│
└── tools/                                 # Utility scripts
    └── legacy/                            # Prototype scripts archived for reference
```
