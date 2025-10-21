package com.lythe.media.im.messager;

import com.google.protobuf.ByteString;
import com.lythe.media.protobuf.ImConversationType;
import com.lythe.media.protobuf.ImImageContent;
import com.lythe.media.protobuf.ImMessage;
import com.lythe.media.protobuf.ImMessageContentType;
import com.lythe.media.protobuf.ImMessageStatus;
import com.lythe.media.protobuf.ImMessageType;
import com.lythe.media.protobuf.ImReplyInfo;
import com.lythe.media.protobuf.ImTextContent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Helper for building ImMessage in a production-safe way.
 * - Prefer oneof for common types (text/image/voice)
 * - For large/encrypted/custom payloads, use content_type + content_bytes
 */
public final class MessageBuildHelper {

    private MessageBuildHelper() {}

    public static ImMessage buildTextMessageAuto(
            String senderId,
            String receiverId,
            String conversationId,
            String text,
            boolean enableCompression,
            int compressThresholdBytes
    ) {
        String clientMsgId = UUID.randomUUID().toString();
        byte[] raw = text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);

        boolean useBytes = enableCompression && raw.length > Math.max(0, compressThresholdBytes);

        ImMessage.Builder b = baseBuilder(senderId, receiverId, conversationId, ImConversationType.PRIVATE_CHAT)
                .setMessageId(clientMsgId)
                .setClientMsgId(clientMsgId)
                .setMessageType(ImMessageType.TEXT)
                .setStatus(ImMessageStatus.SENDING)
                .setTimestamp(System.currentTimeMillis());

        if (useBytes) {
            // 在这里你可以插入你真正的压缩/加密。
            byte[] payload = raw; // 如果需要，替换为压缩/加密
            b.setContentType(ImMessageContentType.CONTENT_TEXT)
             .setContentBytes(ByteString.copyFrom(payload));
        } else {
            b.setTextContent(ImTextContent.newBuilder().setText(text == null ? "" : text).build());
        }
        return b.build();
    }

    public static ImMessage buildImageMessage(
            String senderId,
            String receiverId,
            String conversationId,
            String imageUrl,
            int width,
            int height,
            long fileSize,
            String thumbUrl
    ) {
        String clientMsgId = UUID.randomUUID().toString();
        ImImageContent.Builder ic = ImImageContent.newBuilder()
                .setImageUrl(imageUrl == null ? "" : imageUrl)
                .setWidth(width)
                .setHeight(height)
                .setFileSize(fileSize);
        if (thumbUrl != null) ic.setThumbUrl(thumbUrl);

        return baseBuilder(senderId, receiverId, conversationId, ImConversationType.PRIVATE_CHAT)
                .setMessageId(clientMsgId)
                .setClientMsgId(clientMsgId)
                .setMessageType(ImMessageType.IMAGE)
                .setStatus(ImMessageStatus.SENDING)
                .setTimestamp(System.currentTimeMillis())
                .setImageContent(ic.build())
                .build();
    }

    public static ImMessage buildGenericBytes(
            String senderId,
            String receiverId,
            String conversationId,
            ImMessageType messageType,
            ImMessageContentType contentType,
            byte[] bytes
    ) {
        String clientMsgId = UUID.randomUUID().toString();
        return baseBuilder(senderId, receiverId, conversationId, ImConversationType.PRIVATE_CHAT)
                .setMessageId(clientMsgId)
                .setClientMsgId(clientMsgId)
                .setMessageType(messageType)
                .setStatus(ImMessageStatus.SENDING)
                .setTimestamp(System.currentTimeMillis())
                .setContentType(contentType)
                .setContentBytes(ByteString.copyFrom(bytes == null ? new byte[0] : bytes))
                .build();
    }

    public static ImMessage withReplyMentionsEdited(
            ImMessage message,
            String replyToMessageId,
            ImMessageType replyToType,
            String previewText,
            List<String> mentionUserIds,
            boolean isEdited
    ) {
        ImMessage.Builder b = message.toBuilder();
        if (replyToMessageId != null) {
            b.setReply(ImReplyInfo.newBuilder()
                    .setReplyToMessageId(replyToMessageId)
                    .setReplyToType(replyToType == null ? ImMessageType.TEXT : replyToType)
                    .setPreviewText(previewText == null ? "" : previewText)
                    .build());
        }
        if (mentionUserIds != null) {
            b.clearMentionUserIds();
            b.addAllMentionUserIds(mentionUserIds);
        }
        b.setIsEdited(isEdited);
        return b.build();
    }

    private static ImMessage.Builder baseBuilder(
            String senderId,
            String receiverId,
            String conversationId,
            ImConversationType conversationType
    ) {
        return ImMessage.newBuilder()
                .setSenderId(senderId == null ? "" : senderId)
                .setReceiverId(receiverId == null ? "" : receiverId)
                .setConversationId(conversationId == null ? "" : conversationId)
                .setConversationType(conversationType == null ? ImConversationType.PRIVATE_CHAT : conversationType);
    }
}



