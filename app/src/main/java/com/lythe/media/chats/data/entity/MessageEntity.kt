package com.lythe.media.chats.data.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lythe.media.protobuf.ImConversationType
import com.lythe.media.protobuf.ImFileContent
import com.lythe.media.protobuf.ImImageContent
import com.lythe.media.protobuf.ImMessage
import com.lythe.media.protobuf.ImMessageContentType
import com.lythe.media.protobuf.ImMessageStatus
import com.lythe.media.protobuf.ImMessageType
import com.lythe.media.protobuf.ImStickerContent
import com.lythe.media.protobuf.ImTextContent
import com.lythe.media.protobuf.ImVideoContent
import com.lythe.media.protobuf.ImVoiceContent

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val msgId: String,
    val conversationId: String,
    val conversationType: ImConversationType,
    val fromUid: String,
    val toUid: String,
    val msgType: Int,
    val content: String?,
    val topic: String,
    val retryCount: Int = 0,
    val timestamp: Long,
    val serverMsgSeq: Long,
    val isEdited: Boolean,
//    val mentionUserIds: List<String>,
    var status: Int,
    val filePath: String? = null,
    val fileSize: Long = 0,
    val duration: Int = 0,
    val isRead: Boolean = false,
    var isSelf: Boolean = false,
)
//    val replyToMsgId: String,

object MessageConverter {
    val TAG : String = "MessageConverter"
    fun fromProto(protoMsg: ImMessage, isSelf: Boolean = false): MessageEntity {

        var content: String? = null
        var filePath: String? = null
        var fileSize: Long = 0
        var duration: Int = 0

        when(protoMsg.contentCase) {
            ImMessage.ContentCase.TEXT_CONTENT -> {
                content = protoMsg.textContent.text
            }
            ImMessage.ContentCase.IMAGE_CONTENT -> {
                val image = protoMsg.imageContent
                content = "[image]"
                filePath = image.imageUrl
                fileSize = image.fileSize
            }
            ImMessage.ContentCase.VOICE_CONTENT -> {
                val voice = protoMsg.voiceContent
                content = "[voice ${voice.duration}second]"
                filePath = voice.voiceUrl
                fileSize = voice.fileSize
                duration = voice.duration
            }
            ImMessage.ContentCase.VIDEO_CONTENT -> {
                val video = protoMsg.videoContent
                content = "[video ${video.duration}]"
                filePath = video.videoUrl
                fileSize = video.fileSize
                duration = video.duration
            }
            ImMessage.ContentCase.FILE_CONTENT -> {
                val file = protoMsg.fileContent
                content = if(protoMsg.hasReply()) {
                    "[Reply ${protoMsg.reply.previewText}\n[file ${file.fileName}]"
                } else {
                    "[File ${file.fileName}]"
                }
                filePath = file.fileUrl
                fileSize = file.fileSize
            }

            ImMessage.ContentCase.STICKER_CONTENT -> {
                val sticker = protoMsg.stickerContent
                content = "[sticker ${sticker.stickerId}]"
                filePath = sticker.url
            }
            ImMessage.ContentCase.CONTENT_NOT_SET -> {
                content = "[UNKNOWN MESSAGE]"
            }

        }
        val isRead = protoMsg.status == ImMessageStatus.READ


        return MessageEntity(
            msgId = protoMsg.messageId,
            conversationId = protoMsg.conversationId.ifEmpty { protoMsg.sessionId }, // 优先用conversationId，兼容旧sessionId
            conversationType = protoMsg.conversationType,
            fromUid = protoMsg.senderId,
            toUid = protoMsg.receiverId,
            msgType = protoMsg.messageType.number, // 枚举值转Int
            content = content,
            timestamp = protoMsg.timestamp,
            status = protoMsg.status.number, // 状态枚举值转Int
            filePath = filePath,
            fileSize = fileSize,
            duration = duration,
            isRead = isRead,
            isSelf = isSelf,
            serverMsgSeq = protoMsg.serverMsgSeq,
            isEdited = protoMsg.isEdited,
            topic = protoMsg.topic,
            retryCount = protoMsg.retryCount,
//            mentionUserIds = protoMsg.mentionUserIdsList,
        )
    }

