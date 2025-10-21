package com.lythe.media.chats.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lythe.media.protobuf.ImConversationType
import java.io.Serializable

@Entity(tableName = "conversations")
data class ConversationEntity (
    @PrimaryKey
    val conversationId: String,
    val conversationType: ImConversationType = ImConversationType.UNRECOGNIZED,
    val conversationName: String,
    var lastMessage: String?,
    var lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
): Serializable
