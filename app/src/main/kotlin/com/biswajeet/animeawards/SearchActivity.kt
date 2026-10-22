package com.biswajeet.animeawards

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {

    private val results = mutableListOf<Anime>()
    private lateinit var adapter: AnimeAdapter
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val input: EditText = findViewById(R.id.searchInput)
        val searchButton: Button = findViewById(R.id.searchGoButton)
        val recycler: RecyclerView = findViewById(R.id.resultsRecycler)
        statusText = findViewById(R.id.statusText)

        adapter = AnimeAdapter(results, "+") { anime ->
            val added = Storage.addAnime(this, anime)
            val message = if (added) "Added ${anime.title}" else "Already in your list"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        searchButton.setOnClickListener {
            doSearch(input.text.toString().trim())
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch(input.text.toString().trim())
                true
            } else {
                false
            }
        }
    }

    private fun doSearch(query: String) {
        if (query.isEmpty()) return
        statusText.text = "Searching..."
        statusText.visibility = View.VISIBLE

        AniListApi.search(query) { found ->
            if (found == null) {
                statusText.text = "Couldn't reach AniList. Check your internet and try again."
            } else {
                results.clear()
                results.addAll(found)
                adapter.notifyDataSetChanged()
                if (found.isEmpty()) {
                    statusText.text = "No results."
                } else {
                    statusText.visibility = View.GONE
                }
            }
        }
    }
}
