package com.likhibi.models

/**
 * Encapsulates next-word suggestions produced by the prediction engine.
 *
 * @property suggestions List of predicted word strings ordered by confidence score.
 * @property confidenceScores Optional mapping from suggestion word to probability score.
 * @property source Engine variant that generated the predictions (e.g. TRIE, NGRAM, GEMINI_FALLBACK).
 */
data class PredictionResult(
    val suggestions: List<String>,
    val confidenceScores: Map<String, Double> = emptyMap(),
    val source: PredictionSource = PredictionSource.TRIE
)

enum class PredictionSource {
    TRIE,
    NGRAM,
    HYBRID,
    GEMINI_FALLBACK
}
