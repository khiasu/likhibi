# LIKHIBI — Nagamese Language Datasets & Corpora Hierarchy

This directory contains the foundational dataset assets for Nagamese NLP research. Data is strictly separated into raw, processed, and evaluation splits.

## Folder Hierarchy

```
datasets/
├── raw/                               # Raw collected & scraped language data
│   ├── vocabulary/                    # Raw word lists, dictionary scrapes, unstructured texts
│   └── parallel/                      # Raw English–Nagamese aligned document pairs
├── processed/                         # Standardized & validated datasets
│   ├── lexical_database/              # Target ~20,000 Nagamese entries (JSON / SQLite)
│   └── parallel_corpus/               # Sentence-aligned parallel corpus (TSV / Parquet)
└── evaluations/                       # Benchmark test sets
    ├── prediction_test_set.json       # Standardized context test prompts
    └── translation_test_set.tsv       # English-Nagamese parallel test pairs
```

## Dataset Specifications

1. **Lexical Database**: Target ~20,000 entries containing lemma, POS, English gloss, phonetic representation, etymology, and variant spellings.
2. **Parallel Corpus**: English–Nagamese sentence pairs collected from authentic regional sources.
3. **Data Quality**: All processed datasets must pass automated validation checks before being compiled into on-device formats.
