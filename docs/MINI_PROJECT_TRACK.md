# LIKHIBI — MINI PROJECT TRACKING & STATUS
## B.Tech CSE 7th Sem Project Review – I (Mini Project Phase)

**Project Title**: Likhibi: Computational Resource Curation, Contextual Language Modeling, and Prototype Neural Translation for Nagamese Creole  
**Submitted By**: Khiasuthong T (Reg No: 2306107010), B.Tech CSE 7th Sem  
**Project Coordinators**: Mr. Nzanthung Odyuo & Mr. Nokshangthemba  

---

## 1. Mini Project Scope & Completed Deliverables (100% COMPLETE)

### ✅ Deliverable 1: Validated Digital Lexicon
* **File**: `datasets/processed/lexical_database/nagamese_lexicon.json`
* **Size**: **21,000 Verified Entries** (20,000 Native/Creole + 790 English Loanwords + 210 Hindi/Assamese Borrowings).
* **Validation**: Automated 9-point structural validation check with **0 errors / 0 invalid entries** (`validation_report.json`).

### ✅ Deliverable 2: Computational Language Corpora
* **Monolingual Corpus**: `datasets/raw/vocabulary/monolingual_nagamese.txt` (6,965 sentences, 185,945 tokens across 26 New Testament books).
* **Scripture Parallel Corpus**: `datasets/processed/parallel_corpus/bible_parallel_corpus.tsv` (6,965 verse-aligned English–Nagamese sentence pairs, 1.60 MB).

### ✅ Deliverable 3: Contextual Prediction Engine
* **Statistical Trigrams & Language Model**:
  * `trigrams.json`: 45,024 3-gram contexts.
  * `bigrams.json`: 44,642 2-gram transitions.
  * `unigrams.json`: 23,300 unigram vocabulary frequencies.
  * Add-$k$ smoothing ($k=0.01$) & backoff (**Perplexity: 45.59**).
* **Character-Level Trie Prefix Tree**: `datasets/processed/prediction_models/trie_index.json` (**0.77 MB**).
* **Dual-Ranked Prefix Matching & Phonetic Normalization**: Dialect sound cluster mapping (`s` $\leftrightarrow$ `sh`, `o` $\leftrightarrow$ `u`, `ee` $\leftrightarrow$ `i`, `b` $\leftrightarrow$ `v`).
* **Persistent On-Device Personalization**: User vocabulary and bigrams dynamically saved to local storage.

### ✅ Deliverable 4: Android Demonstration Platform (Likhibi IME)
* **Performance**: < 5 MB RAM footprint, < 5 ms keystroke latency, 100% offline local execution.
* **Ergonomics & Design**:
  * 6 Flagship Themes (Dynamic Day/Night Naga Heritage with Creme `#F9F6F0` & Smoky Black `#121214`, Midnight Glass, Liquid Glass, Material You, Pure Minimal, and Custom Studio).
  * 10.0-unit Gboard-aligned key layout with Number row toggle.
  * Zero-latency instant `ACTION_DOWN` key registration pipeline.
  * Multiline newline / Enter support in WhatsApp, Telegram, Notes, and Messages.

---

## 2. Major Project Continuation Roadmap (8th Sem)

1. **Neural Machine Translation (NMT)**: Train sequence-to-sequence (Transformer / MarianMT / LLaMA-LoRA) baseline on `bible_parallel_corpus.tsv` for English $\leftrightarrow$ Nagamese translation.
2. **Translation Quality Evaluation**: Measure BLEU, chrF++, and METEOR scores on held-out test splits.
3. **On-Device IME Translation**: Embed quantized NMT engine (TFLite / ONNX) into the keyboard shelf for live sentence translation.
4. **Google Play Store & Open-Source Release**: Publish signed release APK and open-source datasets on HuggingFace.
5. **Final Dissertation & Conference Publication**: Complete formal B.Tech thesis and submit paper to computational linguistics conference.
