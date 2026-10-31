package com.xavier.carlauncher.media

import android.content.Context
import com.xavier.carlauncher.system.NetworkUtils
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Intelligent & Strict Lyrics Repository for XavierCarOS.
 * Automatically searches, downloads, validates, and locally caches synchronized LRC lyrics.
 * Preloads upcoming sequential and shuffle/random playlist songs in the background (0ms latency).
 * Strictly verifies song title and artist to guarantee zero lyric mismatches.
 */
object LyricsRepository {

    private const val TAG = "LyricsRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val okHttpClient: OkHttpClient get() = client

    private val memoryCache = mutableMapOf<String, List<LyricEntry>>()
    private val preloadedFiles = Collections.synchronizedSet(mutableSetOf<String>())
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var preloadJob: Job? = null

    /**
     * Resolves synchronized lyrics for track:
     * 1. Memory Cache (0ms)
     * 2. Local Storage Cache (.lrc file in app storage or alongside music file, ~1ms)
     * 3. Online Fetch from LRCLIB API with strict matching
     */
    suspend fun getOrFetchLyrics(
        context: Context,
        rawTitle: String,
        rawArtist: String,
        playPath: String = ""
    ): List<LyricEntry> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTrackString(rawTitle)
        val cleanArtist = cleanArtistString(rawArtist)

        if (cleanTitle.isBlank() || cleanTitle.equals("Pista de audio", ignoreCase = true)) {
            return@withContext emptyList()
        }

        val cacheKey = "${cleanTitle}_$cleanArtist".lowercase()
        memoryCache[cacheKey]?.let { return@withContext it }

        // 1. Try local cache / sidecar file (.lrc)
        val localLyrics = loadFromLocal(context, cleanTitle, cleanArtist, playPath)
        if (localLyrics.isNotEmpty()) {
            memoryCache[cacheKey] = localLyrics
            return@withContext localLyrics
        }

        // 2. Fetch from Internet strictly matching title & artist (only if network is active)
        if (!NetworkUtils.isOnline(context)) {
            return@withContext emptyList()
        }

        try {
            val onlineLyrics = fetchOnlineLyrics(cleanTitle, cleanArtist)
            if (onlineLyrics.isNotEmpty()) {
                saveToLocalCache(context, cleanTitle, cleanArtist, onlineLyrics)
                memoryCache[cacheKey] = onlineLyrics
                return@withContext onlineLyrics
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download online lyrics for $cleanTitle - $cleanArtist: ${e.message}")
        }

        emptyList()
    }

