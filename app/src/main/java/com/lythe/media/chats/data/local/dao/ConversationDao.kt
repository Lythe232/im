package com.lythe.media.chats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lythe.media.chats.data.entity.ConversationEntity

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertConversation(conversationEntity: ConversationEntity): Long

    @Update
    fun updateConversation(conversationEntity: ConversationEntity): Int

    @Query("SELECT * FROM conversations ORDER BY lastMessageTime DESC")
    fun getAllConversations(): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE conversationId = :conversationId LIMIT 1")
    fun getConversationSync(conversationId: String): ConversationEntity?

    @Query("UPDATE conversations SET unreadCount = unreadCount + :delta, lastMessage = :lastMessage, lastMessageTime = :lastTs WHERE conversationId = :conversationId")
    fun updateConversationOnNewMessage(conversationId: String, delta: Int, lastMessage: String?, lastTs: Long)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
    fun resetUnread(conversationId: String): Int

    @Query("DELETE FROM conversations WHERE conversationId = :conversationId")
    fun deleteConversation(conversationId: String)
}