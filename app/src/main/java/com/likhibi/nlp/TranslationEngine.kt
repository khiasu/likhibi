package com.likhibi.nlp

import com.likhibi.models.TranslationResult

/**
 * Interface contract for the English <-> Nagamese Machine Translation Engine.
 *
 * Designed to support prototype translation model integration (on-device TFLite/ONNX
 * or server-backed API fallback).
 */
interface TranslationEngine {

    /**
     * Translates input text between English and Nagamese.
     *
     * @param text Input text string.
     * @param sourceLang BCP-47 source language tag ("en" or "nag").
     * @param targetLang BCP-47 target language tag ("en" or "nag").
     * @return [TranslationResult] containing output string and metadata.
     */
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult
}
