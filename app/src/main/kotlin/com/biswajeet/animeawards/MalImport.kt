package com.biswajeet.animeawards

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ImportException(message: String) : Exception(message)

data class ImportResult(
    val anime: List<Anime>,   // anime we could match on AniList
    val malTotal: Int         // how many completed anime were on the MAL list
)

object MalImport {

    private const val ANILIST_QUERY =
        "query (\$ids: [Int]) { Page(page: 1, perPage: 50) { " +
            "media(idMal_in: \$ids, type: ANIME) { " +
            "id idMal title { romaji english } genres coverImage { large } } } }"

    // Runs on a background thread. onProgress and onDone are called on the main thread.
    // onDone gets either a result or an error message (never both).
    fun importList(
        username: String,
        onProgress: (String) -> Unit,
        onDone: (ImportResult?, String?) -> Unit
    ) {
        val main = Handler(Looper.getMainLooper())
        val progress: (String) -> Unit = { msg -> main.post { onProgress(msg) } }

        Thread {
            var result: ImportResult? = null
            var error: String? = null
            try {
                val malIds = fetchMalIds(username, progress)
                if (malIds.isEmpty()) {
                    throw ImportException("No completed anime found on that MyAnimeList profile.")
                }
                val anime = lookupOnAniList(malIds, progress)
                result = ImportResult(anime, malIds.size)
            } catch (e: ImportException) {
                error = e.message
            } catch (e: Exception) {
                error = "Import failed: " + e.javaClass.simpleName + " - " + e.message
            }
            main.post { onDone(result, error) }
        }.start()
    }

    // ---------- Step 1: get the MAL ids of every Completed anime ----------

    private fun fetchMalIds(username: String, progress: (String) -> Unit): List<Int> {
        val ids = linkedSetOf<Int>()
        val encoded = URLEncoder.encode(username, "UTF-8")
        var page = 1
        while (page <= 40) {
            progress("Reading your MyAnimeList... (page $page)")
            val url = "https://api.jikan.moe/v4/users/$encoded/animelist?status=completed&page=$page"
            val (code, text) = requestWithRetry(url, null)

            if (code == 404) {
                throw ImportException("MyAnimeList user \"$username\" wasn't found, or the list is private.")
            }
            if (code == 400 || code == 403) {
                throw ImportException("MyAnimeList says this list is private or not available (error $code).")
            }
            if (code != 200) {
                throw ImportException("MyAnimeList lookup failed (error $code). Try again in a minute.")
            }

            val json = JSONObject(text)
            val data = json.optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                // The id may sit inside an "anime" object or directly on the item.
                val animeObj = item.optJSONObject("anime") ?: item
                val id = animeObj.optInt("mal_id", -1)
                if (id > 0) ids.add(id)
            }

            val hasNext = json.optJSONObject("pagination")?.optBoolean("has_next_page", false) ?: false
            if (!hasNext || data.length() == 0) break
            page++
            Thread.sleep(700) // be gentle with Jikan's rate limit
        }
        return ids.toList()
    }

    // ---------- Step 2: look the ids up on AniList (poster, genres, title) ----------

    private fun lookupOnAniList(malIds: List<Int>, progress: (String) -> Unit): List<Anime> {
        val found = linkedMapOf<Int, Anime>()
        val chunks = malIds.chunked(50)
        for ((index, chunk) in chunks.withIndex()) {
            progress("Getting posters and genres... (${index + 1}/${chunks.size})")
            val body = JSONObject()
                .put("query", ANILIST_QUERY)
                .put("variables", JSONObject().put("ids", JSONArray(chunk)))
                .toString()
            val (code, text) = requestWithRetry("https://graphql.anilist.co", body)
            if (code != 200) {
                throw ImportException("AniList lookup failed (error $code). Try again in a minute.")
            }
            val media = JSONObject(text)
                .getJSONObject("data")
                .getJSONObject("Page")
                .getJSONArray("media")
            for (i in 0 until media.length()) {
                val anime = parseAnime(media.getJSONObject(i))
                found[anime.id] = anime
            }
            Thread.sleep(300)
        }
        return found.values.toList()
    }

    private fun readString(obj: JSONObject, key: String): String {
        return if (obj.isNull(key)) "" else obj.getString(key)
    }

    private fun parseAnime(item: JSONObject): Anime {
        val titleObject = item.getJSONObject("title")
        val english = readString(titleObject, "english")
        val romaji = readString(titleObject, "romaji")
        val title = if (english.isNotBlank()) english else romaji

        val genresArray = item.getJSONArray("genres")
        val genres = mutableListOf<String>()
        for (g in 0 until genresArray.length()) {
            genres.add(genresArray.getString(g))
        }

        val cover = if (item.isNull("coverImage")) {
            ""
        } else {
            readString(item.getJSONObject("coverImage"), "large")
        }
        return Anime(item.getInt("id"), title, genres, cover)
    }

    // ---------- Network helpers ----------

    // GET if body is null, POST (JSON) otherwise. Retries a few times if the server says "slow down".
    private fun requestWithRetry(url: String, body: String?): Pair<Int, String> {
        var attempt = 0
        while (true) {
            val (code, text) = request(url, body)
            if ((code == 429 || code == 503) && attempt < 3) {
                attempt++
                Thread.sleep(1500L * attempt)
                continue
            }
            return Pair(code, text)
        }
    }

    private fun request(url: String, body: String?): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 25000
        connection.setRequestProperty("Accept", "application/json")
        if (body != null) {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()
        return Pair(code, text)
    }
}
