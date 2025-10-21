package com.lythe.media.ui.chats.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lythe.media.R
import com.lythe.media.ui.chats.data.enums.CardType
import com.lythe.media.ui.chats.data.model.CardModel
import com.lythe.media.ui.chats.data.model.FeatureModel
import com.lythe.media.ui.chats.data.model.ImageFeatureModel
import com.lythe.media.ui.chats.data.model.SwitchFeatureModel
import com.lythe.media.ui.chats.data.model.TextFeatureModel

class MixFeatureCardViewModel {
    private val _featureModel = MutableLiveData<List<FeatureModel>>()
    val featureModel: LiveData<List<FeatureModel>> = _featureModel

    fun fetchData() {
        val featureModels: MutableList<FeatureModel> = ArrayList()
        featureModels.add(ImageFeatureModel(
            id = "avatar_feature",
            title = "头像",
            isEnabled = true,
            imageUrl = "https://example.com/avatar.jpg",
            description = "点击更换头像",
            isClickable = true,
            extraData = mapOf("type" to "profile_avatar")
        ))

        featureModels.add(
            TextFeatureModel(
                id = "text1",
                title = "用户名",
                subtitle = "lin"
            )
        )
        featureModels.add(
            TextFeatureModel(
                id = "text2",
                title = "邮箱",
                subtitle = "lin@example.com"
            )
        )
        featureModels.add(
            TextFeatureModel(
                id = "text3",
                title = "个性签名",
                subtitle = "代码即人生"
            )
        )

        featureModels.add(
            SwitchFeatureModel(
                id = "switch1",
                title = "开启通知",
                isChecked = true,
                summary = "是否接收消息通知"
            )
        )
        featureModels.add(
            SwitchFeatureModel(
                id = "switch2",
                title = "夜间模式",
                isChecked = false,
                summary = "启用深色模式"
            )
        )
        featureModels.add(
            SwitchFeatureModel(
                id = "switch3",
                title = "位置共享",
                isChecked = true,
                summary = "与好友共享实时位置"
            )
        )
        _featureModel.value = featureModels
    }
}