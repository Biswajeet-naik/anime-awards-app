package com.biswajeet.animeawards

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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

    // How many of the next bound rows should slide in (set by playEntrance()).
    private var pendingEntrance = 0

    // Call right before notifyDataSetChanged() to play the slide-in once.
    fun playEntrance() {
        pendingEntrance = items.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.categoryNumber)
        val name: TextView = view.findViewById(R.id.categoryName)
        val winnerLabel: TextView = view.findViewById(R.id.winnerLabel)
        val winnerText: TextView = view.findViewById(R.id.winnerText)
        val winnerPoster: ImageView = view.findViewById(R.id.winnerPoster)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = items[position]
        val context = holder.itemView.context
        val lavender = ContextCompat.getColor(context, R.color.ed_lavender)
        val muted = ContextCompat.getColor(context, R.color.ed_muted)
        val dim = ContextCompat.getColor(context, R.color.ed_dim)

        holder.number.text = String.format("%02d", position + 1)
        holder.name.text = row.name

        val winner = row.winner
        if (winner != null) {
            holder.number.setTextColor(lavender)
            holder.winnerLabel.text = "Winner"
            holder.winnerLabel.setTextColor(lavender)
            holder.winnerText.text = winner.title
            holder.winnerText.visibility = View.VISIBLE
            holder.winnerPoster.setBackgroundColor(ContextCompat.getColor(context, R.color.ed_slot))
            holder.winnerPoster.contentDescription = "Poster: ${winner.title}"
            Glide.with(holder.winnerPoster).load(winner.coverUrl).into(holder.winnerPoster)
        } else {
            holder.number.setTextColor(dim)
            holder.winnerLabel.text = "No winner yet"
            holder.winnerLabel.setTextColor(muted)
            holder.winnerText.visibility = View.GONE
            Glide.with(holder.winnerPoster).clear(holder.winnerPoster)
            holder.winnerPoster.setImageDrawable(null)
            holder.winnerPoster.setBackgroundResource(R.drawable.bg_ed_slot)
            holder.winnerPoster.contentDescription = null
        }

        holder.itemView.setOnClickListener { onClick(row.name) }

        // Small press feedback: the row dips slightly while your finger is down.
        holder.itemView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            }
            false
        }

        // Slide-in when the screen opens or the year changes.
        if (pendingEntrance > 0) {
            pendingEntrance--
            val v = holder.itemView
            v.animate().cancel()
            v.alpha = 0f
            v.translationY = 24f * context.resources.displayMetrics.density
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(position.coerceAtMost(9) * 35L)
                .setDuration(260)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()
        }
    }

    override fun getItemCount() = items.size
}
