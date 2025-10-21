package com.lythe.media.im.messager.config;

/**
 * 生产环境配置
 * 集中管理所有生产环境相关的配置参数
 */
public class ProductionConfig {
    
    // MQTT配置
    public static class MQTT {
        public static final String DEFAULT_BROKER_URL = "tcp://your-mqtt-broker.com:1883";
        public static final String SSL_BROKER_URL = "ssl://your-mqtt-broker.com:8883";
        public static final int CONNECTION_TIMEOUT = 10;
        public static final int KEEP_ALIVE_INTERVAL = 60;
        public static final int MAX_INFLIGHT = 100;
        public static final int MAX_RECONNECT_ATTEMPTS = 10;
        public static final long RECONNECT_DELAY_MS = 3000;
        public static final boolean CLEAN_SESSION = true;
    }
    
    // 消息队列配置
    public static class MessageQueue {
        public static final int MAX_RETRY_ATTEMPTS = 3;
        public static final long RETRY_DELAY_MS = 2000;
        public static final int BATCH_SIZE = 10;
        public static final long BATCH_TIMEOUT_MS = 1000;
        public static final int MAX_QUEUE_SIZE = 1000;
    }
    
    // 网络配置
    public static class Network {
        public static final long NETWORK_CHECK_INTERVAL = 30000; // 30秒
        public static final long CONNECTION_TIMEOUT_MS = 10000; // 10秒
        public static final long READ_TIMEOUT_MS = 30000; // 30秒
        public static final long WRITE_TIMEOUT_MS = 30000; // 30秒
        public static final int MAX_RETRY_COUNT = 3;
    }
    
    // 数据库配置
    public static class Database {
        public static final String DATABASE_NAME = "im_production.db";
        public static final int DATABASE_VERSION = 1;
        public static final int MAX_CONNECTIONS = 4;
        public static final long QUERY_TIMEOUT_MS = 5000; // 5秒
        public static final int MAX_CACHE_SIZE = 1000;
    }
    
    // 缓存配置
    public static class Cache {
        public static final int MAX_MEMORY_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
        public static final int MAX_DISK_CACHE_SIZE = 200 * 1024 * 1024; // 200MB
        public static final long CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时
        public static final int MAX_CACHE_ENTRIES = 10000;
    }
    
    // 性能监控配置
    public static class Performance {
        public static final long MEMORY_CHECK_INTERVAL = 30000; // 30秒
        public static final long PERFORMANCE_REPORT_INTERVAL = 300000; // 5分钟
        public static final long MAX_MEMORY_WARNING_THRESHOLD = 100 * 1024 * 1024; // 100MB
        public static final long CRITICAL_MEMORY_THRESHOLD = 150 * 1024 * 1024; // 150MB
        public static final long SLOW_OPERATION_THRESHOLD = 1000; // 1秒
    }
    
    // 日志配置
    public static class Logging {
        public static final int MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
        public static final int MAX_LOG_FILES = 5;
        public static final long LOG_FLUSH_INTERVAL = 30000; // 30秒
        public static final long LOG_CLEANUP_INTERVAL = 7 * 24 * 60 * 60 * 1000; // 7天
    }
    
    // 安全配置
    public static class Security {
        public static final String KEYSTORE_ALIAS = "IM_SECURE_KEY";
        public static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
        public static final int GCM_IV_LENGTH = 12;
        public static final int GCM_TAG_LENGTH = 16;
        public static final long TOKEN_REFRESH_THRESHOLD = 5 * 60 * 1000; // 5分钟
    }
    
    // 电源管理配置
    public static class Power {
        public static final long BATTERY_CHECK_INTERVAL = 60000; // 1分钟
        public static final int LOW_BATTERY_THRESHOLD = 20; // 20%
        public static final int CRITICAL_BATTERY_THRESHOLD = 10; // 10%
        public static final long BACKGROUND_TASK_INTERVAL = 300000; // 5分钟
    }
    
    // 消息压缩配置
    public static class Compression {
        public static final int COMPRESSION_THRESHOLD = 1024; // 1KB
        public static final int MAX_DEDUP_CACHE_SIZE = 10000;
        public static final long DEDUP_CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时
    }
    
    // 重连配置
    public static class Reconnection {
        public static final long INITIAL_RECONNECT_DELAY = 1000; // 1秒
        public static final long MAX_RECONNECT_DELAY = 30000; // 30秒
        public static final double BACKOFF_MULTIPLIER = 2.0;
        public static final int MAX_BACKOFF_ATTEMPTS = 10;
    }
    
    // 文件上传配置
    public static class FileUpload {
        public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
        public static final int MAX_CONCURRENT_UPLOADS = 3;
        public static final long UPLOAD_TIMEOUT_MS = 300000; // 5分钟
        public static final int CHUNK_SIZE = 64 * 1024; // 64KB
    }
    
    // 推送配置
    public static class Push {
        public static final long PUSH_TOKEN_REFRESH_INTERVAL = 24 * 60 * 60 * 1000; // 24小时
        public static final int MAX_PUSH_RETRY_ATTEMPTS = 3;
        public static final long PUSH_RETRY_DELAY_MS = 5000; // 5秒
    }
    
    // 同步配置
    public static class Sync {
        public static final long SYNC_INTERVAL = 60000; // 1分钟
        public static final int MAX_SYNC_BATCH_SIZE = 100;
        public static final long SYNC_TIMEOUT_MS = 30000; // 30秒
        public static final int MAX_SYNC_RETRY_ATTEMPTS = 3;
    }
    
    // 健康检查配置
    public static class HealthCheck {
        public static final long HEALTH_CHECK_INTERVAL = 60000; // 1分钟
        public static final long HEALTH_CHECK_TIMEOUT_MS = 10000; // 10秒
        public static final int MAX_HEALTH_CHECK_FAILURES = 3;
    }
    
    // 限流配置
    public static class RateLimit {
        public static final int MAX_MESSAGES_PER_MINUTE = 100;
        public static final int MAX_REQUESTS_PER_MINUTE = 200;
        public static final int MAX_CONNECTIONS_PER_IP = 10;
        public static final long RATE_LIMIT_WINDOW_MS = 60000; // 1分钟
    }
    
    // 调试配置（生产环境应设为false）
    public static class Debug {
        public static final boolean ENABLE_DEBUG_LOGS = false;
        public static final boolean ENABLE_PERFORMANCE_LOGS = true;
        public static final boolean ENABLE_NETWORK_LOGS = false;
        public static final boolean ENABLE_DATABASE_LOGS = false;
        public static final boolean ENABLE_CRASH_REPORTING = true;
    }
    
    // 功能开关
    public static class Features {
        public static final boolean ENABLE_MESSAGE_COMPRESSION = true;
        public static final boolean ENABLE_MESSAGE_DEDUPLICATION = true;
        public static final boolean ENABLE_OFFLINE_MESSAGING = true;
        public static final boolean ENABLE_MESSAGE_QUEUE = true;
        public static final boolean ENABLE_PERFORMANCE_MONITORING = true;
        public static final boolean ENABLE_CRASH_RECOVERY = true;
        public static final boolean ENABLE_POWER_OPTIMIZATION = true;
        public static final boolean ENABLE_SECURE_STORAGE = true;
        public static final boolean ENABLE_NETWORK_MONITORING = true;
    }
}
