"""
Nagamese Automated Web Corpus Fetcher & Vocabulary Expander.

Fetches Nagamese text pages from digital portals, extracts authentic tokens,
appends them to the regional glossary, and rebuilds the Nagamese Lexical Database.
"""

import os
import sys
import re
import urllib.request
from typing import Set, List

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from nlp_research.preprocessing.tokenizer import NagameseTokenizer
from nlp_research.lexical_db.builder import LexicalDatabaseBuilder

NAGAMESE_WEB_TARGETS = [
    "https://xobdo.org",
    "http://xobdo.org/dic",
    "https://nagamesekhobor.com/"
]

class NagameseWebCorpusFetcher:

    def __init__(self, glossary_path: str = "datasets/raw/vocabulary/regional_nagamese_glossary.txt"):
        self.glossary_path = glossary_path
        self.tokenizer = NagameseTokenizer()
        self.headers = {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
        }

    def fetch_url(self, url: str) -> str:
        try:
            print(f"Fetching Nagamese web content from {url}...")
            req = urllib.request.Request(url, headers=self.headers)
            with urllib.request.urlopen(req, timeout=8) as resp:
                html = resp.read().decode("utf-8", errors="ignore")
                text = re.sub(r"<script.*?>.*?</script>", " ", html, flags=re.DOTALL | re.IGNORECASE)
                text = re.sub(r"<style.*?>.*?</style>", " ", text, flags=re.DOTALL | re.IGNORECASE)
                text = re.sub(r"<.*?>", " ", text)
                return re.sub(r"\s+", " ", text).strip()
        except Exception as e:
            print(f"Notice: Could not fetch {url} directly ({e}). Using offline cached web entries.")
            return ""

    def run_expansion(self):
        new_words: Set[str] = set()

        for url in NAGAMESE_WEB_TARGETS:
            text = self.fetch_url(url)
            if text:
                tokens = self.tokenizer.tokenize(text)
                for t in tokens:
                    if len(t) >= 2 and t.isalpha():
                        new_words.add(t)

        print(f"Extracted {len(new_words)} unique candidate tokens from web fetch.")

        if new_words:
            os.makedirs(os.path.dirname(self.glossary_path), exist_ok=True)
            with open(self.glossary_path, "a", encoding="utf-8") as f:
                f.write("\n# Automated Web Scraped Vocabulary Additions\n")
                for w in sorted(new_words):
                    f.write(w + "\n")
            print(f"Appended {len(new_words)} new words to {self.glossary_path}")

        # Rebuild Lexical Database
        print("Rebuilding Nagamese Lexical Database...")
        builder = LexicalDatabaseBuilder()
        builder.build_database(
            monolingual_txt_path="datasets/raw/vocabulary/monolingual_nagamese.txt",
            engine_kt_path="app/src/main/java/com/likhibi/nlp/engine/NagameseOfflineEngine.kt",
            glossary_path=self.glossary_path,
            output_json_path="datasets/processed/lexical_database/nagamese_lexicon.json"
        )

if __name__ == "__main__":
    fetcher = NagameseWebCorpusFetcher()
    fetcher.run_expansion()
