package com.lythe.media.ui.chats.data.model

import com.lythe.media.ui.chats.data.enums.FeatureType

sealed class FeatureModel (
    open val id: String,
    open val type: FeatureType,
    open val title: String,
    open val iconRes: Int? = null,
    open val isEnabled: Boolean = true,
    open val extraData: Map<String, Any>? = null
)
data class TextFeatureModel(
    override val id: String,
    override val title: String,
    override val iconRes: Int? = null,
    override val isEnabled: Boolean = true,
    val subtitle: String? = null,
    override val extraData: Map<String, Any>? = null
): FeatureModel(id, FeatureType.TEXT, title, iconRes, isEnabled, extraData)

data class SwitchFeatureModel(
    override val id: String,
    override val title: String,
    override val iconRes: Int? = null,
    override val isEnabled: Boolean = true,
    val isChecked: Boolean = false,
    val summary: String? = null,
    override val extraData: Map<String, Any>? = null
) : FeatureModel(id, FeatureType.SWITCH, title, iconRes, isEnabled, extraData)

data class ImageFeatureModel(
    override val id: String,
    override val title: String,
    override val iconRes: Int? = null,
    override val isEnabled: Boolean = true,
    val imageUrl: String? = null,                // 图片地址（网络、本地均可）
    val description: String? = null,             // 图片说明
    val isClickable: Boolean = false,            // 是否可点击
    override val extraData: Map<String, Any>? = null
) : FeatureModel(id, FeatureType.IMAGE, title, iconRes, isEnabled, extraData)


data class ArrowFeatureModel(
    override val id: String,
    override val title: String,
    override val iconRes: Int? = null,
    override val isEnabled: Boolean = true,
    val rightText: String? = null,
    override val extraData: Map<String, Any>? = null
) : FeatureModel(id, FeatureType.ARROW, title, iconRes, isEnabled, extraData)

data class DividerFeatureModel(
    override val id: String,
    val height: Int = 1,
    val color: Int = 0xFFEEEEEE.toInt(),
    override val extraData: Map<String, Any>? = null
) : FeatureModel(
    id = id,
    type = FeatureType.DIVIDER,
    title = "",
    extraData = extraData
)
