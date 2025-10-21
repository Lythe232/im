package com.lythe.media.ui.chats.base

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.lythe.media.ui.chats.data.model.CardModel
import com.lythe.media.ui.chats.view.card.impl.TextFeatureView
import com.lythe.media.ui.chats.view.card.impl.TextFeatureView.OnTextFeatureActionListener

class TextFeatureCardAdapter() :
    RecyclerView.Adapter<TextFeatureCardAdapter.CardViewHolder>() {
    private val TAG: String = "TextFeatureCardAdapter"
    private lateinit var cards: List<CardModel>

    constructor(cards: List<CardModel>) : this() {
        Log.d(TAG, "constructor");
        this.cards = cards
    }
    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textFeatureView: TextFeatureView = itemView as TextFeatureView
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        Log.d(TAG, "onCreateViewHolder");
        val view = TextFeatureView(parent.context)
        return CardViewHolder(view)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder");
        val cardModel = cards[position]
        cardModel.title?.let { holder.textFeatureView.setTitle(it) }
        cardModel.desc?.let { holder.textFeatureView.setDesc(it) }
        holder.textFeatureView.setTextFeatureActionListener(object : OnTextFeatureActionListener {
            override fun onItemClick() {
                Toast.makeText(holder.itemView.context, cardModel.target, Toast.LENGTH_SHORT).show()
            }
        })
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateCards(cards: List<CardModel>) {
        this.cards = cards;
        notifyDataSetChanged()
    }
}


