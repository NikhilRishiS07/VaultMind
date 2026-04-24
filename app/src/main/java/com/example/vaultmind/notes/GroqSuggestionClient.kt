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

    suspend fun generateSuggestions(noteTitle: String, noteBody: String): List<String> {
        if (noteTitle.isBlank() && noteBody.isBlank()) return emptyList()

        val title = noteTitle.trim()
        val body = noteBody.trim()
        val recentExcerpt = body.takeLast(700)

        val request = GroqChatCompletionRequest(
            model = model,
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = "You write concise productivity suggestions. Stay grounded in the provided title/body and avoid repeating wording or ideas."
                ),
                GroqMessage(
                    role = "user",
                    content = buildPrompt(
                        noteTitle = title,
                        noteBody = body,
                        recentBodyExcerpt = recentExcerpt
                    )
                )
            ),
            temperature = 0.8,
            maxTokens = 96
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

        return parseSuggestions(content)
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

    private fun buildPrompt(noteTitle: String, noteBody: String, recentBodyExcerpt: String): String {
        return """
            Give 3 short productivity suggestions based strictly on the note context below.
            Keep each suggestion under 10 words.
            Each suggestion must target a different angle:
            1) prioritization, 2) follow-up, 3) risk/quality check.
            Use at least one concrete term from the title or body in each suggestion.
            If the body is long, prioritize the RECENT EXCERPT for relevance.
            Do not invent topics not present in title/body.
            Do not repeat words from a previous suggestion unless necessary.
            Return only the suggestions as plain text, one per line.

            TITLE:
            ${if (noteTitle.isBlank()) "(none)" else noteTitle}

            BODY:
            ${if (noteBody.isBlank()) "(none)" else noteBody}

            RECENT EXCERPT (highest priority):
            ${if (recentBodyExcerpt.isBlank()) "(none)" else recentBodyExcerpt}
        """.trimIndent()
    }

    private fun parseSuggestions(content: String): List<String> {
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

        val unique = mutableListOf<String>()
        for (candidate in normalized) {
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