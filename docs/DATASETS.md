# LIKHIBI — Computational Datasets & Resource Specifications

## 1. Validated Lexical Database (`nagamese_lexicon.json`)

* **Location**: `datasets/processed/lexical_database/nagamese_lexicon.json`
* **Total Entries**: **21,000**
* **Composition**:
  * 20,000 Native Nagamese Creole Lemmas
  * 790 High-Frequency English Code-Switched Loanwords (e.g. *school, hospital, car, mobile*)
  * 210 Hindi & Assamese Regional Borrowings
* **Schema Definition**:
```json
{
  "id": "NAG_LEX_00001",
  "lemma": "aru",
  "phonetic_ipa": "/aru/",
  "pos_category": "Conjunction",
  "english_definition": "and; also; in addition",
  "etymology_origin": "Assamese / Eastern Indo-Aryan",
  "orthographic_variants": ["aru", "aro", "aur"],
  "frequency_count": 8131,
  "is_validated": true
}
```

---

## 2. Aligned Scripture Parallel Corpus (`bible_parallel_corpus.tsv`)

* **Location**: `datasets/processed/parallel_corpus/bible_parallel_corpus.tsv`
* **Total Sentences**: **6,965 verse-aligned bilingual pairs** (1.60 MB)
* **Columns**: `id`, `book_name`, `chapter`, `verse`, `english_text`, `nagamese_text`
* **Sample Entry**:
```tsv
MAT_001_001	Matthew	1	1	The book of the generation of Jesus Christ, the son of David, the son of Abraham.	Jisu Khrist laga itihas, jitu David laga bongs te jonom hoise, aru Abraham laga bongs te.
```

---

## 3. Monolingual Corpus (`monolingual_nagamese.txt`)

* **Location**: `datasets/raw/vocabulary/monolingual_nagamese.txt`
* **Total Sentences**: 6,965
* **Total Tokens**: 185,945
* **Unique Vocabulary Tokens**: 23,300
