package com.lythe.media.im.messager.logging;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 高级日志管理器
 * 功能：
 * 1. 分级日志记录
 * 2. 文件日志输出
 * 3. 日志轮转和清理
 * 4. 性能监控日志
 * 5. 崩溃日志收集
 */
public class Logger {
    private static final String TAG = "IM_Logger";
    private static final int MAX_LOG_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_LOG_FILES = 5;
    private static final long LOG_FLUSH_INTERVAL = 30000; // 30秒
    
    public enum LogLevel {
        VERBOSE(0), DEBUG(1), INFO(2), WARN(3), ERROR(4), FATAL(5);
        
        private final int value;
        LogLevel(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    private static volatile Logger instance;
    private final Context context;
    private final ExecutorService logExecutor;
    private final BlockingQueue<LogEntry> logQueue;
    private final AtomicBoolean isLogging = new AtomicBoolean(false);
    
    private LogLevel currentLogLevel = LogLevel.DEBUG;
    private File logDirectory;
    private File currentLogFile;
    private SimpleDateFormat dateFormat;
    
    private Logger(Context context) {
        this.context = context.getApplicationContext();
        this.logExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Logger-Thread");
            t.setDaemon(true);
            return t;
        });
        this.logQueue = new LinkedBlockingQueue<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        
        initLogDirectory();
        startLogProcessor();
    }
    
