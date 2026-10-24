package com.biswajeet.animeawards

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CategoriesActivity : AppCompatActivity() {

    private lateinit var adapter: CategoryAdapter
    private lateinit var yearText: TextView
    private lateinit var statusText: TextView
    private lateinit var countText: TextView
    private lateinit var countLabel: TextView
    private lateinit var winnersText: TextView
    private lateinit var nominationsText: TextView
    private lateinit var categoriesRange: TextView
    private lateinit var winnersProgress: ProgressBar
    private lateinit var prevButton: Button
    private lateinit var nextButton: Button
    private val rows = mutableListOf<CategoryRow>()
    private var year = Storage.currentYear()
    private var firstShow = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This screen only: charcoal window and system bars.
        val charcoal = ContextCompat.getColor(this, R.color.ed_charcoal)
        window.setBackgroundDrawable(ColorDrawable(charcoal))
        window.statusBarColor = charcoal
        window.navigationBarColor = charcoal

        setContentView(R.layout.activity_categories)

        val recycler: RecyclerView = findViewById(R.id.categoryList)
        yearText = findViewById(R.id.yearText)
        statusText = findViewById(R.id.statusText)
        countText = findViewById(R.id.countText)
        countLabel = findViewById(R.id.countLabel)
        winnersText = findViewById(R.id.winnersText)
        nominationsText = findViewById(R.id.nominationsText)
        categoriesRange = findViewById(R.id.categoriesRange)
        winnersProgress = findViewById(R.id.winnersProgress)
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

        // Same as the My Anime screen: ← goes to a newer year, → goes to an older year
        prevButton.setOnClickListener { moveYear(-1) }
        nextButton.setOnClickListener { moveYear(1) }

        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        refresh(animate = firstShow)
        firstShow = false
        setStatus("Checking release years…")
        YearSync.sync(this) { ok ->
            if (isFinishing || isDestroyed) return@sync
            refresh()
            if (ok) {
                setStatus(null)
            } else {
                setStatus("Couldn't reach AniList for some anime.\nCheck your internet and reopen.")
            }
        }
    }

    private fun setStatus(message: String?) {
        statusText.text = message ?: ""
        statusText.visibility = if (message == null) View.GONE else View.VISIBLE
    }

    // step = 1 goes to an older year, step = -1 goes to a newer year.
    private fun moveYear(step: Int) {
        val years = Storage.yearsAvailable(this)
        val index = years.indexOf(year)
        val newIndex = (index + step).coerceIn(0, years.size - 1)
        if (years[newIndex] == year) return
        year = years[newIndex]
        refresh(animate = true)
    }

    private fun refresh(animate: Boolean = false) {
        val years = Storage.yearsAvailable(this)
        if (year !in years) year = Storage.currentYear()

        val index = years.indexOf(year)
        prevButton.isEnabled = index > 0
        nextButton.isEnabled = index < years.size - 1
        prevButton.alpha = if (prevButton.isEnabled) 1f else 0.3f
        nextButton.alpha = if (nextButton.isEnabled) 1f else 0.3f

        yearText.text = year.toString()
        countText.text = Storage.getAnimeForYear(this, year).size.toString()
        countLabel.text = "anime from your list\nreleased in $year"

        rows.clear()
        var winners = 0
        var nominations = 0
        Storage.CATEGORIES.forEach { category ->
            val winner = Storage.getWinner(this, category, year)
            if (winner != null) winners++
            nominations += Storage.getNominees(this, category, year).size
            rows.add(CategoryRow(category, winner))
        }
        val total = Storage.CATEGORIES.size

        val winnersLabel = "$winners / $total"
        val spannable = SpannableString(winnersLabel)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.ed_ink_muted)),
            winners.toString().length,
            winnersLabel.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        winnersText.text = spannable
        nominationsText.text = nominations.toString()
        categoriesRange.text = String.format("01 — %02d", total)
        winnersProgress.max = total
        winnersProgress.setProgress(winners, true)

        if (animate) adapter.playEntrance()
        adapter.notifyDataSetChanged()
    }
}
