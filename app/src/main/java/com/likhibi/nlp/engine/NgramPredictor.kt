package com.likhibi.nlp.engine

import android.content.Context
import android.util.Log
import com.likhibi.models.PredictionResult
import com.likhibi.models.PredictionSource
import com.likhibi.nlp.PredictionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of Statistical N-gram Language Modeling engine.
 * Processes 3-gram, 2-gram, and 1-gram matrices for high-accuracy contextual prediction.
 */
class NgramPredictor(private val context: Context? = null) : PredictionEngine {

    private val trigramMap = ConcurrentHashMap<String, Map<String, Int>>()
    private val bigramMap = ConcurrentHashMap<String, Map<String, Int>>()
    private val unigramMap = ConcurrentHashMap<String, Int>()

    @Volatile
    var isLoaded: Boolean = false
        private set

    init {
        context?.let { ctx ->
            try {
                loadFromAssets(ctx)
            } catch (e: Exception) {
                Log.e("NgramPredictor", "Failed to load N-gram models from assets", e)
            }
        }
    }

    fun loadFromAssets(
        ctx: Context,
        trigramsAsset: String = "models/trigrams.json",
        bigramsAsset: String = "models/bigrams.json",
        unigramsAsset: String = "models/unigrams.json"
    ) {
        try {
            // 1. Load Trigrams
            try {
                val tgStream = ctx.assets.open(trigramsAsset)
                val tgReader = BufferedReader(InputStreamReader(tgStream, "UTF-8"))
                val tgJson = JSONObject(tgReader.use { it.readText() })
                val tgKeys = tgJson.keys()
                while (tgKeys.hasNext()) {
                    val ctxKey = tgKeys.next()
                    val innerObj = tgJson.optJSONObject(ctxKey) ?: continue
                    val innerMap = HashMap<String, Int>()
                    val innerKeys = innerObj.keys()
                    while (innerKeys.hasNext()) {
                        val word3 = innerKeys.next()
                        if (word3 != "</s>") {
                            innerMap[word3] = innerObj.optInt(word3, 1)
                        }
                    }
                    trigramMap[ctxKey] = innerMap
                }
            } catch (e: Exception) {
                Log.w("NgramPredictor", "Trigrams optional load notice: ${e.message}")
            }

            // 2. Load Bigrams
            val bgStream = ctx.assets.open(bigramsAsset)
            val bgReader = BufferedReader(InputStreamReader(bgStream, "UTF-8"))
            val bgJson = JSONObject(bgReader.use { it.readText() })
            val bgKeys = bgJson.keys()
            while (bgKeys.hasNext()) {
                val word1 = bgKeys.next()
                val innerObj = bgJson.optJSONObject(word1) ?: continue
                val innerMap = HashMap<String, Int>()
                val innerKeys = innerObj.keys()
                while (innerKeys.hasNext()) {
                    val word2 = innerKeys.next()
                    if (word2 != "</s>") {
                        innerMap[word2] = innerObj.optInt(word2, 1)
                    }
                }
                bigramMap[word1] = innerMap
            }

            // 3. Load Unigrams
            val ugStream = ctx.assets.open(unigramsAsset)
            val ugReader = BufferedReader(InputStreamReader(ugStream, "UTF-8"))
            val ugJson = JSONObject(ugReader.use { it.readText() })
            val ugKeys = ugJson.keys()
            while (ugKeys.hasNext()) {
                val word = ugKeys.next()
                unigramMap[word] = ugJson.optInt(word, 1)
            }

            isLoaded = true
            Log.d("NgramPredictor", "Successfully loaded N-grams: ${trigramMap.size} trigrams, ${bigramMap.size} bigrams, ${unigramMap.size} unigrams.")
        } catch (e: Exception) {
            Log.e("NgramPredictor", "Error loading N-gram assets", e)
        }
    }

    override suspend fun predictNextWords(
        contextWords: List<String>,
        currentPrefix: String,
        maxSuggestions: Int
    ): PredictionResult = withContext(Dispatchers.Default) {
        val predictions = predictNext(contextWords, currentPrefix, maxSuggestions)
        return@withContext PredictionResult(
            suggestions = predictions,
            source = PredictionSource.NGRAM
        )
    }

    fun predictNext(
        contextWords: List<String>,
        currentPrefix: String = "",
        maxSuggestions: Int = 3
    ): List<String> {
        if (!isLoaded) return emptyList()

        val prefix = currentPrefix.trim().lowercase()
        val scoredCandidates = LinkedHashMap<String, Int>()

        // 1. Trigram Matching (w_{i-2} w_{i-1} -> w_i)
        if (contextWords.size >= 2) {
            val w2 = contextWords[contextWords.size - 2].trim().lowercase()
            val w1 = contextWords[contextWords.size - 1].trim().lowercase()
            val trigramKey = "$w2 $w1"
            val tgMatches = trigramMap[trigramKey]
            if (tgMatches != null) {
                for ((w, freq) in tgMatches) {
                    if (prefix.isEmpty() || w.startsWith(prefix)) {
                        scoredCandidates[w] = freq * 100 // High weight for exact trigram match
                    }
                }
            }
        }

        // 2. Bigram Matching (w_{i-1} -> w_i)
        val lastWord = contextWords.lastOrNull()?.trim()?.lowercase() ?: ""
        if (lastWord.isNotEmpty() && bigramMap.containsKey(lastWord)) {
            val bgMatches = bigramMap[lastWord] ?: emptyMap()
            for ((w, freq) in bgMatches) {
                if (prefix.isEmpty() || w.startsWith(prefix)) {
                    val currentScore = scoredCandidates[w] ?: 0
                    scoredCandidates[w] = currentScore + (freq * 10)
                }
            }
        }

        // 3. Unigram Backoff if more candidates needed
        if (scoredCandidates.size < maxSuggestions) {
            val unigramCandidates = unigramMap.entries
                .filter { (w, _) -> (prefix.isEmpty() || w.startsWith(prefix)) && w != lastWord && !scoredCandidates.containsKey(w) }
                .sortedByDescending { it.value }
                .take(maxSuggestions - scoredCandidates.size)

            for ((w, freq) in unigramCandidates) {
                scoredCandidates[w] = freq
            }
        }

        return scoredCandidates.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(maxSuggestions)
    }

    fun getUnigramFrequency(word: String): Int {
        return unigramMap[word.lowercase().trim()] ?: 0
    }
}
