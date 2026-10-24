package com.biswajeet.animeawards

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

// A square-cornered image, like a printed photograph.
// If its height is wrap_content, it makes itself poster-shaped (taller than wide).
class PosterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER_CROP
        setBackgroundColor(0x22FFFFFF)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        if (heightMode != MeasureSpec.EXACTLY && widthMode == MeasureSpec.EXACTLY) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = (width * 1.5f).toInt()
            super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
