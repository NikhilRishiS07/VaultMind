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

        val request = GroqChatCompletionRequest(
            model = model,
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = "You are a completion engine. Given an incomplete phrase from a personal note, you generate 3 grammatically correct continuations that make the combined text flow seamlessly. Never generate full new sentences; only continue the anchor."
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
                        recentSuggestions = recentSuggestions
                    )
                )
            ),
            temperature = 0.1,
            presencePenalty = 0.95,
            frequencyPenalty = 0.95,
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
        recentSuggestions: List<String>
    ): String {
        val avoidList = recentSuggestions.take(8).joinToString("\n") { "- $it" }
        return """
            Complete the anchor text with 3 grammatically correct continuations.
            Each suggestion MUST be a direct completion of the last phrase in the anchor—not a new sentence.
            The suggestion should make the anchor + suggestion read as one smooth, grammatical phrase or clause.
            
            CRITICAL: If the anchor ends with "i started to", suggest verb phrases that follow it: "feel more awake", "scroll through my phone", "grab more coffee".
            If the anchor ends with "and i", suggest what happened next: "felt tired", "went to the kitchen", "checked my phone".
            If the anchor is a complete sentence, suggest the next clause or action: "and then i", "so i", "but it was".
            
            Do NOT generate:
            - Full new sentences starting with "I" or "It" or "There"
            - Independent clauses that don't connect to the anchor
            - Generic advice or summaries
            - Unrelated memories or ideas
            
            Do ONLY generate:
            - Verb phrases, noun phrases, or clauses that grammatically continue the anchor
            - Words that make the anchor + suggestion one coherent sentence
            - Details grounded in the note's context
            
            Keep each suggestion 2-8 words.
            Avoid repeating suggestions from the recent outputs list.
            Return only the suggestions as plain text, one per line.

            ANCHOR TEXT (continue ONLY this phrase):
            ${if (continuationAnchor.isBlank()) "(none)" else continuationAnchor}

            FULL NOTE (for context only):
            ${if (noteBody.isBlank()) "(none)" else noteBody}

            RECENTLY USED OUTPUTS TO AVOID:
            ${if (avoidList.isBlank()) "(none)" else avoidList}

            EXAMPLE OF CORRECT vs WRONG:
            Anchor: "and i started to"
            CORRECT: "feel tired", "check my phone", "go make coffee"
            WRONG: "i was feeling tired", "it was weird", "i decided to"
            
            Anchor: "so i"
            CORRECT: "grabbed water", "sat down", "turned on the tv"
            WRONG: "i sat down", "there was", "i went to"

            Never output phrases that restart the sentence with a pronoun.
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

        val tailWordCount = minOf(7, words.size)
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

    private fun parseSuggestions(content: String, recentSuggestions: List<String>): List<String> {
        val cleanedLines = content
            .lines()
            .map { line -> line.trim().removePrefix("-").removePrefix("*").trim() }
            .map { line -> line.replace(Regex("^\\d+[.)]\\s*"), "") }
            .map { line -> line.trim() }
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
            .filter { it.isNotBlank() }
            .filter { isValidContinuation(it) }

        val unique = mutableListOf<String>()
        for (candidate in normalized) {
            // skip if similar to recent suggestions
            val isRecentDuplicate = recentSuggestions.any { recent ->
                similarity(recent, candidate) >= 0.75
            }
            if (isRecentDuplicate) continue

            val isNearDuplicate = unique.any { existing ->
                similarity(existing, candidate) >= 0.75
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
        val words = text.split(Regex("\\s+"))
        if (words.isEmpty()) return false
        
        val badStarts = setOf(
            "i ", "it ", "there ", "this ", "that ", "we ", "they ", "you ", "he ", "she ",
            "the ", "a ", "an "
        )
        
        for (bad in badStarts) {
            if (lower.startsWith(bad)) {
                val afterBad = lower.drop(bad.length - 1)
                if (afterBad.startsWith(" was") || afterBad.startsWith(" were") ||
                    afterBad.startsWith(" am") || afterBad.startsWith(" is") ||
                    afterBad.startsWith(" are")) {
                    return false
                }
            }
        }
        
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