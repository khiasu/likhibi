package com.likhibi.models

/**
 * Data class representing a structured lexical database entry for Nagamese.
 *
 * Designed to hold comprehensive lexicographic attributes for ~20,000 target Nagamese vocabulary items.
 *
 * @property id Unique identifier for the lexical entry.
 * @property word Nagamese word/lemma (standardized spelling).
 * @property phonetic IPA or Latin-based phonetic representation.
 * @property partOfSpeech Grammatical category (e.g., Noun, Verb, Adjective, Particle).
 * @property englishGloss Concise English definition / translation.
 * @property etymology Language origin (e.g., Assamese creole, Hindi, Bengali, Indigenous Naga dialect).
 * @property variantForms Common spelling variants and orthographic variations.
 * @property frequency Score or normalized frequency ranking in corpus.
 */
data class LexicalEntry(
    val id: String,
    val word: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val englishGloss: String? = null,
    val etymology: String? = null,
    val variantForms: List<String> = emptyList(),
    val frequency: Int = 0
)
