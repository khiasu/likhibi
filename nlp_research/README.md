# LIKHIBI — NLP Research Pipeline

This directory contains the core Natural Language Processing (NLP) pipeline for Nagamese. It is structured into modular Python packages supporting the full language resource lifecycle:

## Module Breakdown

1. **`preprocessing/`**: Text normalization, diacritic removal, spelling variant handling, tokenization, and morphological annotation.
2. **`lexical_db/`**: Schema definitions and construction tooling for the target ~20,000 Nagamese lexical database.
3. **`corpus/`**: Curation, cleaning, sentence alignment, and split management for the English–Nagamese parallel corpus.
4. **`prediction/`**: Statistical N-gram language model training and Trie index construction for mobile predictive typing.
5. **`translation/`**: Dataset loading, tokenization, model training, and evaluation scripts for the English–Nagamese Neural Machine Translation (NMT) prototype.
6. **`evaluation/`**: Quantitative metrics calculation (BLEU, Perplexity, Lexical Coverage, Exact Match).

---
*Note: All modules currently contain placeholder interfaces and schema stubs to be implemented in subsequent project phases.*
