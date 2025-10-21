package com.lythe.media.im.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.credentials.playservices.CredentialProviderMetadataHolder;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lythe.media.R;
import com.lythe.media.im.MessageProcessor;
import com.lythe.media.im.MqttClientManager;
import com.lythe.media.ui.login.LoginViewModel;
import com.lythe.media.ui.login.LoginViewModelFactory;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;


/// 前台服务维持长连接
public class MqttForegroundService extends Service {

    private static final String TAG = "MqttForegroundService";
    private static final String CHANNEL_ID = "mqtt_receive_channel";
    private static final String CHANNEL_NAME = "MQTT消息接收";
    private static final int NOTIFICATION_ID = 1001;
    private static final String ACTION_USER_LOGIN = "com.lythe.media.USER_LOGIN";
    private static final String ACTION_USER_LOGOUT = "com.lythe.media.USER_LOGOUT";
    private boolean isServiceStarted  = false;
    MqttClientManager mqttClientManager;
    private MessageProcessor messageProcessor;
    private final LocalBinder localBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public MqttForegroundService getService() {
            return MqttForegroundService.this;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MQTT服务启动命令");

        if(intent != null && intent.getAction() != null) {
            handleIntentAction(intent.getAction());
        } else {
            checkUserAndConnect();
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MQTT接收服务创建");
        messageProcessor = MessageProcessor.getInstance(this);
        mqttClientManager = MqttClientManager.getInstance(getApplicationContext());
        registerMqttMessageCallback();
        startForeground(NOTIFICATION_ID, createNotification("正在连接", "启动MQTT连接..."));
        isServiceStarted = true;
    }
    public void startMqttConnection() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser == null) {
            Log.w(TAG, "尝试连接MQTT, 但用户未登录");
            updateNotification("连接失败", "用户未登录");
            return;
        }
        if(mqttClientManager.isConnected()) {
            Log.d(TAG, "MQTT已经连接, 跳过重复连接");
            return;
        }
        updateNotification("正在连接", "正在建立MQTT连接...");
        mqttClientManager.connect();
    }

    public void stopMqttService() {
        Log.d(TAG, "停止MQTT服务");
        unsubscribeAllTopic();
        if(mqttClientManager != null) {
            mqttClientManager.disconnect();
            mqttClientManager.removeOnMqttStatusListener();
        }
        stopForeground(true);
        stopSelf();
    }
    private void handleIntentAction(String action) {
        switch (action) {
            case ACTION_USER_LOGIN:
                Log.d(TAG, "收到用户登录广播, 启动MQTT连接");
                startMqttConnection();
                break;
            case ACTION_USER_LOGOUT:
                Log.d(TAG, "收到用户登出广播, 停止MQTT连接");
                stopMqttService();
                break;
            default:
                checkUserAndConnect();
                break;
        }
    }

    private void checkUserAndConnect() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            Log.d(TAG, "服务启动时检测到用户已登录，启动MQTT连接");
            startMqttConnection();
        } else {
            Log.d(TAG, "服务启动时用户未登录，等待登录事件");
            updateNotification("等待登录", "用户未登录，等待登录...");
        }
    }

    private void registerMqttMessageCallback() {
        mqttClientManager.setOnMqttStatusListener(new MqttClientManager.OnMqttStatusListener() {
            @Override
            public void onConnectSuccess(IMqttToken mqttToken) {
                Log.d(TAG, "MQTT连接成功");
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if(currentUser != null) {
                    subscribeUserTopics();
                    updateNotification("已连接", "MQTT连接已建立");
                }
            }

            @Override
            public void onConnectFailed(Throwable exception) {
                Log.e(TAG, "MQTT连接失败: " + exception.getMessage());
                updateNotification("连接失败", "MQTT连接失败: " + exception.getMessage());
            }

            @Override
            public void onConnectLost(String reason) {
                Log.e(TAG, "MQTT连接断开: " + reason);
                updateNotification("连接断开", "MQTT连接已断开");
            }

            @Override
            public void onMessageSendSuccess(String msgId) {
                Log.d(TAG, "消息发送成功: msgId=" + msgId);
            }

            @Override
            public void onMessageSendFailed(String msgId, String errorMsg) {
                Log.e(TAG, "消息发送失败: msgId=" + msgId + ", error=" + errorMsg);
            }

            @Override
            public void onMessageArrived(String topic, MqttMessage message) {
                Log.d(TAG, "收到MQTT消息:topic=" + topic
//                        + ", 内容=" + message
                );
                messageProcessor.processReceivedMessage(topic, message.getPayload());
            }
        });
    }
    private void subscribeUserTopics() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) {
            Log.w(TAG, "用户未登录，无法订阅主题");
            return ;
        }
        String uid = currentUser.getUid();
        String[] topics = {
          "chat/" + uid,
          "notify/" + uid,
          "group/chat",
          "system/broadcast"
        };
        for(String topic : topics) {
            try {
                mqttClientManager.subscribe(topic, 1);
                Log.d(TAG, "已订阅MQTT主题");
            } catch (Exception e) {
                Log.e(TAG, "订阅MQTT主题失败", e);
            }
        }

    }
    private void unsubscribeAllTopic() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null || mqttClientManager == null) {
            return ;
        }
        String uid = currentUser.getUid();
        String[] topics = {
                "chat/" + uid,
                "notify/" + uid,
                "group/chat",
                "system/broadcast"
        };
        for (String topic : topics) {
            try {
                mqttClientManager.unsubscribe(topic);
                Log.d(TAG, "已取消订阅主题: " + topic);
            } catch (Exception e) {
                Log.e(TAG, "取消订阅主题失败: " + topic, e);
            }
        }
    }
    private Notification createNotification(String title, String content) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    public void updateNotification(String title, String content) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if(notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, createNotification(title, content));
        }
    }

    public static void startService(Context context) {
        Intent intent = new Intent(context, MqttForegroundService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    public static void stop(Context context) {
        Intent intent = new Intent(context, MqttForegroundService.class);
        context.stopService(intent);
    }
    public static void notifyUserLogin(Context context) {
        Intent intent = new Intent(context, MqttForegroundService.class);
        intent.setAction(ACTION_USER_LOGIN);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        Log.d(TAG, "已发送用户登录通知到MQTT服务");
    }
    public static void notifyUserLogout(Context context ) {
        Intent intent = new Intent(context, MqttForegroundService.class);
        intent.setAction(ACTION_USER_LOGOUT);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        Log.d(TAG, "已发送用户登出通知到MQTT服务");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MQTT接收服务销毁");
        stopMqttService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }
}
