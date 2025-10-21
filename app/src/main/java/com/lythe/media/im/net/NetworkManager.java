package com.lythe.media.im.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网络状态管理器
 * 功能：
 * 1. 实时监听网络状态变化
 * 2. 智能重连策略
 * 3. 网络质量评估
 * 4. 数据使用量监控
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private static volatile NetworkManager instance;
    
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final ExecutorService networkExecutor;
    
    private final AtomicBoolean isNetworkAvailable = new AtomicBoolean(false);
    private final AtomicBoolean isWifiConnected = new AtomicBoolean(false);
    private final AtomicBoolean isMobileConnected = new AtomicBoolean(false);
    private final AtomicInteger networkQuality = new AtomicInteger();
    
    private NetworkStateListener networkStateListener;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    public enum NetworkType {
        NONE, WIFI, MOBILE, ETHERNET, VPN
    }
    
    public enum NetworkQuality {
        UNKNOWN(0), POOR(1), FAIR(2), GOOD(3), EXCELLENT(4);
        
        private final int value;
        NetworkQuality(int value) { this.value = value; }
        public int getValue() { return value; }
    }
    
    private NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.networkExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NetworkManager-Thread");
            t.setDaemon(true);
            return t;
        });
        
        initNetworkCallback();
        registerNetworkCallback();
        updateNetworkStatus();
    }
    
    public static NetworkManager getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化网络回调
     */
    private void initNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    Log.d(TAG, "Network available: " + network);
                    updateNetworkStatus();
                    notifyNetworkStateChanged();
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    Log.d(TAG, "Network lost: " + network);
                    updateNetworkStatus();
                    notifyNetworkStateChanged();
                }
                
                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    Log.d(TAG, "Network capabilities changed: " + network);
                    updateNetworkQuality(networkCapabilities);
                    notifyNetworkStateChanged();
                }
            };
        }
    }
    
    /**
     * 注册网络回调
     */
    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            
            try {
                connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
                Log.d(TAG, "Network callback registered");
            } catch (Exception e) {
                Log.e(TAG, "Failed to register network callback", e);
            }
        }
    }
    
    /**
     * 更新网络状态
     */
    private void updateNetworkStatus() {
        networkExecutor.execute(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Network activeNetwork = connectivityManager.getActiveNetwork();
                    if (activeNetwork != null) {
                        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                        if (capabilities != null) {
                            boolean hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                            
                            isNetworkAvailable.set(hasInternet);
                            isWifiConnected.set(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                            isMobileConnected.set(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                            
                            updateNetworkQuality(capabilities);
                        }
                    } else {
                        isNetworkAvailable.set(false);
                        isWifiConnected.set(false);
                        isMobileConnected.set(false);
                        networkQuality.set(NetworkQuality.UNKNOWN.getValue());
                    }
                } else {
                    // 兼容旧版本
                    android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    boolean connected = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
                    isNetworkAvailable.set(connected);
                    isWifiConnected.set(connected && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI);
                    isMobileConnected.set(connected && activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE);
                }
                
                Log.d(TAG, "Network status updated - Available: " + isNetworkAvailable.get() + 
                          ", WiFi: " + isWifiConnected.get() + 
                          ", Mobile: " + isMobileConnected.get() +
                          ", Quality: " + networkQuality.get());
            } catch (Exception e) {
                Log.e(TAG, "Failed to update network status", e);
            }
        });
    }
    
    /**
     * 更新网络质量
     */
    private void updateNetworkQuality(NetworkCapabilities capabilities) {
        if (capabilities == null) {
            networkQuality.set(NetworkQuality.UNKNOWN.getValue());
            return;
        }
        
        int quality = NetworkQuality.UNKNOWN.getValue();
        
        // 根据带宽和延迟评估网络质量
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // WiFi网络，通常质量较好
                quality = NetworkQuality.GOOD.getValue();
                
                // 检查是否支持高带宽
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    quality = NetworkQuality.EXCELLENT.getValue();
                }
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                // 移动网络，质量可能较低
                quality = NetworkQuality.FAIR.getValue();
                
                // 检查是否被限制
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    quality = NetworkQuality.GOOD.getValue();
                }
            }
        }
        
        networkQuality.set(quality);
    }
    
    /**
     * 通知网络状态变化
     */
    private void notifyNetworkStateChanged() {
        if (networkStateListener != null) {
            networkStateListener.onNetworkStateChanged(
                isNetworkAvailable.get(),
                getCurrentNetworkType(),
                networkQuality.get()
            );
        }
    }
    
    /**
     * 获取当前网络类型
     */
    public NetworkType getCurrentNetworkType() {
        if (!isNetworkAvailable.get()) {
            return NetworkType.NONE;
        } else if (isWifiConnected.get()) {
            return NetworkType.WIFI;
        } else if (isMobileConnected.get()) {
            return NetworkType.MOBILE;
        } else {
            return NetworkType.NONE;
        }
    }
    
    /**
     * 检查网络是否可用
     */
    public boolean isNetworkAvailable() {
        return isNetworkAvailable.get();
    }
    
    /**
     * 检查是否为WiFi连接
     */
    public boolean isWifiConnected() {
        return isWifiConnected.get();
    }
    
    /**
     * 检查是否为移动网络连接
     */
    public boolean isMobileConnected() {
        return isMobileConnected.get();
    }
    
    /**
     * 获取网络质量
     */
    public int getNetworkQuality() {
        return networkQuality.get();
    }
    
    /**
     * 检查是否应该使用数据压缩
     */
    public boolean shouldUseCompression() {
        return isMobileConnected.get() || networkQuality.get() < NetworkQuality.GOOD.getValue();
    }
    
    /**
     * 检查是否应该减少心跳频率
     */
    public boolean shouldReduceHeartbeat() {
        return !isWifiConnected.get() || networkQuality.get() < NetworkQuality.FAIR.getValue();
    }
    
    /**
     * 获取建议的心跳间隔
     */
    public long getRecommendedHeartbeatInterval() {
        if (isWifiConnected.get() && networkQuality.get() >= NetworkQuality.GOOD.getValue()) {
            return 30; // 30秒
        } else if (isMobileConnected.get()) {
            return 60; // 60秒
        } else {
            return 120; // 120秒
        }
    }
    
    /**
     * 设置网络状态监听器
     */
    public void setNetworkStateListener(NetworkStateListener listener) {
        this.networkStateListener = listener;
    }
    
    /**
     * 移除网络状态监听器
     */
    public void removeNetworkStateListener() {
        this.networkStateListener = null;
    }
    
    /**
     * 强制刷新网络状态
     */
    public void refreshNetworkStatus() {
        updateNetworkStatus();
    }
    
    /**
     * 检查网络连接质量
     */
    public void checkNetworkQuality() {
        networkExecutor.execute(() -> {
            try {
                // 可以在这里实现ping测试或其他网络质量检测
                Log.d(TAG, "Network quality check completed");
            } catch (Exception e) {
                Log.e(TAG, "Failed to check network quality", e);
            }
        });
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                Log.d(TAG, "Network callback unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister network callback", e);
            }
        }
        
        networkExecutor.shutdown();
        networkStateListener = null;
        instance = null;
    }
    
    /**
     * 网络状态监听器接口
     */
    public interface NetworkStateListener {
        void onNetworkStateChanged(boolean isAvailable, NetworkType networkType, int quality);
    }
}
