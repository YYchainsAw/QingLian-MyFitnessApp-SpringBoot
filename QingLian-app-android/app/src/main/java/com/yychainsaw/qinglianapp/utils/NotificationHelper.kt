package com.yychainsaw.qinglianapp.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yychainsaw.qinglianapp.MainActivity
import com.yychainsaw.qinglianapp.R

object NotificationHelper {
    private const val CHANNEL_ID = "chat_message_channel"
    private const val CHANNEL_NAME = "聊天消息"

    // 1. 创建通知渠道 (在 App 启动时调用一次)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "接收好友发送的即时消息"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // 2. 显示通知
    fun showNotification(context: Context, title: String, content: String) {
        // Android 13+ 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // 没有权限则不显示
            }
        }

        // 点击通知跳转到 MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // 使用你的 App 图标
            .setContentTitle(title)   // 显示发送者名字
            .setContentText(content)  // 显示消息内容
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)      // 点击后自动消失

        val notificationManager = NotificationManagerCompat.from(context)
        // 使用当前时间作为 ID，保证多条消息不会互相覆盖
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
