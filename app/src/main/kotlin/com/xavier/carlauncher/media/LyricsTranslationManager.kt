package com.xavier.carlauncher.media

import android.content.Context
import android.util.Log
import com.xavier.carlauncher.system.NetworkUtils
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Intelligent Bilingual Lyrics Translation Pipeline for XavierCarOS2.
 * - Detects if lyrics are already in Spanish (no duplicate translation needed).
 * - High-speed batch translation for English/foreign lyrics.
 * - Persistent offline disk cache in `lyrics_translations.json`.
 */
object LyricsTranslationManager {

    private const val TAG = "LyricsTranslation"
    private const val CACHE_FILE = "lyrics_translations.json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Key: lowercase English text -> Value: Spanish translation
    private val memoryCache = ConcurrentHashMap<String, String>()
    private var isInitialized = false
    private var cacheFile: File? = null

    // Common Spanish word markers for offline detection
    private val spanishMarkers = setOf(
        "de", "la", "que", "el", "en", "y", "a", "los", "del", "se", "las", "por", "un",
        "para", "con", "no", "una", "su", "al", "lo", "como", "más", "pero", "sus", "le",
        "ya", "o", "porque", "cuando", "muy", "sin", "sobre", "me", "te", "mi", "tu", "todo",
        "nada", "donde", "si", "yo", "ella", "él", "amor", "corazón", "vida", "noche", "tiempo",
        "siento", "quiero", "estoy", "está", "somos", "eres", "tengo", "tiene", "pa", "tú", "mí"
    )

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        cacheFile = File(context.filesDir, CACHE_FILE)

        scope.launch {
            try {
                val f = cacheFile ?: return@launch
                if (f.exists()) {
                    val json = JSONObject(f.readText())
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        memoryCache[key.lowercase().trim()] = json.getString(key)
                    }
                    Log.d(TAG, "Loaded ${memoryCache.size} offline translations from cache")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading offline translation cache", e)
            }
        }
    }

    /**
     * Determines whether the given lyrics collection is already in Spanish.
     */
    fun isSongInSpanish(lyrics: List<LyricEntry>): Boolean {
        if (lyrics.isEmpty()) return false

        // Check for Spanish specific characters (á, é, í, ó, ú, ñ, ¿, ¡)
        val textSample = lyrics.take(20).joinToString(" ") { it.text.lowercase() }
        val spanishChars = listOf('á', 'é', 'í', 'ó', 'ú', 'ñ', '¿', '¡')
        if (spanishChars.any { textSample.contains(it) }) {
            return true
        }

        // Tokenize words and count Spanish marker words
        val words = textSample.split(Regex("[\\s.,!?;:\"'()]+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return false

        val markerHits = words.count { it in spanishMarkers }
        val ratio = markerHits.toFloat() / words.size.toFloat()
        return ratio > 0.18f // Over 18% Spanish function words indicates Spanish lyrics
    }

    fun getTranslationSync(rawText: String): String? {
        val clean = rawText.lowercase().trim()
        if (clean.isBlank()) return null
        return memoryCache[clean]
    }

    /**
     * Enriches a list of lyrics with Spanish translations.
     * If the song is in Spanish, returns the list unchanged.
     * If the song is in English, fetches batch translations and saves to offline cache.
     */
    suspend fun enrichLyricsWithTranslation(
        context: Context,
        lyrics: List<LyricEntry>
    ): List<LyricEntry> = withContext(Dispatchers.IO) {
        initialize(context)
        if (lyrics.isEmpty()) return@withContext lyrics

        // If lyrics are already Spanish, do not show duplicate Spanish translation
        if (isSongInSpanish(lyrics)) {
            Log.d(TAG, "Lyrics detected as Spanish. Skipping translation.")
            return@withContext lyrics.map { it.copy(translation = null) }
        }

        // Fast pass: Check memory cache
        val missingLines = mutableListOf<String>()
        val result = lyrics.map { entry ->
            val clean = entry.text.lowercase().trim()
            val cached = memoryCache[clean]
            if (cached == null && clean.isNotBlank() && clean.length > 1) {
                missingLines.add(entry.text)
            }
            entry.copy(translation = cached)
        }.toMutableList()

        if (missingLines.isEmpty() || !NetworkUtils.isOnline(context)) {
            return@withContext result
        }

        // Batch translate missing lines in chunks of 35 lines in a single fast call
        try {
            val chunks = missingLines.chunked(35)
            var diskCacheModified = false

            for (chunk in chunks) {
                val combined = chunk.joinToString("\n")
                val encoded = URLEncoder.encode(combined, "UTF-8")
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=es&dt=t&q=$encoded"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .header("Accept", "*/*")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body()?.string() ?: return@use
                        val jsonArr = JSONArray(body)
                        val detectedLang = if (jsonArr.length() > 2) jsonArr.optString(2, "") else ""

                        if (detectedLang.equals("es", ignoreCase = true)) {
                            // Online detector confirmed song is Spanish
                            return@withContext lyrics.map { it.copy(translation = null) }
                        }

                        val sentences = jsonArr.getJSONArray(0)
                        val sb = StringBuilder()
                        for (i in 0 until sentences.length()) {
                            val part = sentences.getJSONArray(i).getString(0)
                            sb.append(part)
                        }

                        val translatedParts = sb.toString().split("\n")
                        for (i in chunk.indices) {
                            val orig = chunk[i].lowercase().trim()
                            val trans = translatedParts.getOrNull(i)?.trim()
                            if (!trans.isNullOrBlank()) {
                                memoryCache[orig] = trans
                                diskCacheModified = true
                            }
                        }
                    }
                }
            }

            if (diskCacheModified) {
                saveCacheToDisk()
            }

            // Re-apply translations
            return@withContext lyrics.map { entry ->
                val clean = entry.text.lowercase().trim()
                val trans = memoryCache[clean]
                entry.copy(translation = trans)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Batch translation failed: ${e.message}")
        }

        return@withContext result
    }

    private fun saveCacheToDisk() {
        try {
            val f = cacheFile ?: return
            val json = JSONObject()
            memoryCache.forEach { (k, v) ->
                json.put(k, v)
            }
            f.writeText(json.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed writing translation cache to disk", e)
        }
    }
}
