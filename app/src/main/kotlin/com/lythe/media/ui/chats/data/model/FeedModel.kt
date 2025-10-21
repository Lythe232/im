package com.lythe.media.ui.chats.data.model

data class FeedModel(
    val feedId: String,
    val author: UserModel,
    val content: String,
    val imageUrls: List<String>?,
    val createTime: String,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val isLiked: Boolean,
    val comments: List<CommentModel>?
)
