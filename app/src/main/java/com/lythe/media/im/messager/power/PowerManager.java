package com.lythe.media.im.messager.power;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.lythe.media.im.net.NetworkManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 电池和电源管理器
 * 功能：
 * 1. 电池状态监控
 * 2. 后台服务优化
 * 3. 网络请求优化
 * 4. 智能休眠策略
 * 5. 省电模式适配
 */
public class PowerManager {
    private static final String TAG = "PowerManager";
    private static final long BATTERY_CHECK_INTERVAL = 60000; // 1分钟
    private static final long BACKGROUND_TASK_INTERVAL = 300000; // 5分钟
    private static final int LOW_BATTERY_THRESHOLD = 20; // 20%
    private static final int CRITICAL_BATTERY_THRESHOLD = 10; // 10%
    
    private static volatile PowerManager instance;
    private final Context context;
    private final android.os.PowerManager systemPowerManager;
    private final BatteryManager batteryManager;
    private final ActivityManager activityManager;
    private final ScheduledExecutorService powerExecutor;
    
    private final AtomicBoolean isLowPowerMode = new AtomicBoolean(false);
    private final AtomicBoolean isCriticalBattery = new AtomicBoolean(false);
    private final AtomicInteger batteryLevel = new AtomicInteger(100);
    private final AtomicBoolean isCharging = new AtomicBoolean(false);
    
    private PowerStateListener powerStateListener;
    
    public enum PowerState {
        NORMAL, LOW_BATTERY, CRITICAL_BATTERY, CHARGING
    }
    
