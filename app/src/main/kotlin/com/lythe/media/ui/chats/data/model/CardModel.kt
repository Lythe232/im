package com.lythe.media.ui.chats.data.model

import com.lythe.media.ui.chats.data.enums.CardType

data class CardModel(
    val id: String,
    val type: CardType,
    val title: String? = null,
    val desc: String? = null,
    val features: MutableList<FeatureModel> = mutableListOf(),
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    val cornerRadius: Float = 8f,
    val padding: Int = 16,
    val target: String? = null,
    val targetParams: Map<String, String>? = null
) {
    fun addFeature(feature: FeatureModel) {
        features.add(feature)
    }
    fun addFeature(newFeatures: List<FeatureModel>) {
        features.addAll(newFeatures)
    }
}
