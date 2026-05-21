package com.likhibi.keyboard

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val apiKey: String,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun suggestNextWords(contextWords: List<String>): List<String> {
        if (apiKey.isBlank()) return emptyList()
        if (contextWords.isEmpty()) return emptyList()

        val prompt = buildPrompt(contextWords)
        val bodyJson = JSONObject()
            .put(
                "contents",
                JSONArray()
                    .put(
                        JSONObject()
                            .put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", prompt)),
                            ),
                    ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.2)
                    .put("maxOutputTokens", 24)
            )
            .toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return httpClient.newCall(request).execute().use { response ->
            Log.d("GeminiClient", "Response code: ${response.code}")
            if (!response.isSuccessful) {
                Log.e("GeminiClient", "Error response: ${response.body?.string()}")
                return emptyList()
            }
            val payload = response.body?.string() ?: return emptyList()
            Log.d("GeminiClient", "Response payload: $payload")
            parseSuggestions(payload)
        }
    }

    private fun buildPrompt(contextWords: List<String>): String {
        val ctx = contextWords.joinToString(" ")
        return """
You are a keyboard assistant for Nagamese language.
Nagamese is a creole spoken in Nagaland, India.
It mixes Assamese, Hindi, tribal languages and English freely.
Spelling is inconsistent — accept all variants.
The user has typed: $ctx
Suggest the 3 most likely next words.
Reply with exactly 3 words separated by commas.
No explanation. No punctuation. Just the 3 words.
        """.trim()
    }

    private fun parseSuggestions(payload: String): List<String> {
        val root = JSONObject(payload)
        val candidates = root.optJSONArray("candidates") ?: return emptyList()
        if (candidates.length() == 0) return emptyList()
        val first = candidates.optJSONObject(0) ?: return emptyList()
        val content = first.optJSONObject("content") ?: return emptyList()
        val parts = content.optJSONArray("parts") ?: return emptyList()
        if (parts.length() == 0) return emptyList()
        val raw = parts.optJSONObject(0)?.optString("text") ?: return emptyList()
        val text = raw
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
        return text
            .split(",")
            .map { it.trim().split(Regex("\\s+")).firstOrNull().orEmpty() }
            .filter { it.isNotEmpty() && it.length <= 25 }
            .take(3)
    }
}
