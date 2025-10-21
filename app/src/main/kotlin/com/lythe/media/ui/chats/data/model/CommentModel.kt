package com.lythe.media.ui.chats.data.model

data class CommentModel(
    val commentId: String,
    val user: UserModel,
    val content: String,
    val createTime: String,
    val replyTo: CommentModel? = null
)