    fun toProto(entity: MessageEntity): ImMessage {

        // 1. 基础字段构建
        val builder = ImMessage.newBuilder()
            .setMessageId(entity.msgId)
            .setConversationId(entity.conversationId)
            .setSessionId(entity.conversationId) // 兼容旧字段
            .setConversationType(entity.conversationType)
            .setSenderId(entity.fromUid)
            .setReceiverId(entity.toUid)
            .setTimestamp(entity.timestamp)
            .setStatus(ImMessageStatus.forNumber(entity.status) ?: ImMessageStatus.FAILED)
            .setMessageType(ImMessageType.forNumber(entity.msgType) ?: ImMessageType.TEXT)
            // 幂等与排序字段
            .setClientMsgId(entity.msgId ?: "")
            .setServerMsgSeq(entity.serverMsgSeq)
            .setServerTimestamp(entity.timestamp)
            // 编辑标记
            .setIsEdited(entity.isEdited)
            // @提及用户列表
//            .addAllMentionUserIds(entity.mentionUserIds ?: emptyList())
        // 2. 引用回复信息（ImReplyInfo）
//        if (!entity.replyToMsgId.isNullOrEmpty() && entity.replyToType != null) {
//            builder.reply = ImReplyInfo.newBuilder()
//                .setReplyToMessageId(entity.replyToMsgId)
//                .setReplyToType(ImMessageType.forNumber(entity.replyToType) ?: ImMessageType.TEXT)
//                .setPreviewText(entity.replyPreview ?: "[回复内容]")
//                .build()
//        }
        when (ImMessageType.forNumber(entity.msgType)) {
            ImMessageType.TEXT -> {
                // 文本消息：直接使用content字段
                builder.textContent = ImTextContent.newBuilder()
                    .setText(entity.content ?: "")
                    .build()
                // 统一内容类型标记
                builder.contentType = ImMessageContentType.CONTENT_TEXT
            }

            ImMessageType.IMAGE -> {
                // 图片消息：包含URL、宽高、大小、本地路径等
                builder.imageContent = ImImageContent.newBuilder()
                    .setImageUrl(entity.filePath ?: "")
                    .setLocalPath(entity.filePath ?: "")
//                    .setWidth(entity.width)
//                    .setHeight(entity.height)
                    .setFileSize(entity.fileSize)
                    .setThumbUrl("") // 若实体有缩略图路径，可补充
                    .setBlurHash("") // 若实体有BlurHash，可补充
                    .build()
                builder.contentType = ImMessageContentType.CONTENT_IMAGE
            }

            ImMessageType.VOICE -> {
                // 语音消息：包含URL、时长、大小等
                builder.voiceContent = ImVoiceContent.newBuilder()
                    .setVoiceUrl(entity.filePath ?: "")
                    .setLocalPath(entity.filePath ?: "")
                    .setDuration(entity.duration)
                    .setFileSize(entity.fileSize)
                    .build()
                builder.contentType = ImMessageContentType.CONTENT_VOICE
            }

            ImMessageType.FILE -> {
                // 文件消息：包含文件名、URL、大小、MIME类型
                builder.fileContent = ImFileContent.newBuilder()
//                    .setFileName(entity.fileName ?: "未知文件")
                    .setFileUrl(entity.filePath ?: "")
                    .setFileSize(entity.fileSize)
//                    .setMimeType(entity.fileName?.let { getMimeType(it) }
//                        ?: "application/octet-stream")
                    .build()
                builder.contentType = ImMessageContentType.CONTENT_FILE
            }

            ImMessageType.VIDEO -> {
                // 视频消息：包含URL、封面、时长、宽高、大小
                builder.videoContent = ImVideoContent.newBuilder()
                    .setVideoUrl(entity.filePath ?: "")
                    .setCoverUrl("") // 若实体有封面路径，可补充
//                    .setWidth(entity.width)
//                    .setHeight(entity.height)
                    .setDuration(entity.duration)
                    .setFileSize(entity.fileSize)
                    .build()
                builder.contentType = ImMessageContentType.CONTENT_VIDEO
            }

            // 假设MessageType包含STICKER（原枚举可能需补充，或用自定义类型）
            else -> {
                if (entity.msgType == 5) { // 对应ImMessageType.STICKER（假设值为5）
                    builder.stickerContent = ImStickerContent.newBuilder()
//                        .setStickerId(entity.stickerId ?: "")
//                        .setPackId(entity.packId ?: "")
                        .setUrl(entity.filePath ?: "")
                        .build()
                    builder.contentType = ImMessageContentType.CONTENT_STICKER
                } else {
                    // 未知类型：默认文本处理
                    builder.textContent = ImTextContent.newBuilder()
                        .setText(entity.content ?: "[未知消息]")
                        .build()
                    builder.contentType = ImMessageContentType.CONTENT_TEXT
                }
            }
        }
            // 4. 统一内容字节（可选：若需要序列化整体内容）
            // 若业务需要，可在这里设置content_bytes（如加密后的内容）
            // builder.contentBytes = ...
        return builder.build()
    }
    fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".doc") || fileName.endsWith(".docx") -> "application/msword"
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".mp4") -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
    fun serialize(message: ImMessage): ByteArray {
        return message.toByteArray()
    }
    fun deserialize(data: ByteArray): ImMessage {
        return try {
            ImMessage.parseFrom(data)
        } catch (e: Exception) {
            Log.e(TAG, "反序列化Protobuf消息失败", e)
            throw RuntimeException("Protobuf反序列化失败", e)
        }
    }
}