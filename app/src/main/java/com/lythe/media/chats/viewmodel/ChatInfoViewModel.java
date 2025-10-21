package com.lythe.media.chats.viewmodel;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.lythe.media.chats.data.entity.MessageConverter;
import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.chats.data.repository.MessageRepository;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatInfoViewModel extends AndroidViewModel {
    private final static String TAG = "ChatInfoViewModel";
    private MutableLiveData<List<MessageEntity>> messages = new MutableLiveData<>();
    private List<MessageEntity> loadedMessages = new CopyOnWriteArrayList<>();
    private MessageRepository messageRepository;
//    private int currentPage = 1; // 当前页数  //不能用分页
    private final int pageSize = 20; // 每页加载的数据量
    private Long earliestTimestamp = Long.MAX_VALUE;
    private Boolean hasMoreHistory = true;
    private Integer currentLoadedMessageIndex = 0;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> toastMsg = new MutableLiveData<>();

    public ChatInfoViewModel(@NonNull Application application) {
        super(application);
        messageRepository = MessageRepository.Companion.getInstance(application);
    }

    public void loadAllMessages(String conversationId) {
        Executors.newSingleThreadExecutor().execute(()-> {
            List<@NotNull MessageEntity> allMessages = messageRepository.getAllMessages(conversationId);
            Log.d(TAG, allMessages.toString());
            messages.postValue(allMessages);
        });
    }
    public void loadMessagePaged(String conversationId) {
        if(!hasMoreHistory) {
            toastMsg.postValue("没有更多历史消息了");
            return;
        }
        isLoading.postValue(true);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<@NotNull MessageEntity> messagesPages = messageRepository
                        .getMessagesPages(conversationId, pageSize, earliestTimestamp);
                if(messagesPages != null && !messagesPages.isEmpty()) {
                    loadedMessages.addAll(currentLoadedMessageIndex, messagesPages);
                    currentLoadedMessageIndex += messagesPages.size();
                    earliestTimestamp = messagesPages.stream()
                                    .mapToLong(MessageEntity::getTimestamp)
                                            .min()
                                                    .orElse(earliestTimestamp);
                    messages.postValue(new ArrayList<>(loadedMessages));
                    if(messagesPages.size() < pageSize) {
                        hasMoreHistory = false;
                    }
                } else {
                    hasMoreHistory = false;
                    toastMsg.postValue("没有更多历史消息了");
                }
            } catch (Exception e) {
                toastMsg.postValue("加载失败，请重试");
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public LiveData<List<MessageEntity>> getMessages() {
        return messages;
    }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMsg() { return toastMsg; }
}
