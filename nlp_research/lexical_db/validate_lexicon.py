"""
Nagamese Lexical Database Validator — Research-Grade Word-by-Word Scanner.

Scans EVERY SINGLE ENTRY in the lexical database and validates:
  - Nagamese words: must appear in at least one authentic source
    (Bible corpus, legacy engine, curated glossary, additional vocab)
    OR be a valid morphological inflection of a verified stem.
  - English loanwords: must be a real English word (checked against corpus).
  - Hindi loanwords: must be a real Hindi/Assamese borrowing.

Flags and removes any word that cannot be verified from ANY source.
"""

import os
import sys
import re
import json
from typing import Dict, Set, List, Tuple

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from nlp_research.preprocessing.tokenizer import NagameseTokenizer

# Nagamese morphological suffixes (valid word-final patterns)
VALID_NAG_SUFFIXES = ["khan", "laga", "ke", "pora", "te", "se", "bo", "bole", "ina", "thaki"]

class LexiconValidator:

    def __init__(self):
        self.tokenizer = NagameseTokenizer()

    def load_source_words(self, path: str) -> Set[str]:
        """Load a text/glossary file as a set of lowercase words."""
        words = set()
        if not path or not os.path.exists(path):
            return words
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                s = line.strip()
                if s and not s.startswith("#"):
                    for token in self.tokenizer.tokenize(s):
                        if len(token) >= 2 and token.isalpha():
                            words.add(token)
        return words

    def load_legacy_engine_words(self, kt_path: str) -> Set[str]:
        """Parse words from NagameseOfflineEngine.kt."""
        words = set()
        if not os.path.exists(kt_path):
            return words
        pattern = re.compile(r'"([a-zA-Z]{2,})"\s*to\s*\d+')
        with open(kt_path, "r", encoding="utf-8") as f:
            for line in f:
                m = pattern.search(line)
                if m:
                    words.add(m.group(1).lower())
        return words

    def load_loanword_file(self, path: str) -> Set[str]:
        """Load English/Hindi loanword file as a set of words."""
        words = set()
        if not path or not os.path.exists(path):
            return words
        with open(path, "r", encoding="utf-8") as f:
            for line in f:
                s = line.strip()
                if s and not s.startswith("#") and s.isalpha() and len(s) >= 2:
                    words.add(s.lower())
        return words

    def is_valid_inflection(self, word: str, base_vocab: Set[str]) -> bool:
        """
        Check if word is a valid morphological inflection of a known base word.
        E.g. 'manukhan' = 'manu' + 'khan' -> valid if 'manu' is in base_vocab.
        """
        for suffix in VALID_NAG_SUFFIXES:
            if word.endswith(suffix) and len(word) > len(suffix) + 1:
                stem = word[:-len(suffix)]
                if stem in base_vocab:
                    return True
        return False

    def validate_database(
        self,
        lexicon_path: str,
        corpus_path: str,
        engine_path: str,
        glossary_path: str,
        additional_path: str,
        loanwords_path: str,
        output_clean_path: str,
        output_report_path: str
    ):
        print("=" * 70)
        print("NAGAMESE LEXICAL DATABASE VALIDATOR — WORD-BY-WORD SCAN")
        print("=" * 70)

        # Load all source vocabularies
        print("\nLoading source vocabularies...")
        corpus_words = self.load_source_words(corpus_path)
        print(f"  Bible corpus:        {len(corpus_words)} words")

        engine_words = self.load_legacy_engine_words(engine_path)
        print(f"  Legacy engine:       {len(engine_words)} words")

        glossary_words = self.load_source_words(glossary_path)
        print(f"  Regional glossary:   {len(glossary_words)} words")

        additional_words = self.load_source_words(additional_path)
        print(f"  Additional vocab:    {len(additional_words)} words")

        loanwords = self.load_loanword_file(loanwords_path)
        print(f"  Loanwords file:      {len(loanwords)} words")

        # Combined Nagamese base vocabulary (all authentic sources)
        all_nagamese_base = corpus_words | engine_words | glossary_words | additional_words
        print(f"\n  TOTAL Nagamese base vocabulary: {len(all_nagamese_base)} unique words")

        # Load lexicon
        print(f"\nLoading lexicon from {lexicon_path}...")
        with open(lexicon_path, "r", encoding="utf-8") as f:
            lexicon = json.load(f)
        print(f"  Total entries to validate: {len(lexicon)}")

        # Validate each entry
        print("\nScanning every entry...")
        valid_entries = []
        invalid_entries = []
        stats = {
            "in_corpus": 0,
            "in_engine": 0,
            "in_glossary": 0,
            "in_additional": 0,
            "in_loanwords": 0,
            "valid_inflection": 0,
            "INVALID": 0
        }

        for i, entry in enumerate(lexicon):
            word = entry["lemma"]
            etym = entry["etymology_origin"]
            is_valid = False
            reason = ""

            if etym in ("English Loanword", "Assamese / Hindi Borrowing"):
                # Loanword: must be in our curated loanword file
                if word in loanwords:
                    is_valid = True
                    reason = "in_loanwords"
                    stats["in_loanwords"] += 1
                else:
                    reason = "LOANWORD_NOT_IN_FILE"
            else:
                # Nagamese word: check all sources
                if word in corpus_words:
                    is_valid = True
                    reason = "in_corpus"
                    stats["in_corpus"] += 1
                elif word in engine_words:
                    is_valid = True
                    reason = "in_engine"
                    stats["in_engine"] += 1
                elif word in glossary_words:
                    is_valid = True
                    reason = "in_glossary"
                    stats["in_glossary"] += 1
                elif word in additional_words:
                    is_valid = True
                    reason = "in_additional"
                    stats["in_additional"] += 1
                elif self.is_valid_inflection(word, all_nagamese_base):
                    is_valid = True
                    reason = "valid_inflection"
                    stats["valid_inflection"] += 1
                else:
                    reason = "NAGAMESE_NOT_VERIFIED"

            if is_valid:
                valid_entries.append(entry)
            else:
                stats["INVALID"] += 1
                invalid_entries.append({
                    "id": entry["id"],
                    "lemma": word,
                    "etymology": etym,
                    "reason": reason,
                    "frequency": entry["frequency_count"]
                })

            # Progress indicator every 5000 words
            if (i + 1) % 5000 == 0:
                print(f"  Scanned {i + 1}/{len(lexicon)} entries...")

        print(f"\n{'=' * 70}")
        print("VALIDATION RESULTS")
        print(f"{'=' * 70}")
        print(f"  Total scanned:       {len(lexicon)}")
        print(f"  VALID entries:       {len(valid_entries)}")
        print(f"  INVALID entries:     {len(invalid_entries)}")
        print(f"\n  Validation sources:")
        print(f"    Bible corpus:      {stats['in_corpus']}")
        print(f"    Legacy engine:     {stats['in_engine']}")
        print(f"    Regional glossary: {stats['in_glossary']}")
        print(f"    Additional vocab:  {stats['in_additional']}")
        print(f"    Loanwords file:    {stats['in_loanwords']}")
        print(f"    Valid inflection:  {stats['valid_inflection']}")
        print(f"    FAILED:            {stats['INVALID']}")

        # Re-index valid entries
        for idx, entry in enumerate(valid_entries, start=1):
            entry["id"] = f"NAG_LEX_{idx:05d}"

        # Save clean lexicon
        with open(output_clean_path, "w", encoding="utf-8") as f:
            json.dump(valid_entries, f, ensure_ascii=False, indent=2)
        print(f"\nSaved CLEAN lexicon ({len(valid_entries)} entries) to: {output_clean_path}")

        # Save invalid report
        with open(output_report_path, "w", encoding="utf-8") as f:
            json.dump(invalid_entries, f, ensure_ascii=False, indent=2)
        print(f"Saved INVALID report ({len(invalid_entries)} entries) to: {output_report_path}")

        # Count etymology in clean set
        etym_counts = {}
        for e in valid_entries:
            et = e["etymology_origin"]
            etym_counts[et] = etym_counts.get(et, 0) + 1
        print(f"\nClean Lexicon Etymology Breakdown:")
        for k, v in sorted(etym_counts.items(), key=lambda x: -x[1]):
            print(f"  {k}: {v}")

        return len(valid_entries), len(invalid_entries)


if __name__ == "__main__":
    validator = LexiconValidator()
    validator.validate_database(
        lexicon_path="datasets/processed/lexical_database/nagamese_lexicon.json",
        corpus_path="datasets/raw/vocabulary/monolingual_nagamese.txt",
        engine_path="app/src/main/java/com/likhibi/nlp/engine/NagameseOfflineEngine.kt",
        glossary_path="datasets/raw/vocabulary/regional_nagamese_glossary.txt",
        additional_path="datasets/raw/vocabulary/additional_nagamese_vocab.txt",
        loanwords_path="datasets/raw/vocabulary/english_hindi_loanwords.txt",
        output_clean_path="datasets/processed/lexical_database/nagamese_lexicon.json",
        output_report_path="datasets/processed/lexical_database/validation_report.json"
    )
