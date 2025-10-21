package com.lythe.media.chats.data.local;

import com.lythe.media.chats.data.entity.MessageEntity;

import java.util.List;

public interface LocalMessageCache {
    //对话ID，最大消息ID，加载数量
    List<MessageEntity> getMessagesFromCache(Long dialogId, Long maxId, Integer count);
    void saveMessageToCache(MessageEntity messages);
}
