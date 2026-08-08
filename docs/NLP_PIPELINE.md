# LIKHIBI — Research Methodology & NLP Pipeline

## Pipeline Flowchart

```
Language Data Collection
       │
       ▼
Vocabulary Collection & Curation
       │
       ▼
Corpus Construction
       │
       ▼
Data Cleaning & Normalization
       │
       ▼
Tokenization & Morphological Annotation
       │
       ▼
Lexical Database Development (~20k Entries)
       │
       ▼
English–Nagamese Parallel Corpus
       │
   ┌───┴───────────────────────┐
   │                           │
   ▼                           ▼
Contextual Word        Prototype Machine
Prediction (N-gram+Trie)   Translation (NMT)
   │                           │
   └───┬───────────────────────┘
       ▼
Android Demonstration Platform
       │
       ▼
Evaluation & Documentation
```

## Stage Descriptions

| Stage | Activity | Primary Output |
|---|---|---|
| **Data Acquisition** | Collect vocabulary and bilingual text from reliable field and digital sources. | Raw data files in `datasets/raw/` |
| **Resource Development** | Build structured lexical database (~20k entries) and parallel corpus. | Processed database & corpus in `datasets/processed/` |
| **Language Processing** | Train statistical N-gram/Trie models and prototype NMT models. | Serialized model binaries |
| **Validation** | Evaluate lexical coverage, prediction accuracy (Top-1, Top-3, Top-5), and translation BLEU scores. | Benchmark reports in `datasets/evaluations/` |
| **Demonstration** | Integrate model binaries into Android IME host app. | Android APK build |
