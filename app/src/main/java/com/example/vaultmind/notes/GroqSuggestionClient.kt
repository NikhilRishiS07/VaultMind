package com.example.vaultmind.notes

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class GroqSuggestionClient(
    private val apiKey: String,
    private val model: String
) {
    private val service = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GroqApiService::class.java)

    suspend fun generateSuggestions(
        noteTitle: String,
        noteBody: String,
        cursorBeforeText: String,
        cursorAfterText: String,
        recentSuggestions: List<String>
    ): List<String> {
        if (noteTitle.isBlank() && noteBody.isBlank()) return emptyList()

        val title = noteTitle.trim()
        val body = noteBody.trim()
        val beforeContext = cursorBeforeText.trim()
        val afterContext = cursorAfterText.trim()
        val recentExcerpt = extractRecentExcerpt(beforeContext.ifBlank { body })
        val continuationAnchor = extractContinuationAnchor(beforeContext, recentExcerpt)

        val styleHint = when {
            continuationAnchor.endsWith("and") -> "continue with a natural next detail"
            continuationAnchor.endsWith("to") -> "continue with a verb phrase"
            continuationAnchor.endsWith("i") -> "continue describing an action"
            else -> "continue the thought naturally"
        }

        val request = GroqChatCompletionRequest(
            model = model,
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = "You are a precise text continuation engine. You ONLY extend the given phrase naturally. Never start a new sentence."
                ),
                GroqMessage(
                    role = "user",
                    content = buildPrompt(
                        noteTitle = title,
                        noteBody = body,
                        cursorBeforeText = beforeContext,
                        cursorAfterText = afterContext,
                        recentBodyExcerpt = recentExcerpt,
                        continuationAnchor = continuationAnchor,
                        recentSuggestions = recentSuggestions,
                        styleHint = styleHint
                    )
                )
            ),
            temperature = 0.2,
            presencePenalty = 0.3,
            frequencyPenalty = 0.3,
            maxTokens = 48
        )

        val response = service.chatCompletions(
            authorization = "Bearer $apiKey",
            contentType = "application/json",
            request = request
        )

        val content = response.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            .orEmpty()

        return parseSuggestions(content, recentSuggestions)
    }

    suspend fun testConnection(): String {
        val response = service.chatCompletions(
            authorization = "Bearer $apiKey",
            contentType = "application/json",
            request = GroqChatCompletionRequest(
                model = model,
                messages = listOf(
                    GroqMessage(role = "user", content = "Reply with exactly: ok")
                ),
                temperature = 0.0,
                maxTokens = 4
            )
        )

        return response.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            .orEmpty()
    }

    private fun buildPrompt(
        noteTitle: String,
        noteBody: String,
        cursorBeforeText: String,
        cursorAfterText: String,
        recentBodyExcerpt: String,
        continuationAnchor: String,
        recentSuggestions: List<String>,
        styleHint: String
    ): String {
        val avoidList = recentSuggestions.take(8).joinToString("\n") { "- $it" }

        return """
            Complete the anchor text with 3 natural continuations.

            STRICT RULES:
            - ONLY continue the given phrase
            - DO NOT start a new sentence
            - DO NOT restart with pronouns (I, It, There, etc.)
            - Keep it realistic and consistent with the note
            - Avoid dramatic or sudden events unless implied
            - Each suggestion must be 2-8 words
            - Never repeat wording from instructions

            STYLE: $styleHint

            ANCHOR TEXT:
            ${if (continuationAnchor.isBlank()) "(none)" else continuationAnchor}

            NOTE CONTEXT:
            ${if (noteBody.isBlank()) "(none)" else noteBody}

            AVOID THESE:
            ${if (avoidList.isBlank()) "(none)" else avoidList}

            OUTPUT:
            Return only 3 suggestions, one per line.
        """.trimIndent()
    }

    private fun extractContinuationAnchor(noteBody: String, recentExcerpt: String): String {
        val source = when {
            recentExcerpt.isNotBlank() -> recentExcerpt
            noteBody.isNotBlank() -> noteBody
            else -> return ""
        }

        val words = source.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (words.isEmpty()) return ""

        val tailWordCount = minOf(5, words.size) // tightened
        return words.takeLast(tailWordCount).joinToString(" ")
    }

    private fun extractRecentExcerpt(noteBody: String): String {
        if (noteBody.isBlank()) return ""

        val sentences = noteBody
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val recentSentence = sentences.lastOrNull().orEmpty()
        if (recentSentence.isNotBlank()) {
            return recentSentence.takeLast(220)
        }

        val clauses = noteBody.split(Regex("(?<=[,;:])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return clauses.lastOrNull().orEmpty().takeLast(220)
    }

    private val badVerbs = setOf(
        "slipped", "fell", "crashed", "died", "broke", "hurt", "lost"
    )

    private fun parseSuggestions(content: String, recentSuggestions: List<String>): List<String> {
        val cleanedLines = content
            .lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .map { it.replace(Regex("^\\d+[.)]\\s*"), "") }
            .filter { it.isNotBlank() }

        val items = if (cleanedLines.isNotEmpty()) {
            cleanedLines
        } else {
            content.split(Regex("[;|]"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
        }

        val normalized = items
            .map { it.trimEnd('.', '!', '?').trim() }
            .filter { it.split(" ").size >= 2 } // avoid junk
            .filter { isValidContinuation(it) }

        val unique = mutableListOf<String>()
        for (candidate in normalized) {

            val isRecentDuplicate = recentSuggestions.any {
                similarity(it, candidate) >= 0.75
            }
            if (isRecentDuplicate) continue

            val isNearDuplicate = unique.any {
                similarity(it, candidate) >= 0.75
            }

            if (!isNearDuplicate) {
                unique.add(candidate)
            }

            if (unique.size == 3) break
        }

        return unique
    }

    private fun isValidContinuation(text: String): Boolean {
        val lower = text.lowercase()
        val words = lower.split(Regex("\\s+"))

        if (words.isEmpty()) return false

        val badStarts = setOf(
            "i", "it", "there", "this", "that", "we", "they", "you", "he", "she"
        )

        if (badStarts.contains(words.first())) return false

        if (words.any { it in badVerbs }) return false

        return text.length in 2..60
    }

    private fun similarity(a: String, b: String): Double {
        val aTokens = a.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        val bTokens = b.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0

        val overlap = aTokens.intersect(bTokens).size.toDouble()
        val base = minOf(aTokens.size, bTokens.size).toDouble()
        return overlap / base
    }

    private interface GroqApiService {
        @POST("chat/completions")
        suspend fun chatCompletions(
            @Header("Authorization") authorization: String,
            @Header("Content-Type") contentType: String,
            @Body request: GroqChatCompletionRequest
        ): GroqChatCompletionResponse
    }

    private data class GroqChatCompletionRequest(
        val model: String,
        val messages: List<GroqMessage>,
        val temperature: Double,
        @SerializedName("presence_penalty")
        val presencePenalty: Double? = null,
        @SerializedName("frequency_penalty")
        val frequencyPenalty: Double? = null,
        @SerializedName("max_tokens")
        val maxTokens: Int
    )

    private data class GroqMessage(
        val role: String,
        val content: String
    )

    private data class GroqChatCompletionResponse(
        val choices: List<GroqChoice>?
    )

    private data class GroqChoice(
        val message: GroqResponseMessage?
    )

    private data class GroqResponseMessage(
        val content: String?
    )

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/"
    }
}