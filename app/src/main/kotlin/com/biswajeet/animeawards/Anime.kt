package com.biswajeet.animeawards

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Anime(
    val id: Int,
    val title: String,
    val genres: List<String>,
    val coverUrl: String
)

object AniListApi {

    private const val QUERY =
        "query (\$search: String) { Page(page: 1, perPage: 15) { " +
            "media(search: \$search, type: ANIME, sort: SEARCH_MATCH) { " +
            "id title { romaji english } genres coverImage { large } } } }"

    // Runs the search on a background thread, then calls onResult on the main thread.
    // onResult gets null if something went wrong (for example no internet).
    fun search(query: String, onResult: (List<Anime>?) -> Unit) {
        Thread {
            var result: List<Anime>? = null
            try {
                val body = JSONObject()
                    .put("query", QUERY)
                    .put("variables", JSONObject().put("search", query))
                    .toString()

                val connection = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray()) }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    result = parse(response)
                }
                connection.disconnect()
            } catch (e: Exception) {
                result = null
            }
            Handler(Looper.getMainLooper()).post { onResult(result) }
        }.start()
    }

    private fun readString(obj: JSONObject, key: String): String {
        return if (obj.isNull(key)) "" else obj.getString(key)
    }

    private fun parse(json: String): List<Anime> {
        val media = JSONObject(json)
            .getJSONObject("data")
            .getJSONObject("Page")
            .getJSONArray("media")
        val list = mutableListOf<Anime>()
        for (i in 0 until media.length()) {
            val item = media.getJSONObject(i)
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

            list.add(Anime(item.getInt("id"), title, genres, cover))
        }
        return list
    }
}
