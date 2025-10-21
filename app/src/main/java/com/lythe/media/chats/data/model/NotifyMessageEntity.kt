package com.lythe.media.chats.data.model

data class NotifyMessageEntity (
    private val notifyId : String,
    private val title : String,
    private val content : String,
    private val createTimestamp : String
)