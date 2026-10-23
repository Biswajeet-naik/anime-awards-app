package com.biswajeet.animeawards

import android.content.ContentResolver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.zip.GZIPInputStream

class ImportException(message: String) : Exception(message)

data class ImportResult(
    val anime: List<Anime>,      // anime we matched on AniList
    val malTotal: Int,           // how many anime we tried to match
    val notes: List<String>      // extra info to show the user
)

private class ParsedList(
    val ids: List<Int>,
    val completedTotal: Int,
    val noDate: Int
)

object MalImport {

    private const val ANILIST_QUERY =
        "query (\$ids: [Int]) { Page(page: 1, perPage: 50) { " +
            "media(idMal_in: \$ids, type: ANIME) { " +
            "id idMal title { romaji english } genres coverImage { large } } } }"

    // ======================================================================
    // Way 1: MyAnimeList username (official MAL API)
    // ======================================================================

    fun importUser(
        username: String,
        year: Int?,
        onProgress: (String) -> Unit,
        onDone: (ImportResult?, String?) -> Unit
    ) {
        val main = Handler(Looper.getMainLooper())
        val progress: (String) -> Unit = { msg -> main.post { onProgress(msg) } }

        Thread {
            var result: ImportResult? = null
            var error: String? = null
            try {
                val parsed = fetchUserList(username, year, progress)
                result = finish(parsed, year, progress)
            } catch (e: ImportException) {
                error = e.message
            } catch (e: SocketTimeoutException) {
                error = "Couldn't reach MyAnimeList from your network (timed out). " +
                    "Use \"Import from a file instead\" below."
            } catch (e: UnknownHostException) {
                error = "No internet connection, or MyAnimeList is blocked on your network. " +
                    "Use \"Import from a file instead\" below."
            } catch (e: Exception) {
                error = "Import failed: " + e.javaClass.simpleName + " - " + e.message
            }
            main.post { onDone(result, error) }
        }.start()
    }

