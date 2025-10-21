package com.lythe.media.chats.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lythe.media.chats.data.local.LocalMessageCache;
import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.chats.utils.SendMessagesHelper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageViewModel extends ViewModel {
    private MutableLiveData<SendMessagesHelper.SendStatus> sendStatusMutableLiveData = new MutableLiveData<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new android.os.Handler(Looper.getMainLooper());
    private final LocalMessageCache localMessageCache;
    public MessageViewModel(LocalMessageCache localMessageCache) {
        this.localMessageCache = localMessageCache;
    }


    public interface OnMessageLoadListener  {
        void onSuccess(List<MessageEntity> messages, boolean hasMore);
        void onFailure(String errorMsg);
        default void onNoMoreData() {
            // 无更多消息（用于分页终止）
            onSuccess(Collections.emptyList(), false);
        }
    }

    public void loadMessage(
            Long dialogId,                  //对话唯一ID
            Integer count,                  //本次加载数量
            Long maxId,                     //分页标识：只加载ID < maxId 的消息
            Long offsetTime,                //分页辅助：只加载时间 < offsetTime 的消息
            Boolean loadFromCacheFirst,     //是否优先从缓存加载
            OnMessageLoadListener listener  //加载结果回调
    ) {
        if (dialogId <= 0 || count <= 0 || listener == null) {
            postFailure(listener, "参数错误：对话ID和加载数量必须大于0");
            return;
        }
        if(loadFromCacheFirst) {
            executor.execute(() -> {
                List<MessageEntity> messagesFromCache = localMessageCache.getMessagesFromCache(dialogId, maxId, count);
                if(messagesFromCache != null && messagesFromCache.size() >= count) {
                    postSuccess(listener, messagesFromCache, true);
                }
                else {

                }
            });
        } else {

        }
    }

    private void loadMessagesFromServer(
            Long dialogId,
            Integer count,
            Long maxId,
            Long offsetTime,
            OnMessageLoadListener listener,
            List<MessageEntity> cacheMessages
    ) {
        executor.execute(() -> {

        });
    }



    private void postSuccess(OnMessageLoadListener listener, List<MessageEntity> messageEntities, Boolean hasMore) {
        mainHandler.post(() -> listener.onSuccess(messageEntities, hasMore));
    }
    private void postFailure(OnMessageLoadListener listener, String errorMsg) {
        mainHandler.post(() -> listener.onFailure(errorMsg));
    }
    private void postNoMoreData(OnMessageLoadListener listener) {
        mainHandler.post(listener::onNoMoreData);
    }

}
