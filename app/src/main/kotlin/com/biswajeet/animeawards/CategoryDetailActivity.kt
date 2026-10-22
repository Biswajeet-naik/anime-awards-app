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
    private var animeList: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        category = intent.getStringExtra("category") ?: "Award"
        findViewById<TextView>(R.id.categoryTitle).text = category

        listView = findViewById(R.id.animeChoiceList)
        animeList = Storage.getAnime(this)

        val emptyText: TextView = findViewById(R.id.emptyText)
        emptyText.visibility = if (animeList.isEmpty()) View.VISIBLE else View.GONE

        listView.setOnItemClickListener { _, _, position, _ ->
            showOptions(animeList[position])
        }

        refresh()
    }

    private fun refresh() {
        val nominees = Storage.getNominees(this, category)
        val winner = Storage.getWinner(this, category)
        val rows = animeList.map { title ->
            when {
                title == winner -> "🏆  $title  (Winner)"
                title in nominees -> "⭐  $title  (Nominee)"
                else -> title
            }
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun showOptions(title: String) {
        val nominees = Storage.getNominees(this, category)
        val winner = Storage.getWinner(this, category)
        val isNominee = title in nominees
        val isWinner = title == winner

        val options = arrayOf(
            if (isNominee) "Remove nomination" else "Nominate",
            if (isWinner) "Remove winner status" else "Make winner"
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (isNominee) {
                            nominees.remove(title)
                            if (isWinner) Storage.setWinner(this, category, null)
                        } else {
                            nominees.add(title)
                        }
                        Storage.saveNominees(this, category, nominees)
                    }
                    1 -> {
                        if (isWinner) {
                            Storage.setWinner(this, category, null)
                        } else {
                            if (!isNominee) {
                                nominees.add(title)
                                Storage.saveNominees(this, category, nominees)
                            }
                            Storage.setWinner(this, category, title)
                        }
                    }
                }
                refresh()
            }
            .show()
    }
}
