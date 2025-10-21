package com.lythe.media.chats.utils;

import android.content.Context;

import com.lythe.media.im.MqttClientManager;
import com.lythe.media.protobuf.ImMessage;

import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MqttMessageSender implements SendMessagesHelper.MessageSender {
    private static final String TAG = "MqttMessageSender";
    private final MqttClientManager mqttClientManager;
    private static final long CONNECT_WAIT_TIMEOUT = 10000;
    public MqttMessageSender(Context context) {
        this.mqttClientManager = MqttClientManager.getInstance(context);
    }

    @Override
    public SendMessageResponse sendMessage(ImMessage request, String to, String fileServerUrl) throws Exception {
//        if(!waitForMqttConnect(tempMsgId)) {
//            throw new Exception("MQTT连接超时(" + CONNECT_WAIT_TIMEOUT + "ms), 无法发送消息");
//        }
        SendMessageResponse resultResponse = new SendMessageResponse();
//        SendMessagesHelper.getInstance().sendMessage(request, to, 1);
        return resultResponse;
    }

    @Override
    public void cancelSend() {
        if(mqttClientManager != null) {
            //TODO
        }
    }
    public void release() {
        if(mqttClientManager != null) {
            mqttClientManager.disconnect();
        }
    }
    private String getFinalMsgContent(SendMessageRequest request, String fileServerUrl) {
        switch (request.getMessageType()) {
            case TEXT:
                return request.getTextContent();
            case IMAGE:
            case FILE:
                return fileServerUrl != null ? fileServerUrl : "file_url_empty";
            default:
                return "";
        }
    }
//    private boolean waitForMqttConnect(String tempMsgId) throws InterruptedException {
//        if(mqttClientManager.isConnected()) {
//            return true;
//        }
//
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        boolean[] connectResult = {false};
//        mqttClientManager.setOnMqttStatusListener(new MqttClientManager.OnMqttStatusListener() {
//            @Override
//            public void onConnectSuccess(IMqttToken mqttToken) {
//                connectResult[0] = true;
//                countDownLatch.countDown();
//            }
//            @Override
//            public void onConnectFailed(Throwable errorMsg) {
//                connectResult[0] = false;
//                countDownLatch.countDown();
//            }
//            @Override
//            public void onConnectLost(String reason) {
//            }
//            @Override
//            public void onMessageSendSuccess(String msgId) {
//            }
//            @Override
//            public void onMessageSendFailed(String msgId, String errorMsg) {
//            }
//            @Override
//            public void onMessageArrived(String topic, MqttMessage message) {
//            }
//        });
//        mqttClientManager.connect();
//        countDownLatch.await(CONNECT_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
//        return connectResult[0];
//    }
}
