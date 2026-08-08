package com.likhibi.nlp

import com.likhibi.models.PredictionResult

/**
 * High-level contract for the Contextual Word Prediction Engine.
 *
 * Implementations combine Trie-based prefix matching and N-gram language models
 * to generate context-aware suggestions for Nagamese typing.
 */
interface PredictionEngine {

    /**
     * Predicts next words or auto-completes the current partial word based on preceding context.
     *
     * @param contextWords Historical tokens typed before the active cursor.
     * @param currentPrefix Current partial word being actively typed by the user.
     * @param maxSuggestions Maximum number of predictions to return (default 3).
     * @return [PredictionResult] containing ordered suggestions and metadata.
     */
    suspend fun predictNextWords(
        contextWords: List<String>,
        currentPrefix: String,
        maxSuggestions: Int = 3
    ): PredictionResult
}
