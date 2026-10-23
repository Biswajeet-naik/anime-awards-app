package com.biswajeet.animeawards

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesActivity : AppCompatActivity() {

    private lateinit var adapter: CategoryAdapter
    private lateinit var yearText: TextView
    private lateinit var statusText: TextView
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private val rows = mutableListOf<CategoryRow>()
    private var year = Storage.currentYear()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        val recycler: RecyclerView = findViewById(R.id.categoryList)
        yearText = findViewById(R.id.yearText)
        statusText = findViewById(R.id.statusText)
        prevButton = findViewById(R.id.prevYear)
        nextButton = findViewById(R.id.nextYear)

        adapter = CategoryAdapter(rows) { categoryName ->
            val intent = Intent(this, CategoryDetailActivity::class.java)
            intent.putExtra("category", categoryName)
            intent.putExtra("year", year)
            startActivity(intent)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Same as the My Anime screen: ◀ goes to a newer year, ▶ goes to an older year
        prevButton.setOnClickListener { moveYear(-1) }
        nextButton.setOnClickListener { moveYear(1) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        statusText.text = "Checking release years…"
        YearSync.sync(this) { ok ->
            if (isFinishing || isDestroyed) return@sync
            refresh()
            if (!ok) {
                statusText.text = statusText.text.toString() +
                    "\n(Couldn't reach AniList for some anime. Check your internet and reopen.)"
            }
        }
    }

    // step = 1 goes to an older year, step = -1 goes to a newer year.
    private fun moveYear(step: Int) {
        val years = Storage.yearsAvailable(this)
        val index = years.indexOf(year)
        val newIndex = (index + step).coerceIn(0, years.size - 1)
        year = years[newIndex]
        refresh()
    }

    private fun refresh() {
        val years = Storage.yearsAvailable(this)
        if (year !in years) year = Storage.currentYear()

        val index = years.indexOf(year)
        prevButton.isEnabled = index > 0
        nextButton.isEnabled = index < years.size - 1
        prevButton.alpha = if (prevButton.isEnabled) 1f else 0.3f
        nextButton.alpha = if (nextButton.isEnabled) 1f else 0.3f

        yearText.text = year.toString()
        val count = Storage.getAnimeForYear(this, year).size
        statusText.text = "$count anime in your list were released in $year"

        rows.clear()
        Storage.CATEGORIES.forEach { category ->
            rows.add(CategoryRow(category, Storage.getWinner(this, category, year)))
        }
        adapter.notifyDataSetChanged()
    }
}
