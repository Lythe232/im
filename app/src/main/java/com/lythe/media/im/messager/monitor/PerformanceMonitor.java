package com.lythe.media.im.messager.monitor;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控器
 * 功能：
 * 1. 内存使用监控
 * 2. 数据库操作性能监控
 * 3. 网络请求性能监控
 * 4. 崩溃检测和恢复
 * 5. 性能报告生成
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    private static final long MEMORY_CHECK_INTERVAL = 30000; // 30秒
    private static final long PERFORMANCE_REPORT_INTERVAL = 300000; // 5分钟
    private static final long MAX_MEMORY_WARNING_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final long CRITICAL_MEMORY_THRESHOLD = 150 * 1024 * 1024; // 150MB
    
    private static volatile PerformanceMonitor instance;
    private final Context context;
    private final ScheduledExecutorService monitorExecutor;
    private final Handler mainHandler;
    
    // 性能统计
    private final ConcurrentHashMap<String, OperationStats> operationStats = new ConcurrentHashMap<>();
    private final AtomicLong totalMemoryUsed = new AtomicLong(0);
    private final AtomicLong peakMemoryUsed = new AtomicLong(0);
    private final AtomicLong gcCount = new AtomicLong(0);
    
    // 监控状态
    private volatile boolean isMonitoring = false;
    private volatile long lastGcTime = 0;
    
    private PerformanceMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceMonitor-Thread");
            t.setDaemon(true);
            return t;
        });
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static PerformanceMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (PerformanceMonitor.class) {
                if (instance == null) {
                    instance = new PerformanceMonitor(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 开始监控
     */
    public void startMonitoring() {
        if (isMonitoring) {
            return;
        }
        
        isMonitoring = true;
        Log.d(TAG, "Performance monitoring started");
        
        // 定期检查内存使用
        monitorExecutor.scheduleWithFixedDelay(this::checkMemoryUsage, 
            MEMORY_CHECK_INTERVAL, MEMORY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        
        // 定期生成性能报告
        monitorExecutor.scheduleWithFixedDelay(this::generatePerformanceReport, 
            PERFORMANCE_REPORT_INTERVAL, PERFORMANCE_REPORT_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        isMonitoring = false;
        Log.d(TAG, "Performance monitoring stopped");
    }
    
    /**
     * 记录操作开始时间
     */
    public void startOperation(String operationName) {
        if (!isMonitoring) {
            return;
        }
        
        OperationStats stats = operationStats.computeIfAbsent(operationName, 
            k -> new OperationStats());
        stats.startTime = System.currentTimeMillis();
        stats.activeCount.incrementAndGet();
    }
    
    /**
     * 记录操作结束时间
     */
    public void endOperation(String operationName) {
        if (!isMonitoring) {
            return;
        }
        
        OperationStats stats = operationStats.get(operationName);
        if (stats != null) {
            long duration = System.currentTimeMillis() - stats.startTime;
            stats.totalTime.addAndGet(duration);
            stats.operationCount.incrementAndGet();
            stats.activeCount.decrementAndGet();
            
            // 记录最大耗时
            long currentMax = stats.maxTime.get();
            while (duration > currentMax && !stats.maxTime.compareAndSet(currentMax, duration)) {
                currentMax = stats.maxTime.get();
            }
            
            // 记录最小耗时
            long currentMin = stats.minTime.get();
            while (duration < currentMin && !stats.minTime.compareAndSet(currentMin, duration)) {
                currentMin = stats.minTime.get();
            }
            
            // 检查是否超过警告阈值
            if (duration > 1000) { // 超过1秒
                Log.w(TAG, "Slow operation detected: " + operationName + " took " + duration + "ms");
            }
        }
    }
    
    /**
     * 记录数据库操作
     */
    public void recordDatabaseOperation(String operation, long duration) {
        if (!isMonitoring) {
            return;
        }
        
        String key = "DB_" + operation;
        OperationStats stats = operationStats.computeIfAbsent(key, k -> new OperationStats());
        stats.totalTime.addAndGet(duration);
        stats.operationCount.incrementAndGet();
        
        if (duration > 100) { // 数据库操作超过100ms记录警告
            Log.w(TAG, "Slow database operation: " + operation + " took " + duration + "ms");
        }
    }
    
    /**
     * 记录网络请求
     */
    public void recordNetworkRequest(String url, long duration, boolean success) {
        if (!isMonitoring) {
            return;
        }
        
        String key = "NET_" + url;
        OperationStats stats = operationStats.computeIfAbsent(key, k -> new OperationStats());
        stats.totalTime.addAndGet(duration);
        stats.operationCount.incrementAndGet();
        
        if (success) {
            stats.successCount.incrementAndGet();
        } else {
            stats.failureCount.incrementAndGet();
        }
        
        if (duration > 5000) { // 网络请求超过5秒记录警告
            Log.w(TAG, "Slow network request: " + url + " took " + duration + "ms");
        }
    }
    
    /**
     * 检查内存使用情况
     */
    private void checkMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            totalMemoryUsed.set(usedMemory);
            
            // 更新峰值内存使用
            long currentPeak = peakMemoryUsed.get();
            while (usedMemory > currentPeak && !peakMemoryUsed.compareAndSet(currentPeak, usedMemory)) {
                currentPeak = peakMemoryUsed.get();
            }
            
            // 检查内存警告
            if (usedMemory > MAX_MEMORY_WARNING_THRESHOLD) {
                Log.w(TAG, "High memory usage: " + formatBytes(usedMemory) + 
                          " / " + formatBytes(maxMemory) + 
                          " (" + (usedMemory * 100 / maxMemory) + "%)");
                
                // 如果内存使用过高，建议GC
                if (usedMemory > CRITICAL_MEMORY_THRESHOLD) {
                    suggestGarbageCollection();
                }
            }
            
            // 检查是否需要GC
            long timeSinceLastGc = System.currentTimeMillis() - lastGcTime;
            if (timeSinceLastGc > 60000 && usedMemory > maxMemory * 0.8) { // 1分钟未GC且内存使用超过80%
                suggestGarbageCollection();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to check memory usage", e);
        }
    }
    
    /**
     * 建议进行垃圾回收
     */
    private void suggestGarbageCollection() {
        Log.d(TAG, "Suggesting garbage collection");
        gcCount.incrementAndGet();
        lastGcTime = System.currentTimeMillis();
        
        // 在后台线程执行GC
        monitorExecutor.execute(() -> {
            System.gc();
            Log.d(TAG, "Garbage collection completed");
        });
    }
    
    /**
     * 生成性能报告
     */
    private void generatePerformanceReport() {
        if (!isMonitoring) {
            return;
        }
        
        try {
            StringBuilder report = new StringBuilder();
            report.append("=== Performance Report ===\n");
            
            // 内存使用情况
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            
            report.append("Memory Usage: ").append(formatBytes(usedMemory))
                  .append(" / ").append(formatBytes(maxMemory))
                  .append(" (").append(usedMemory * 100 / maxMemory).append("%)\n");
            
            report.append("Peak Memory: ").append(formatBytes(peakMemoryUsed.get())).append("\n");
            report.append("GC Count: ").append(gcCount.get()).append("\n");
            
            // 操作统计
            report.append("\nOperation Statistics:\n");
            for (String operation : operationStats.keySet()) {
                OperationStats stats = operationStats.get(operation);
                if (stats.operationCount.get() > 0) {
                    double avgTime = (double) stats.totalTime.get() / stats.operationCount.get();
                    report.append(operation).append(": ")
                          .append(stats.operationCount.get()).append(" ops, ")
                          .append(String.format("%.2f", avgTime)).append("ms avg, ")
                          .append(stats.maxTime.get()).append("ms max\n");
                }
            }
            
            Log.i(TAG, report.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate performance report", e);
        }
    }
    
    /**
     * 获取性能统计信息
     */
    public PerformanceStats getPerformanceStats() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        return new PerformanceStats(
            usedMemory,
            maxMemory,
            peakMemoryUsed.get(),
            gcCount.get(),
            operationStats.size()
        );
    }
    
    /**
     * 获取操作统计信息
     */
    public OperationStats getOperationStats(String operationName) {
        return operationStats.get(operationName);
    }
    
    /**
     * 清理统计信息
     */
    public void clearStats() {
        operationStats.clear();
        totalMemoryUsed.set(0);
        peakMemoryUsed.set(0);
        gcCount.set(0);
        Log.d(TAG, "Performance stats cleared");
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopMonitoring();
        monitorExecutor.shutdown();
        operationStats.clear();
        instance = null;
    }
    
    /**
     * 操作统计信息
     */
    public static class OperationStats {
        public volatile long startTime = 0;
        public final AtomicLong totalTime = new AtomicLong(0);
        public final AtomicLong operationCount = new AtomicLong(0);
        public final AtomicLong activeCount = new AtomicLong(0);
        public final AtomicLong successCount = new AtomicLong(0);
        public final AtomicLong failureCount = new AtomicLong(0);
        public final AtomicLong maxTime = new AtomicLong(0);
        public final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        
        public double getAverageTime() {
            long count = operationCount.get();
            return count > 0 ? (double) totalTime.get() / count : 0;
        }
        
        public double getSuccessRate() {
            long total = successCount.get() + failureCount.get();
            return total > 0 ? (double) successCount.get() / total * 100 : 0;
        }
    }
    
    /**
     * 性能统计信息
     */
    public static class PerformanceStats {
        public final long currentMemoryUsed;
        public final long maxMemory;
        public final long peakMemoryUsed;
        public final long gcCount;
        public final int operationCount;
        
        public PerformanceStats(long currentMemoryUsed, long maxMemory, long peakMemoryUsed, 
                              long gcCount, int operationCount) {
            this.currentMemoryUsed = currentMemoryUsed;
            this.maxMemory = maxMemory;
            this.peakMemoryUsed = peakMemoryUsed;
            this.gcCount = gcCount;
            this.operationCount = operationCount;
        }
        
        public double getMemoryUsagePercentage() {
            return maxMemory > 0 ? (double) currentMemoryUsed / maxMemory * 100 : 0;
        }
    }
}
