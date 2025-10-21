package com.lythe.media;


import android.app.Application;


import com.lythe.media.chats.utils.MqttMessageSender;
import com.lythe.media.chats.utils.OkHttpFileUploader;
import com.lythe.media.chats.utils.SendMessagesHelper;
import com.lythe.media.im.messager.compression.MessageCompressor;
import com.lythe.media.im.messager.queue.MessageQueue;
import com.lythe.media.im.net.AuthManager;
import com.lythe.media.im.net.RetrofitClient;
import com.lythe.media.im.service.MqttForegroundService;
import com.lythe.media.im.utils.MqttServiceManager;
import com.lythe.media.im.messager.logging.Logger;
import com.lythe.media.im.messager.monitor.PerformanceMonitor;
import com.lythe.media.im.messager.recovery.CrashRecovery;
import com.lythe.media.im.net.NetworkManager;
import com.lythe.media.im.messager.power.PowerManager;
import com.lythe.media.im.messager.security.SecureStorage;

public class ImApplication extends Application {
    private static final String TAG = "ImApplication";
    private Logger logger;
    private PerformanceMonitor performanceMonitor;
    private CrashRecovery crashRecovery;
    private NetworkManager networkManager;
    private PowerManager powerManager;
    private SecureStorage secureStorage;
    private MessageQueue messageQueue;
    
    @Override
     public void onCreate() {
        super.onCreate();
        
        // 初始化日志系统
        logger = Logger.getInstance(this);
        logger.info(TAG, "Application starting up");
        
        // 初始化性能监控
        performanceMonitor = PerformanceMonitor.getInstance(this);
        performanceMonitor.startMonitoring();
        
        // 初始化崩溃恢复
        crashRecovery = CrashRecovery.getInstance(this);
        crashRecovery.performStartupRecovery();
        
        // 初始化网络管理器
        networkManager = NetworkManager.getInstance(this);
        
        // 初始化电源管理器
        powerManager = PowerManager.getInstance(this);
        
        // 初始化安全存储
        secureStorage = SecureStorage.getInstance(this);

        messageQueue = MessageQueue.getInstance(this);

        // 初始化原有组件
        RetrofitClient.INSTANCE.init(getApplicationContext());
        SendMessagesHelper.getInstance().initMessageHelper(new OkHttpFileUploader(),
                new MqttMessageSender(getApplicationContext()),
                getApplicationContext());
        MqttServiceManager.getInstance(getApplicationContext()).initialize();
        initAuthInfo();
        
        logger.info(TAG, "Application startup completed");
    }
    public void setupListeners() {
        networkManager.setNetworkStateListener(new NetworkManager.NetworkStateListener() {
            @Override
            public void onNetworkStateChanged(boolean isAvailable, NetworkManager.NetworkType networkType, int quality) {
                logger.info(TAG, "Network state changed: " + networkType + ", quality: " + quality);
                if(isAvailable) {

                } else {

                }
            }
        });
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
    @Override
    public void onTerminate() {
        super.onTerminate();
        logger.info(TAG, "Application terminating");
        
        // 停止服务
        MqttForegroundService.stop(ImApplication.this);
        
        // 释放资源
        if (performanceMonitor != null) {
            performanceMonitor.release();
        }
        if (crashRecovery != null) {
            crashRecovery.release();
        }
        if (networkManager != null) {
            networkManager.release();
        }
        if (powerManager != null) {
            powerManager.release();
        }
        if (secureStorage != null) {
            secureStorage.release();
        }
        if (logger != null) {
            logger.release();
        }
    }

    private void initAuthInfo() {
        new Thread(() -> {
            try {
                performanceMonitor.startOperation("initAuthInfo");
                AuthManager.getInstance().ensureAuthInfo(new AuthManager.TokenRefreshCallback() {
                    @Override
                    public void onTokenRefreshed(boolean success) {
                        logger.info(TAG, "Preload auth info: " + (success ? "success" : "failed"));
                    }
                });
            } finally {
                performanceMonitor.endOperation("initAuthInfo");
            }
        }).start();
    }
    
    // Getter方法供其他组件使用
    public Logger getLogger() {
        return logger;
    }
    
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    public CrashRecovery getCrashRecovery() {
        return crashRecovery;
    }
    
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
    
    public PowerManager getPowerManager() {
        return powerManager;
    }
    
    public SecureStorage getSecureStorage() {
        return secureStorage;
    }

    public MessageQueue getMessageQueue() { return messageQueue; }
}