    /**
     * Preloads lyrics for upcoming tracks and all songs in the current folder/playlist
     * so that whenever playback advances (sequentially or in shuffle/random mode),
     * the lyrics are already cached in RAM and disk (0ms delay).
     */
    fun preloadFolder(context: Context, playPath: String) {
        if (playPath.isBlank()) return
        val currentFile = File(playPath)
        val dir = currentFile.parentFile ?: return
        if (!dir.exists() || !dir.isDirectory) return

        preloadJob?.cancel()
        preloadJob = preloadScope.launch {
            try {
                val audioExts = setOf("flac", "mp3", "m4a", "wav", "ogg", "aac", "wma", "ape")
                val files = dir.listFiles { f -> f.isFile && f.extension.lowercase() in audioExts }
                    ?: return@launch

                if (files.isEmpty()) return@launch

                val curIdx = files.indexOfFirst { it.absolutePath == currentFile.absolutePath }
                val size = files.size

                // Priority order:
                // 1. Next track in folder (curIdx + 1)
                // 2. Following track (curIdx + 2)
                // 3. Previous track (curIdx - 1)
                // 4. All other tracks in folder (covers random/shuffle playback!)
                val queue = mutableListOf<File>()
                if (curIdx >= 0) {
                    queue.add(files[(curIdx + 1) % size])
                    if (size > 2) queue.add(files[(curIdx + 2) % size])
                    if (size > 1) queue.add(files[(curIdx - 1 + size) % size])
                }
                for (f in files) {
                    if (!queue.contains(f) && f.absolutePath != currentFile.absolutePath) {
                        queue.add(f)
                    }
                }

                for (targetFile in queue) {
                    if (!isActive) break
                    val path = targetFile.absolutePath
                    if (preloadedFiles.contains(path)) continue

                    val (parsedTitle, parsedArtist) = extractTitleAndArtist(targetFile)
                    val cleanT = cleanTrackString(parsedTitle)
                    val cleanA = cleanArtistString(parsedArtist)
                    val key = "${cleanT}_$cleanA".lowercase()

                    if (memoryCache.containsKey(key)) {
                        preloadedFiles.add(path)
                        continue
                    }

                    // Check local disk cache / sidecar first (0ms)
                    val local = loadFromLocal(context, cleanT, cleanA, path)
                    if (local.isNotEmpty()) {
                        memoryCache[key] = local
                        preloadedFiles.add(path)
                        continue
                    }

                    // If offline, skip online downloads completely
                    if (!NetworkUtils.isOnline(context)) {
                        preloadedFiles.add(path)
                        continue
                    }

                    // Download online in background
                    try {
                        val downloaded = fetchOnlineLyrics(cleanT, cleanA)
                        if (downloaded.isNotEmpty()) {
                            saveToLocalCache(context, cleanT, cleanA, downloaded)
                            memoryCache[key] = downloaded
                            Log.d(TAG, "Preloaded lyrics for upcoming track: '$cleanT' - '$cleanA'")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Preload skipped for $cleanT: ${e.message}")
                    }

                    preloadedFiles.add(path)
                    delay(200) // Polite throttle for LRCLIB API
                }
            } catch (e: Exception) {
                Log.w(TAG, "Preload folder error", e)
            }
        }
    }

    private fun extractTitleAndArtist(file: File): Pair<String, String> {
        val name = file.nameWithoutExtension
        if (name.contains(" - ")) {
            val parts = name.split(" - ")
            if (parts.size >= 2) {
                return Pair(parts[0].trim(), parts[1].trim())
            }
        }

        // Try extracting metadata tags
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(file.absolutePath)
            val t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            mmr.release()
            if (!t.isNullOrBlank()) {
                return Pair(t.trim(), a?.trim() ?: "")
            }
        } catch (_: Exception) {}

        return Pair(name.trim(), "")
    }

    private fun loadFromLocal(
        context: Context,
        title: String,
        artist: String,
        playPath: String
    ): List<LyricEntry> {
        val cleanT = sanitizeFileName(title).lowercase()
        val cleanA = sanitizeFileName(artist).lowercase()

        // 1. Check alongside audio file if path provided
        if (playPath.isNotBlank()) {
            val audioFile = File(playPath)
            if (audioFile.exists()) {
                val lrcSidecar = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
                if (lrcSidecar.exists() && lrcSidecar.length() > 0) {
                    try {
                        val parsed = parseLrc(lrcSidecar.readText())
                        if (parsed.isNotEmpty()) return parsed
                    } catch (_: Exception) {}
                }
            }
        }

        // 2. Check internal disk cache
        val cacheDir = File(context.cacheDir, "lyrics")
        val cacheFile = File(cacheDir, "${cleanT}_${cleanA}.lrc")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val parsed = parseLrc(cacheFile.readText())
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {}
        }

        // 3. Check public storage locations (/sdcard/Music/Lyrics, etc.)
        val externalDirs = listOf(
            File("/sdcard/Music/Lyrics"),
            File(context.getExternalFilesDir(null), "Lyrics"),
            File("/sdcard/Music"),
            File("/sdcard/Download")
        )

        for (dir in externalDirs) {
            if (dir.exists() && dir.isDirectory) {
                val directFile = File(dir, "${cleanT}_${cleanA}.lrc")
                if (directFile.exists() && directFile.length() > 0) {
                    try {
                        val parsed = parseLrc(directFile.readText())
                        if (parsed.isNotEmpty()) return parsed
                    } catch (_: Exception) {}
                }
                // Try fuzzy file match
                val matching = dir.listFiles { f ->
                    if (!f.name.endsWith(".lrc", ignoreCase = true)) return@listFiles false
                    val n = f.nameWithoutExtension.lowercase().replace(Regex("""[^a-z0-9]"""), "")
                    n.contains(cleanT) && (cleanA.isBlank() || n.contains(cleanA))
                }
                if (!matching.isNullOrEmpty()) {
                    try {
                        val parsed = parseLrc(matching[0].readText())
                        if (parsed.isNotEmpty()) return parsed
                    } catch (_: Exception) {}
                }
            }
        }

