package com.biswajeet.animeawards

import android.content.Context
import org.json.JSONArray

object Storage {

    val CATEGORIES = listOf(
        "Best Opening",
        "Best Ending",
        "Best Romance",
        "Best Action",
        "Best Fantasy",
        "Anime of the Year"
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)

    private fun readList(context: Context, key: String): MutableList<String> {
        val json = prefs(context).getString(key, null) ?: return mutableListOf()
        val array = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun writeList(context: Context, key: String, list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs(context).edit().putString(key, array.toString()).apply()
    }

    fun getAnime(context: Context): List<String> = readList(context, "anime_list")

    fun getNominees(context: Context, category: String): MutableList<String> {
        val anime = getAnime(context)
        return readList(context, "nominees_$category")
            .filter { it in anime }
            .toMutableList()
    }

    fun saveNominees(context: Context, category: String, nominees: List<String>) {
        writeList(context, "nominees_$category", nominees)
    }

    fun getWinner(context: Context, category: String): String? {
        val winner = prefs(context).getString("winner_$category", null) ?: return null
        return if (winner in getAnime(context)) winner else null
    }

    fun setWinner(context: Context, category: String, winner: String?) {
        val editor = prefs(context).edit()
        if (winner == null) {
            editor.remove("winner_$category")
        } else {
            editor.putString("winner_$category", winner)
        }
        editor.apply()
    }
}
