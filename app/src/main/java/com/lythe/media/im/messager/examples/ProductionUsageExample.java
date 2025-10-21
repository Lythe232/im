package com.lythe.media.im.messager.examples;

import android.content.Context;

import com.lythe.media.im.messager.logging.Logger;
import com.lythe.media.im.messager.monitor.PerformanceMonitor;
import com.lythe.media.im.net.NetworkManager;
import com.lythe.media.im.messager.power.PowerManager;
import com.lythe.media.im.messager.queue.MessageQueue;
import com.lythe.media.im.messager.recovery.CrashRecovery;
import com.lythe.media.im.messager.security.SecureStorage;
import com.lythe.media.im.messager.compression.MessageCompressor;
import com.lythe.media.protobuf.ImMessage;

/**
 * 生产环境使用示例
 * 展示如何正确使用所有优化组件
 */
public class ProductionUsageExample {
    private static final String TAG = "ProductionExample";
    
    private final Context context;
    private final Logger logger;
    private final PerformanceMonitor performanceMonitor;
    private final NetworkManager networkManager;
    private final PowerManager powerManager;
    private final MessageQueue messageQueue;
    private final CrashRecovery crashRecovery;
    private final SecureStorage secureStorage;
    private final MessageCompressor messageCompressor;
    
    public ProductionUsageExample(Context context) {
        this.context = context;
        
        // 初始化所有组件
        this.logger = Logger.getInstance(context);
        this.performanceMonitor = PerformanceMonitor.getInstance(context);
        this.networkManager = NetworkManager.getInstance(context);
        this.powerManager = PowerManager.getInstance(context);
        this.messageQueue = MessageQueue.getInstance(context);
        this.crashRecovery = CrashRecovery.getInstance(context);
        this.secureStorage = SecureStorage.getInstance(context);
        this.messageCompressor = MessageCompressor.getInstance();
        
        setupListeners();
    }
    
    /**
     * 设置各种监听器
     */
    private void setupListeners() {
        // 网络状态监听
        networkManager.setNetworkStateListener(new NetworkManager.NetworkStateListener() {
            @Override
            public void onNetworkStateChanged(boolean isAvailable, NetworkManager.NetworkType networkType, int quality) {
                logger.info(TAG, "Network state changed: " + networkType + ", quality: " + quality);
                
                if (isAvailable) {
                    // 网络恢复，可以重新发送消息
                    //TODO
//                    messageQueue.processQueue();
                }
            }
        });
        
        // 电源状态监听
        powerManager.setPowerStateListener(new PowerManager.PowerStateListener() {
            @Override
            public void onPowerStateChanged(PowerManager.PowerState state, int batteryLevel, boolean isCharging) {
                logger.info(TAG, "Power state changed: " + state + ", battery: " + batteryLevel + "%");
                
                if (state == PowerManager.PowerState.LOW_BATTERY || 
                    state == PowerManager.PowerState.CRITICAL_BATTERY) {
                    // 低电量模式，优化性能
                    powerManager.optimizeBackgroundTasks();
                }
            }
        });
    }
    
