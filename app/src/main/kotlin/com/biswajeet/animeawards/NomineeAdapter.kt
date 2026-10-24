package com.biswajeet.animeawards

import android.util.TypedValue
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

// Everything that can appear in the Category screen list
sealed class CatItem {
    data class Header(
        val year: Int,
        val number: Int,
        val title: String,
        val nominees: Int,
        val total: Int
    ) : CatItem()

    data class Winner(val anime: Anime?) : CatItem()
    data class Section(val label: String, val count: Int) : CatItem()
    data class Note(val text: String) : CatItem()
    data class Tile(val row: NomineeRow, val index: Int) : CatItem()
}

class NomineeAdapter(
    private val items: List<CatItem>,
    private val onAnime: (Anime) -> Unit,
    private val onRemoveWinner: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private class Holder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is CatItem.Header -> 0
        is CatItem.Winner -> 1
        is CatItem.Section -> 2
        is CatItem.Note -> 3
        is CatItem.Tile -> 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = when (viewType) {
            0 -> R.layout.item_cat_header
            1 -> R.layout.item_cat_winner
            2, 3 -> R.layout.item_cat_section
            else -> R.layout.item_cat_tile
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val v = holder.itemView
        val ctx = v.context

        when (val item = items[position]) {
            is CatItem.Header -> {
                v.findViewById<TextView>(R.id.catKicker).text = "Awards / ${item.year}"
                val number = v.findViewById<TextView>(R.id.catNumber)
                if (item.number > 0) {
                    number.visibility = View.VISIBLE
                    number.text = "%02d".format(item.number)
                } else {
                    number.visibility = View.GONE
                }
                v.findViewById<TextView>(R.id.catTitle).text = item.title
                v.findViewById<TextView>(R.id.metaYear).text = "Year: ${item.year}"
                v.findViewById<TextView>(R.id.metaNominees).text = "Nominees: %02d".format(item.nominees)
                v.findViewById<TextView>(R.id.metaAnime).text = "Anime: %02d".format(item.total)
            }

            is CatItem.Winner -> {
                val art = v.findViewById<ImageView>(R.id.winnerArt)
                val shade = v.findViewById<View>(R.id.winnerShade)
                val label = v.findViewById<TextView>(R.id.winnerLabel)
                val title = v.findViewById<TextView>(R.id.winnerTitle)
                val remove = v.findViewById<TextView>(R.id.winnerRemove)
                val anime = item.anime

                if (anime != null) {
                    v.setBackgroundResource(R.drawable.bg_cat_winner)
                    art.visibility = View.VISIBLE
                    shade.visibility = View.VISIBLE
                    remove.visibility = View.VISIBLE
                    label.text = "Crowned winner"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.ed_lavender))
                    title.text = anime.title
                    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    title.setTextColor(ContextCompat.getColor(ctx, R.color.ed_ivory))
                    Glide.with(art).load(anime.coverUrl).into(art)
                    v.contentDescription = "Crowned winner: ${anime.title}"
                    v.setOnClickListener { onAnime(anime) }
                    remove.setOnClickListener { onRemoveWinner() }
                } else {
                    v.setBackgroundResource(R.drawable.bg_cat_empty)
                    Glide.with(art).clear(art)
                    art.visibility = View.GONE
                    shade.visibility = View.GONE
                    remove.visibility = View.GONE
                    label.text = "No winner yet"
                    label.setTextColor(ContextCompat.getColor(ctx, R.color.ed_muted))
                    title.text = "Tap an anime below"
                    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    title.setTextColor(ContextCompat.getColor(ctx, R.color.ed_dim))
                    v.contentDescription = "No winner yet"
                    v.setOnClickListener(null)
                    v.isClickable = false
                }
            }

            is CatItem.Section -> {
                val label = v.findViewById<TextView>(R.id.secLabel)
                val count = v.findViewById<TextView>(R.id.secCount)
                label.text = item.label
                label.setTextColor(ContextCompat.getColor(ctx, R.color.ed_ivory))
                label.isAllCaps = true
                label.letterSpacing = 0.16f
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                count.visibility = View.VISIBLE
                count.text = "%02d".format(item.count)
                v.findViewById<View>(R.id.secLine).visibility = View.VISIBLE
            }

            is CatItem.Note -> {
                val label = v.findViewById<TextView>(R.id.secLabel)
                label.text = item.text
                label.setTextColor(ContextCompat.getColor(ctx, R.color.ed_muted))
                label.isAllCaps = false
                label.letterSpacing = 0.02f
                label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                v.findViewById<TextView>(R.id.secCount).visibility = View.GONE
                v.findViewById<View>(R.id.secLine).visibility = View.GONE
            }

            is CatItem.Tile -> {
                val row = item.row
                val isNominee = row.rank == 1
                val poster = v.findViewById<ImageView>(R.id.tilePoster)
                val mark = v.findViewById<TextView>(R.id.tileMark)
                val label = v.findViewById<TextView>(R.id.tileLabel)
                val title = v.findViewById<TextView>(R.id.tileTitle)

                v.setBackgroundResource(
                    if (isNominee) R.drawable.bg_cat_tile_nominee else R.drawable.bg_cat_tile
                )
                mark.visibility = if (isNominee) View.VISIBLE else View.GONE

                label.text = if (isNominee) "[ Nominee %02d ]".format(item.index) else row.label
                label.setTextColor(
                    ContextCompat.getColor(
                        ctx,
                        if (isNominee) R.color.ed_lavender else R.color.ed_muted
                    )
                )
                title.text = row.anime.title
                Glide.with(poster).load(row.anime.coverUrl).into(poster)

                v.contentDescription =
                    if (isNominee) "${row.anime.title}, nominee" else row.anime.title
                v.setOnClickListener { onAnime(row.anime) }
            }
        }
    }

    override fun getItemCount() = items.size
}
