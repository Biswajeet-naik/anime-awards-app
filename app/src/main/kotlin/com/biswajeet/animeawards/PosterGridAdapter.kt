package com.biswajeet.animeawards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PosterGridAdapter(
    private val items: List<Anime>,
    private val onRemove: (Anime) -> Unit
) : RecyclerView.Adapter<PosterGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.poster)
        val title: TextView = view.findViewById(R.id.animeTitle)
        val genres: TextView = view.findViewById(R.id.animeGenres)
        val remove: TextView = view.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_poster_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val anime = items[position]
        holder.title.text = anime.title
        holder.genres.text = anime.genres.joinToString(" • ")
        Glide.with(holder.poster).load(anime.coverUrl).into(holder.poster)
        holder.remove.setOnClickListener { onRemove(anime) }
    }

    override fun getItemCount() = items.size
}
