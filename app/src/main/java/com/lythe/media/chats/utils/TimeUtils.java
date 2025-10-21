package com.lythe.media.chats.utils;

import com.lythe.media.protobuf.ImMessageStatus;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static String formatChatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);

        Calendar weekStart = Calendar.getInstance();
        weekStart.add(Calendar.DATE, -7);

        // 今天：显示时间
        if (timestamp >= today.getTimeInMillis()) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }
        // 昨天：显示"昨天"
        else if (timestamp >= yesterday.getTimeInMillis()) {
            return "昨天";
        }
        // 一周内：显示星期几
        else if (timestamp >= weekStart.getTimeInMillis()) {
            String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            if (dayOfWeek < 0) dayOfWeek = 0;
            return weekDays[dayOfWeek];
        }
        // 今年内：显示月日
        else {
            Calendar currentYear = Calendar.getInstance();
            currentYear.set(Calendar.MONTH, Calendar.JANUARY);
            currentYear.set(Calendar.DAY_OF_MONTH, 1);
            currentYear.set(Calendar.HOUR_OF_DAY, 0);
            currentYear.set(Calendar.MINUTE, 0);
            currentYear.set(Calendar.SECOND, 0);
            currentYear.set(Calendar.MILLISECOND, 0);

            if (timestamp >= currentYear.getTimeInMillis()) {
                return new SimpleDateFormat("MM/dd", Locale.getDefault()).format(new Date(timestamp));
            }
            // 跨年：显示年月日
            else {
                return new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(new Date(timestamp));
            }
        }
    }

    // 更详细的时间格式（可选）
    public static String formatDetailedTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // 如果时间差小于1分钟，显示"刚刚"
        if (diff < 60 * 1000) {
            return "刚刚";
        }

        // 如果时间差小于1小时，显示"x分钟前"
        if (diff < 60 * 60 * 1000) {
            long minutes = diff / (60 * 1000);
            return minutes + "分钟前";
        }

        // 其余情况使用标准聊天时间格式
        return formatChatTime(timestamp);
    }

    // 根据消息状态获取显示时间
    public static String getDisplayTime(long timestamp, ImMessageStatus messageStatus) {
        // 如果是发送中的消息
        if (messageStatus == ImMessageStatus.SENDING) {
            return "发送中";
        }
        // 如果是发送失败的消息
        else if (messageStatus == ImMessageStatus.FAILED) {
            return "发送失败";
        }
        // 正常消息使用相对时间
        else {
            return formatChatTime(timestamp);
        }
    }
}