package com.biswajeet.animeawards

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val animeList = mutableListOf<Anime>()
    private lateinit var adapter: PosterGridAdapter

    private lateinit var prevYearButton: Button
    private lateinit var nextYearButton: Button
    private lateinit var yearText: TextView
    private lateinit var countText: TextView
    private lateinit var emptyText: TextView

    // 0 means "All"
    private var selectedYear = 0
    private var options: List<Int> = listOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root: View = findViewById(R.id.mainRoot)
        val content: View = findViewById(R.id.mainContent)
        val recycler: RecyclerView = findViewById(R.id.animeRecycler)
        val searchButton: Button = findViewById(R.id.searchButton)
        val importButton: Button = findViewById(R.id.importButton)
        val awardsButton: Button = findViewById(R.id.awardsButton)
        prevYearButton = findViewById(R.id.mainPrevYear)
        nextYearButton = findViewById(R.id.mainNextYear)
        yearText = findViewById(R.id.mainYearText)
        countText = findViewById(R.id.mainCountText)
        emptyText = findViewById(R.id.emptyText)

        // Keep the header below the status bar, and keep the last row
        // of posters scrollable above the floating Awards pill.
        val pillBaseMargin = (awardsButton.layoutParams as FrameLayout.LayoutParams).bottomMargin
        val listBasePadding = recycler.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            content.setPadding(0, bars.top, 0, 0)
            val lp = awardsButton.layoutParams as FrameLayout.LayoutParams
            lp.bottomMargin = pillBaseMargin + bars.bottom
            awardsButton.layoutParams = lp
            recycler.setPadding(
                recycler.paddingLeft,
                recycler.paddingTop,
                recycler.paddingRight,
                listBasePadding + bars.bottom
            )
            insets
        }

        adapter = PosterGridAdapter(animeList) { anime -> confirmRemove(anime) }
        recycler.layoutManager = GridLayoutManager(this, 2)
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

    private fun confirmRemove(anime: Anime) {
        AlertDialog.Builder(this)
            .setTitle("Remove ${anime.title}?")
            .setMessage("It will be removed from your whole list.")
            .setPositiveButton("Remove") { _, _ ->
                // Remove from the FULL saved list, not just the filtered one
                val all = Storage.getAnime(this).toMutableList()
                all.removeAll { it.id == anime.id }
                Storage.saveAnime(this, all)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        prevYearButton.alpha = if (prevYearButton.isEnabled) 1f else 0.3f
        nextYearButton.alpha = if (nextYearButton.isEnabled) 1f else 0.3f
        countText.text = "${animeList.size} anime"
        emptyText.visibility = if (animeList.isEmpty()) View.VISIBLE else View.GONE
    }
}