    public static Logger getInstance(Context context) {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化日志目录
     */
    private void initLogDirectory() {
        try {
            logDirectory = new File(context.getExternalFilesDir(null), "logs");
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            
            // 创建当前日志文件
            String fileName = "im_log_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".txt";
            currentLogFile = new File(logDirectory, fileName);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize log directory", e);
        }
    }
    
    /**
     * 启动日志处理器
     */
    private void startLogProcessor() {
        isLogging.set(true);
        logExecutor.execute(() -> {
            while (isLogging.get()) {
                try {
                    LogEntry entry = logQueue.poll(1, TimeUnit.SECONDS);
                    if (entry != null) {
                        processLogEntry(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in log processor", e);
                }
            }
        });
    }
    
    /**
     * 处理日志条目
     */
    private void processLogEntry(LogEntry entry) {
        try {
            // 输出到控制台
            outputToConsole(entry);
            
            // 输出到文件
            outputToFile(entry);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to process log entry", e);
        }
    }
    
    /**
     * 输出到控制台
     */
    private void outputToConsole(LogEntry entry) {
        String message = formatLogMessage(entry);
        
        switch (entry.level) {
            case VERBOSE:
                Log.v(entry.tag, message);
                break;
            case DEBUG:
                Log.d(entry.tag, message);
                break;
            case INFO:
                Log.i(entry.tag, message);
                break;
            case WARN:
                Log.w(entry.tag, message);
                break;
            case ERROR:
                Log.e(entry.tag, message);
                break;
            case FATAL:
                Log.wtf(entry.tag, message);
                break;
        }
    }
    
    /**
     * 输出到文件
     */
    private void outputToFile(LogEntry entry) {
        if (currentLogFile == null) {
            return;
        }
        
        try {
            // 检查文件大小，如果过大则轮转
            if (currentLogFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFile();
            }
            
            String message = formatLogMessage(entry) + "\n";
            
            try (FileWriter writer = new FileWriter(currentLogFile, true)) {
                writer.write(message);
                writer.flush();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    /**
     * 轮转日志文件
     */
    private void rotateLogFile() {
        try {
            // 删除最旧的日志文件
            File[] logFiles = logDirectory.listFiles((dir, name) -> name.startsWith("im_log_") && name.endsWith(".txt"));
            if (logFiles != null && logFiles.length >= MAX_LOG_FILES) {
                // 按修改时间排序，删除最旧的
                java.util.Arrays.sort(logFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
                for (int i = 0; i < logFiles.length - MAX_LOG_FILES + 1; i++) {
                    logFiles[i].delete();
                }
            }
            
            // 创建新的日志文件
            String fileName = "im_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".txt";
            currentLogFile = new File(logDirectory, fileName);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate log file", e);
        }
    }
    
    /**
     * 格式化日志消息
     */
    private String formatLogMessage(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(dateFormat.format(new Date(entry.timestamp)));
        sb.append(" [").append(entry.level.name()).append("] ");
        sb.append(entry.tag).append(": ");
        sb.append(entry.message);
        
        if (entry.throwable != null) {
            sb.append("\n").append(Log.getStackTraceString(entry.throwable));
        }
        
        return sb.toString();
    }
    
    /**
     * 记录日志
     */
    private void log(LogLevel level, String tag, String message, Throwable throwable) {
        if (level.getValue() < currentLogLevel.getValue()) {
            return;
        }
        
        LogEntry entry = new LogEntry(level, tag, message, throwable, System.currentTimeMillis());
        
        try {
            logQueue.offer(entry, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 记录详细日志
     */
    public void verbose(String tag, String message) {
        log(LogLevel.VERBOSE, tag, message, null);
    }
    
    /**
     * 记录调试日志
     */
    public void debug(String tag, String message) {
        log(LogLevel.DEBUG, tag, message, null);
    }
    
    /**
     * 记录信息日志
     */
    public void info(String tag, String message) {
        log(LogLevel.INFO, tag, message, null);
    }
    
    /**
     * 记录警告日志
     */
    public void warn(String tag, String message) {
        log(LogLevel.WARN, tag, message, null);
    }
    
    /**
     * 记录错误日志
     */
    public void error(String tag, String message) {
        log(LogLevel.ERROR, tag, message, null);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public void error(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message, throwable);
    }
    
    /**
     * 记录致命错误日志
     */
    public void fatal(String tag, String message) {
        log(LogLevel.FATAL, tag, message, null);
    }
    
    /**
     * 记录致命错误日志（带异常）
     */
    public void fatal(String tag, String message, Throwable throwable) {
        log(LogLevel.FATAL, tag, message, throwable);
    }
    
    /**
     * 记录性能日志
     */
    public void performance(String operation, long duration) {
        String message = String.format("Operation '%s' took %dms", operation, duration);
        if (duration > 1000) {
            warn("PERFORMANCE", message);
        } else {
            debug("PERFORMANCE", message);
        }
    }
    
    /**
     * 记录网络日志
     */
    public void network(String url, long duration, boolean success) {
        String message = String.format("Network request to '%s' took %dms, success: %s", url, duration, success);
        if (duration > 5000 || !success) {
            warn("NETWORK", message);
        } else {
            debug("NETWORK", message);
        }
    }
    
    /**
     * 记录数据库日志
     */
    public void database(String operation, long duration) {
        String message = String.format("Database operation '%s' took %dms", operation, duration);
        if (duration > 100) {
            warn("DATABASE", message);
        } else {
            debug("DATABASE", message);
        }
    }
    
    /**
     * 设置日志级别
     */
    public void setLogLevel(LogLevel level) {
        this.currentLogLevel = level;
    }
    
    /**
     * 获取当前日志级别
     */
    public LogLevel getLogLevel() {
        return currentLogLevel;
    }
    
    /**
     * 清理旧日志文件
     */
    public void cleanupOldLogs() {
        logExecutor.execute(() -> {
            try {
                if (logDirectory == null || !logDirectory.exists()) {
                    return;
                }
                
                File[] logFiles = logDirectory.listFiles((dir, name) -> name.startsWith("im_log_") && name.endsWith(".txt"));
                if (logFiles == null) {
                    return;
                }
                
                long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 7天前
                int deletedCount = 0;
                
                for (File file : logFiles) {
                    if (file.lastModified() < cutoffTime) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
                
                info(TAG, "Cleaned up " + deletedCount + " old log files");
                
            } catch (Exception e) {
                error(TAG, "Failed to cleanup old logs", e);
            }
        });
    }
    
    /**
     * 获取日志统计信息
     */
    public LogStats getLogStats() {
        return new LogStats(
            logQueue.size(),
            currentLogFile != null ? currentLogFile.length() : 0,
            logDirectory != null ? logDirectory.listFiles().length : 0
        );
    }
    
    /**
     * 释放资源
     */
    public void release() {
        isLogging.set(false);
        logExecutor.shutdown();
        try {
            if (!logExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        instance = null;
    }
    
    /**
     * 日志条目
     */
    private static class LogEntry {
        final LogLevel level;
        final String tag;
        final String message;
        final Throwable throwable;
        final long timestamp;
        
        LogEntry(LogLevel level, String tag, String message, Throwable throwable, long timestamp) {
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 日志统计信息
     */
    public static class LogStats {
        public final int queueSize;
        public final long currentFileSize;
        public final int totalLogFiles;
        
        public LogStats(int queueSize, long currentFileSize, int totalLogFiles) {
            this.queueSize = queueSize;
            this.currentFileSize = currentFileSize;
            this.totalLogFiles = totalLogFiles;
        }
    }
}
