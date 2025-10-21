package com.lythe.media.im.messager.compression;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 消息压缩和去重管理器
 * 功能：
 * 1. 消息内容压缩
 * 2. 消息去重检测
 * 3. 压缩率统计
 * 4. 自动清理过期数据
 */
public class MessageCompressor {
    private static final String TAG = "MessageCompressor";
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB以上才压缩
    private static final int MAX_DEDUP_CACHE_SIZE = 10000;
    private static final long DEDUP_CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 24小时
    
    private static volatile MessageCompressor instance;
    private final ScheduledExecutorService cleanupExecutor;
    
    // 去重缓存：消息ID -> 时间戳
    private final ConcurrentHashMap<String, Long> dedupCache = new ConcurrentHashMap<>();
    
    // 压缩统计
    private volatile long totalOriginalSize = 0;
    private volatile long totalCompressedSize = 0;
    private volatile int compressionCount = 0;
    
    private MessageCompressor() {
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MessageCompressor-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // 定期清理过期的去重缓存
        cleanupExecutor.scheduleWithFixedDelay(this::cleanupExpiredCache, 1, 1, TimeUnit.HOURS);
    }
    
    public static MessageCompressor getInstance() {
        if (instance == null) {
            synchronized (MessageCompressor.class) {
                if (instance == null) {
                    instance = new MessageCompressor();
                }
            }
        }
        return instance;
    }
    
    /**
     * 压缩消息内容
     */
    public CompressedMessage compressMessage(String content) {
        if (content == null || content.isEmpty()) {
            return new CompressedMessage(content, false, 0, 0);
        }
        
        byte[] originalBytes = content.getBytes(StandardCharsets.UTF_8);
        
        // 小于阈值不压缩
        if (originalBytes.length < COMPRESSION_THRESHOLD) {
            return new CompressedMessage(content, false, originalBytes.length, originalBytes.length);
        }
        
        try {
            byte[] compressedBytes = compress(originalBytes);
            
            // 如果压缩后反而更大，则不使用压缩
            if (compressedBytes.length >= originalBytes.length) {
                return new CompressedMessage(content, false, originalBytes.length, originalBytes.length);
            }
            
            // 更新统计
            totalOriginalSize += originalBytes.length;
            totalCompressedSize += compressedBytes.length;
            compressionCount++;
            
            String compressedContent = android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP);
            return new CompressedMessage(compressedContent, true, originalBytes.length, compressedBytes.length);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to compress message", e);
            return new CompressedMessage(content, false, originalBytes.length, originalBytes.length);
        }
    }
    
    /**
     * 解压消息内容
     */
    public String decompressMessage(String content, boolean isCompressed) {
        if (content == null || content.isEmpty() || !isCompressed) {
            return content;
        }
        
        try {
            byte[] compressedBytes = android.util.Base64.decode(content, android.util.Base64.NO_WRAP);
            byte[] decompressedBytes = decompress(compressedBytes);
            return new String(decompressedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decompress message", e);
            return content; // 返回原始内容
        }
    }
    
    /**
     * 检查消息是否重复
     */
    public boolean isDuplicateMessage(String messageId, String content) {
        if (messageId == null || content == null) {
            return false;
        }
        
        // 生成内容哈希
        String contentHash = generateContentHash(content);
        String dedupKey = messageId + ":" + contentHash;
        
        long currentTime = System.currentTimeMillis();
        Long lastSeenTime = dedupCache.get(dedupKey);
        
        if (lastSeenTime != null && (currentTime - lastSeenTime) < DEDUP_CACHE_EXPIRE_TIME) {
            Log.d(TAG, "Duplicate message detected: " + messageId);
            return true;
        }
        
        // 更新缓存
        dedupCache.put(dedupKey, currentTime);
        
        // 如果缓存过大，清理最旧的条目
        if (dedupCache.size() > MAX_DEDUP_CACHE_SIZE) {
            cleanupOldestEntries();
        }
        
        return false;
    }
    
    /**
     * 生成内容哈希
     */
    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available", e);
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * GZIP压缩
     */
    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }
    
    /**
     * GZIP解压
     */
    private byte[] decompress(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * 清理过期的去重缓存
     */
    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        dedupCache.entrySet().removeIf(entry -> {
            long age = currentTime - entry.getValue();
            return age > DEDUP_CACHE_EXPIRE_TIME;
        });
        
        Log.d(TAG, "Cleaned up expired dedup cache entries: " + removedCount);
    }
    
    /**
     * 清理最旧的缓存条目
     */
    private void cleanupOldestEntries() {
        int targetSize = MAX_DEDUP_CACHE_SIZE * 3 / 4; // 保留75%
        
        dedupCache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue(), e2.getValue()))
            .limit(dedupCache.size() - targetSize)
            .forEach(entry -> dedupCache.remove(entry.getKey()));
        
        Log.d(TAG, "Cleaned up oldest dedup cache entries, current size: " + dedupCache.size());
    }
    
    /**
     * 获取压缩统计信息
     */
    public CompressionStats getCompressionStats() {
        double compressionRatio = totalOriginalSize > 0 ? 
            (double) totalCompressedSize / totalOriginalSize : 1.0;
        
        return new CompressionStats(
            totalOriginalSize,
            totalCompressedSize,
            compressionCount,
            compressionRatio,
            dedupCache.size()
        );
    }
    
    /**
     * 重置统计信息
     */
    public void resetStats() {
        totalOriginalSize = 0;
        totalCompressedSize = 0;
        compressionCount = 0;
        Log.d(TAG, "Compression stats reset");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        cleanupExecutor.shutdown();
        dedupCache.clear();
        instance = null;
    }
    
    /**
     * 压缩后的消息
     */
    public static class CompressedMessage {
        public final String content;
        public final boolean isCompressed;
        public final int originalSize;
        public final int compressedSize;
        
        public CompressedMessage(String content, boolean isCompressed, int originalSize, int compressedSize) {
            this.content = content;
            this.isCompressed = isCompressed;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }
        
        public double getCompressionRatio() {
            return originalSize > 0 ? (double) compressedSize / originalSize : 1.0;
        }
    }
    
    /**
     * 压缩统计信息
     */
    public static class CompressionStats {
        public final long totalOriginalSize;
        public final long totalCompressedSize;
        public final int compressionCount;
        public final double averageCompressionRatio;
        public final int dedupCacheSize;
        
        public CompressionStats(long totalOriginalSize, long totalCompressedSize, int compressionCount, 
                              double averageCompressionRatio, int dedupCacheSize) {
            this.totalOriginalSize = totalOriginalSize;
            this.totalCompressedSize = totalCompressedSize;
            this.compressionCount = compressionCount;
            this.averageCompressionRatio = averageCompressionRatio;
            this.dedupCacheSize = dedupCacheSize;
        }
        
        public long getSpaceSaved() {
            return totalOriginalSize - totalCompressedSize;
        }
        
        public double getSpaceSavedPercentage() {
            return totalOriginalSize > 0 ? (double) getSpaceSaved() / totalOriginalSize * 100 : 0;
        }
    }
}
