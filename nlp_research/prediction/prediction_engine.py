"""
Combined Prediction Engine for Nagamese Intelligent Keyboard.

Blends N-gram language model context predictions with Trie prefix completions
to provide real-time, hybrid word suggestions for IME integration.
"""

import os
import sys
from typing import List, Dict, Any, Optional

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from nlp_research.prediction.ngram_model import NgramLanguageModel
from nlp_research.prediction.trie_builder import TrieBuilder

class PredictionEngine:
    """
    Hybrid contextual prediction and prefix completion engine.
    """

    def __init__(self, ngram_model: NgramLanguageModel, trie_builder: TrieBuilder):
        self.ngram_model = ngram_model
        self.trie_builder = trie_builder

    def get_suggestions(
        self,
        context: List[str],
        prefix: str = "",
        top_k: int = 5
    ) -> List[Dict[str, Any]]:
        """
        Generates ranked keyboard predictions based on active prefix and context.

        Args:
            context: List of previously committed word strings (e.g. ["moi", "bazaar"]).
            prefix: Current partially typed prefix string (e.g. "ja").
            top_k: Maximum number of suggestions to return.

        Returns:
            List of prediction objects containing candidate word, score, and source.
        """
        prefix = prefix.lower().strip()
        suggestions: List[Dict[str, Any]] = []
        seen_words = set()

        if prefix:
            # Mode A: User is typing a word -> Perform Trie Prefix Completion
            prefix_candidates = self.trie_builder.search_prefix(prefix, top_k=top_k * 2)

            # Rerank prefix candidates using N-gram context if context exists
            for item in prefix_candidates:
                word = item["word"]
                base_freq = item["frequency"]

                # Calculate N-gram contextual probability bonus
                context_score = 0.0
                if context and self.ngram_model.is_trained:
                    if len(context) >= 2:
                        context_score = self.ngram_model._trigram_prob(word, context[-2], context[-1])
                    else:
                        context_score = self.ngram_model._bigram_prob(word, context[-1])

                # Hybrid score blending static frequency log and contextual probability
                final_score = base_freq + (context_score * 1000.0)

                seen_words.add(word)
                suggestions.append({
                    "word": word,
                    "score": final_score,
                    "source": "prefix_completion",
                    "etymology": item.get("etymology", "Nagamese Creole / Native")
                })

        else:
            # Mode B: User just typed space / word separator -> Next-Word Prediction
            if self.ngram_model.is_trained:
                ngram_preds = self.ngram_model.predict_next_word(context, top_k=top_k)
                for word, prob in ngram_preds:
                    seen_words.add(word)
                    suggestions.append({
                        "word": word,
                        "score": prob,
                        "source": "next_word_ngram",
                        "etymology": "Nagamese Creole / Native"
                    })

        # Fallback / Padding using highest frequency words if suggestions are scarce
        if len(suggestions) < top_k:
            fallback_words = self.trie_builder.search_prefix(prefix if prefix else "a", top_k=top_k)
            for item in fallback_words:
                word = item["word"]
                if word not in seen_words:
                    seen_words.add(word)
                    suggestions.append({
                        "word": word,
                        "score": item["frequency"] * 0.1,
                        "source": "fallback_lexicon",
                        "etymology": item.get("etymology", "Nagamese Creole / Native")
                    })
                if len(suggestions) >= top_k:
                    break

        # Final ranking sort
        suggestions.sort(key=lambda x: x["score"], reverse=True)
        return suggestions[:top_k]
