"""
Statistical N-gram Language Model for Nagamese Word Prediction.

Trains unigram, bigram, and trigram frequency tables from the monolingual
Nagamese corpus with add-k smoothing for unseen n-grams.
Provides next-word prediction ranked by conditional probability.
"""

import os
import sys
import json
import math
from collections import Counter, defaultdict
from typing import List, Dict, Tuple, Optional

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from nlp_research.preprocessing.tokenizer import NagameseTokenizer


class NgramLanguageModel:
    """
    Statistical N-gram language model with add-k smoothing.
    Supports unigram, bigram, and trigram prediction.
    """

    def __init__(self, smoothing_k: float = 0.01):
        self.smoothing_k = smoothing_k
        self.tokenizer = NagameseTokenizer()

        # Frequency tables
        self.unigram_counts: Counter = Counter()
        self.bigram_counts: Dict[str, Counter] = defaultdict(Counter)
        self.trigram_counts: Dict[Tuple[str, str], Counter] = defaultdict(Counter)

        # Totals for probability computation
        self.total_unigrams: int = 0
        self.vocab_size: int = 0
        self.is_trained: bool = False

    def train(self, corpus_path: str):
        """
        Trains unigram, bigram, and trigram models from a monolingual text corpus.
        Each line in the corpus is treated as a sentence.
        """
        print(f"Training N-gram model from {corpus_path}...")

        unigrams = Counter()
        bigrams: Dict[str, Counter] = defaultdict(Counter)
        trigrams: Dict[Tuple[str, str], Counter] = defaultdict(Counter)

        line_count = 0
        with open(corpus_path, "r", encoding="utf-8") as f:
            for line in f:
                tokens = self.tokenizer.tokenize(line)
                if len(tokens) < 2:
                    continue

                line_count += 1
                # Add sentence boundary markers
                tokens = ["<s>"] + tokens + ["</s>"]

                # Count unigrams (excluding markers)
                for t in tokens[1:-1]:
                    unigrams[t] += 1

                # Count bigrams
                for i in range(len(tokens) - 1):
                    bigrams[tokens[i]][tokens[i + 1]] += 1

                # Count trigrams
                for i in range(len(tokens) - 2):
                    key = (tokens[i], tokens[i + 1])
                    trigrams[key][tokens[i + 2]] += 1

        self.unigram_counts = unigrams
        self.bigram_counts = bigrams
        self.trigram_counts = trigrams
        self.total_unigrams = sum(unigrams.values())
        self.vocab_size = len(unigrams)
        self.is_trained = True

        print(f"  Corpus lines:     {line_count}")
        print(f"  Vocabulary size:  {self.vocab_size}")
        print(f"  Total tokens:     {self.total_unigrams}")
        print(f"  Unique bigrams:   {sum(len(v) for v in bigrams.values())}")
        print(f"  Unique trigrams:  {sum(len(v) for v in trigrams.values())}")

    def _unigram_prob(self, word: str) -> float:
        """P(word) with add-k smoothing."""
        count = self.unigram_counts.get(word, 0)
        return (count + self.smoothing_k) / (self.total_unigrams + self.smoothing_k * self.vocab_size)

    def _bigram_prob(self, word: str, prev: str) -> float:
        """P(word | prev) with add-k smoothing."""
        prev_total = sum(self.bigram_counts[prev].values())
        count = self.bigram_counts[prev].get(word, 0)
        if prev_total == 0:
            return self._unigram_prob(word)
        return (count + self.smoothing_k) / (prev_total + self.smoothing_k * self.vocab_size)

    def _trigram_prob(self, word: str, prev1: str, prev2: str) -> float:
        """P(word | prev2, prev1) with backoff to bigram."""
        key = (prev2, prev1)
        context_total = sum(self.trigram_counts[key].values())
        count = self.trigram_counts[key].get(word, 0)
        if context_total == 0:
            return self._bigram_prob(word, prev1)
        return (count + self.smoothing_k) / (context_total + self.smoothing_k * self.vocab_size)

    def predict_next_word(self, context: List[str], top_k: int = 5) -> List[Tuple[str, float]]:
        """
        Predicts the top-k most likely next words given context.

        Args:
            context: List of previous words (last 1-2 words used).
            top_k: Number of predictions to return.

        Returns:
            List of (word, probability) tuples sorted by probability descending.
        """
        if not self.is_trained:
            return []

        candidates: Dict[str, float] = {}

        if len(context) >= 2:
            # Use trigram model
            prev2, prev1 = context[-2], context[-1]
            for word in self.unigram_counts:
                prob = self._trigram_prob(word, prev1, prev2)
                candidates[word] = prob
        elif len(context) == 1:
            # Use bigram model
            prev = context[-1]
            for word in self.unigram_counts:
                prob = self._bigram_prob(word, prev)
                candidates[word] = prob
        else:
            # Unigram fallback
            for word, count in self.unigram_counts.items():
                candidates[word] = self._unigram_prob(word)

        # Sort by probability descending, return top_k
        ranked = sorted(candidates.items(), key=lambda x: x[1], reverse=True)
        return ranked[:top_k]

    def calculate_perplexity(self, test_corpus_path: str) -> float:
        """
        Computes perplexity of the model on a test corpus using bigram probabilities.
        Lower perplexity = better model.
        """
        total_log_prob = 0.0
        total_tokens = 0

        with open(test_corpus_path, "r", encoding="utf-8") as f:
            for line in f:
                tokens = self.tokenizer.tokenize(line)
                if len(tokens) < 2:
                    continue
                tokens = ["<s>"] + tokens + ["</s>"]
                for i in range(1, len(tokens)):
                    prob = self._bigram_prob(tokens[i], tokens[i - 1])
                    if prob > 0:
                        total_log_prob += math.log2(prob)
                    total_tokens += 1

        if total_tokens == 0:
            return float("inf")

        avg_log_prob = total_log_prob / total_tokens
        perplexity = 2 ** (-avg_log_prob)
        return perplexity

    def export_model(self, output_dir: str):
        """
        Exports N-gram frequency tables as JSON files for Android integration.
        """
        os.makedirs(output_dir, exist_ok=True)

        # Export unigrams
        unigram_path = os.path.join(output_dir, "unigrams.json")
        with open(unigram_path, "w", encoding="utf-8") as f:
            json.dump(dict(self.unigram_counts.most_common()), f, ensure_ascii=False)

        # Export bigrams as {prev_word: {next_word: count, ...}}
        bigram_path = os.path.join(output_dir, "bigrams.json")
        bigram_dict = {}
        for prev, nexts in self.bigram_counts.items():
            # Keep top 20 next words per context to limit file size
            top_nexts = dict(nexts.most_common(20))
            if top_nexts:
                bigram_dict[prev] = top_nexts
        with open(bigram_path, "w", encoding="utf-8") as f:
            json.dump(bigram_dict, f, ensure_ascii=False)

        # Export trigrams as {"prev2|prev1": {next_word: count, ...}}
        trigram_path = os.path.join(output_dir, "trigrams.json")
        trigram_dict = {}
        for (prev2, prev1), nexts in self.trigram_counts.items():
            key = f"{prev2}|{prev1}"
            top_nexts = dict(nexts.most_common(10))
            if top_nexts:
                trigram_dict[key] = top_nexts
        with open(trigram_path, "w", encoding="utf-8") as f:
            json.dump(trigram_dict, f, ensure_ascii=False)

        # Export model metadata
        meta_path = os.path.join(output_dir, "model_meta.json")
        meta = {
            "vocab_size": self.vocab_size,
            "total_tokens": self.total_unigrams,
            "smoothing_k": self.smoothing_k,
            "unique_bigrams": sum(len(v) for v in self.bigram_counts.values()),
            "unique_trigrams": sum(len(v) for v in self.trigram_counts.values())
        }
        with open(meta_path, "w", encoding="utf-8") as f:
            json.dump(meta, f, indent=2)

        print(f"Exported N-gram model to {output_dir}/")
        print(f"  unigrams.json: {self.vocab_size} entries")
        print(f"  bigrams.json:  {len(bigram_dict)} context entries")
        print(f"  trigrams.json: {len(trigram_dict)} context entries")


if __name__ == "__main__":
    model = NgramLanguageModel(smoothing_k=0.01)
    model.train("datasets/raw/vocabulary/monolingual_nagamese.txt")

    print("\nSample predictions:")
    for ctx in [["moi"], ["tai", "laga"], ["isor", "laga"]]:
        preds = model.predict_next_word(ctx, top_k=5)
        print(f"  Context {ctx} -> {[(w, round(p, 4)) for w, p in preds]}")

    perp = model.calculate_perplexity("datasets/raw/vocabulary/monolingual_nagamese.txt")
    print(f"\nCorpus perplexity: {perp:.2f}")
