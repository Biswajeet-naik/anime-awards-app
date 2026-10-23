package com.biswajeet.animeawards

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var yearText: TextView
    private lateinit var statusText: TextView
    private var year = Storage.currentYear()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        listView = findViewById(R.id.categoryList)
        yearText = findViewById(R.id.yearText)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.prevYear).setOnClickListener { moveYear(1) }
        findViewById<Button>(R.id.nextYear).setOnClickListener { moveYear(-1) }

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, CategoryDetailActivity::class.java)
            intent.putExtra("category", Storage.CATEGORIES[position])
            intent.putExtra("year", year)
            startActivity(intent)
        }
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

        yearText.text = year.toString()
        val count = Storage.getAnimeForYear(this, year).size
        statusText.text = "$count anime in your list were released in $year"

        val rows = Storage.CATEGORIES.map { category ->
            val winner = Storage.getWinner(this, category, year)
            if (winner != null) "$category\n🏆 ${winner.title}" else category
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }
}
