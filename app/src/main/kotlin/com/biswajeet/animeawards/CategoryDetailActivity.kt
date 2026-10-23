package com.biswajeet.animeawards

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var category: String
    private lateinit var listView: ListView
    private var year = 0
    private var candidates: List<Anime> = emptyList()
    private var genre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        category = intent.getStringExtra("category") ?: "Award"
        year = intent.getIntExtra("year", Storage.currentYear())
        genre = Storage.GENRE_CATEGORIES[category]
        findViewById<TextView>(R.id.categoryTitle).text = "$category · $year"

        val all = Storage.getAnimeForYear(this, year)
        val g = genre
        candidates = if (g != null) all.filter { g in it.genres } else all

        val hint: TextView = findViewById(R.id.hintText)
        hint.text = if (g != null) {
            "Every $g anime from $year in your list is nominated automatically. Tap one to pick the winner."
        } else {
            "Tap an anime from $year to nominate it or pick the winner."
        }

        val emptyText: TextView = findViewById(R.id.emptyText)
        if (candidates.isEmpty()) {
            emptyText.text = if (g != null) {
                "No $g anime from $year in your list yet."
            } else {
                "No anime from $year in your list yet. Add some with Search & Add or import."
            }
            emptyText.visibility = View.VISIBLE
        }

        listView = findViewById(R.id.animeChoiceList)
        listView.setOnItemClickListener { _, _, position, _ ->
            showOptions(candidates[position])
        }

        refresh()
    }

    private fun refresh() {
        val nominees = Storage.getNominees(this, category, year)
        val winner = Storage.getWinner(this, category, year)
        val rows = candidates.map { anime ->
            when {
                anime.id == winner?.id -> "🏆  ${anime.title}  (Winner)"
                genre != null || anime.id in nominees -> "⭐  ${anime.title}  (Nominee)"
                else -> anime.title
            }
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun showOptions(anime: Anime) {
        val winner = Storage.getWinner(this, category, year)
        val isWinner = winner?.id == anime.id

        // Genre categories: everyone is already nominated, so only the winner can be chosen.
        if (genre != null) {
            val options = arrayOf(if (isWinner) "Remove winner status" else "Make winner")
            AlertDialog.Builder(this)
                .setTitle(anime.title)
                .setItems(options) { _, _ ->
                    Storage.setWinner(this, category, if (isWinner) null else anime.id, year)
                    refresh()
                }
                .show()
            return
        }

        // Other categories: nominate manually, then pick a winner.
        val nominees = Storage.getNominees(this, category, year).toMutableList()
        val isNominee = anime.id in nominees

        val options = arrayOf(
            if (isNominee) "Remove nomination" else "Nominate",
            if (isWinner) "Remove winner status" else "Make winner"
        )

        AlertDialog.Builder(this)
            .setTitle(anime.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (isNominee) {
                            nominees.remove(anime.id)
                            if (isWinner) Storage.setWinner(this, category, null, year)
                        } else {
                            nominees.add(anime.id)
                        }
                        Storage.saveNominees(this, category, nominees, year)
                    }
                    1 -> {
                        if (isWinner) {
                            Storage.setWinner(this, category, null, year)
                        } else {
                            if (!isNominee) {
                                nominees.add(anime.id)
                                Storage.saveNominees(this, category, nominees, year)
                            }
                            Storage.setWinner(this, category, anime.id, year)
                        }
                    }
                }
                refresh()
            }
            .show()
    }
}
