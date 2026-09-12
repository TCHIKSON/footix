package com.footix.tv.ui.home

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import coil.dispose
import coil.load
import com.footix.tv.R
import com.footix.tv.domain.displaySubtitle
import com.footix.tv.domain.displayTitle
import com.footix.tv.domain.model.Match

class MatchCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val card = ImageCardView(context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(
                resources.getDimensionPixelSize(R.dimen.card_width),
                resources.getDimensionPixelSize(R.dimen.card_height)
            )
            setInfoAreaBackgroundColor(ContextCompat.getColor(context, R.color.card_info))
            mainImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val match = item as Match
        val card = viewHolder.view as ImageCardView

        card.titleText = match.displayTitle
        card.contentText = match.displaySubtitle
        card.badgeImage = if (match.isLive) {
            ContextCompat.getDrawable(card.context, R.drawable.ic_live)
        } else {
            null
        }

        card.mainImageView.alpha = 1f
        card.mainImageView.load(match.posterUrl.ifBlank { null }) {
            crossfade(true)
            placeholder(R.drawable.poster_placeholder)
            error(R.drawable.poster_placeholder)
            fallback(R.drawable.poster_placeholder)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.mainImageView.dispose()
        card.mainImageView.setImageDrawable(null)
        card.badgeImage = null
    }
}
