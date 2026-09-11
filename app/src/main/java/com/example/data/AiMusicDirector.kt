package com.example.data

import com.example.BuildConfig
import com.example.model.MusicTrack
import com.example.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

// --- API Data Classes ---
@Serializable data class GenerateContentRequest(val contents: List<Content>, val generationConfig: GenerationConfig? = null, val systemInstruction: Content? = null)
@Serializable data class Content(val parts: List<Part>, val role: String? = null)
@Serializable data class Part(val text: String? = null)
@Serializable data class GenerationConfig(val responseFormat: ResponseFormat? = null, val temperature: Float? = null)
@Serializable data class ResponseFormat(val type: String, val schema: JsonObject? = null) // Added 'type' based on standard schema, though format text varies
@Serializable data class GenerateContentResponse(val candidates: List<Candidate>)
@Serializable data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}

class AiMusicDirector {
    
    // We expect the result to be a list of indices or IDs matching the input tracks.
    @Serializable
    data class AiPlaylistResponse(val trackIds: List<String>, val name: String, val description: String)
    
    @Serializable
    data class AiReasoningResponse(val reason: String)

    suspend fun generatePlaylist(prompt: String, availableTracks: List<MusicTrack>): Playlist? = withContext(Dispatchers.IO) {
        if (availableTracks.isEmpty()) return@withContext null
        
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback mock AI if API key not set
            return@withContext mockGeneratePlaylist(prompt, availableTracks)
        }

        // Prepare library metadata for AI to select from (limit to 100 tracks to avoid large prompt)
        val libraryStr = availableTracks.take(100).joinToString("\n") { 
            "${it.id}: ${it.title} by ${it.artist} (${it.album})"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(
                text = "User Request: '$prompt'\n\nAvailable Library:\n$libraryStr"
            )))),
            systemInstruction = Content(parts = listOf(Part(
                text = "You are Overtone's AI Music Director. Based on the user request, select appropriate tracks from the library. Return ONLY a JSON object with 'trackIds' (array of strings), 'name' (playlist name), and 'description' (playlist description)."
            )))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val textResponse = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext null
            
            // Clean up JSON block
            val cleanJson = textResponse.replace("```json", "").replace("```", "").trim()
            val jsonParser = Json { ignoreUnknownKeys = true }
            val parsedResponse = jsonParser.decodeFromString<AiPlaylistResponse>(cleanJson)
            
            val selectedTracks = availableTracks.filter { it.id in parsedResponse.trackIds }
            if (selectedTracks.isEmpty()) return@withContext null
            
            Playlist(
                id = "ai_${System.currentTimeMillis()}",
                name = parsedResponse.name,
                description = parsedResponse.description,
                tracks = selectedTracks,
                isAiGenerated = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            mockGeneratePlaylist(prompt, availableTracks)
        }
    }
    
    private fun mockGeneratePlaylist(prompt: String, tracks: List<MusicTrack>): Playlist {
        val shuffled = tracks.shuffled().take(5)
        return Playlist(
            id = "mock_${System.currentTimeMillis()}",
            name = "AI: $prompt",
            description = "A custom playlist curated for your mood.",
            tracks = shuffled,
            isAiGenerated = true
        )
    }
    
    suspend fun getWhyRecommended(track: MusicTrack): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Based on your recent listening history of similar artists."
        }
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(
                text = "Explain why the track '${track.title}' by '${track.artist}' is recommended for the user right now in a short, engaging sentence."
            ))))
        )
        
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Recommended for you based on your taste."
        } catch (e: Exception) {
            "Because we think you'll love it."
        }
    }
}
