package com.biswajeet.animeawards

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        listView = findViewById(R.id.categoryList)
        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, CategoryDetailActivity::class.java)
            intent.putExtra("category", Storage.CATEGORIES[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val rows = Storage.CATEGORIES.map { category ->
            val winner = Storage.getWinner(this, category)
            if (winner != null) "$category\n🏆 $winner" else category
        }
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }
}
