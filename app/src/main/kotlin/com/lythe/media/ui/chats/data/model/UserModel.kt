package com.lythe.media.ui.chats.data.model

data class UserModel(
    val userId: String,
    val avatarUrl: String,
    val nickname: String,
    val isVip: Boolean = false
)

