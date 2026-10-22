package com.biswajeet.animeawards

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val animeList = mutableListOf<Anime>()
    private lateinit var adapter: AnimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler: RecyclerView = findViewById(R.id.animeRecycler)
        val searchButton: Button = findViewById(R.id.searchButton)
        val awardsButton: Button = findViewById(R.id.awardsButton)

        adapter = AnimeAdapter(animeList, "✕") { anime ->
            val index = animeList.indexOfFirst { it.id == anime.id }
            if (index >= 0) {
                animeList.removeAt(index)
                adapter.notifyItemRemoved(index)
                Storage.saveAnime(this, animeList)
            }
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        searchButton.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        awardsButton.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        animeList.clear()
        animeList.addAll(Storage.getAnime(this))
        adapter.notifyDataSetChanged()
    }
}
