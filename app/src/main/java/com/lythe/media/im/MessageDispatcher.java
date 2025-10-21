package com.lythe.media.im;

import android.os.Handler;
import android.os.Looper;

import com.lythe.media.chats.data.entity.MessageEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageDispatcher {
    private static volatile MessageDispatcher instance;
    private final List<MessageSubscriber> subscribers = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MessageDispatcher() {}
    public static MessageDispatcher getInstance() {
        if(instance == null) {
            synchronized (MessageDispatcher.class) {
                if(instance == null) {
                    instance = new MessageDispatcher();
                }
            }
        }
        return instance;
    }

    public void subscribe(MessageSubscriber subscriber) {
        if(subscriber != null && !subscribers.contains(subscriber)) {
            subscribers.add(subscriber);
        }
    }
    public void unsubscribe(MessageSubscriber subscriber) {
        if(subscriber != null) {
            subscribers.remove(subscriber);
        }
    }
    public void dispatch(MessageEntity message) {
        if(message == null || subscribers.isEmpty()) {
            return;
        }
        for (MessageSubscriber subscriber : subscribers) {
            if(subscriber.needMainThread()) {
                mainHandler.post(() -> subscriber.onMessageReceived(message));
            } else {
                subscriber.onMessageReceived(message);
            }
        }
    }


    public interface MessageSubscriber {
        void onMessageReceived(MessageEntity message);
        boolean needMainThread();
    }

}
