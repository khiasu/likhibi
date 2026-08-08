# Likhibi: Computational Resource Curation, Contextual Language Modeling, and Prototype Neural Translation for Nagamese Creole

**Major Project Proposal (B.Tech CSE 7th Semester)**  
**Author**: Khiasuthong T (RegNo: 2306107010)  

---

## 📌 Project Overview

**LIKHIBI** is a research-driven Natural Language Processing (NLP) project dedicated to building foundational computational language resources for **Nagamese**, an indigenous creole language spoken across Nagaland, India.

While Nagamese is widely spoken in daily life, it remains severely under-resourced in computational NLP research. LIKHIBI bridges this gap by creating:
1. A **Structured Lexical Database** (~20,000 validated entries with POS tags, definitions, and morphological variants).
2. A **Curated Parallel Corpus** (English–Nagamese aligned sentence pairs).
3. **Statistical Language Modeling & Word Prediction** (N-gram language models + Trie prefix index).
4. A **Prototype Neural Machine Translation Model** (English ↔ Nagamese NMT).
5. An **Android Demonstration Application** (Custom IME Keyboard) that acts as a practical demonstration platform.

---

## 📁 Repository Structure

```
.
├── context.md                             # Context tracking & execution log
├── README.md                              # Project overview (THIS FILE)
├── documentation.md                       # Complete Technical Reference
│
├── docs/                                  # Project & Research Specifications
│   ├── ARCHITECTURE.md                    # System architecture & decoupling rules
│   ├── RESEARCH_PROPOSAL.md               # Major Project Proposal (Review-I)
│   ├── NLP_PIPELINE.md                    # Data acquisition & modeling pipeline
│   ├── DATASETS.md                        # Lexical DB & Parallel Corpus schemas
│   └── ROADMAP.md                         # 6-Month timeline & milestones
│
├── datasets/                              # Datasets & Language Resources
│   ├── raw/                               # Unprocessed vocabulary & parallel texts
│   ├── processed/                         # Validated Lexical DB (~20k target) & Parallel Corpus
│   └── evaluations/                       # Prediction & translation benchmark test suites
│
├── nlp_research/                          # NLP Core Pipeline & Modeling (Python)
│   ├── preprocessing/                     # Cleaning, normalization, tokenization
│   ├── lexical_db/                        # Lexical database builder & schema
│   ├── corpus/                            # Sentence alignment & corpus manager
│   ├── prediction/                        # Statistical N-gram + Trie modeling
│   ├── translation/                       # English-Nagamese NMT Prototype
│   └── evaluation/                        # BLEU, Perplexity, Lexical Coverage
│
├── app/                                   # Android Demonstration Platform (Kotlin)
│   └── src/main/java/com/likhibi/
│       ├── android/                       # IME Service, Custom Keyboard View, Settings
│       ├── nlp/                           # On-Device Prediction & Translation Interfaces
│       └── models/                        # Domain data classes (LexicalEntry, PredictionResult)
│
└── tools/                                 # Utility & Legacy Tools
    └── legacy/                            # Prototype scripts archived for reference
```

---
