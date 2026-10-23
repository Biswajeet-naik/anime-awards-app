package com.biswajeet.animeawards

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// One award category. If genre is set, it fills itself from that AniList genre.
// If genre is null, you nominate by hand.
data class Category(val name: String, val genre: String? = null)

object Storage {

    // To add a category, add one line here. To remove one, delete its line.
    val ALL_CATEGORIES = listOf(
        Category("Best Opening"),
        Category("Best Ending"),
        Category("Best Character"),
        Category("Best Soundtrack"),
        Category("Best Animation"),
        Category("Best Romance", "Romance"),
        Category("Best Action", "Action"),
        Category("Best Fantasy", "Fantasy"),
        Category("Best Comedy", "Comedy"),
        Category("Best Drama", "Drama"),
        Category("Best Sci-Fi", "Sci-Fi"),
        Category("Best Slice of Life", "Slice of Life"),
        Category("Most Underrated"),
        Category("Biggest Disappointment"),
        Category("Anime of the Year")
    )

    val CATEGORIES: List<String> = ALL_CATEGORIES.map { it.name }

    // Categories that fill themselves from the anime's genres.
    val GENRE_CATEGORIES: Map<String, String> =
        ALL_CATEGORIES.mapNotNull { c -> c.genre?.let { c.name to it } }.toMap()

    // Picks you made before years existed are treated as this year's.
    private const val LEGACY_YEAR = 2026

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    private fun prefs(context: Context) =
        context.getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)

    // ---------- Anime list ----------

    fun getAnime(context: Context): List<Anime> {
        val json = prefs(context).getString("anime_v2", null) ?: return emptyList()
        val array = JSONArray(json)
        val list = mutableListOf<Anime>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val genresArray = obj.getJSONArray("genres")
            val genres = mutableListOf<String>()
            for (g in 0 until genresArray.length()) {
                genres.add(genresArray.getString(g))
            }
            list.add(
                Anime(
                    obj.getInt("id"),
                    obj.getString("title"),
                    genres,
                    obj.getString("cover")
                )
            )
        }
        return list
    }

    fun saveAnime(context: Context, list: List<Anime>) {
        val array = JSONArray()
        list.forEach { anime ->
            val obj = JSONObject()
            obj.put("id", anime.id)
            obj.put("title", anime.title)
            obj.put("genres", JSONArray(anime.genres))
            obj.put("cover", anime.coverUrl)
            array.put(obj)
        }
        prefs(context).edit().putString("anime_v2", array.toString()).apply()
    }

    // Returns false if this anime is already in the list.
    fun addAnime(context: Context, anime: Anime): Boolean {
        val list = getAnime(context).toMutableList()
        if (list.any { it.id == anime.id }) return false
        list.add(anime)
        saveAnime(context, list)
        return true
    }

    // ---------- Release years (AniList id -> year the anime came out) ----------

    fun getYears(context: Context): Map<Int, Int> {
        val json = prefs(context).getString("release_years", null) ?: return emptyMap()
        val obj = JSONObject(json)
        val map = mutableMapOf<Int, Int>()
        obj.keys().forEach { key -> map[key.toInt()] = obj.getInt(key) }
        return map
    }

    fun saveYears(context: Context, map: Map<Int, Int>) {
        val obj = JSONObject()
        map.forEach { (id, year) -> obj.put(id.toString(), year) }
        prefs(context).edit().putString("release_years", obj.toString()).apply()
    }

    // Only the anime that came out in the given year.
    fun getAnimeForYear(context: Context, year: Int): List<Anime> {
        val years = getYears(context)
        return getAnime(context).filter { years[it.id] == year }
    }

    // Years shown in the switcher (newest first). Always includes this year.
    fun yearsAvailable(context: Context): List<Int> {
        val inList = getAnime(context).map { it.id }.toSet()
        val years = getYears(context).filterKeys { it in inList }.values.filter { it > 0 }
        return (years + currentYear()).distinct().sortedDescending()
    }

    // ---------- Nominees and winners (saved per year) ----------

    private fun nomineesKey(category: String, year: Int) =
        if (year == LEGACY_YEAR) "nominees2_$category" else "nominees3_${year}_$category"

    private fun winnerKey(category: String, year: Int) =
        if (year == LEGACY_YEAR) "winner2_$category" else "winner3_${year}_$category"

    fun getNominees(context: Context, category: String, year: Int = currentYear()): List<Int> {
        val validIds = getAnimeForYear(context, year).map { it.id }
        val json = prefs(context).getString(nomineesKey(category, year), null) ?: return emptyList()
        val array = JSONArray(json)
        val ids = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            val id = array.getInt(i)
            if (id in validIds) ids.add(id)
        }
        return ids
    }

    fun saveNominees(context: Context, category: String, ids: List<Int>, year: Int = currentYear()) {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        prefs(context).edit().putString(nomineesKey(category, year), array.toString()).apply()
    }

    fun getWinner(context: Context, category: String, year: Int = currentYear()): Anime? {
        val id = prefs(context).getInt(winnerKey(category, year), -1)
        if (id == -1) return null
        return getAnimeForYear(context, year).firstOrNull { it.id == id }
    }

    fun setWinner(context: Context, category: String, id: Int?, year: Int = currentYear()) {
        val editor = prefs(context).edit()
        if (id == null) {
            editor.remove(winnerKey(category, year))
        } else {
            editor.putInt(winnerKey(category, year), id)
        }
        editor.apply()
    }
}
