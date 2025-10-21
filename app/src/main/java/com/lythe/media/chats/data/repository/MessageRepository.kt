package com.lythe.media.chats.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.lythe.media.chats.data.entity.ConversationEntity
import com.lythe.media.chats.data.local.dao.MessageDao
import com.lythe.media.chats.data.local.database.AppDatabase
import com.lythe.media.chats.data.entity.MessageEntity
import com.lythe.media.chats.data.local.dao.ConversationDao
import com.lythe.media.chats.data.local.dao.FriendDao
import com.lythe.media.chats.data.local.dao.GroupDao
import com.lythe.media.protobuf.ImConversationType
import com.lythe.media.protobuf.ImMessageStatus
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MessageRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "MessageReposiory"

        @Volatile
        private var instance: MessageRepository? = null
        fun getInstance(context: Context): MessageRepository {
            return instance ?: synchronized(this) {
                instance ?: MessageRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    private val db = AppDatabase.getInstance(context)
    private val messageDao: MessageDao = db.messageDao()
    private val friendDao: FriendDao = db.friendDao()
    private val groupDao: GroupDao = db.groupDao()
    private val conversationDao: ConversationDao = db.conversationDao()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun insertMessage(message: MessageEntity) {
        executor.execute {
            try {
                db.runInTransaction {
                    messageDao.insert(message)
                    // 使用异步查询避免阻塞
                    val conversation = conversationDao.getConversationSync(message.conversationId)
                    if(conversation == null) {
                        val conversationName = getConversationName(message.conversationId, message.conversationType)
                        val newConversation = ConversationEntity(
                            message.conversationId,
                            message.conversationType,
                            conversationName,
                            message.content,
                            message.timestamp,
                            1)
                        conversationDao.insertConversation(newConversation)
                    } else {
                        val conversationName = getConversationName(message.conversationId, message.conversationType)

                        conversationDao.updateConversationOnNewMessage(
                            message.conversationId, 
                            1,
                            message.content,
                            message.timestamp
                        )
                    }
                }
                Log.d(TAG, "消息插入成功: ${message.msgId}")
            } catch (e: Exception) {
                Log.e(TAG, "消息插入失败: ${message.msgId}", e)
            }
        }
    }
    fun getConversationName(conversationId: String, conversationType: ImConversationType): String {
        return when(conversationType) {
            ImConversationType.PRIVATE_CHAT -> {
                friendDao.getUserNameById(conversationId) ?: "User${conversationId}"
            }
            ImConversationType.GROUP_CHAT -> {
                groupDao.getGroupNameById(conversationId) ?: "Group${conversationId}"
            }
            ImConversationType.SYSTEM_NOTICE -> TODO()
            ImConversationType.CUSTOMER_SERVICE -> TODO()
            ImConversationType.UNRECOGNIZED -> TODO()
            else -> "UNKNOWN CONVERSATION"
        }
    }
//    fun buildAndSaveMessage(
//        conversationId: String,
//        conversationType: ImConversationType,
//        senderId: String,
//        receiver: String,
//        content: String,
//        contentType: Int): MessageEntity {
//        val clientId: String = UUID.randomUUID().toString()
//        val currentTimeMillis = System.currentTimeMillis()
//        val messageEntity = MessageEntity(
//            msgId = clientId,
//            conversationId = conversationId,
//            conversationType = conversationType,
//            fromUid = senderId,
//            toUid = receiver,
//            msgType = contentType,
//            content = content,
//            timestamp = currentTimeMillis,
//            status = ImMessageStatus.SENDING_VALUE,
//            isSelf = true
//        )
//        executor.execute {
//            val conversationSync = conversationDao.getConversationSync(conversationId)
//            db.runInTransaction {
//                messageDao.insert(messageEntity)
//
//                if(conversationSync == null) {
//                    ConversationEntity(conversationId = conversationId,
//                        conversationType = conversationType,
//                        lastMessage = content,
//                        lastMessageTime = currentTimeMillis,
//                        unreadCount = 0)
//                } else {
//                    conversationSync.lastMessage = content
//                    conversationSync.lastMessageTime = currentTimeMillis
//                    conversationDao.insertConversation(conversationSync)
//                }
//            }
//        }
//        return messageEntity
//    }

    fun getAllConversations(): List<ConversationEntity> {
        return conversationDao.getAllConversations();
    }

    fun getMessagesPages(conversationId: String, limit: Int, earliestTimestamp: Long): List<MessageEntity> {
        return messageDao.getMessagesPaged(conversationId, limit, earliestTimestamp)
    }
    fun getAllMessages(conversationId: String): List<MessageEntity> {
        return messageDao.getMessages(conversationId)
    }
    fun markConversationRead(conversationId: String) {
        executor.execute {
            conversationDao.resetUnread(conversationId)
        }
    }

}