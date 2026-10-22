package com.biswajeet.animeawards

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity() {

    private val results = mutableListOf<Anime>()
    private lateinit var adapter: AnimeAdapter
    private lateinit var statusText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var pendingSearch: Runnable? = null
    private var latestRequest = 0

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

        // Suggestions while typing: wait for a short pause, then search.
        input.doAfterTextChanged { text ->
            scheduleSearch(text?.toString()?.trim() ?: "")
        }

        // The Search button and keyboard search key search right away.
        searchButton.setOnClickListener {
            searchNow(input.text.toString().trim())
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchNow(input.text.toString().trim())
                true
            } else {
                false
            }
        }
    }

    private fun cancelPending() {
        pendingSearch?.let { handler.removeCallbacks(it) }
        pendingSearch = null
    }

    private fun scheduleSearch(query: String) {
        cancelPending()

        if (query.length < 2) {
            // Too short: clear everything and ignore any search still loading.
            latestRequest++
            results.clear()
            adapter.notifyDataSetChanged()
            statusText.visibility = View.GONE
            return
        }

        val runnable = Runnable { doSearch(query) }
        pendingSearch = runnable
        handler.postDelayed(runnable, 500)
    }

    private fun searchNow(query: String) {
        cancelPending()
        if (query.isNotEmpty()) doSearch(query)
    }

    private fun doSearch(query: String) {
        val requestId = ++latestRequest
        statusText.text = "Searching..."
        statusText.visibility = View.VISIBLE

        AniListApi.search(query) { found ->
            // A newer search started while this one was loading: drop this answer.
            if (requestId != latestRequest) return@search

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

    override fun onDestroy() {
        super.onDestroy()
        cancelPending()
    }
}
