package com.lythe.media.im.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lythe.media.im.service.MqttForegroundService;

public class MqttServiceManager {
    private static final String TAG = "MqttServiceManager";
    private static MqttServiceManager instance;
    private Context appContext;
    public MqttServiceManager(Context context) {
        this.appContext = context;
    }

    public static synchronized MqttServiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new MqttServiceManager(context.getApplicationContext());
        }
        return instance;
    }
    public void initialize() {
        Log.d(TAG, "初始化MQTT服务管理器");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null) {
            Log.d(TAG, "应用启动时检测到用户已登录，启动MQTT服务");
            startMqttService();
        } else {
            Log.d(TAG, "应用启动时用户未登录，先启动服务等待登录");
            MqttForegroundService.startService(appContext);
        }
    }

    public void onUserLogin() {
        Log.d(TAG, "用户登录成功，启动MQTT连接");
        MqttForegroundService.notifyUserLogin(appContext);
        startMqttService();
    }
    public void onUserLogout() {
        Log.d(TAG, "用户登出，停止MQTT连接");
        MqttForegroundService.notifyUserLogout(appContext);
        stopMqttService();
    }
    private void startMqttService() {
        MqttForegroundService.startService(appContext);
    }
    private void stopMqttService() {
        MqttForegroundService.stop(appContext);
    }
}
