package com.biswajeet.animeawards

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val animeList = mutableListOf<Anime>()
    private lateinit var adapter: AnimeAdapter

    private lateinit var prevYearButton: Button
    private lateinit var nextYearButton: Button
    private lateinit var yearText: TextView
    private lateinit var countText: TextView

    // 0 means "All"
    private var selectedYear = 0
    private var options: List<Int> = listOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler: RecyclerView = findViewById(R.id.animeRecycler)
        val searchButton: Button = findViewById(R.id.searchButton)
        val importButton: Button = findViewById(R.id.importButton)
        val awardsButton: Button = findViewById(R.id.awardsButton)
        prevYearButton = findViewById(R.id.mainPrevYear)
        nextYearButton = findViewById(R.id.mainNextYear)
        yearText = findViewById(R.id.mainYearText)
        countText = findViewById(R.id.mainCountText)

        adapter = AnimeAdapter(animeList, "✕") { anime ->
            // Remove from the FULL saved list, not just the filtered one
            val all = Storage.getAnime(this).toMutableList()
            all.removeAll { it.id == anime.id }
            Storage.saveAnime(this, all)
            refresh()
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        prevYearButton.setOnClickListener {
            val i = options.indexOf(selectedYear)
            if (i > 0) {
                selectedYear = options[i - 1]
                refresh()
            }
        }
        nextYearButton.setOnClickListener {
            val i = options.indexOf(selectedYear)
            if (i < options.size - 1) {
                selectedYear = options[i + 1]
                refresh()
            }
        }

        searchButton.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        importButton.setOnClickListener {
            startActivity(Intent(this, ImportActivity::class.java))
        }
        awardsButton.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        // Newest year first, so the arrow after "All" goes to the newest year
        val years = Storage.yearsAvailable(this).toList().sortedDescending()
        options = listOf(0) + years
        if (selectedYear !in options) selectedYear = 0

        val shown = if (selectedYear == 0) {
            Storage.getAnime(this)
        } else {
            Storage.getAnimeForYear(this, selectedYear)
        }
        animeList.clear()
        animeList.addAll(shown)
        adapter.notifyDataSetChanged()

        val index = options.indexOf(selectedYear)
        yearText.text = if (selectedYear == 0) "All" else selectedYear.toString()
        prevYearButton.isEnabled = index > 0
        nextYearButton.isEnabled = index < options.size - 1
        countText.text = "${animeList.size} anime"
    }
}
