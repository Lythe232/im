package com.lythe.media.im;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.lythe.media.chats.data.entity.MessageConverter;
import com.lythe.media.chats.data.repository.base.BaseRemoteRepository;
import com.lythe.media.protobuf.ImMessage;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttClientManager {
    private static final String TAG = "MqttClientManager";
    private static volatile MqttClientManager instance;
    private MqttAsyncClient mqttClient;
    private String brokerUrl;
    private String clientId;
    private MqttConnectOptions connectOptions;
    private ScheduledExecutorService reconnectExecutor;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPT = 10;
    private OnMqttStatusListener onMqttStatusListener;
    // pending callbacks keyed by messageId (userContext)
    private final ConcurrentHashMap<String, Callback> pendingSendCallbacks = new ConcurrentHashMap<>();

    private enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
    }
    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private Context appContext;


    public static MqttClientManager getInstance(Context context) {
        if(instance == null) {
            synchronized (MqttClientManager.class) {
                if(instance == null) {
                    instance = new MqttClientManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private MqttClientManager(Context context) {
        this.appContext = context;
        // 初始化MQTT服务器地址
//        brokerUrl = "tcp://172.28.230.2:1883";
        brokerUrl = "tcp://192.168.156.180:1883";
        // 生成唯一客户端ID
        clientId = "android-client-" + System.currentTimeMillis();

        //配置连接选项
        connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        // 如果需要认证，添加用户名和密码
        // connectOptions.setUserName("username");
        // connectOptions.setPassword("password".toCharArray());
        // 设置超时时间
        connectOptions.setConnectionTimeout(10);
        // 设置心跳间隔
        connectOptions.setKeepAliveInterval(60);
        connectOptions.setMaxInflight(100);
    }

    public void connect(String username, String password) {
        if(username != null) {
            connectOptions.setUserName(username);
        }
        if(password != null) {
            connectOptions.setPassword(password.toCharArray());
        }
        connect();
    }
    // 连接到MQTT服务器
    public void connect() {
        if(connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            Log.w(TAG, "Connection already in progress or established, skip connect");
            return ;
        }
        if(!isNetworkAvailable()) {
            Log.w(TAG, "Network not available, postpone connection");
            if(onMqttStatusListener != null) {
                onMqttStatusListener.onConnectFailed(new Exception("Network not available"));
            }
            startReconnectTask(5000);
            return;
        }
        connectionState = ConnectionState.CONNECTING;
        try {
            IMqttActionListener listener = new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "MQTT connection established successfully");
                    connectionState = ConnectionState.CONNECTED;
                    reconnectAttempts.set(0);
                    cancelReconnectTask();
                    if(onMqttStatusListener != null) {
                        onMqttStatusListener.onConnectSuccess(asyncActionToken);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "MQTT connection failed: " + exception.getMessage());
                    connectionState = ConnectionState.DISCONNECTED;
                    if(onMqttStatusListener != null) {
                        onMqttStatusListener.onConnectFailed(exception);
                    }
                    Log.e(TAG, "Connect onFailure " + "start reconnect task");
                    if(reconnectAttempts.get() < MAX_RECONNECT_ATTEMPT) {
                        startReconnectTask();
                    } else {
                        Log.w(TAG, "Max reconnection attempts reached, stop reconnecting");
                    }
                }
            };
            if(mqttClient == null) {
                mqttClient = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());
                mqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e(TAG, "Connection lost: " + cause.getMessage());
                        connectionState = ConnectionState.DISCONNECTED;
                        if(onMqttStatusListener != null) {
                            onMqttStatusListener.onConnectLost(cause.getMessage());
                        }
                        //连接丢失时启动重连
                        startReconnectTask();
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        Log.d(TAG, "Message arrived - Topic: " + topic + ", Content: " + new String(message.getPayload()));
                        if(onMqttStatusListener != null) {
                            onMqttStatusListener.onMessageArrived(topic, message);
                        }

                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        Log.d(TAG, "消息发送完成");
                        try {
                            boolean isSuccess = token != null && token.isComplete();
                            Object userCtxObj = token != null ? token.getUserContext() : null;
                            // userContext = msgId
                            String userContext = null;
                            if (userCtxObj instanceof String) {
                                userContext = (String) userCtxObj;
                            } else if (userCtxObj != null) {
                                userContext = userCtxObj.toString();
                            }
                            Log.d(TAG, "Message delivery complete - Success: " + isSuccess + ", Context: " + userContext);

                            if (userContext != null) {
                                Callback cb = pendingSendCallbacks.remove(userContext);
                                if (cb != null) {
                                    try {
                                        if (isSuccess) cb.onSendSuccess();
                                        else cb.onSendFailed();
                                    } catch (Exception ignore) {}
                                }
                            }

                            if(onMqttStatusListener != null) {
                                if(isSuccess) {
                                    onMqttStatusListener.onMessageSendSuccess(userContext);
                                } else {
                                    onMqttStatusListener.onMessageSendFailed(userContext, "发送完成但未确认");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error in deliveryComplete callback", e);
                        }
                    }
                });
            }
            if(mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            IMqttToken connect = mqttClient.connect(connectOptions, null, listener);
        } catch (MqttException e) {
            Log.e(TAG, "MQTT connection exception: " + e.getMessage());
            connectionState = ConnectionState.DISCONNECTED;
            if (onMqttStatusListener != null) {
                onMqttStatusListener.onConnectFailed(e);
            }
            startReconnectTask(3000);
        }
    }
    public void sendMessage(String topic, ImMessage payload, int qos, Callback callback) {
//        if(!isConnected()) {
//            Log.w(TAG, "MQTT not connected, cannot send message: msgId=" + payload.getMessageId());
//            if(onMqttStatusListener != null) {
//                onMqttStatusListener.onMessageSendFailed(payload.getMessageId(), "MQTT not connected");
//            }
//            //TODO  这里要实现消息队列，保证不丢失消息
//            connect();
//            return ;
//        }
        try {
            if (mqttClient == null || !mqttClient.isConnected()) {
                Log.w(TAG, "MQTT not connected, cannot publish message now: msgId=" + payload.getMessageId());
                // initiate connect and let the queue/retry handle re-sending
                callback.onSendFailed();
                connect();
                return;
            }

            MqttMessage message = new MqttMessage();
            message.setPayload(MessageConverter.INSTANCE.serialize(payload));
            message.setQos(qos);
            message.setRetained(false);

            // register callback BEFORE publishing to avoid race with deliveryComplete
            String msgId = payload.getMessageId();
            pendingSendCallbacks.put(msgId, callback);

            mqttClient.publish(topic, message, msgId, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // publish request accepted by client library; final delivery will be signaled in deliveryComplete
                    Log.d(TAG, "Message publish request accepted: msgId=" + msgId);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    String errorMsg = exception != null ? exception.getMessage() : "unknown";
                    Log.e(TAG, "Message publish request failed: msgId=" + msgId, exception);
                    // remove pending callback and notify failure
                    Callback cb = pendingSendCallbacks.remove(msgId);
                    if (cb != null) {
                        try { cb.onSendFailed(); } catch (Exception ignore) {}
                    }
                    if (onMqttStatusListener != null) {
                        onMqttStatusListener.onMessageSendFailed(msgId, errorMsg);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Message send exception: msgId=" + payload.getMessageId(), e);
            // ensure pending callback removed
            Callback cb = pendingSendCallbacks.remove(payload.getMessageId());
            if (cb != null) {
                try { cb.onSendFailed(); } catch (Exception ignore) {}
            }
            if (onMqttStatusListener != null) {
                onMqttStatusListener.onMessageSendFailed(payload.getMessageId(), e.getMessage());
            }
        }
    }
    private void startReconnectTask() {
        startReconnectTask(3000);
    }
    private void startReconnectTask(long delay) {
        if(reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            Log.d(TAG, "Reconnection task already running");
            return ;
        }
        if(reconnectAttempts.get() >= MAX_RECONNECT_ATTEMPT) {
            Log.w(TAG, "Max reconnection attempts reached: " + reconnectAttempts.get());
            return;
        }
        reconnectAttempts.incrementAndGet();
        connectionState = ConnectionState.RECONNECTING;
        Log.d(TAG, "Starting reconnection task, attempt: " + reconnectAttempts.get());

        // 复用线程池，避免频繁创建销毁
        if(reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MQTT-Reconnect-Thread");
                t.setDaemon(true);
                return t;
            });
        }
        
        reconnectExecutor.scheduleWithFixedDelay(() -> {
            if(isNetworkAvailable()) {
                Log.i(TAG, "Network available, attempting reconnection...");
                connect();
            } else {
                Log.w(TAG, "Network not available, skipping reconnection attempt");
            }
        }, delay, calculateReconnectDelay(), TimeUnit.MILLISECONDS);
    }
    public void cancelReconnectTask() {
        Log.d(TAG, "cancelReconnectTask");
        if(reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.shutdown();
            try {
                if(!reconnectExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    reconnectExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconnectExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            reconnectExecutor = null;
        }
    }
    private long calculateReconnectDelay() {
        int attempt = reconnectAttempts.get();
        // 指数退避策略：2^attempt 秒，最大30秒
        long delay = Math.min(30000, (long) Math.pow(2, attempt) * 1000);
        Log.d(TAG, "Reconnect delay: " + delay + "ms for attempt: " + attempt);
        return delay;
    }
    private boolean isNetworkAvailable() {
        if(appContext == null) {
            Log.w(TAG, "Context not available for network check");
            return false;
        }
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm == null) {
                return false;
            }
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();

        } catch (SecurityException e) {
            Log.e(TAG, "Network permission not granted", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability", e);
            return false;
        }
    }

    // 订阅主题
    public void subscribe(String topic, int qos) {
        if(connectionState != ConnectionState.CONNECTED || mqttClient == null || !mqttClient.isConnected()) {
            Log.e(TAG, "Cannot subscribe, MQTT not connected");
            return;
        }
        try {
            if(mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Successfully subscribed to topic: " + topic);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.e(TAG, "Failed to subscribe to topic: " + topic, exception);
                    }
                });
            } else {
                Log.e(TAG, "无法订阅主题，未连接到服务器");
            }
        } catch(MqttException e) {
            Log.e(TAG, "Subscribe topic failed: " + topic, e);
        }
    }

    // 取消订阅
    public void unsubscribe(String topic) {
        if(mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
                Log.d(TAG, "Successfully unsubscribed from topic: " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "Unsubscribe topic failed: " + topic, e);
            }
        }

    }

    public void disconnect() {
        Log.i(TAG, "Disconnecting MQTT client");
        cancelReconnectTask();
        connectionState = ConnectionState.DISCONNECTED;
        reconnectAttempts.set(0);
        // notify pending send callbacks of failure when disconnecting
        for (String msgId : pendingSendCallbacks.keySet()) {
            Callback cb = pendingSendCallbacks.remove(msgId);
            if (cb != null) {
                try { cb.onSendFailed(); } catch (Exception ignore) {}
            }
        }
        if(mqttClient != null) {
            try {
                if(mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                mqttClient = null;
                Log.d(TAG, "MQTT client disconnected and closed");
            } catch (MqttException e) {
                Log.e(TAG, "Error disconnecting MQTT client", e);
            }
        }

    }
    public Boolean isConnected() {
        return connectionState == ConnectionState.CONNECTED &&
                mqttClient != null &&
                mqttClient.isConnected();
    }
    public ConnectionState getConnectionState() {
        return connectionState;
    }
    public void resetReconnectAttempts() {
        reconnectAttempts.set(0);
    }
    public void cancelCurrentSend() {
        //TODO
        Log.w(TAG, "cancelCurrentSend not implemented");
    }
    public void setOnMqttStatusListener(OnMqttStatusListener listener) {
        this.onMqttStatusListener = listener;
    }

    public void removeOnMqttStatusListener() {
        this.onMqttStatusListener = null; // 清空引用以避免内存泄漏
    }

    public void release() {
        disconnect();
        removeOnMqttStatusListener();

        instance = null;
    }
    public interface OnMqttStatusListener {
        void onConnectSuccess(IMqttToken mqttToken);
        void onConnectFailed(Throwable exception);
        void onConnectLost(String reason);
        void onMessageSendSuccess(String msgId);
        void onMessageSendFailed(String msgId, String errorMsg);
        void onMessageArrived(String topic, MqttMessage message);
    }
    public interface Callback {
        void onSendSuccess();
        void onSendFailed();
    }
}
