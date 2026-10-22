package com.biswajeet.animeawards

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private val animeList = mutableListOf<String>()
    private lateinit var adapter: AnimeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadAnime()

        val input: EditText = findViewById(R.id.animeInput)
        val addButton: Button = findViewById(R.id.addButton)
        val recycler: RecyclerView = findViewById(R.id.animeRecycler)

        adapter = AnimeAdapter(animeList) { position ->
            animeList.removeAt(position)
            adapter.notifyItemRemoved(position)
            adapter.notifyItemRangeChanged(position, animeList.size - position)
            saveAnime()
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        addButton.setOnClickListener {
            val title = input.text.toString().trim()
            if (title.isNotEmpty()) {
                animeList.add(title)
                adapter.notifyItemInserted(animeList.size - 1)
                recycler.scrollToPosition(animeList.size - 1)
                input.text.clear()
                saveAnime()
            }
        }
    }

    private fun saveAnime() {
        val array = JSONArray()
        animeList.forEach { array.put(it) }
        getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("anime_list", array.toString())
            .apply()
    }

    private fun loadAnime() {
        val json = getSharedPreferences("anime_prefs", Context.MODE_PRIVATE)
            .getString("anime_list", null) ?: return
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            animeList.add(array.getString(i))
        }
    }
}

class AnimeAdapter(
    private val items: List<String>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<AnimeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.animeTitle)
        val delete: TextView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items[position]
        holder.delete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(pos)
        }
    }

    override fun getItemCount() = items.size
}