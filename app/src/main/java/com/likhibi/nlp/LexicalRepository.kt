package com.likhibi.nlp

import com.likhibi.models.LexicalEntry

/**
 * Interface contract for accessing the structured Nagamese Lexical Database.
 *
 * Provides abstraction over local storage (SQLite/Room/Binary Indexed Dictionary)
 * containing ~20,000 Nagamese lexical items.
 */
interface LexicalRepository {

    /**
     * Looks up exact lexical details for a given Nagamese lemma.
     */
    suspend fun getEntry(word: String): LexicalEntry?

    /**
     * Searches lexical database for words matching a given prefix.
     */
    suspend fun searchByPrefix(prefix: String, limit: Int = 10): List<LexicalEntry>

    /**
     * Retrieves total entry count in the lexical repository.
     */
    suspend fun getEntryCount(): Int
}
