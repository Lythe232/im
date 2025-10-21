package com.lythe.media.chats.utils;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.lythe.media.ImApplication;
import com.lythe.media.im.MqttClientManager;
import com.lythe.media.im.messager.compression.MessageCompressor;
import com.lythe.media.im.messager.logging.Logger;
import com.lythe.media.im.messager.monitor.PerformanceMonitor;
import com.lythe.media.im.messager.power.PowerManager;
import com.lythe.media.im.messager.queue.MessageQueue;
import com.lythe.media.im.net.NetworkManager;
import com.lythe.media.protobuf.ImMessage;

import org.json.JSONException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SendMessagesHelper {
    public enum MessageType {
        TEXT(1),
        IMAGE(2),
        FILE(3),
        VIDEO(4),
        AUDIO(5);
        private final int type;
        MessageType(int type) { this.type = type; }
        public int getType() { return type; }
    }
    public enum SendStatus {
        PENDING(0),
        SENDING(1),
        UPLOADING(2),
        SUCCESS(3),
        FAILED(4),
        CANCELED(5);
        private final int type;
        SendStatus(int type) {
            this.type = type;
        }
    }

    public interface OnSendMessageListener {
        void onSendStatusChanged(String tempMsgId, SendStatus status, String msg);

        default void onFileUploadProgress(String tempMsgId, int progress, long uploadedSize, long totalSize) {}
        default void onSendSuccess(String tempMsgId, String serverMsgId, long serverTimestamp) {}
        default void onSendFailed(String tempMsgId, int errorCode, String errorMsg) {}
        default void onSendCanceled(String tempMsgId) {}
    }
    public interface FileUploader {
        String uploadFile(ImMessage request, OnFileUploadProgressListener onFileUploadProgressListener) throws Exception;
        void cancelUpload();
        public interface OnFileUploadProgressListener {
            void onProgress(int progress, Long uploadedSize, Long totalSize);
            void onCanceled();
        }
    }
    public interface MessageSender {
        SendMessageResponse sendMessage(ImMessage request, String to, String fileServerUrl) throws Exception;

        void cancelSend();
        class SendMessageResponse {
            private String serverMsgId;
            private Long serverTimestamp;
            public String getServerMsgId() { return serverMsgId; }
            public void setServerMsgId(String serverMsgId) { this.serverMsgId = serverMsgId; }
            public long getServerTimestamp() { return serverTimestamp; }
            public void setServerTimestamp(long serverTimestamp) { this.serverTimestamp = serverTimestamp; }
        }
    }
    private final static String TAG = "SendMessagesHelper";
    private final ExecutorService sendExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private FileUploader fileUploader;
    private MessageSender messageSender;
    private final ConcurrentHashMap<String, AtomicBoolean> cancelFlags = new ConcurrentHashMap<>();
    private static final int MAX_RETRY_COUNT = 2;
    private static final Long RETRY_DELAY = 1000L;
    private ImApplication app;
    private static volatile SendMessagesHelper instance;
    private boolean inited = false;
    private Logger logger;
    private PerformanceMonitor performanceMonitor;
    private NetworkManager networkManager;
    private PowerManager powerManager;
    private MessageQueue messageQueue;
    private MessageCompressor messageCompressor;
    private MqttClientManager mqttClientManager;

    public static SendMessagesHelper getInstance() {
        if(instance == null) {
            synchronized (SendMessagesHelper.class) {
                if(instance == null) {
                    instance = new SendMessagesHelper();
                }
            }
        }
        return instance;
    }
    public void initMessageHelper(FileUploader fileUploader, MessageSender messageSender, Context context) {
        if(this.inited) {
            return ;
        }
        this.fileUploader = fileUploader;
        this.messageSender = messageSender;
        this.app = (ImApplication) context.getApplicationContext();
        this.logger = app.getLogger();
        this.performanceMonitor = app.getPerformanceMonitor();
        this.networkManager = app.getNetworkManager();
        this.powerManager = app.getPowerManager();
        this.messageQueue = app.getMessageQueue();
        this.messageCompressor = MessageCompressor.getInstance();
        this.mqttClientManager = MqttClientManager.getInstance(app);
        this.inited = true;
    }
    private SendMessagesHelper() {
    }
    public void sendMessage(ImMessage message, String to, Integer qos) {
        if(!getInstance().inited) {
            Log.e(TAG, "SendMessagesHelper not impl");
            return ;
        }
        performanceMonitor.startOperation("sendMessage");

        try {
            if (!networkManager.isNetworkAvailable()) {
                logger.warn(TAG, "Network not available, message will be queued");
            }
            if(!mqttClientManager.isConnected()) {
                Log.w(TAG, "MQTT not connected, cannot send message: msgId=" + message.getMessageId());
                mqttClientManager.connect();
//                return ;
            }
            String dedupPayload = "";
            switch (message.getContentCase()) {
                case TEXT_CONTENT:
                    dedupPayload = message.getTextContent().getText();
                    break;
                case IMAGE_CONTENT:
                    dedupPayload = message.getImageContent().getImageUrl();
                    break;
                case VOICE_CONTENT:
                    dedupPayload = message.getVoiceContent().getVoiceUrl();
                    break;
                case VIDEO_CONTENT:
                    dedupPayload = message.getVideoContent().getVideoUrl();
                    break;
                case FILE_CONTENT:
                    dedupPayload = message.getFileContent().getFileUrl();
                    break;
                case STICKER_CONTENT:
                    dedupPayload = message.getStickerContent().getStickerId();
                    break;
                case CONTENT_NOT_SET:
                default:
                    if (!message.getContentBytes().isEmpty()) {
                        // 注意：这里只用于快速去重，真实内容应按 content_type 正确解码
                        dedupPayload = message.getContentBytes().toStringUtf8();
                    }
                    break;
            }

            if(messageCompressor.isDuplicateMessage(message.getMessageId(), dedupPayload)) {
                logger.warn(TAG, "Duplicate message detected, skipping: " + message.getMessageId());
                return ;
            }
            messageQueue.enqueueMessage(message, to, qos, true);

            logger.info(TAG, "Message enqueued successfully: " + message.getMessageId());

        } catch (Exception e) {
            logger.error(TAG, "Failed to send message: " + message.getMessageId(), e);
        } finally {
            performanceMonitor.endOperation("sendMessage");
        }
//        AtomicBoolean cancelFlag = new AtomicBoolean(false);

//        cancelFlags.put(message.getMessageId(), cancelFlag);
//        sendExecutor.submit(() -> processMessageSending(message, to, listener, message.getMessageId(), cancelFlag));
    }
    private void processMessageSending(ImMessage request, String to, OnSendMessageListener listener,
                                       String tempMsgId, AtomicBoolean cancelFlag) {
        try {
            notifyStatus(listener, tempMsgId, SendStatus.SENDING, "正在发送");
            String fileServerUrl = null;
            switch (request.getMessageType()) {
                case TEXT:
                    sendTextMessage(request, to, listener, cancelFlag);
                    break;
                case IMAGE:
                case FILE:
                    fileServerUrl = uploadFileBeforeSend(request, listener, cancelFlag);
                    if(cancelFlag.get()) {
                        notifyCanceled(listener, tempMsgId);
                        return ;
                    }
                    if(fileServerUrl == null) {
                        notifyStatus(listener, tempMsgId, SendStatus.FAILED, "文件上传失败");
                        notifyFailed(listener, tempMsgId, 2000, "文件上传失败");
                    }
                    sendFileRelatedMessage(request, to, fileServerUrl, listener, cancelFlag);
                    break;
            }
        } catch (Exception e) {
            String s = e.getMessage() != null ? e.getMessage() : "发送异常";
            notifyStatus(listener, tempMsgId, SendStatus.FAILED, s);
            notifyFailed(listener, tempMsgId, 1001, s);
        } finally {
            cancelFlags.remove(tempMsgId);
        }
    }
    public void retrySendMessage(ImMessage request, String to, OnSendMessageListener listener) {
//        sendMessage(request, to, 1);
    }

    public void cancelSendMessage(String tempMsgId) {
        AtomicBoolean cancelFlag = cancelFlags.get(tempMsgId);
        if(cancelFlag != null) {
            cancelFlag.set(true);
        }

        if(fileUploader != null) {
            this.fileUploader.cancelUpload();
        }
        if(messageSender != null) {
            messageSender.cancelSend();
        }
        notifyCanceled(null, tempMsgId);
    }
    private void sendTextMessage(ImMessage request, String to, OnSendMessageListener listener, AtomicBoolean cancelFlag) throws Exception {
        if(cancelFlag.get()) {
            return ;
        }
        MessageSender.SendMessageResponse response = retrySendWithStrategy(
                request,
                to,
                null,
                listener,
                cancelFlag,
                0
        );
        if(cancelFlag.get()) {
            return ;
        }
        if(response != null && response.getServerMsgId() != null) {
            notifyStatus(listener, request.getMessageId(), SendStatus.SUCCESS, "发送成功");
            notifySuccess(listener, request.getMessageId(), response.getServerMsgId(), response.getServerTimestamp());
        } else {
            throw new Exception("服务器未返回有效ID");
        }
    }

    private String uploadFileBeforeSend(ImMessage request, OnSendMessageListener listener, AtomicBoolean cancelFlag ) throws Exception {
        String tempMsgId = request.getMessageId();
        FileUploader.OnFileUploadProgressListener progressListener = new FileUploader.OnFileUploadProgressListener() {
            @Override
            public void onProgress(int progress, Long uploadedSize, Long totalSize) {
                notifyUploadProgress(listener, tempMsgId, progress, uploadedSize, totalSize);
            }

            @Override
            public void onCanceled() {
                cancelFlag.set(true);
            }
        };
        return fileUploader.uploadFile(request, progressListener);
    }

    private void sendFileRelatedMessage(ImMessage request, String to, String fileServerUrl, OnSendMessageListener listener, AtomicBoolean cancelFlag) throws Exception {
        if (cancelFlag.get()) {
            return;
        }
        MessageSender.SendMessageResponse response = retrySendWithStrategy(
                request, to, fileServerUrl, listener, cancelFlag, 0
        );
        if (cancelFlag.get()) {
            return;
        }
        if (response != null && response.getServerMsgId() != null) {
            notifyStatus(listener, request.getMessageId(), SendStatus.SUCCESS, "发送成功");
            notifySuccess(listener, request.getMessageId(), response.getServerMsgId(), response.getServerTimestamp());
        } else {
            throw new Exception("服务器未返回有效消息ID");
        }
    }

    private MessageSender.SendMessageResponse retrySendWithStrategy(
            ImMessage request, String to, String fileServerUrl,
            OnSendMessageListener listener, AtomicBoolean cancelFlag,
            int retryCount
    ) throws Exception {
        try {
            return messageSender.sendMessage(request, to, fileServerUrl);
        } catch (Exception e) {
            if(cancelFlag.get() || retryCount >= MAX_RETRY_COUNT) {
                throw e;
            }
            String retryMsg = "发送失败，正在重试（" + (retryCount + 1) + "/" + MAX_RETRY_COUNT + "）";
            notifyStatus(listener, request.getMessageId(), SendStatus.SENDING, retryMsg);
            long delay = RETRY_DELAY + (retryCount * 1000);
            Thread.sleep(delay);
            return retrySendWithStrategy(request, to, fileServerUrl, listener, cancelFlag, retryCount + 1);
        }
    }

    private void notifyStatus(OnSendMessageListener listener, String tempMsgId, SendStatus status, String msg) {
        if(listener == null) return;
        mainHandler.post(() -> listener.onSendStatusChanged(tempMsgId, status, msg));
    }
    private void notifyUploadProgress(OnSendMessageListener listener, String tempMsgId, int progress, long uploadedSize, long totalSize) {
        if(listener == null) return;
        mainHandler.post(() -> listener.onFileUploadProgress(tempMsgId, progress, uploadedSize, totalSize));
    }
    private void notifySuccess(OnSendMessageListener listener, String tempMsgId, String serverMsgId, Long serverTimestamp) {
        if(listener == null) return;
        mainHandler.post(() -> listener.onSendSuccess(tempMsgId, serverMsgId, serverTimestamp));
    }
    private void notifyFailed(OnSendMessageListener listener, String tempMsgId, int errorCode, String errorMsg) {
        if(listener == null) return ;
        mainHandler.post(() -> listener.onSendFailed(tempMsgId, errorCode, errorMsg));
    }

    private void notifyCanceled(OnSendMessageListener listener, String tempMsgId) {
        if(listener != null) {
            mainHandler.post(() -> listener.onSendCanceled(tempMsgId));
        }
        notifyStatus(listener, tempMsgId, SendStatus.CANCELED, "已取消发送");
    }
    private String generateTempMsgId() {
        long currentTimeMillis = System.currentTimeMillis();
        int random = (int) (Math.random() * 1000);
        return "TEMP_" + currentTimeMillis + "_" + random;
    }
    public void release() {
        sendExecutor.shutdown();
        mainHandler.removeCallbacksAndMessages(null);
        cancelFlags.clear();
    }

}

