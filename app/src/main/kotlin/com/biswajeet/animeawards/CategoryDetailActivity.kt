package com.biswajeet.animeawards

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var category: String
    private lateinit var adapter: NomineeAdapter
    private var year = 0
    private var number = -1
    private var allAnime: List<Anime> = emptyList()
    private val items = mutableListOf<CatItem>()
    private var genre: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        category = intent.getStringExtra("category") ?: "Award"
        year = intent.getIntExtra("year", Storage.currentYear())
        // Optional: the Awards screen can pass the category's number (1, 2, 3...)
        number = intent.getIntExtra("number", -1)
        genre = Storage.GENRE_CATEGORIES[category]

        // Every anime released this year can be nominated in any category.
        allAnime = Storage.getAnimeForYear(this, year)

        val recycler: RecyclerView = findViewById(R.id.animeChoiceList)
        adapter = NomineeAdapter(items, { anime -> showOptions(anime) }, { removeWinner() })

        // Tiles take 1 of 2 columns; headers, winner panel and sections take both.
        val grid = GridLayoutManager(this, 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (items.getOrNull(position) is CatItem.Tile) 1 else 2
        }
        recycler.layoutManager = grid
        recycler.adapter = adapter

        findViewById<Button>(R.id.backButton).setOnClickListener { finish() }

        refresh()
    }

    private fun removeWinner() {
        Storage.setWinner(this, category, null, year)
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

        val sorted = allAnime.sortedBy { rank(it) }

        items.clear()
        items.add(CatItem.Header(year, number, category, nominees.size, allAnime.size))
        items.add(CatItem.Winner(winner))

        val hint = when {
            allAnime.isEmpty() ->
                "No anime from $year in your list yet. Add some with Search & Add or import."
            g != null ->
                "Tap an anime from $year to nominate it or pick the winner. $g anime are listed first."
            else ->
                "Tap an anime from $year to nominate it or pick the winner."
        }
        items.add(CatItem.Note(hint))

        val nomineeAnime = sorted.filter { rank(it) == 1 }
        if (nomineeAnime.isNotEmpty()) {
            items.add(
                CatItem.Section(
                    if (winner != null) "Other nominees" else "Nominees",
                    nomineeAnime.size
                )
            )
            nomineeAnime.forEachIndexed { i, anime ->
                items.add(CatItem.Tile(NomineeRow(anime, 1, ""), i + 1))
            }
        }

        val rest = sorted.filter { rank(it) >= 2 }
        if (rest.isNotEmpty()) {
            items.add(CatItem.Section("All anime from $year", rest.size))
            rest.forEach { anime ->
                val r = rank(anime)
                val label = if (r == 2) (g ?: "") else ""
                items.add(CatItem.Tile(NomineeRow(anime, r, label), 0))
            }
        }

        adapter.notifyDataSetChanged()
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
