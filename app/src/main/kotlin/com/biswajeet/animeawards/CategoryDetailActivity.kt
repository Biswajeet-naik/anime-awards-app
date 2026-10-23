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
    private var allAnime: List<Anime> = emptyList()
    private var display: List<Anime> = emptyList()
    private var genre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        category = intent.getStringExtra("category") ?: "Award"
        year = intent.getIntExtra("year", Storage.currentYear())
        genre = Storage.GENRE_CATEGORIES[category]
        findViewById<TextView>(R.id.categoryTitle).text = "$category · $year"

        // Every anime released this year can be nominated in any category.
        allAnime = Storage.getAnimeForYear(this, year)
        val g = genre

        val hint: TextView = findViewById(R.id.hintText)
        hint.text = if (g != null) {
            "Tap an anime from $year to nominate it or pick the winner. $g anime are listed first."
        } else {
            "Tap an anime from $year to nominate it or pick the winner."
        }

        val emptyText: TextView = findViewById(R.id.emptyText)
        if (allAnime.isEmpty()) {
            emptyText.text = "No anime from $year in your list yet. Add some with Search & Add or import."
            emptyText.visibility = View.VISIBLE
        }

        listView = findViewById(R.id.animeChoiceList)
        listView.setOnItemClickListener { _, _, position, _ ->
            showOptions(display[position])
        }

        refresh()
    }

    private fun refresh() {
        val nominees = Storage.getNominees(this, category, year)
        val winner = Storage.getWinner(this, category, year)
        val g = genre

        // 0 = winner, 1 = nominees, 2 = genre matches, 3 = everything else
        fun rank(anime: Anime): Int = when {
            anime.id == winner?.id -> 0
            anime.id in nominees -> 1
            g != null && g in anime.genres -> 2
            else -> 3
        }

        display = allAnime.sortedBy { rank(it) }

        val rows = display.map { anime ->
            when (rank(anime)) {
                0 -> "🏆  ${anime.title}  (Winner)"
                1 -> "⭐  ${anime.title}  (Nominee)"
                2 -> "${anime.title}  · $g"
                else -> anime.title
            }
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun showOptions(anime: Anime) {
        val winner = Storage.getWinner(this, category, year)
        val isWinner = winner?.id == anime.id

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
