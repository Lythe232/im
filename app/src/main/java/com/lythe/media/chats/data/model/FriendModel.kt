package com.lythe.media.chats.data.model

import android.text.TextUtils

data class FriendModel(
    var id: String,
    var username: String?,
    var status: Int,
    var signature: String?,
    var avatar: String?,
    var remark: String?,
    var relationStatus: Int = 0,
    var createTime: Long?,
    var updateTime: Long?
) {

    fun getName(): String? = username

    fun getLetter(): String {
        return if (!TextUtils.isEmpty(this.username)) {
            username!!.substring(0, 1).uppercase()
        } else {
            "#"
        }
    }
}