    private fun fetchUserList(username: String, year: Int?, progress: (String) -> Unit): ParsedList {
        val clientId = BuildConfig.MAL_CLIENT_ID
        if (clientId.isBlank()) {
            throw ImportException(
                "This app build has no MyAnimeList client ID. " +
                    "Check that the GitHub secret MAL_CLIENT_ID exists, then push again."
            )
        }

        val ids = linkedSetOf<Int>()
        var completed = 0
        var noDate = 0

        val encoded = URLEncoder.encode(username, "UTF-8")
        var url: String? =
            "https://api.myanimelist.net/v2/users/$encoded/animelist" +
                "?status=completed&limit=500&nsfw=1&fields=list_status"
        var page = 1

        while (url != null && page <= 20) {
            progress("Reading your MyAnimeList... (page $page)")
            val (code, text) = getWithRetry(url, clientId)

            when (code) {
                200 -> {}
                404 -> throw ImportException("MyAnimeList user \"$username\" wasn't found.")
                401, 403 -> throw ImportException(
                    "MyAnimeList refused the request (error $code). " +
                        "Make sure your anime list is public, and that the client ID is correct."
                )
                else -> throw ImportException(
                    "MyAnimeList lookup failed (error $code). " +
                        text.take(120)
                )
            }

            val json = JSONObject(text)
            val data = json.optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val node = item.optJSONObject("node") ?: continue
                val id = node.optInt("id", -1)
                if (id <= 0) continue

                val listStatus = item.optJSONObject("list_status")
                val status = listStatus?.optString("status", "completed") ?: "completed"
                if (!status.equals("completed", ignoreCase = true)) continue
                completed++

                if (year == null) {
                    ids.add(id)
                } else {
                    val finish = listStatus?.optString("finish_date", "") ?: ""
                    if (finish.length < 4) {
                        noDate++
                    } else if (finish.startsWith(year.toString())) {
                        ids.add(id)
                    }
                }
            }

            val next = json.optJSONObject("paging")?.optString("next", "") ?: ""
            url = if (next.isNotBlank() && data.length() > 0) next else null
            page++
            if (url != null) Thread.sleep(500)
        }
        return ParsedList(ids.toList(), completed, noDate)
    }

    // ======================================================================
    // Way 2: MyAnimeList export file (.xml or .xml.gz) — backup route
    // ======================================================================

    fun importFile(
        resolver: ContentResolver,
        uri: Uri,
        year: Int?,
        onProgress: (String) -> Unit,
        onDone: (ImportResult?, String?) -> Unit
    ) {
        val main = Handler(Looper.getMainLooper())
        val progress: (String) -> Unit = { msg -> main.post { onProgress(msg) } }

        Thread {
            var result: ImportResult? = null
            var error: String? = null
            try {
                progress("Reading your file...")
                val parsed = readExport(resolver, uri, year)
                result = finish(parsed, year, progress)
            } catch (e: ImportException) {
                error = e.message
            } catch (e: Exception) {
                error = "Import failed: " + e.javaClass.simpleName + " - " + e.message
            }
            main.post { onDone(result, error) }
        }.start()
    }

    private fun readExport(resolver: ContentResolver, uri: Uri, year: Int?): ParsedList {
        val opened = resolver.openInputStream(uri)
            ?: throw ImportException("Couldn't open that file.")
        val buffered = BufferedInputStream(opened)

        // MAL exports are usually gzip-compressed (.xml.gz). Detect that automatically.
        buffered.mark(2)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        val stream: InputStream =
            if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(buffered) else buffered

        stream.use { input ->
            val parser = Xml.newPullParser()
            parser.setInput(input, null)

            val ids = linkedSetOf<Int>()
            var completed = 0
            var noDate = 0

            var inAnime = false
            var id = -1
            var status = ""
            var finish = ""

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "anime" -> {
                            inAnime = true
                            id = -1
                            status = ""
                            finish = ""
                        }
                        "series_animedb_id" ->
                            if (inAnime) id = parser.nextText().trim().toIntOrNull() ?: -1
                        "my_status" ->
                            if (inAnime) status = parser.nextText().trim()
                        "my_finish_date" ->
                            if (inAnime) finish = parser.nextText().trim()
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "anime") {
                    inAnime = false
                    val isCompleted = status.equals("Completed", ignoreCase = true) || status == "2"
                    if (id > 0 && isCompleted) {
                        completed++
                        if (year == null) {
                            ids.add(id)
                        } else {
                            val hasDate = finish.length >= 4 && !finish.startsWith("0000")
                            if (!hasDate) {
                                noDate++
                            } else if (finish.startsWith(year.toString())) {
                                ids.add(id)
                            }
                        }
                    }
                }
                event = parser.next()
            }
            return ParsedList(ids.toList(), completed, noDate)
        }
    }

    // ======================================================================
    // Shared: turn the MAL ids into full anime using AniList
    // ======================================================================

    private fun finish(parsed: ParsedList, year: Int?, progress: (String) -> Unit): ImportResult {
        if (parsed.completedTotal == 0) {
            throw ImportException("No Completed anime found on that list.")
        }
        if (parsed.ids.isEmpty()) {
            throw ImportException(
                "None of your ${parsed.completedTotal} completed anime have a finish date in $year. " +
                    "Try a different year, or clear the year box to import everything."
            )
        }

        val anime = lookupOnAniList(parsed.ids, progress)

        val notes = mutableListOf<String>()
        if (year != null && parsed.noDate > 0) {
            notes.add(
                "${parsed.noDate} completed anime have no finish date on MAL, so they were skipped. " +
                    "Clear the year box to include them."
            )
        }
        return ImportResult(anime, parsed.ids.size, notes)
    }

    private fun lookupOnAniList(malIds: List<Int>, progress: (String) -> Unit): List<Anime> {
        val found = linkedMapOf<Int, Anime>()
        val chunks = malIds.chunked(50)
        for ((index, chunk) in chunks.withIndex()) {
            progress("Getting posters and genres... (${index + 1}/${chunks.size})")
            val body = JSONObject()
                .put("query", ANILIST_QUERY)
                .put("variables", JSONObject().put("ids", JSONArray(chunk)))
                .toString()
            val (code, text) = postWithRetry("https://graphql.anilist.co", body)
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

    // ======================================================================
    // Network helpers
    // ======================================================================

    private fun getWithRetry(url: String, clientId: String): Pair<Int, String> {
        var attempt = 0
        while (true) {
            val (code, text) = request(url, null, clientId)
            if ((code == 429 || code == 503) && attempt < 3) {
                attempt++
                Thread.sleep(1500L * attempt)
                continue
            }
            return Pair(code, text)
        }
    }

    private fun postWithRetry(url: String, body: String): Pair<Int, String> {
        var attempt = 0
        while (true) {
            val (code, text) = request(url, body, null)
            if ((code == 429 || code == 503) && attempt < 3) {
                attempt++
                Thread.sleep(1500L * attempt)
                continue
            }
            return Pair(code, text)
        }
    }

    // GET (with the MAL client id header) when body is null, otherwise POST JSON.
    private fun request(url: String, body: String?, clientId: String?): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 25000
        connection.setRequestProperty("Accept", "application/json")
        if (clientId != null) {
            connection.setRequestProperty("X-MAL-CLIENT-ID", clientId)
        }
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
