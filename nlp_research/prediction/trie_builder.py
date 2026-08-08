"""
Trie Prefix Tree Index Generator for Nagamese Word Completion.

Builds a character-level Trie from the 21,000 entry lexical database.
Enables fast sub-millisecond prefix completion and exports compact JSON payloads
suitable for offline Android IME integration.
"""

import os
import sys
import json
from typing import List, Dict, Any, Optional, Tuple

class TrieNode:
    """Node structure within character-level Trie."""
    def __init__(self):
        self.children: Dict[str, 'TrieNode'] = {}
        self.is_end_of_word: bool = False
        self.frequency: int = 0
        self.word: Optional[str] = None
        self.etymology: Optional[str] = None

class TrieBuilder:
    """
    Trie Prefix Tree Builder and Serializer.
    """
    def __init__(self):
        self.root = TrieNode()
        self.total_words = 0

    def insert(self, word: str, frequency: int = 1, etymology: str = "Nagamese Creole / Native"):
        """
        Inserts a word into the Trie with associated metadata.
        """
        word = word.lower().strip()
        if not word:
            return

        node = self.root
        for char in word:
            if char not in node.children:
                node.children[char] = TrieNode()
            node = node.children[char]

        node.is_end_of_word = True
        node.frequency = frequency
        node.word = word
        node.etymology = etymology
        self.total_words += 1

    def build_from_lexicon(self, lexicon_path: str):
        """
        Populates the Trie from nagamese_lexicon.json.
        """
        print(f"Building Trie from lexicon: {lexicon_path}...")
        if not os.path.exists(lexicon_path):
            raise FileNotFoundError(f"Lexicon file not found: {lexicon_path}")

        with open(lexicon_path, "r", encoding="utf-8") as f:
            lexicon_entries = json.load(f)

        for entry in lexicon_entries:
            lemma = entry.get("lemma", "")
            freq = entry.get("frequency_count", 1)
            etym = entry.get("etymology_origin", "Nagamese Creole / Native")
            self.insert(lemma, frequency=freq, etymology=etym)

        print(f"Successfully inserted {self.total_words} unique lemmas into Trie.")

    def search_prefix(self, prefix: str, top_k: int = 10) -> List[Dict[str, Any]]:
        """
        Finds top-k word completions matching a prefix, ordered by corpus frequency.
        """
        prefix = prefix.lower().strip()
        if not prefix:
            return []

        node = self.root
        for char in prefix:
            if char not in node.children:
                return []
            node = node.children[char]

        # DFS to collect all words under this prefix branch
        results: List[Dict[str, Any]] = []

        def _dfs(curr: TrieNode):
            if curr.is_end_of_word and curr.word:
                results.append({
                    "word": curr.word,
                    "frequency": curr.frequency,
                    "etymology": curr.etymology
                })
            for child in curr.children.values():
                _dfs(child)

        _dfs(node)
        # Sort candidates descending by frequency
        results.sort(key=lambda x: x["frequency"], reverse=True)
        return results[:top_k]

    def _node_to_dict(self, node: TrieNode) -> Dict[str, Any]:
        """Recursive helper to convert Trie structure to nested dictionary."""
        d: Dict[str, Any] = {}
        if node.is_end_of_word:
            d["$"] = 1
            d["f"] = node.frequency
            if node.etymology != "Nagamese Creole / Native":
                d["e"] = node.etymology

        for char, child in node.children.items():
            d[char] = self._node_to_dict(child)
        return d

    def export_trie_json(self, output_path: str):
        """
        Exports serialized Trie data structure to JSON for Android engine.
        """
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        print(f"Serializing Trie to {output_path}...")
        serialized_data = {
            "total_words": self.total_words,
            "trie_root": self._node_to_dict(self.root)
        }

        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(serialized_data, f, ensure_ascii=False)

        file_size_mb = os.path.getsize(output_path) / (1024 * 1024)
        print(f"Trie export complete ({file_size_mb:.2f} MB).")

if __name__ == "__main__":
    builder = TrieBuilder()
    lex_path = "datasets/processed/lexical_database/nagamese_lexicon.json"
    builder.build_from_lexicon(lex_path)

    test_prefixes = ["ja", "am", "ko", "sch"]
    print("\nPrefix Search Benchmark:")
    for pfix in test_prefixes:
        completions = builder.search_prefix(pfix, top_k=5)
        print(f"  Prefix '{pfix}' -> {[c['word'] for c in completions]}")
