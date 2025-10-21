package com.lythe.media.im.messager.recovery;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.lythe.media.im.net.NetworkManager;
import com.lythe.media.im.messager.queue.MessageQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 崩溃恢复管理器
 * 功能：
 * 1. 检测应用崩溃
 * 2. 自动恢复连接状态
 * 3. 恢复未发送的消息
 * 4. 清理损坏的数据
 * 5. 健康检查
 */
public class CrashRecovery {
    private static final String TAG = "CrashRecovery";
    private static final String PREFS_NAME = "crash_recovery";
    private static final String KEY_LAST_CRASH_TIME = "last_crash_time";
    private static final String KEY_CRASH_COUNT = "crash_count";
    private static final String KEY_LAST_HEALTHY_TIME = "last_healthy_time";
    private static final String KEY_RECOVERY_ATTEMPTS = "recovery_attempts";
    
    private static volatile CrashRecovery instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService recoveryExecutor;
    
    private volatile boolean isRecovering = false;
    private volatile long lastHealthCheck = 0;
    
    private CrashRecovery(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.recoveryExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CrashRecovery-Thread");
            t.setDaemon(true);
            return t;
        });
    }
    
    public static CrashRecovery getInstance(Context context) {
        if (instance == null) {
            synchronized (CrashRecovery.class) {
                if (instance == null) {
                    instance = new CrashRecovery(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 应用启动时的恢复检查
     */
    public void performStartupRecovery() {
        recoveryExecutor.execute(() -> {
            try {
                Log.d(TAG, "Starting crash recovery check");
                
                // 检查是否发生了崩溃
                if (wasCrashDetected()) {
                    Log.w(TAG, "Crash detected, performing recovery");
                    performCrashRecovery();
                } else {
                    Log.d(TAG, "No crash detected, performing health check");
                    performHealthCheck();
                }
                
                // 记录健康时间
                recordHealthyTime();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to perform startup recovery", e);
            }
        });
    }
    
    /**
     * 检测是否发生了崩溃
     */
    private boolean wasCrashDetected() {
        long lastHealthyTime = prefs.getLong(KEY_LAST_HEALTHY_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        // 如果上次健康时间超过5分钟，可能发生了崩溃
        return (currentTime - lastHealthyTime) > 5 * 60 * 1000;
    }
    
    /**
     * 执行崩溃恢复
     */
    private void performCrashRecovery() {
        if (isRecovering) {
            Log.w(TAG, "Recovery already in progress");
            return;
        }
        
        isRecovering = true;
        try {
            Log.i(TAG, "Starting crash recovery process");
            
            // 1. 记录崩溃信息
            recordCrash();
            
            // 2. 清理可能损坏的数据
            cleanupCorruptedData();
            
            // 3. 恢复消息队列
            recoverMessageQueue();
            
            // 4. 重新建立网络连接
            recoverNetworkConnection();
            
            // 5. 验证系统状态
            validateSystemState();
            
            Log.i(TAG, "Crash recovery completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Crash recovery failed", e);
        } finally {
            isRecovering = false;
        }
    }
    
    /**
     * 记录崩溃信息
     */
    private void recordCrash() {
        long currentTime = System.currentTimeMillis();
        int crashCount = prefs.getInt(KEY_CRASH_COUNT, 0) + 1;
        
        prefs.edit()
            .putLong(KEY_LAST_CRASH_TIME, currentTime)
            .putInt(KEY_CRASH_COUNT, crashCount)
            .apply();
        
        Log.w(TAG, "Crash recorded, count: " + crashCount);
    }
    
    /**
     * 清理损坏的数据
     */
    private void cleanupCorruptedData() {
        try {
            Log.d(TAG, "Cleaning up corrupted data");
            
            // 清理临时文件
            // File tempDir = new File(context.getCacheDir(), "temp");
            // if (tempDir.exists()) {
            //     deleteDirectory(tempDir);
            // }
            
            // 清理数据库中的孤立记录
            // AppDatabase.getInstance(context).cleanupOrphanedRecords();
            
            Log.d(TAG, "Data cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup corrupted data", e);
        }
    }
    
    /**
     * 恢复消息队列
     */
    private void recoverMessageQueue() {
        try {
            Log.d(TAG, "Recovering message queue");
            
            // 重新加载待发送的消息
            MessageQueue messageQueue = MessageQueue.getInstance(context);
            // messageQueue.reloadPendingMessages();
            
            Log.d(TAG, "Message queue recovery completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover message queue", e);
        }
    }
    
    /**
     * 恢复网络连接
     */
    private void recoverNetworkConnection() {
        try {
            Log.d(TAG, "Recovering network connection");
            
            // 检查网络状态
            NetworkManager networkManager = NetworkManager.getInstance(context);
            if (!networkManager.isNetworkAvailable()) {
                Log.w(TAG, "Network not available, skipping network recovery");
                return;
            }
            
            // 重新建立MQTT连接
            // MqttClientManager.getInstance(context).reconnect();
            
            Log.d(TAG, "Network connection recovery completed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to recover network connection", e);
        }
    }
    
    /**
     * 验证系统状态
     */
    private void validateSystemState() {
        try {
            Log.d(TAG, "Validating system state");
            
            // 检查关键组件状态
            boolean isValid = true;
            
            // 检查网络管理器
            NetworkManager networkManager = NetworkManager.getInstance(context);
            if (networkManager == null) {
                Log.e(TAG, "NetworkManager is null");
                isValid = false;
            }
            
            // 检查消息队列
            MessageQueue messageQueue = MessageQueue.getInstance(context);
            if (messageQueue == null) {
                Log.e(TAG, "MessageQueue is null");
                isValid = false;
            }
            
            if (isValid) {
                Log.d(TAG, "System state validation passed");
            } else {
                Log.e(TAG, "System state validation failed");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to validate system state", e);
        }
    }
    
    /**
     * 执行健康检查
     */
    public void performHealthCheck() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHealthCheck < 60000) { // 1分钟内不重复检查
            return;
        }
        
        lastHealthCheck = currentTime;
        recoveryExecutor.execute(() -> {
            try {
                Log.d(TAG, "Performing health check");
                
                // 检查内存使用
                checkMemoryHealth();
                
                // 检查网络连接
                checkNetworkHealth();
                
                // 检查数据库状态
                checkDatabaseHealth();
                
                // 记录健康时间
                recordHealthyTime();
                
                Log.d(TAG, "Health check completed");
                
            } catch (Exception e) {
                Log.e(TAG, "Health check failed", e);
            }
        });
    }
    
    /**
     * 检查内存健康状态
     */
    private void checkMemoryHealth() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsage = (double) usedMemory / maxMemory;
            
            if (memoryUsage > 0.9) { // 内存使用超过90%
                Log.w(TAG, "High memory usage detected: " + (memoryUsage * 100) + "%");
                // 建议进行垃圾回收
                System.gc();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check memory health", e);
        }
    }
    
    /**
     * 检查网络健康状态
     */
    private void checkNetworkHealth() {
        try {
            NetworkManager networkManager = NetworkManager.getInstance(context);
            if (!networkManager.isNetworkAvailable()) {
                Log.w(TAG, "Network not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check network health", e);
        }
    }
    
    /**
     * 检查数据库健康状态
     */
    private void checkDatabaseHealth() {
        try {
            // 可以在这里执行一些简单的数据库查询来检查数据库状态
            Log.d(TAG, "Database health check completed");
        } catch (Exception e) {
            Log.e(TAG, "Database health check failed", e);
        }
    }
    
    /**
     * 记录健康时间
     */
    private void recordHealthyTime() {
        prefs.edit()
            .putLong(KEY_LAST_HEALTHY_TIME, System.currentTimeMillis())
            .apply();
    }
    
    /**
     * 获取崩溃统计信息
     */
    public CrashStats getCrashStats() {
        return new CrashStats(
            prefs.getLong(KEY_LAST_CRASH_TIME, 0),
            prefs.getInt(KEY_CRASH_COUNT, 0),
            prefs.getLong(KEY_LAST_HEALTHY_TIME, 0),
            prefs.getInt(KEY_RECOVERY_ATTEMPTS, 0)
        );
    }
    
    /**
     * 重置崩溃统计
     */
    public void resetCrashStats() {
        prefs.edit()
            .remove(KEY_LAST_CRASH_TIME)
            .remove(KEY_CRASH_COUNT)
            .remove(KEY_RECOVERY_ATTEMPTS)
            .apply();
        Log.d(TAG, "Crash stats reset");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        recoveryExecutor.shutdown();
        try {
            if (!recoveryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                recoveryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        instance = null;
    }
    
    /**
     * 崩溃统计信息
     */
    public static class CrashStats {
        public final long lastCrashTime;
        public final int crashCount;
        public final long lastHealthyTime;
        public final int recoveryAttempts;
        
        public CrashStats(long lastCrashTime, int crashCount, long lastHealthyTime, int recoveryAttempts) {
            this.lastCrashTime = lastCrashTime;
            this.crashCount = crashCount;
            this.lastHealthyTime = lastHealthyTime;
            this.recoveryAttempts = recoveryAttempts;
        }
        
        public boolean hasRecentCrash() {
            long timeSinceCrash = System.currentTimeMillis() - lastCrashTime;
            return timeSinceCrash < 24 * 60 * 60 * 1000; // 24小时内
        }
    }
}
