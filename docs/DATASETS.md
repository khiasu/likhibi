# LIKHIBI — Dataset Specifications & Schema Documentation

## 1. Structured Lexical Database

- **Target Size**: ~20,000 entries
- **Format**: JSON / SQLite database
- **Storage Path**: `datasets/processed/lexical_database/`
- **Composition & Lexical Borrowing**:
  - ~17,000 Native Nagamese lemmas and creole expressions.
  - ~2,000–3,000 Borrowed English & Hindi terms used in daily code-switching (e.g. *school, college, office, doctor, mobile, time, javo, dukan*).

### Entry Schema Specification
```json
{
  "id": "NAG_LEX_00001",
  "lemma": "school",
  "phonetic_ipa": "/skuːl/",
  "pos_category": "Noun",
  "english_definition": "school, educational institution",
  "etymology_origin": "English Loanword",
  "orthographic_variants": ["iskul", "school"],
  "usage_examples": ["Moi school javo."],
  "frequency_count": 1850,
  "is_validated": true
}
```

```json
{
  "id": "NAG_LEX_00002",
  "lemma": "javo",
  "phonetic_ipa": "/dʒaːboː/",
  "pos_category": "Verb",
  "english_definition": "will go, to go",
  "etymology_origin": "Assamese / Hindi Borrowing",
  "orthographic_variants": ["jabo", "zabo"],
  "usage_examples": ["Moi school javo."],
  "frequency_count": 2100,
  "is_validated": true
}
```

## 2. English–Nagamese Parallel Corpus

- **Format**: TSV / Parquet dataset with `src_en` and `tgt_nag` fields
- **Storage Path**: `datasets/processed/parallel_corpus/`

### Corpus Pair Specification
```tsv
src_id	english_sentence	nagamese_sentence	domain	is_aligned
EN_NAG_0001	I am going to school.	Moi school javo.	Conversational / Code-Switching	true
EN_NAG_0002	Where are you going?	Tui kot ja ase?	Conversational	true
EN_NAG_0003	I am reading a book.	Moi ekta kitap porhi ase.	General	true
```

## 3. Evaluation Benchmarks

- **Prediction Test Set**: `datasets/evaluations/prediction_test_set.json`
- **Translation Test Set**: `datasets/evaluations/translation_test_set.tsv`
