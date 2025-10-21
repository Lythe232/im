package com.lythe.media.im;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.lythe.media.chats.data.local.LocalMessageCache;
import com.lythe.media.chats.data.local.RoomCache;
import com.lythe.media.chats.data.entity.MessageConverter;
import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.protobuf.ImMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageProcessor {
    private static final String TAG = "MessageProcessor";
    private static volatile MessageProcessor instance;
    private final ExecutorService processorExecutor = Executors.newSingleThreadExecutor();
    private final MessageDispatcher dispatcher;
    private final LocalMessageCache localMessageCache;
    private final Gson gson = new Gson();
    private MessageProcessor(Context context) {
        dispatcher = MessageDispatcher.getInstance();
        localMessageCache = new RoomCache(context);
    }
    public static MessageProcessor getInstance(Context context) {
        if(instance == null) {
            synchronized (MessageProcessor.class) {
                if(instance == null) {
                    instance = new MessageProcessor(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void processReceivedMessage(String topic, byte[] rawContent) {
        processorExecutor.submit(() -> {
            try {
                if(!validateMessage(topic, rawContent)) {
                    Log.e(TAG, "无效消息：topic=" + topic
//                            + "，内容=" + rawContent
                    );
                    return ;
                }
                MessageEntity message = parseMessage(topic, rawContent);
                if(message == null) {
                    Log.e(TAG, "消息解析失败：" + rawContent);
                    return ;
                }
                saveMessageToLocal(message);
                dispatchMessage(message);
            } catch (Exception e) {
                Log.e(TAG, "消息处理异常", e);
            }
        });
    }
    private void saveMessageToLocal(MessageEntity message) {
        try {
            //插入数据库
            localMessageCache.saveMessageToCache(message);
        } catch (Exception e) {
            Log.e(TAG, "消息存储失败", e);
        }
    }
    private void dispatchMessage(MessageEntity message) {
        dispatcher.dispatch(message);
    }
    private boolean validateMessage(String topic, byte[] rawContent) {
        return topic != null && !topic.trim().isEmpty()
                && rawContent != null ;
    }
    public void release() {
        processorExecutor.shutdown();
    }

    private MessageEntity parseMessage(String topic, byte[] rawContent) {

        ImMessage imMessage = MessageConverter.INSTANCE.deserialize(rawContent);
        MessageEntity messageEntity = MessageConverter.INSTANCE.fromProto(imMessage, false);

        Log.d(TAG, "收到聊天消息: "
                + messageEntity.getMsgId()
                + ", 发送者: " );


        return messageEntity;
    }
}
