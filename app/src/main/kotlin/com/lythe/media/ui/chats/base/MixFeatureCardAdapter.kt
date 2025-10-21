package com.lythe.media.ui.chats.base


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lythe.media.ui.chats.data.model.FeatureModel
import com.lythe.media.ui.chats.data.model.ImageFeatureModel
import com.lythe.media.ui.chats.data.model.SwitchFeatureModel
import com.lythe.media.ui.chats.data.model.TextFeatureModel
import com.lythe.media.ui.chats.view.card.impl.ImageFeatureView
import com.lythe.media.ui.chats.view.card.impl.SwitchFeatureView
import com.lythe.media.ui.chats.view.card.impl.TextFeatureView

class MixFeatureCardAdapter() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var cards: List<FeatureModel>

    constructor(cards: List<FeatureModel>): this() {
        this.cards = cards
    }

    companion object {
        private const val TYPE_DEFAULT = -1
        private const val TYPE_TEXT = 0
        private const val TYPE_SWITCH = 1
        private const val TYPE_IMAGE = 2
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_TEXT -> {
                TextFeatureViewHolder(TextFeatureView(parent.context))
            }
            TYPE_SWITCH -> {
                SwitchFeatureViewHolder(SwitchFeatureView(parent.context))
            }
            TYPE_IMAGE -> {
                ImageFeatureViewHolder(ImageFeatureView(parent.context))
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val card = cards[position]) {
            is TextFeatureModel -> (holder as TextFeatureViewHolder).bind(card)
            is SwitchFeatureModel -> (holder as SwitchFeatureViewHolder).bind(card)
            is ImageFeatureModel -> (holder as ImageFeatureViewHolder).bind(card)
            else -> TYPE_DEFAULT
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(cards[position]) {
            is TextFeatureModel -> TYPE_TEXT
            is SwitchFeatureModel-> TYPE_SWITCH
            is ImageFeatureModel -> TYPE_IMAGE
            else -> TYPE_DEFAULT
        }
    }
    class TextFeatureViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val textFeatureView = itemView as TextFeatureView;

        fun bind(cardModel: TextFeatureModel) {
            textFeatureView.setTitle(cardModel.title)
            cardModel.subtitle?.let { textFeatureView.setDesc(it) }
        }
    }
    class SwitchFeatureViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val switchFeatureView = itemView as SwitchFeatureView

        fun bind(cardModel: SwitchFeatureModel) {
            switchFeatureView.setSwitchState(cardModel.isChecked)
            switchFeatureView.setTitle(cardModel.title)
        }
    }
    class ImageFeatureViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val imageFeatureView = itemView as ImageFeatureView

        fun bind(cardModel: ImageFeatureModel) {
            imageFeatureView.setTitle(cardModel.title)
            cardModel.description?.let { imageFeatureView.setDesc(it) }
        }
    }
    fun updateCards(cards: List<FeatureModel>)  {
        this.cards = cards
    }
}