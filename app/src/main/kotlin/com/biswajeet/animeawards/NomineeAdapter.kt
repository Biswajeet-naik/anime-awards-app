package com.biswajeet.animeawards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// rank: 0 = winner, 1 = nominee, 2 = genre match, 3 = everything else
data class NomineeRow(val anime: Anime, val rank: Int, val label: String)

class NomineeAdapter(
    private val items: List<NomineeRow>,
    private val onClick: (Anime) -> Unit
) : RecyclerView.Adapter<NomineeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val poster: ImageView = view.findViewById(R.id.nomineePoster)
        val title: TextView = view.findViewById(R.id.nomineeTitle)
        val label: TextView = view.findViewById(R.id.nomineeLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nominee, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = items[position]
        val context = holder.itemView.context
        val density = context.resources.displayMetrics.density
        val isWinner = row.rank == 0

        holder.title.text = row.anime.title

        // The winner gets a bigger poster and a gold card
        val params = holder.poster.layoutParams
        params.width = ((if (isWinner) 96 else 64) * density).toInt()
        params.height = ((if (isWinner) 138 else 92) * density).toInt()
        holder.poster.layoutParams = params

        holder.itemView.setBackgroundResource(
            if (isWinner) R.drawable.bg_winner_card else R.drawable.bg_glass_card
        )
        holder.itemView.alpha = if (row.rank >= 2) 0.85f else 1f

        if (row.label.isEmpty()) {
            holder.label.visibility = View.GONE
        } else {
            holder.label.visibility = View.VISIBLE
            holder.label.text = row.label
            holder.label.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isWinner) R.color.gold else R.color.glass_muted
                )
            )
        }

        Glide.with(holder.poster).load(row.anime.coverUrl).into(holder.poster)
        holder.itemView.setOnClickListener { onClick(row.anime) }
    }

    override fun getItemCount() = items.size
}
