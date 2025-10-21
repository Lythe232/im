package com.lythe.media.im.messager;

import com.lythe.media.protobuf.ImImageContent;
import com.lythe.media.protobuf.ImMessage;
import com.lythe.media.protobuf.ImMessageContentType;
import com.lythe.media.protobuf.ImMessageType;
import com.lythe.media.protobuf.ImReplyInfo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser helper to unify ImMessage into a simple structure for UI/domain.
 * - Prefer parsing oneof first
 * - If CONTENT_NOT_SET and content_bytes present, decode by content_type
 */
public final class MessageParseHelper {

    private MessageParseHelper() {}

    public static ParsedMessage parse(ImMessage msg, ContentDecoder decoder) {
        if (decoder == null) decoder = ContentDecoder.NOOP;

        ParsedMessage p = new ParsedMessage();
        p.messageId = msg.getMessageId();
        p.clientMsgId = msg.getClientMsgId();
        p.conversationId = msg.getConversationId();
        p.messageType = msg.getMessageType();
        p.status = msg.getStatus().name();
        p.timestamp = msg.getTimestamp();
        p.serverMsgSeq = msg.getServerMsgSeq();
        p.serverTimestamp = msg.getServerTimestamp();
        p.isEdited = msg.getIsEdited();

        if (msg.hasReply()) {
            ImReplyInfo r = msg.getReply();
            p.replyToMessageId = r.getReplyToMessageId();
            p.replyToType = r.getReplyToType();
            p.replyPreviewText = r.getPreviewText();
        }
        p.mentionUserIds = new ArrayList<>(msg.getMentionUserIdsList());

        switch (msg.getContentCase()) {
            case TEXT_CONTENT:
                p.text = msg.getTextContent().getText();
                break;
            case IMAGE_CONTENT: {
                ImImageContent ic = msg.getImageContent();
                p.imageUrl = ic.getImageUrl();
                p.imageWidth = ic.getWidth();
                p.imageHeight = ic.getHeight();
                p.imageFileSize = ic.getFileSize();
                String thumb = ic.getThumbUrl();
                p.imageThumbUrl = (thumb == null || thumb.isEmpty()) ? null : thumb;
                break;
            }
            case VOICE_CONTENT:
                p.voiceUrl = msg.getVoiceContent().getVoiceUrl();
                p.voiceDuration = msg.getVoiceContent().getDuration();
                p.voiceFileSize = msg.getVoiceContent().getFileSize();
                break;
            case VIDEO_CONTENT:
                p.videoUrl = msg.getVideoContent().getVideoUrl();
                p.videoCoverUrl = msg.getVideoContent().getCoverUrl();
                p.videoWidth = msg.getVideoContent().getWidth();
                p.videoHeight = msg.getVideoContent().getHeight();
                p.videoDuration = msg.getVideoContent().getDuration();
                p.videoFileSize = msg.getVideoContent().getFileSize();
                break;
            case FILE_CONTENT:
                p.fileName = msg.getFileContent().getFileName();
                p.fileUrl = msg.getFileContent().getFileUrl();
                p.fileSize = msg.getFileContent().getFileSize();
                p.fileMimeType = msg.getFileContent().getMimeType();
                break;
            case STICKER_CONTENT:
                p.stickerId = msg.getStickerContent().getStickerId();
                p.stickerPackId = msg.getStickerContent().getPackId();
                p.stickerUrl = msg.getStickerContent().getUrl();
                break;
            case CONTENT_NOT_SET:
            default:
                // If unified bytes exist, decode by content_type
                if (!msg.getContentBytes().isEmpty()) {
                    byte[] plain = decoder.decode(msg.getContentBytes().toByteArray());
                    ImMessageContentType t = msg.getContentType();
                    p.contentType = t;
                    p.contentBytes = plain; // for upper-layer custom parsing
                    if (t == ImMessageContentType.CONTENT_TEXT) {
                        p.text = new String(plain, StandardCharsets.UTF_8);
                    }
                }
                break;
        }
        return p;
    }

    public interface ContentDecoder {
        ContentDecoder NOOP = bytes -> bytes;
        byte[] decode(byte[] bytes);
    }

    public static final class ParsedMessage {
        public String messageId;
        public String clientMsgId;
        public String conversationId;
        public ImMessageType messageType;
        public String status;
        public long timestamp;
        public long serverMsgSeq;
        public long serverTimestamp;

        public boolean isEdited;
        public String replyToMessageId;
        public ImMessageType replyToType;
        public String replyPreviewText;
        public List<String> mentionUserIds;

        // Text
        public String text;

        // Image
        public String imageUrl;
        public int imageWidth;
        public int imageHeight;
        public long imageFileSize;
        public String imageThumbUrl;

        // Voice
        public String voiceUrl;
        public int voiceDuration;
        public long voiceFileSize;

        // Video
        public String videoUrl;
        public String videoCoverUrl;
        public int videoWidth;
        public int videoHeight;
        public int videoDuration;
        public long videoFileSize;

        // File
        public String fileName;
        public String fileUrl;
        public long fileSize;
        public String fileMimeType;

        // Sticker
        public String stickerId;
        public String stickerPackId;
        public String stickerUrl;

        // Unified bytes path
        public ImMessageContentType contentType;
        public byte[] contentBytes;
    }
}