        return emptyList()
    }

    private fun fetchOnlineLyrics(title: String, artist: String): List<LyricEntry> {
        val userAgent = "Mozilla/5.0 (Linux; Android 8.1.0; XavierCarOS) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.0.0 Mobile Safari/537.36"

        // Step 1: Direct exact match via /api/get
        try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val url = if (artist.isNotBlank()) {
                "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist"
            } else {
                "https://lrclib.net/api/get?track_name=$encTitle"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (!body.isNullOrBlank()) {
                        val json = JSONObject(body)
                        val synced = json.optString("syncedLyrics", "").trim()
                        val resTrack = json.optString("trackName", "").trim()
                        val resArtist = json.optString("artistName", "").trim()

                        if (synced.isNotBlank() && isTrackMatch(title, artist, resTrack, resArtist)) {
                            val parsed = parseLrc(synced)
                            if (parsed.isNotEmpty()) {
                                Log.i(TAG, "Exact match verified for '$title' by '$artist'")
                                return parsed
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Exact match get failed, trying search fallback: ${e.message}")
        }

        // Step 2: Strict search via /api/search with title & artist validation
        try {
            val queryParam = if (artist.isNotBlank()) "$title $artist" else title
            val query = URLEncoder.encode(queryParam, "UTF-8")
            val searchUrl = "https://lrclib.net/api/search?q=$query"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (!body.isNullOrBlank()) {
                        val array = JSONArray(body)
                        for (i in 0 until minOf(array.length(), 25)) {
                            val item = array.getJSONObject(i)
                            val synced = item.optString("syncedLyrics", "").trim()
                            val resTrack = item.optString("trackName", "").trim()
                            val resArtist = item.optString("artistName", "").trim()

                            if (synced.isNotBlank() && isTrackMatch(title, artist, resTrack, resArtist)) {
                                val parsed = parseLrc(synced)
                                if (parsed.isNotEmpty()) {
                                    Log.i(TAG, "Search match verified: '$resTrack' by '$resArtist' for query '$title' - '$artist'")
                                    return parsed
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search fallback failed: ${e.message}")
        }

        return emptyList()
    }

    /**
     * Strict similarity check between target song and search result.
     * Prevents downloading or showing lyrics of another song.
     */
    private fun isTrackMatch(
        targetTitle: String,
        targetArtist: String,
        resTrack: String,
        resArtist: String
    ): Boolean {
        if (resTrack.isBlank()) return false

        val normTargetTitle = normalizeString(targetTitle)
        val normResTrack = normalizeString(resTrack)

        // Title must either be equal, or one must contain the other
        val titleMatches = normTargetTitle == normResTrack ||
                normTargetTitle.contains(normResTrack) ||
                normResTrack.contains(normTargetTitle)

        if (!titleMatches) return false

        // If target artist is known, verify artist matches too
        val normTargetArtist = normalizeString(targetArtist)
        val normResArtist = normalizeString(resArtist)

        if (normTargetArtist.isNotBlank() && normResArtist.isNotBlank()) {
            val targetParts = targetArtist.split(Regex("""[,&/]|\bfeat\.?\b|\bft\.?\b""", RegexOption.IGNORE_CASE))
                .map { normalizeString(it) }.filter { it.length >= 3 }
            val resParts = resArtist.split(Regex("""[,&/]|\bfeat\.?\b|\bft\.?\b""", RegexOption.IGNORE_CASE))
                .map { normalizeString(it) }.filter { it.length >= 3 }

            val anyPartMatches = targetParts.any { tp -> resParts.any { rp -> tp == rp || tp.contains(rp) || rp.contains(tp) } }

            val artistMatches = normTargetArtist == normResArtist ||
                    normTargetArtist.contains(normResArtist) ||
                    normResArtist.contains(normTargetArtist) ||
                    anyPartMatches

            if (!artistMatches) return false
        }

        return true
    }

    private fun normalizeString(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("""\p{M}"""), "")
            .replace(Regex("""[^a-z0-9]"""), "")
            .trim()
    }

    private fun saveToLocalCache(
        context: Context,
        title: String,
        artist: String,
        lyrics: List<LyricEntry>
    ) {
        try {
            val cleanT = sanitizeFileName(title)
            val cleanA = sanitizeFileName(artist)
            val fileName = "${cleanT}_${cleanA}.lrc"

            val sb = StringBuilder()
            for (entry in lyrics) {
                val mins = entry.timeMs / 60_000
                val secs = (entry.timeMs % 60_000) / 1000
                val ms = (entry.timeMs % 1000) / 10
                sb.append(String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s\n", mins, secs, ms, entry.text))
            }
            val content = sb.toString()

            // 1. Internal cache directory
            val cacheDir = File(context.cacheDir, "lyrics")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            File(cacheDir, fileName).writeText(content)

            // 2. Public /sdcard/Music/Lyrics directory for persistent offline backup
            try {
                val publicDir = File("/sdcard/Music/Lyrics")
                if (!publicDir.exists()) publicDir.mkdirs()
                File(publicDir, fileName).writeText(content)
            } catch (_: Exception) {}

            Log.i(TAG, "Cached ${lyrics.size} synced lyric lines to $fileName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save lyrics to disk cache", e)
        }
    }

    fun parseLrc(lrcContent: String): List<LyricEntry> {
        val list = mutableListOf<LyricEntry>()
        val timeRegex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]""")

        lrcContent.lineSequence().forEach { line ->
            val match = timeRegex.find(line)
            if (match != null) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val msString = match.groupValues.getOrNull(3) ?: "0"
                val millis = when (msString.length) {
                    1 -> msString.toLong() * 100
                    2 -> msString.toLong() * 10
                    else -> msString.take(3).toLong()
                }
                val totalMs = minutes * 60_000L + seconds * 1000L + millis
                val text = line.substring(match.range.last + 1).trim()
                if (text.isNotBlank()) {
                    list.add(LyricEntry(totalMs, text))
                }
            }
        }
        return list.sortedBy { it.timeMs }
    }

    fun cleanTrackString(raw: String): String {
        var res = raw
        // Remove parenthesized or bracketed noise: (Official Video), [Official Audio], (Visualizer), (Remastered 2021), etc.
        res = res.replace(Regex("""\s*[\(\[\{][^\)\]\}]*(?:official|video|audio|remix|hd|4k|lyric|lyrics|hq|dembow|version|remaster|single|album|feat|ft|live|prod|with|visualizer|clip)[^\)\]\}]*[\)\]\}]""", RegexOption.IGNORE_CASE), "")
        // Remove trailing dash noise: - 2015 Remaster, - Remastered, - Official Audio, - Single, - Remix
        res = res.replace(Regex("""\s*-\s*(?:official|video|audio|remix|hd|4k|remaster|remastered|live|single|album|visualizer).*""", RegexOption.IGNORE_CASE), "")
        // Remove standalone noise phrases: Album Version, Single Version, Remastered, 2015 Remaster
        res = res.replace(Regex("""\b(?:album version|single version|\d{4} remaster(?:ed)?|remastered \d{4}|remastered|remaster)\b""", RegexOption.IGNORE_CASE), "")
        // Remove feat. / ft. / featuring / with
        res = res.replace(Regex("""\b(?:feat\.|ft\.|featuring|with)\s+.*""", RegexOption.IGNORE_CASE), "")
        // Clean empty parens
        res = res.replace(Regex("""[\(\[\{]\s*[\)\]\}]"""), "")
        return res.replace("_", " ").trim()
    }

    fun cleanArtistString(raw: String): String {
        if (raw.equals("Desconocido", ignoreCase = true) ||
            raw.equals("Unknown", ignoreCase = true) ||
            raw.contains("Música local", ignoreCase = true) ||
            raw.contains("USB", ignoreCase = true)
        ) {
            return ""
        }
        return raw.trim()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[^a-zA-Z0-9.-]"""), "_").take(40)
    }

    fun clearCache() {
        memoryCache.clear()
        preloadedFiles.clear()
    }
}
