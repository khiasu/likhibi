# LIKHIBI — MINI PROJECT TRACKING & STATUS
## B.Tech CSE 7th Sem Project Review – I (Mini Project Phase)

**Project Title**: Likhibi: Computational Resource Curation, Contextual Language Modeling, and Prototype Neural Translation for Nagamese Creole  
**Submitted By**: Khiasuthong T (Reg No: 2306107010), B.Tech CSE 7th Sem  
**Project Coordinators**: Mr. Nzanthung Odyuo & Mr. Nokshangthemba  

---

## 1. Mini Project Scope & Completed Deliverables

### ✅ Completed Deliverables (Review – I)
1. **Validated Digital Dictionary**: `datasets/processed/lexical_database/nagamese_lexicon.json`
   - **21,000 Verified Entries** (20,000 Native/Creole + 790 English Loanwords + 210 Hindi/Assamese Borrowings).
   - Word-by-word automated validation scan: **0 invalid entries** (`validation_report.json`).
2. **Language Corpora**:
   - Monolingual Corpus: `datasets/raw/vocabulary/monolingual_nagamese.txt` (6,965 sentences, 185,945 tokens).
   - Parallel Translation Corpus: `datasets/processed/parallel_corpus/bible_parallel_corpus.tsv` (6,965 aligned English-Nagamese verse pairs).
3. **Contextual Prediction Models**:
   - Statistical N-Gram Language Model with Add-$k$ smoothing ($k=0.01$) & backoff (**Perplexity: 45.59**).
   - Character-Level Trie Prefix Tree Index (`datasets/processed/prediction_models/trie_index.json`, **0.77 MB**).
4. **Android IME Demonstration Platform**:
   - Native Kotlin keyboard application (`app/src/main/java/com/likhibi/`) loading model assets offline.
   - **Performance**: <5 MB RAM footprint, <10 ms response latency, 100% offline.

---

## 2. Major Project Continuation Roadmap (Next Phase)

- **Neural Machine Translation (NMT)**: Train sequence-to-sequence (seq2seq / Transformer) model baseline on `bible_parallel_corpus.tsv` for English $\leftrightarrow$ Nagamese translation.
- **Translation Quality Evaluation**: Measure BLEU scores on held-out test translations.
- **Production APK Packaging & Release**: Refine UI/UX, minify bytecode via ProGuard/R8, and publish release bundle on GitHub and Google Play Store.
- **Final Research Paper & Documentation**: Complete publication-ready NLP resource paper.
