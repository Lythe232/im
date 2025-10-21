package com.lythe.media.chats.viewmodel;

import android.app.Application;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lythe.media.chats.data.entity.ConversationEntity;
import com.lythe.media.chats.data.entity.FriendConverter;
import com.lythe.media.chats.data.entity.FriendEntity;
import com.lythe.media.chats.data.entity.FriendListResponse;
import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.chats.data.model.FriendModel;
import com.lythe.media.chats.data.repository.FriendRepository;
import com.lythe.media.chats.data.repository.MessageRepository;
import com.lythe.media.chats.data.repository.base.BaseRemoteRepository;
import com.lythe.media.im.MessageDispatcher;
import com.lythe.media.protobuf.ImMessage;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ChatViewModel extends AndroidViewModel {
    private final static String TAG = "ChatViewModel";
    private final MutableLiveData<List<ConversationEntity>> _chatListLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<FriendModel>> friendListLiveData = new MutableLiveData<>();
    private final MessageRepository messageRepository;
    private final FriendRepository friendRepository;
    private final ExecutorService processorExecutor = Executors.newSingleThreadExecutor();
    private MessageDispatcher.MessageSubscriber messageSubscriber;
    public ChatViewModel(@NonNull Application application) {
        super(application);
        this.messageRepository = MessageRepository.Companion.getInstance(application);
        this.friendRepository = FriendRepository.Companion.getInstance(application);
        this.messageSubscriber = new MessageDispatcher.MessageSubscriber() {
            @Override
            public void onMessageReceived(MessageEntity message) {
                handleNewMessage(message);
            }

            @Override
            public boolean needMainThread() {
                return false;
            }
        };
        MessageDispatcher.getInstance().subscribe(messageSubscriber);
    }

    public LiveData<List<ConversationEntity>> getChatList() {
        return _chatListLiveData;
    }
    public LiveData<List<FriendModel>> getFriendModels() { return friendListLiveData; }
    public void loadChats() {
        processorExecutor.execute(() -> {
            List<ConversationEntity> allConversations = messageRepository.getAllConversations();
            _chatListLiveData.postValue(allConversations);
        });
    }
    public void loadFriends() {
        friendRepository.getLocalFriendList(new BaseRemoteRepository.Callback<List<FriendModel>>() {
            @Override
            public void onSuccess(List<FriendModel> data) {
                friendListLiveData.postValue(data);
            }

            @Override
            public void onError(Throwable t) {
                friendRepository.getRemoteFriendList(new BaseRemoteRepository.Callback<FriendListResponse>() {
                    @Override
                    public void onSuccess(FriendListResponse data) {
                        List<@NotNull FriendEntity> friendEntities = data.getData();
                        List<@NotNull FriendModel> friendModels = FriendConverter.INSTANCE.fromEntityList(friendEntities);
                        friendListLiveData.postValue(friendModels);
                    }
                    @Override
                    public void onError(Throwable t) {
                        Log.d(TAG, "loadFriends error");
                    }
                });
            }
        });
    }
    public void handleNewMessage(MessageEntity message) {
        List<ConversationEntity> currentList = _chatListLiveData.getValue();
        if(currentList == null) {
            currentList = new ArrayList<>();
        }
        ConversationEntity existingConversation = findExistingConversation(currentList, message.getFromUid());
        if(existingConversation != null) {
            updateExistingConversation(currentList, existingConversation, message);
        } else {
            addNewConversation(currentList, message);
        }
        sortAndUpdateList(currentList);
    }
    private void updateExistingConversation(
            List<ConversationEntity> conversationEntities,
            ConversationEntity conversationEntity,
            MessageEntity message
    ) {
        conversationEntities.remove(conversationEntity);
        ConversationEntity entity = new ConversationEntity(
                conversationEntity.getConversationId(),
                conversationEntity.getConversationType(),
                conversationEntity.getConversationName(),
                message.getContent(),
                message.getTimestamp(),
                conversationEntity.getUnreadCount() + 1,
                true
        );
        conversationEntities.add(entity);
    }
    private void addNewConversation(List<ConversationEntity> conversationEntities, MessageEntity message) {
        ConversationEntity entity = new ConversationEntity(
                message.getFromUid(),
                message.getConversationType(),
                messageRepository.getConversationName(message.getConversationId(), message.getConversationType()),
                message.getContent(),
                message.getTimestamp(), 1, true);
        conversationEntities.add(entity);
    }
    public ConversationEntity findExistingConversation(List<ConversationEntity> conversationEntities, String senderId) {
        for(ConversationEntity entity : conversationEntities) {
            if(entity.getConversationId().equals(senderId)) {
                return entity;
            }
        }
        return null;
    }
    private void sortAndUpdateList(List<ConversationEntity> conversationEntities) {
        // 直接在主线程排序，避免不必要的线程切换
        conversationEntities.sort((c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
        _chatListLiveData.postValue(new ArrayList<>(conversationEntities)); // 创建新列表避免并发修改
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        shutdownExecutor();
        MessageDispatcher.getInstance().unsubscribe(messageSubscriber);
    }
    private void shutdownExecutor() {
        if(!processorExecutor.isShutdown()) {
            processorExecutor.shutdown();
            try {
                if (!processorExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    processorExecutor.shutdownNow(); // 强制立即关闭
                }
            } catch (InterruptedException e) {
                processorExecutor.shutdownNow();
            }
        }
    }

}
