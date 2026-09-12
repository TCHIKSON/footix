package com.footix.tv.ui.home

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.footix.tv.R

/** Carte affichee quand le catalogue est vide ou en erreur. */
data class MessageCard(
    val message: String,
    val actionLabel: String
)

class MessageCardPresenter : Presenter() {

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
            mainImage = ContextCompat.getDrawable(context, R.drawable.poster_placeholder)
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val card = viewHolder.view as ImageCardView
        val messageCard = item as MessageCard
        card.titleText = messageCard.message
        card.contentText = messageCard.actionLabel
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) = Unit
}
