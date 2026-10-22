package com.biswajeet.animeawards

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Storage {

    val CATEGORIES = listOf(
        "Best Opening",
        "Best Ending",
        "Best Romance",
        "Best Action",
        "Best Fantasy",
        "Anime of the Year"
    )

    // Categories that fill themselves from the anime's genres.
    val GENRE_CATEGORIES = mapOf(
        "Best Romance" to "Romance",
        "Best Action" to "Action",
        "Best Fantasy" to "Fantasy"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)

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

    fun getNominees(context: Context, category: String): List<Int> {
        val validIds = getAnime(context).map { it.id }
        val json = prefs(context).getString("nominees2_$category", null) ?: return emptyList()
        val array = JSONArray(json)
        val ids = mutableListOf<Int>()
        for (i in 0 until array.length()) {
            val id = array.getInt(i)
            if (id in validIds) ids.add(id)
        }
        return ids
    }

    fun saveNominees(context: Context, category: String, ids: List<Int>) {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        prefs(context).edit().putString("nominees2_$category", array.toString()).apply()
    }

    fun getWinner(context: Context, category: String): Anime? {
        val id = prefs(context).getInt("winner2_$category", -1)
        if (id == -1) return null
        return getAnime(context).firstOrNull { it.id == id }
    }

    fun setWinner(context: Context, category: String, id: Int?) {
        val editor = prefs(context).edit()
        if (id == null) {
            editor.remove("winner2_$category")
        } else {
            editor.putInt("winner2_$category", id)
        }
        editor.apply()
    }
}
