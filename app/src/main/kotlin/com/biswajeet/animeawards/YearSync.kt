package com.biswajeet.animeawards

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Asks AniList for the release year of every anime in your list that has no year saved yet.
object YearSync {

    private const val API_URL = "https://graphql.anilist.co"
    private const val QUERY =
        "query(\$ids: [Int]) { Page(perPage: 50) { media(id_in: \$ids, type: ANIME) { id startDate { year } seasonYear } } }"

    // onDone(true) = everything went fine, onDone(false) = a network problem happened.
    fun sync(context: Context, onDone: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        val known = Storage.getYears(appContext)
        val missing = Storage.getAnime(appContext).map { it.id }.filter { (known[it] ?: 0) == 0 }
        if (missing.isEmpty()) {
            onDone(true)
            return
        }

        Thread {
            val years = known.toMutableMap()
            var ok = true
            try {
                for (batch in missing.chunked(50)) {
                    fetchBatch(batch, years)
                }
            } catch (e: Exception) {
                ok = false
            }
            Storage.saveYears(appContext, years)
            Handler(Looper.getMainLooper()).post { onDone(ok) }
        }.start()
    }

    private fun fetchBatch(batch: List<Int>, years: MutableMap<Int, Int>) {
        val body = JSONObject()
        body.put("query", QUERY)
        body.put("variables", JSONObject().put("ids", JSONArray(batch)))

        val conn = URL(API_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val media = JSONObject(text)
                .getJSONObject("data")
                .getJSONObject("Page")
                .getJSONArray("media")

            for (i in 0 until media.length()) {
                val obj = media.getJSONObject(i)
                val id = obj.getInt("id")
                val startYear = if (obj.isNull("startDate")) 0
                else obj.getJSONObject("startDate").optInt("year", 0)
                val seasonYear = obj.optInt("seasonYear", 0)
                val year = if (startYear > 0) startYear else seasonYear
                if (year > 0) years[id] = year
            }
        } finally {
            conn.disconnect()
        }
    }
}
