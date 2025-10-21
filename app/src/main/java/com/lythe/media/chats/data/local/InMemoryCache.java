package com.lythe.media.chats.data.local;

import com.lythe.media.chats.data.entity.MessageEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCache implements LocalMessageCache{
    private ConcurrentHashMap<Long, List<MessageEntity>> cache = new ConcurrentHashMap<>();

    @Override
    public List<MessageEntity> getMessagesFromCache(Long dialogId, Long maxId, Integer count) {
        List<MessageEntity> allMessages = cache.get(dialogId);
        if(allMessages == null || allMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessageEntity> result = new ArrayList<>();
        for(MessageEntity msg : allMessages) {
            if(maxId == null || msg.getTimestamp() < maxId) {
                result.add(msg);
                if(count != null && result.size() >= count) {
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void saveMessageToCache(MessageEntity message) {
        if(message == null) {
            return;
        }
        Long dialogId = Long.parseLong(message.getConversationId());
        List<MessageEntity> allMessages = cache.getOrDefault(dialogId, new ArrayList<>());
        
        // 检查是否已存在，避免重复
        boolean exists = allMessages.stream()
            .anyMatch(msg -> msg.getMsgId().equals(message.getMsgId()));
        
        if(!exists) {
            allMessages.add(message);
            // 按时间戳排序，最新的在前
            allMessages.sort((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
            
            // 限制缓存大小，避免内存溢出
            if(allMessages.size() > 1000) {
                allMessages = allMessages.subList(0, 1000);
            }
            
            cache.put(dialogId, allMessages);
        }
    }
}
