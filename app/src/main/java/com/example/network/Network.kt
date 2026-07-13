package com.example.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-pro-latest:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val response_format: OpenAiResponseFormat? = null,
    val temperature: Float? = null
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiResponseFormat(
    val type: String
)

@Serializable
data class OpenAiResponse(
    val choices: List<OpenAiChoice>? = null
)

@Serializable
data class OpenAiChoice(
    val message: OpenAiMessage? = null
)

interface OpenAiApiService {
    @POST
    suspend fun generateContent(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}

object RetrofitClient {
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    val openAiService: OpenAiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.groq.com/openai/v1/") // Will be overridden
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(OpenAiApiService::class.java)
    }
}
