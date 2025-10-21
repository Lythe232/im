package com.lythe.media.chats.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lythe.media.chats.data.entity.MessageEntity

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(message: MessageEntity): Long
    @Update
    fun update(message: MessageEntity)
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId")
    fun getMessages(conversationId: String): List<MessageEntity>
    @Query("SELECT * FROM messages " +
            "WHERE conversationId = :conversationId " +
            "AND timestamp < :earliestTimestamp " +
            "ORDER BY timestamp DESC LIMIT :limit")
    fun getMessagesPaged(conversationId: String, limit: Int, earliestTimestamp: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE  status = 0")
    fun getPendingMessages(): List<MessageEntity>;
//
//    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY server_time ASC")
//    fun getMessagesBySession(sessionId: String): Flow<List<MessageEntity>>
//
//    @Query("UPDATE messages SET status = :status WHERE msg_id = :msgId")
//    suspend fun updateStatus(msgId: String, status: Int)
//
//    @Query("UPDATE messages SET is_read = 1 WHERE session_id = :sessionId AND is_self = 0")
//    suspend fun markAsRead(sessionId: String)
//
//    @Query("SELECT COUNT(*) FROM messages WHERE session_id = :sessionId AND is_read = 0 AND is_self = 0")
//    suspend fun getUnreadCount(sessionId: String): Int
}