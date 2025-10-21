package com.lythe.media.im.messager.queue;

import android.content.Context;
import android.util.Log;

import com.lythe.media.chats.data.entity.MessageConverter;
import com.lythe.media.chats.data.entity.MessageEntity;
import com.lythe.media.chats.data.local.database.AppDatabase;
import com.lythe.media.chats.data.local.dao.MessageDao;
import com.lythe.media.chats.data.repository.MessageRepository;
import com.lythe.media.im.MqttClientManager;
import com.lythe.media.protobuf.ImMessage;
import com.lythe.media.protobuf.ImMessageStatus;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息队列管理器 - 确保消息可靠发送
 * 功能：
 * 1. 消息持久化存储
 * 2. 自动重试机制
 * 3. 优先级队列
 * 4. 批量处理优化
 */
public class MessageQueue {
    private static final String TAG = "MessageQueue";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 1000;
    
    private static volatile MessageQueue instance;
    private final Context context;
    private final MessageDao messageDao;
    private final ExecutorService queueProcessor;
    private final ScheduledExecutorService retryProcessor;
    
    // 高优先级队列（重要消息）
    private final BlockingQueue<QueuedMessage> highPriorityQueue = new LinkedBlockingQueue<>();
    // 普通优先级队列
    private final BlockingQueue<QueuedMessage> normalPriorityQueue = new LinkedBlockingQueue<>();
    
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicInteger processingCount = new AtomicInteger(0);
    private MessageQueue(Context context) {
        this.context = context.getApplicationContext();
        this.messageDao = AppDatabase.Companion.getInstance(this.context).messageDao();
        this.queueProcessor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MessageQueue-Processor");
            t.setDaemon(true);
            return t;
        });
        this.retryProcessor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "MessageQueue-Retry");
                t.setDaemon(true);
                return t;
            }
        });
        startProcessing();
        loadPendingMessages();
    }
    
    public static MessageQueue getInstance(Context context) {
        if (instance == null) {
            synchronized (MessageQueue.class) {
                if (instance == null) {
                    instance = new MessageQueue(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 添加消息到队列
     */
    public void enqueueMessage(ImMessage message, String topic, int qos, boolean isHighPriority) {
        QueuedMessage queuedMessage = new QueuedMessage(message, topic, qos, System.currentTimeMillis(), isHighPriority);
        
        try {
            // 先保存到数据库
            MessageEntity entity = MessageConverter.INSTANCE.fromProto(message, true);;
            entity.setStatus(ImMessageStatus.SENDING_VALUE);
//            messageDao.insert(entity);
            MessageRepository.Companion.getInstance(context).insertMessage(entity);
            // 添加到内存队列
            if (isHighPriority) {
                highPriorityQueue.offer(queuedMessage);
            } else {
                normalPriorityQueue.offer(queuedMessage);
            }
            Log.d(TAG, "Message enqueued: " + message.getMessageId() + ", priority: " + (isHighPriority ? "HIGH" : "NORMAL"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to enqueue message: " + message.getMessageId(), e);
        }
    }
    
    /**
     * 启动队列处理
     */
    private void startProcessing() {
        queueProcessor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    processBatch();
                    Thread.sleep(100); // 避免CPU占用过高
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in queue processing", e);
                }
            }
        });
    }
    
    /**
     * 批量处理消息
     */
    private void processBatch() {
        if (isProcessing.get()) {
            return;
        }
        
        isProcessing.set(true);
        try {
            // 优先处理高优先级消息
            processQueue(highPriorityQueue, true);
            processQueue(normalPriorityQueue, false);
        } finally {
            isProcessing.set(false);
        }
    }
    
    private void processQueue(BlockingQueue<QueuedMessage> queue, boolean isHighPriority) {
        int processed = 0;
        long startTime = System.currentTimeMillis();
        
        while (processed < BATCH_SIZE && (System.currentTimeMillis() - startTime) < BATCH_TIMEOUT_MS) {
            QueuedMessage queuedMessage = queue.poll();
            if (queuedMessage == null) {
                break;
            }
            
            if (shouldRetry(queuedMessage)) {
                processMessage(queuedMessage);
                processed++;
            } else {
                // 超过重试次数，标记为失败
                markMessageAsFailed(queuedMessage);
            }
        }
    }
    
    /**
     * 处理单个消息
     */
    private void processMessage(QueuedMessage queuedMessage) {
        processingCount.incrementAndGet();
        
        try {
            // Ensure MQTT is connected before attempting to publish.
            // If not connected, request a connect and requeue the message after a short delay
            // This avoids calling mqttClientManager.sendMessage when it will immediately callback failure
            MqttClientManager mqttManager = MqttClientManager.getInstance(context);
            if (mqttManager == null || !mqttManager.isConnected()) {
                Log.w(TAG, "MQTT not connected, will request connect and requeue message: " + queuedMessage.message.getMessageId());
                try {
                    if (mqttManager != null) {
                        mqttManager.connect();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error while requesting MQTT connect", e);
                }

                // Requeue after short delay without increasing retryCount (connection-related retry)
                retryProcessor.schedule(() -> {
                    if (queuedMessage.isHighPriority) {
                        highPriorityQueue.offer(queuedMessage);
                    } else {
                        normalPriorityQueue.offer(queuedMessage);
                    }
                }, 500, TimeUnit.MILLISECONDS);

                return; // don't proceed to send now
            }

            // MQTT is connected - proceed to send and let MqttClientManager/deliveryComplete drive final status
            MqttClientManager.getInstance(context).sendMessage(queuedMessage.topic,
                    queuedMessage.message,
                    queuedMessage.qos,
                    new MqttClientManager.Callback() {
                        @Override
                        public void onSendSuccess() {
                            markMessageAsSent(queuedMessage);

                            Log.d(TAG, "Message sent successfully: " + queuedMessage.message.getMessageId());
                        }

                        @Override
                        public void onSendFailed() {
                            Log.e(TAG, "Failed to send message: " + queuedMessage.message.getMessageId());
                            scheduleRetry(queuedMessage);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message: " + queuedMessage.message.getMessageId(), e);
            scheduleRetry(queuedMessage);
        } finally {
            processingCount.decrementAndGet();
        }
    }
    
    /**
     * 安排重试
     */
    private void scheduleRetry(QueuedMessage queuedMessage) {
        queuedMessage.retryCount++;
        queuedMessage.lastRetryTime = System.currentTimeMillis();
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        retryProcessor.schedule(() -> {
            if (queuedMessage.retryCount <= MAX_RETRY_ATTEMPTS) {
                // 重新入队
                if (queuedMessage.isHighPriority) {
                    highPriorityQueue.offer(queuedMessage);
                } else {
                    normalPriorityQueue.offer(queuedMessage);
                }
            } else {
                markMessageAsFailed(queuedMessage);
            }
        }, RETRY_DELAY_MS * queuedMessage.retryCount, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(QueuedMessage queuedMessage) {
        return queuedMessage.retryCount < MAX_RETRY_ATTEMPTS;
    }
    
    /**
     * 标记消息为已发送
     */
    private void markMessageAsSent(QueuedMessage queuedMessage) {
        try {
            MessageEntity entity = MessageConverter.INSTANCE.fromProto(queuedMessage.message, true);
            entity.setStatus(ImMessageStatus.SENT_VALUE);
            messageDao.update(entity);
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark message as sent", e);
        }
    }
    
    /**
     * 标记消息为发送失败
     */
    private void markMessageAsFailed(QueuedMessage queuedMessage) {
        try {
            MessageEntity entity = MessageConverter.INSTANCE.fromProto(queuedMessage.message, true);
            entity.setStatus(ImMessageStatus.FAILED_VALUE);
            messageDao.update(entity);
            Log.w(TAG, "Message marked as failed after " + queuedMessage.retryCount + " retries: " + queuedMessage.message.getMessageId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark message as failed", e);
        }
    }
    
    /**
     * 从数据库加载待发送消息
     */
    private void loadPendingMessages() {
        queueProcessor.execute(() -> {
            try {
                // 加载状态为SENDING的消息
                 List<MessageEntity> pendingMessages = messageDao.getPendingMessages();
                 for (MessageEntity entity : pendingMessages) {
                     ImMessage message = MessageConverter.INSTANCE.toProto(entity);
                     boolean priority = false;
                     try {
                         // 如果实体包含优先级字段，可在此读取；目前默认 false
                         // priority = entity.isHighPriority();
                     } catch (Exception ignore) {}
                     QueuedMessage queuedMessage = new QueuedMessage(message, entity.getTopic(), 1, entity.getTimestamp(), priority);
                     queuedMessage.retryCount = entity.getRetryCount();
                     normalPriorityQueue.offer(queuedMessage);
                 }
                Log.d(TAG, "Loaded pending messages from database");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load pending messages", e);
            }
        });
    }
    
    /**
     * 获取队列状态
     */
    public QueueStatus getQueueStatus() {
        return new QueueStatus(
            highPriorityQueue.size(),
            normalPriorityQueue.size(),
            processingCount.get(),
            isProcessing.get()
        );
    }
    
    /**
     * 清空队列
     */
    public void clearQueue() {
        highPriorityQueue.clear();
        normalPriorityQueue.clear();
        Log.d(TAG, "Message queue cleared");
    }
    
    /**
     * 关闭队列
     */
    public void shutdown() {
        queueProcessor.shutdown();
        retryProcessor.shutdown();
        try {
            if (!queueProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                queueProcessor.shutdownNow();
            }
            if (!retryProcessor.awaitTermination(5, TimeUnit.SECONDS)) {
                retryProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Log.d(TAG, "Message queue shutdown");
    }
    

    /**
     * 队列中的消息
     */
    private static class QueuedMessage {
        final ImMessage message;
        final String topic;
        final int qos;
        final long enqueueTime;
        final boolean isHighPriority;
        
        int retryCount = 0;
        long lastRetryTime = 0;
        
        QueuedMessage(ImMessage message, String topic, int qos, long enqueueTime, boolean isHighPriority) {
            this.message = message;
            this.topic = topic;
            this.qos = qos;
            this.enqueueTime = enqueueTime;
            this.isHighPriority = isHighPriority; // 可以根据消息类型判断
        }
    }
    
    /**
     * 队列状态
     */
    public static class QueueStatus {
        public final int highPriorityCount;     //当前队列中高优先级消息的数量。
        public final int normalPriorityCount;   //当前队列中普通优先级消息的数量。
        public final int processingCount;       //正在处理中消息的数量。
        public final boolean isProcessing;      //指示队列是否正在处理消息的状态。
        
        public QueueStatus(int highPriorityCount, int normalPriorityCount, int processingCount, boolean isProcessing) {
            this.highPriorityCount = highPriorityCount;
            this.normalPriorityCount = normalPriorityCount;
            this.processingCount = processingCount;
            this.isProcessing = isProcessing;
        }
        
        public int getTotalCount() {
            return highPriorityCount + normalPriorityCount;
        }
    }
}
