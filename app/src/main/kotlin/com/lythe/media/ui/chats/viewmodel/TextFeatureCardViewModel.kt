package com.lythe.media.ui.chats.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lythe.media.ui.chats.data.enums.CardType
import com.lythe.media.ui.chats.data.model.CardModel

class TextFeatureCardViewModel {
    private val _cardModel = MutableLiveData<List<CardModel>>()
    val cardModel: LiveData<List<CardModel>> = _cardModel
    fun fetchData() {
        val cards: MutableList<CardModel> = ArrayList()
        cards.add(CardModel("", CardType.CUSTOM, "通知", "", target = "target1"))
        _cardModel.value = cards
    }
}