    private PowerManager(Context context) {
        this.context = context.getApplicationContext();
        this.systemPowerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.powerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PowerManager-Thread");
            t.setDaemon(true);
            return t;
        });
        
        startBatteryMonitoring();
    }
    
    public static PowerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PowerManager.class) {
                if (instance == null) {
                    instance = new PowerManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 开始电池监控
     */
    private void startBatteryMonitoring() {
        powerExecutor.scheduleWithFixedDelay(this::checkBatteryStatus, 
            0, BATTERY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 检查电池状态
     */
    private void checkBatteryStatus() {
        try {
            Intent batteryIntent = context.registerReceiver(null, 
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                
                if (level != -1 && scale != -1) {
                    int batteryPercent = (level * 100) / scale;
                    boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                     status == BatteryManager.BATTERY_STATUS_FULL;
                    
                    updateBatteryStatus(batteryPercent, charging);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check battery status", e);
        }
    }
    
    /**
     * 更新电池状态
     */
    private void updateBatteryStatus(int level, boolean charging) {
        boolean wasLowPower = isLowPowerMode.get();
        boolean wasCritical = isCriticalBattery.get();
        
        batteryLevel.set(level);
        isCharging.set(charging);
        
        // 更新省电模式状态
        boolean isLowPower = level <= LOW_BATTERY_THRESHOLD && !charging;
        boolean isCritical = level <= CRITICAL_BATTERY_THRESHOLD && !charging;
        
        isLowPowerMode.set(isLowPower);
        isCriticalBattery.set(isCritical);
        
        // 通知状态变化
        if (wasLowPower != isLowPower || wasCritical != isCritical) {
            notifyPowerStateChanged();
        }
        
        Log.d(TAG, "Battery status updated - Level: " + level + "%, Charging: " + charging + 
                  ", Low Power: " + isLowPower + ", Critical: " + isCritical);
    }
    
    /**
     * 通知电源状态变化
     */
    private void notifyPowerStateChanged() {
        if (powerStateListener != null) {
            PowerState state = getCurrentPowerState();
            powerStateListener.onPowerStateChanged(state, batteryLevel.get(), isCharging.get());
        }
    }
    
    /**
     * 获取当前电源状态
     */
    public PowerState getCurrentPowerState() {
        if (isCriticalBattery.get()) {
            return PowerState.CRITICAL_BATTERY;
        } else if (isLowPowerMode.get()) {
            return PowerState.LOW_BATTERY;
        } else if (isCharging.get()) {
            return PowerState.CHARGING;
        } else {
            return PowerState.NORMAL;
        }
    }
    
    /**
     * 检查是否应该减少后台活动
     */
    public boolean shouldReduceBackgroundActivity() {
        return isLowPowerMode.get() || isCriticalBattery.get();
    }
    
    /**
     * 检查是否应该暂停非关键服务
     */
    public boolean shouldPauseNonCriticalServices() {
        return isCriticalBattery.get();
    }
    
    /**
     * 获取建议的网络请求间隔
     */
    public long getRecommendedNetworkInterval() {
        if (isCriticalBattery.get()) {
            return 600000; // 10分钟
        } else if (isLowPowerMode.get()) {
            return 300000; // 5分钟
        } else {
            return 60000; // 1分钟
        }
    }
    
    /**
     * 获取建议的心跳间隔
     */
    public long getRecommendedHeartbeatInterval() {
        if (isCriticalBattery.get()) {
            return 300; // 5分钟
        } else if (isLowPowerMode.get()) {
            return 120; // 2分钟
        } else {
            return 60; // 1分钟
        }
    }
    
    /**
     * 检查是否应该使用数据压缩
     */
    public boolean shouldUseDataCompression() {
        return isLowPowerMode.get() || isCriticalBattery.get();
    }
    
    /**
     * 检查是否应该减少同步频率
     */
    public boolean shouldReduceSyncFrequency() {
        return isLowPowerMode.get() || isCriticalBattery.get();
    }
    
    /**
     * 优化后台任务
     */
    public void optimizeBackgroundTasks() {
        powerExecutor.execute(() -> {
            try {
                Log.d(TAG, "Optimizing background tasks for power saving");
                
                if (shouldReduceBackgroundActivity()) {
                    // 减少后台任务频率
                    reduceBackgroundTaskFrequency();
                    
                    // 暂停非关键服务
                    if (shouldPauseNonCriticalServices()) {
                        pauseNonCriticalServices();
                    }
                }
                
                // 优化网络使用
                optimizeNetworkUsage();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to optimize background tasks", e);
            }
        });
    }
    
    /**
     * 减少后台任务频率
     */
    private void reduceBackgroundTaskFrequency() {
        Log.d(TAG, "Reducing background task frequency");
        // 这里可以实现具体的后台任务频率调整逻辑
    }
    
    /**
     * 暂停非关键服务
     */
    private void pauseNonCriticalServices() {
        Log.d(TAG, "Pausing non-critical services");
        // 这里可以实现暂停非关键服务的逻辑
    }
    
    /**
     * 优化网络使用
     */
    private void optimizeNetworkUsage() {
        try {
            NetworkManager networkManager = NetworkManager.getInstance(context);
            
            // 在低电量模式下，减少网络请求
            if (shouldReduceBackgroundActivity()) {
                // 可以在这里实现网络请求的优化逻辑
                Log.d(TAG, "Optimizing network usage for power saving");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to optimize network usage", e);
        }
    }
    
    /**
     * 检查设备是否处于省电模式
     */
    public boolean isDeviceInPowerSaveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return systemPowerManager.isPowerSaveMode();
        }
        return false;
    }
    
    /**
     * 检查设备是否处于深度睡眠
     */
    public boolean isDeviceInDeepSleep() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return !systemPowerManager.isInteractive();
        }
        return false;
    }
    
    /**
     * 获取内存使用情况
     */
    public MemoryInfo getMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        return new MemoryInfo(
            memoryInfo.totalMem,
            memoryInfo.availMem,
            memoryInfo.threshold,
            memoryInfo.lowMemory
        );
    }
    
    /**
     * 检查是否应该清理内存
     */
    public boolean shouldCleanupMemory() {
        MemoryInfo memoryInfo = getMemoryInfo();
        return memoryInfo.lowMemory || 
               (memoryInfo.availMem < memoryInfo.threshold * 1.5);
    }
    
    /**
     * 设置电源状态监听器
     */
    public void setPowerStateListener(PowerStateListener listener) {
        this.powerStateListener = listener;
    }
    
    /**
     * 移除电源状态监听器
     */
    public void removePowerStateListener() {
        this.powerStateListener = null;
    }
    
    /**
     * 获取电源统计信息
     */
    public PowerStats getPowerStats() {
        return new PowerStats(
            batteryLevel.get(),
            isCharging.get(),
            isLowPowerMode.get(),
            isCriticalBattery.get(),
            isDeviceInPowerSaveMode(),
            isDeviceInDeepSleep()
        );
    }
    
    /**
     * 释放资源
     */
    public void release() {
        powerExecutor.shutdown();
        powerStateListener = null;
        instance = null;
    }
    
    /**
     * 电源状态监听器接口
     */
    public interface PowerStateListener {
        void onPowerStateChanged(PowerState state, int batteryLevel, boolean isCharging);
    }
    
    /**
     * 内存信息
     */
    public static class MemoryInfo {
        public final long totalMem;
        public final long availMem;
        public final long threshold;
        public final boolean lowMemory;
        
        public MemoryInfo(long totalMem, long availMem, long threshold, boolean lowMemory) {
            this.totalMem = totalMem;
            this.availMem = availMem;
            this.threshold = threshold;
            this.lowMemory = lowMemory;
        }
        
        public double getMemoryUsagePercentage() {
            return totalMem > 0 ? (double) (totalMem - availMem) / totalMem * 100 : 0;
        }
    }
    
    /**
     * 电源统计信息
     */
    public static class PowerStats {
        public final int batteryLevel;
        public final boolean isCharging;
        public final boolean isLowPowerMode;
        public final boolean isCriticalBattery;
        public final boolean isDeviceInPowerSaveMode;
        public final boolean isDeviceInDeepSleep;
        
        public PowerStats(int batteryLevel, boolean isCharging, boolean isLowPowerMode, 
                         boolean isCriticalBattery, boolean isDeviceInPowerSaveMode, 
                         boolean isDeviceInDeepSleep) {
            this.batteryLevel = batteryLevel;
            this.isCharging = isCharging;
            this.isLowPowerMode = isLowPowerMode;
            this.isCriticalBattery = isCriticalBattery;
            this.isDeviceInPowerSaveMode = isDeviceInPowerSaveMode;
            this.isDeviceInDeepSleep = isDeviceInDeepSleep;
        }
    }
}
