"""
Nagamese Tokenizer & Frequency Extractor Module.

Segments Nagamese text into word tokens, isolates punctuation, removes noise,
and builds unigram frequency tables and bigram transition matrices.
"""

import re
from collections import Counter
from typing import List, Tuple, Dict

class NagameseTokenizer:
    """
    Tokenizer and frequency extractor for Nagamese text.
    """

    def __init__(self):
        # Regex matching clean word tokens of length >= 2
        self.word_token_regex = re.compile(r"\b[a-zA-Z]{2,}\b")

    def tokenize(self, text: str) -> List[str]:
        """
        Tokenizes text string into lowercase word tokens.
        """
        if not text:
            return []
        # Replace non-standard quote characters and punctuation with space
        cleaned = re.sub(r"[\'\`\"\’\‘\“\”\^\*\_\-\.\,\:\;\!\?\(\)\[\]]", " ", text)
        return [w.lower() for w in self.word_token_regex.findall(cleaned)]

    def extract_vocabulary_and_frequencies(self, txt_file_path: str) -> Tuple[Counter, Counter]:
        """
        Reads monolingual text corpus file and returns unigram and bigram frequency counters.
        """
        unigram_counts = Counter()
        bigram_counts = Counter()

        with open(txt_file_path, "r", encoding="utf-8") as f:
            for line in f:
                tokens = self.tokenize(line)
                if not tokens:
                    continue

                unigram_counts.update(tokens)

                for i in range(len(tokens) - 1):
                    bigram = (tokens[i], tokens[i+1])
                    bigram_counts[bigram] += 1

        return unigram_counts, bigram_counts

if __name__ == "__main__":
    tokenizer = NagameseTokenizer()
    txt_path = "datasets/raw/vocabulary/monolingual_nagamese.txt"
    uni, bi = tokenizer.extract_vocabulary_and_frequencies(txt_path)

    print(f"Total Unique Tokens Extracted: {len(uni)}")
    print(f"Total Bigram Transitions: {len(bi)}")
    print("\nTop 30 Most Frequent Words in Nagamese Corpus:")
    for w, count in uni.most_common(30):
        print(f"  {w}: {count}")
