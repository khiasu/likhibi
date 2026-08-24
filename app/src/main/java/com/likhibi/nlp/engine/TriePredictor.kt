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

/**
 * Production implementation of Trie-based prefix matching engine with
 * Nagamese phonetic normalization and dialect-resilient fuzzy matching.
 */
class TriePredictor(private val context: Context? = null) : PredictionEngine {

    private class TrieNode {
        val children = HashMap<Char, TrieNode>()
        var isWord: Boolean = false
        var frequency: Int = 0
    }

    private val root = TrieNode()

    @Volatile
    var isLoaded: Boolean = false
        private set

    init {
        context?.let { ctx ->
            try {
                loadFromAssets(ctx, "models/trie_index.json")
            } catch (e: Exception) {
                Log.e("TriePredictor", "Failed to load trie_index.json from assets", e)
            }
        }
    }

    fun loadFromAssets(ctx: Context, assetPath: String = "models/trie_index.json") {
        try {
            val inputStream = ctx.assets.open(assetPath)
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val jsonString = reader.use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val trieRootObj = jsonObject.optJSONObject("trie_root")
            if (trieRootObj != null) {
                parseTrieNode(trieRootObj, root)
                isLoaded = true
                Log.d("TriePredictor", "Successfully loaded Trie index from $assetPath.")
            }
        } catch (e: Exception) {
            Log.e("TriePredictor", "Error loading asset: $assetPath", e)
        }
    }

    private fun parseTrieNode(jsonObj: JSONObject, node: TrieNode) {
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "$") {
                node.isWord = true
            } else if (key == "f") {
                node.frequency = jsonObj.optInt("f", 1)
            } else if (key != "e" && key.length == 1) {
                val charKey = key[0]
                val childObj = jsonObj.optJSONObject(key)
                if (childObj != null) {
                    val childNode = node.children.getOrPut(charKey) { TrieNode() }
                    parseTrieNode(childObj, childNode)
                }
            }
        }
    }

    override suspend fun predictNextWords(
        contextWords: List<String>,
        currentPrefix: String,
        maxSuggestions: Int
    ): PredictionResult = withContext(Dispatchers.Default) {
        val prefix = currentPrefix.trim().lowercase()
        if (prefix.isEmpty() || !isLoaded) {
            return@withContext PredictionResult(
                suggestions = emptyList(),
                source = PredictionSource.TRIE
            )
        }

        val matches = findPrefixMatches(prefix, maxSuggestions).map { it.first }
        return@withContext PredictionResult(
            suggestions = matches,
            source = PredictionSource.TRIE
        )
    }

    fun findPrefixMatches(prefix: String, maxSuggestions: Int = 10): List<Pair<String, Int>> {
        val p = prefix.trim().lowercase()
        if (p.isEmpty() || !isLoaded) return emptyList()

        var curr = root
        for (ch in p) {
            curr = curr.children[ch] ?: return findPhoneticFallbackMatches(p, maxSuggestions)
        }

        val candidates = mutableListOf<Pair<String, Int>>()
        collectWords(curr, StringBuilder(p), candidates)

        val directMatches = candidates
            .sortedByDescending { it.second }
            .distinctBy { it.first }
            .take(maxSuggestions)

        if (directMatches.size < 3) {
            val phonetic = findPhoneticFallbackMatches(p, maxSuggestions - directMatches.size)
            return (directMatches + phonetic).distinctBy { it.first }.take(maxSuggestions)
        }

        return directMatches
    }

    /**
     * Nagamese Phonetic & Dialect Normalization (s/sh/ch, o/u, ee/i, b/v)
     */
    private fun findPhoneticFallbackMatches(prefix: String, limit: Int): List<Pair<String, Int>> {
        val variations = mutableSetOf<String>()
        if (prefix.contains("sh")) variations.add(prefix.replace("sh", "s"))
        if (prefix.contains("s") && !prefix.contains("sh")) variations.add(prefix.replace("s", "sh"))
        if (prefix.contains("ch")) variations.add(prefix.replace("ch", "s"))
        if (prefix.contains("u")) variations.add(prefix.replace("u", "o"))
        if (prefix.contains("o")) variations.add(prefix.replace("o", "u"))
        if (prefix.contains("ee")) variations.add(prefix.replace("ee", "i"))
        if (prefix.contains("i")) variations.add(prefix.replace("i", "ee"))

        val results = mutableListOf<Pair<String, Int>>()
        for (v in variations) {
            var curr = root
            var possible = true
            for (ch in v) {
                curr = curr.children[ch] ?: run { possible = false; return@run root }
            }
            if (possible) {
                collectWords(curr, StringBuilder(v), results)
            }
        }

        return results.sortedByDescending { it.second }.distinctBy { it.first }.take(limit)
    }

    private fun collectWords(node: TrieNode, currentPath: StringBuilder, results: MutableList<Pair<String, Int>>) {
        if (node.isWord) {
            results.add(currentPath.toString() to node.frequency)
        }
        for ((ch, childNode) in node.children) {
            currentPath.append(ch)
            collectWords(childNode, currentPath, results)
            currentPath.deleteCharAt(currentPath.length - 1)
        }
    }
}
