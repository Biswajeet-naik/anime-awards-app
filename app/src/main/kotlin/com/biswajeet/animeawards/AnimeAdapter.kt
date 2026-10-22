package com.biswajeet.animeawards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AnimeAdapter(
    private val items: List<Anime>,
    private val actionLabel: String,
    private val onAction: (Anime) -> Unit
) : RecyclerView.Adapter<AnimeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster)
        val title: TextView = view.findViewById(R.id.animeTitle)
        val genres: TextView = view.findViewById(R.id.animeGenres)
        val action: TextView = view.findViewById(R.id.actionButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val anime = items[position]
        holder.title.text = anime.title
        holder.genres.text = anime.genres.joinToString(" • ")
        holder.action.text = actionLabel
        Glide.with(holder.poster).load(anime.coverUrl).into(holder.poster)
        holder.action.setOnClickListener { onAction(anime) }
    }

    override fun getItemCount() = items.size
}
