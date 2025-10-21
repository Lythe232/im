package com.lythe.media.chats.data.local;

import android.content.Context;

import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.chats.data.repository.MessageRepository;

import java.util.Collections;
import java.util.List;

public class RoomCache implements LocalMessageCache{
    private MessageRepository messageRepository;

    public RoomCache(Context context) {
        messageRepository = MessageRepository.Companion.getInstance(context.getApplicationContext());
    }
    @Override
    public List<MessageEntity> getMessagesFromCache(Long dialogId, Long maxId, Integer count) {
        return Collections.emptyList();
    }

    @Override
    public void saveMessageToCache(MessageEntity message) {
        if(message == null) {
            return;
        }
        messageRepository.insertMessage(message);
    }
}
