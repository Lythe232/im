package com.lythe.media.chats.utils;

import android.net.Uri;

public class SendMessageRequest {
    private Long dialogId;
    private SendMessagesHelper.MessageType messageType;
    private String textContent;
    private String fileLocalPath;
    private Uri fileUri;
    private Long fileSize;
    private String fileMimeType;
    private String tempMsgId;
    private String mqttTopic;
    private int mqttQos = 1;    //至少一次

    public static SendMessageRequest create() {
        return new SendMessageRequest();
    }
    public SendMessageRequest dialogId(Long dialogId) {
        this.dialogId = dialogId;
        return this;
    }
    public SendMessageRequest type(SendMessagesHelper.MessageType type) {
        this.messageType = type;
        return this;
    }

    public SendMessageRequest text(String text) {
        this.textContent = text;
        return this;
    }

    public SendMessageRequest file(String localPath, String mimeType, long size) {
        this.fileLocalPath = localPath;
        this.fileMimeType = mimeType;
        this.fileSize = size;
        return this;
    }

    public SendMessageRequest fileUri(Uri uri, String mimeType, long size) {
        this.fileUri = uri;
        this.fileMimeType = mimeType;
        this.fileSize = size;
        return this;
    }

    public SendMessageRequest tempMsgId(String tempMsgId) {
        this.tempMsgId = tempMsgId;
        return this;
    }

    public SendMessageRequest mqttTopic(String topic) {
        this.mqttTopic = topic;
        return this;
    }

    public SendMessageRequest mqttQos(int qos) {
        if(qos >= 0 && qos <= 2) {
            this.mqttQos = qos;
        }
        return this;
    }
    public String getMqttTopic() {
        return mqttTopic;
    }

    public int getMqttQos() {
        return mqttQos;
    }


    public long getDialogId() { return dialogId; }
    public SendMessagesHelper.MessageType getMessageType() { return messageType; }
    public String getTextContent() { return textContent; }
    public String getFileLocalPath() { return fileLocalPath; }
    public Uri getFileUri() { return fileUri; }
    public long getFileSize() { return fileSize; }
    public String getFileMimeType() { return fileMimeType; }
    public String getTempMsgId() { return tempMsgId; }

    public String validate() {
        if (dialogId <= 0) {
            return "对话ID不能为空";
        }
        if (messageType == null) {
            return "消息类型未指定";
        }
        if (mqttTopic == null || mqttTopic.trim().isEmpty()) {
            return "MQTT主题（mqttTopic）不能为空";
        }
        switch (messageType) {
            case TEXT:
                if (textContent == null || textContent.trim().isEmpty()) {
                    return "文本消息内容不能为空";
                }
                break;
            case IMAGE:
            case FILE:
                if ((fileLocalPath == null || fileLocalPath.isEmpty()) && fileUri == null) {
                    return "图片/文件路径或Uri不能为空";
                }
                if (fileSize <= 0) {
                    return "图片/文件大小无效";
                }
                break;
        }
        return null;
    }
}