    /**
     * 发送消息的完整流程
     */
    public void sendMessage(ImMessage message, String topic, boolean isHighPriority) {
        performanceMonitor.startOperation("sendMessage");
        
        try {
            // 1. 检查网络状态
            if (!networkManager.isNetworkAvailable()) {
                logger.warn(TAG, "Network not available, message will be queued");
            }
            
            // 2. 提取用于去重的关键信息（按 oneof 分支或通用承载）
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
                    // proto3 对 bytes 字段没有 has*，用是否为空判断
                    if (!message.getContentBytes().isEmpty()) {
                        // 注意：这里只用于快速去重，真实内容应按 content_type 正确解码
                        dedupPayload = message.getContentBytes().toStringUtf8();
                    }
                    break;
            }
            
            // 3. 检查消息去重（使用提取的关键信息）
            if (messageCompressor.isDuplicateMessage(message.getMessageId(), dedupPayload)) {
                logger.warn(TAG, "Duplicate message detected, skipping: " + message.getMessageId());
                return;
            }

            // 4. 添加到消息队列
            messageQueue.enqueueMessage(message, topic, 1, isHighPriority);
            
            logger.info(TAG, "Message enqueued successfully: " + message.getMessageId());
            
        } catch (Exception e) {
            logger.error(TAG, "Failed to send message: " + message.getMessageId(), e);
        } finally {
            performanceMonitor.endOperation("sendMessage");
        }
    }
    
    /**
     * 处理接收到的消息
     */
    public void handleReceivedMessage(String topic, byte[] rawContent) {
        performanceMonitor.startOperation("handleReceivedMessage");
        
        try {
            // 1. 解压消息（如果需要）
            String content = new String(rawContent);
            // 这里需要根据消息格式判断是否压缩
            
            // 2. 检查去重
            String messageId = extractMessageId(content); // 需要实现
            if (messageCompressor.isDuplicateMessage(messageId, content)) {
                logger.warn(TAG, "Duplicate received message, ignoring: " + messageId);
                return;
            }
            
            // 3. 处理消息
            processMessageContent(content);
            
            logger.info(TAG, "Message processed successfully: " + messageId);
            
        } catch (Exception e) {
            logger.error(TAG, "Failed to handle received message", e);
        } finally {
            performanceMonitor.endOperation("handleReceivedMessage");
        }
    }
    
    /**
     * 数据库操作示例
     */
    public void performDatabaseOperation(String operation, Runnable dbOperation) {
        long startTime = System.currentTimeMillis();
        
        try {
            dbOperation.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordDatabaseOperation(operation, duration);
            logger.database(operation, duration);
        }
    }
    
    /**
     * 网络请求示例
     */
    public void performNetworkRequest(String url, Runnable networkOperation) {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        
        try {
            // 检查网络状态
            if (!networkManager.isNetworkAvailable()) {
                throw new IllegalStateException("Network not available");
            }
            
            // 检查电源状态，决定请求频率
            if (powerManager.shouldReduceBackgroundActivity()) {
                logger.info(TAG, "Reducing network activity due to power constraints");
            }
            
            networkOperation.run();
            success = true;
            
        } catch (Exception e) {
            logger.error(TAG, "Network request failed: " + url, e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            performanceMonitor.recordNetworkRequest(url, duration, success);
            logger.network(url, duration, success);
        }
    }
    
    /**
     * 安全存储示例
     */
    public void storeSensitiveData(String key, String value) {
        try {
            boolean success = secureStorage.putSecureString(key, value);
            if (success) {
                logger.info(TAG, "Sensitive data stored securely: " + key);
            } else {
                logger.error(TAG, "Failed to store sensitive data: " + key);
            }
        } catch (Exception e) {
            logger.error(TAG, "Error storing sensitive data: " + key, e);
        }
    }
    
    /**
     * 获取敏感数据示例
     */
    public String getSensitiveData(String key, String defaultValue) {
        try {
            String value = secureStorage.getSecureString(key, defaultValue);
            logger.debug(TAG, "Retrieved sensitive data: " + key);
            return value;
        } catch (Exception e) {
            logger.error(TAG, "Error retrieving sensitive data: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * 性能监控示例
     */
    public void logPerformanceMetrics() {
        // 获取性能统计
        PerformanceMonitor.PerformanceStats stats = performanceMonitor.getPerformanceStats();
        logger.info(TAG, "Memory usage: " + stats.currentMemoryUsed + " / " + stats.maxMemory);
        logger.info(TAG, "GC count: " + stats.gcCount);
        
        // 获取网络统计
        NetworkManager.NetworkQuality quality = NetworkManager.NetworkQuality.values()[networkManager.getNetworkQuality()];
        logger.info(TAG, "Network quality: " + quality);
        
        // 获取电源统计
        PowerManager.PowerStats powerStats = powerManager.getPowerStats();
        logger.info(TAG, "Battery level: " + powerStats.batteryLevel + "%, Low power: " + powerStats.isLowPowerMode);
        
        // 获取消息队列统计
        MessageQueue.QueueStatus queueStatus = messageQueue.getQueueStatus();
        logger.info(TAG, "Message queue: " + queueStatus.getTotalCount() + " messages pending");
        
        // 获取压缩统计
        MessageCompressor.CompressionStats compressionStats = messageCompressor.getCompressionStats();
        logger.info(TAG, "Compression ratio: " + compressionStats.averageCompressionRatio + 
                         ", Space saved: " + compressionStats.getSpaceSavedPercentage() + "%");
    }
    
    /**
     * 健康检查示例
     */
    public void performHealthCheck() {
        logger.info(TAG, "Performing health check");
        
        // 执行崩溃恢复的健康检查
        crashRecovery.performHealthCheck();
        
        // 检查系统状态
        boolean isHealthy = true;
        
        // 检查网络
        if (!networkManager.isNetworkAvailable()) {
            logger.warn(TAG, "Network not available");
            isHealthy = false;
        }
        
        // 检查内存
        if (powerManager.shouldCleanupMemory()) {
            logger.warn(TAG, "Memory cleanup recommended");
            System.gc();
        }
        
        // 检查消息队列
        MessageQueue.QueueStatus queueStatus = messageQueue.getQueueStatus();
        if (queueStatus.getTotalCount() > 1000) {
            logger.warn(TAG, "Message queue is getting large: " + queueStatus.getTotalCount());
        }
        
        logger.info(TAG, "Health check completed, system healthy: " + isHealthy);
    }
    
    /**
     * 清理资源示例
     */
    public void cleanup() {
        logger.info(TAG, "Cleaning up resources");
        
        // 清理日志
        logger.cleanupOldLogs();
        
        // 清理压缩缓存
        messageCompressor.resetStats();
        
        // 清理性能统计
        performanceMonitor.clearStats();
        
        logger.info(TAG, "Cleanup completed");
    }
    
    // 辅助方法
    private String extractMessageId(String content) {
        // 实现消息ID提取逻辑
        return "extracted_id";
    }
    
    private void processMessageContent(String content) {
        // 实现消息内容处理逻辑
    }
}
