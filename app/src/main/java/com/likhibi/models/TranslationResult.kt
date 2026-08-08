package com.likhibi.models

/**
 * Encapsulates the output of the English <-> Nagamese Machine Translation prototype.
 *
 * @property sourceText Original input text.
 * @property translatedText Translated target output text.
 * @property sourceLanguage BCP-47 language tag of source (e.g. "en", "nag").
 * @property targetLanguage BCP-47 language tag of target.
 * @property confidence BLEU/Confidence metric estimated by MT model.
 */
data class TranslationResult(
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val confidence: Double = 0.0
)
