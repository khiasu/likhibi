"""
End-to-End Prediction Model Training and Asset Export Pipeline.

Trains statistical N-gram language models and compiles the Trie prefix tree index,
exporting production-ready payloads into datasets/processed/prediction_models/.
"""

import os
import sys
import json

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

from nlp_research.prediction.ngram_model import NgramLanguageModel
from nlp_research.prediction.trie_builder import TrieBuilder
from nlp_research.prediction.prediction_engine import PredictionEngine

def main():
    print("=" * 70)
    print("PHASE 3 — CONTEXTUAL WORD PREDICTION ENGINE TRAINING PIPELINE")
    print("=" * 70)

    corpus_path = "datasets/raw/vocabulary/monolingual_nagamese.txt"
    lexicon_path = "datasets/processed/lexical_database/nagamese_lexicon.json"
    output_dir = "datasets/processed/prediction_models"

    # Step 1: Train Statistical N-gram Model
    print("\n[1/3] Training Statistical N-gram Model...")
    ngram_model = NgramLanguageModel(smoothing_k=0.01)
    ngram_model.train(corpus_path)
    ngram_model.export_model(output_dir)

    # Step 2: Build and Export Trie Index
    print("\n[2/3] Building Character-Level Trie Index from 21,000 Lexicon...")
    trie_builder = TrieBuilder()
    trie_builder.build_from_lexicon(lexicon_path)
    trie_path = os.path.join(output_dir, "trie_index.json")
    trie_builder.export_trie_json(trie_path)

    # Step 3: Initialize Prediction Engine & Verification
    print("\n[3/3] Testing Hybrid Prediction Engine...")
    engine = PredictionEngine(ngram_model, trie_builder)

    print("\n--- Test Suite 1: Next-Word Context Prediction ---")
    test_contexts = [
        ["moi"],
        ["tai", "laga"],
        ["isor", "pora"]
    ]
    for ctx in test_contexts:
        suggestions = engine.get_suggestions(context=ctx, prefix="", top_k=5)
        words = [s["word"] for s in suggestions]
        print(f"Context: {' '.join(ctx):<15} -> Predictions: {words}")

    print("\n--- Test Suite 2: Prefix Completion & Hybrid Reranking ---")
    test_prefixes = [
        (["moi"], "ja"),
        (["tai"], "ba"),
        ([], "scho")
    ]
    for ctx, pfix in test_prefixes:
        suggestions = engine.get_suggestions(context=ctx, prefix=pfix, top_k=5)
        words = [s["word"] for s in suggestions]
        print(f"Context: {' '.join(ctx):<10} Prefix: '{pfix}' -> Suggestions: {words}")

    # Calculate Model Perplexity
    perplexity = ngram_model.calculate_perplexity(corpus_path)
    print(f"\nModel Perplexity Score: {perplexity:.2f}")

    # Write Metadata Summary Report
    report = {
        "status": "COMPLETED",
        "vocabulary_size": trie_builder.total_words,
        "ngram_total_tokens": ngram_model.total_unigrams,
        "perplexity": round(perplexity, 2),
        "exported_assets": [
            "datasets/processed/prediction_models/unigrams.json",
            "datasets/processed/prediction_models/bigrams.json",
            "datasets/processed/prediction_models/trigrams.json",
            "datasets/processed/prediction_models/trie_index.json",
            "datasets/processed/prediction_models/model_meta.json"
        ]
    }
    report_path = os.path.join(output_dir, "training_summary.json")
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    print(f"\nPhase 3 training summary saved to {report_path}")
    print("=" * 70)

if __name__ == "__main__":
    main()
