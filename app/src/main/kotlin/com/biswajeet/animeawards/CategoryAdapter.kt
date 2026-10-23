package com.biswajeet.animeawards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class CategoryRow(val name: String, val winner: Anime?)

class CategoryAdapter(
    private val items: List<CategoryRow>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.categoryName)
        val winnerText: TextView = view.findViewById(R.id.winnerText)
        val winnerPoster: ImageView = view.findViewById(R.id.winnerPoster)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = items[position]
        val context = holder.itemView.context
        holder.name.text = row.name

        val winner = row.winner
        if (winner != null) {
            holder.winnerText.text = "🏆 ${winner.title}"
            holder.winnerText.setTextColor(ContextCompat.getColor(context, R.color.gold))
            holder.winnerPoster.visibility = View.VISIBLE
            Glide.with(holder.winnerPoster).load(winner.coverUrl).into(holder.winnerPoster)
        } else {
            holder.winnerText.text = "No winner yet"
            holder.winnerText.setTextColor(ContextCompat.getColor(context, R.color.glass_muted))
            holder.winnerPoster.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(row.name) }
    }

    override fun getItemCount() = items.size
}
