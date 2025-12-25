package com.yychainsaw.qinglianapp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.network.WebSocketManager
import com.yychainsaw.qinglianapp.utils.AppState
import com.yychainsaw.qinglianapp.utils.NotificationHelper
import com.yychainsaw.qinglianapp.utils.TokenManager
import kotlinx.coroutines.*

class WebSocketService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentUserId: String? = null
    private val friendNicknameCache = mutableMapOf<String, String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = TokenManager.getToken(this)
        if (!token.isNullOrEmpty()) {
            // 确保 WebSocket 连接
            WebSocketManager.connect(token)
            
            // 预加载数据用于通知展示
            loadUserInfoAndFriends()
            
            // 开始监听消息
            observeMessages()
        }
        // START_STICKY 保证服务被杀后尝试重启
        return START_STICKY
    }

    private fun loadUserInfoAndFriends() {
        serviceScope.launch {
            try {
                val userRes = RetrofitClient.apiService.getUserInfo()
                if (userRes.isSuccess()) {
                    currentUserId = userRes.data?.userId ?: userRes.data?.username
                }
                val friendsRes = RetrofitClient.apiService.getFriends()
                if (friendsRes.isSuccess()) {
                    friendsRes.data?.forEach { friend ->
                        val displayName = friend.nickname?.takeIf { it.isNotBlank() } ?: friend.username
                        friendNicknameCache[friend.userId] = displayName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeMessages() {
        serviceScope.launch {
            WebSocketManager.messageFlow.collect { message ->
                // 1. 过滤自己发送的消息
                if (currentUserId != null && message.senderId == currentUserId) {
                    return@collect
                }

                // 2. 判断是否需要免打扰
                // 如果应用在前台 且 (正在与该发送者聊天 OR 停留在消息列表页)
                val isChattingWithSender = AppState.currentChatFriendId == message.senderId
                val shouldSuppress = AppState.isAppInForeground && (isChattingWithSender || AppState.isAtMessageList)

                if (!shouldSuppress) {
                    val cachedName = friendNicknameCache[message.senderId]
                    val title = cachedName ?: message.senderName ?: "新消息"
                    
                    NotificationHelper.showNotification(
                        this@WebSocketService,
                        title,
                        message.content
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        // 服务销毁时断开连接
        WebSocketManager.disconnect()
    }
}
