package com.yychainsaw.qinglianapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yychainsaw.qinglianapp.network.WebSocketManager
import com.yychainsaw.qinglianapp.service.WebSocketService
import com.yychainsaw.qinglianapp.ui.friend.AddFriendScreen
import com.yychainsaw.qinglianapp.ui.login.LoginScreen
import com.yychainsaw.qinglianapp.ui.main.MainScreen
import com.yychainsaw.qinglianapp.ui.message.ChatScreen
import com.yychainsaw.qinglianapp.ui.profile.FriendsScreen
import com.yychainsaw.qinglianapp.ui.profile.RecordsScreen
import com.yychainsaw.qinglianapp.ui.profile.SettingsScreen
import com.yychainsaw.qinglianapp.ui.theme.QingLianAppTheme
import com.yychainsaw.qinglianapp.utils.AppState
import com.yychainsaw.qinglianapp.utils.NotificationHelper
import com.yychainsaw.qinglianapp.utils.TokenManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化通知渠道 (必须)
        NotificationHelper.createNotificationChannel(this)

        // 2. 启动 WebSocket 服务 (用于后台保活)
        startService(Intent(this, WebSocketService::class.java))

        // 3. 全局监听 WebSocket 消息 -> 触发系统通知
        // 只要 Activity 存活（包括后台），这里就能收到消息并弹窗
        lifecycleScope.launch {
            WebSocketManager.messageFlow.collect { message ->
                // 逻辑：如果消息发送者不是当前正在聊天的对象，或者 App 在后台，则弹出通知
                if (AppState.currentChatFriendId != message.senderId || !AppState.isAppInForeground) {
                    NotificationHelper.showNotification(
                        context = this@MainActivity,
                        title = message.senderName ?: "新消息",
                        content = message.content // 好友请求通常也是一条文本消息，如"请求添加好友"
                    )
                }
            }
        }

        setContent {
            // 假设你有一个主题定义，如果没有可直接用 MaterialTheme
            QingLianAppTheme {
                val navController = rememberNavController()

                // 判断初始页面
                val startDestination = if (TokenManager.getToken(this).isNullOrEmpty()) "login" else "main"

                NavHost(navController = navController, startDestination = startDestination) {

                    // 登录页 (假设存在)
                    composable("login") {
                        LoginScreen(navController)
                    }

                    // 主页
                    composable("main") {
                        MainScreen(navController)
                    }

                    // 添加好友
                    composable("add_friend") {
                        AddFriendScreen(navController)
                    }

                    // 设置
                    composable("settings") {
                        SettingsScreen(navController)
                    }

                    // 我的好友列表 (Profile -> Friends)
                    composable("friends") {
                        FriendsScreen(navController)
                    }

                    // 健身记录 (Profile -> Records)
                    composable("records") {
                        RecordsScreen(navController)
                    }

                    // 聊天界面
                    composable(
                        route = "chat/{friendId}/{friendName}?avatar={avatar}",
                        arguments = listOf(
                            navArgument("friendId") { type = NavType.StringType },
                            navArgument("friendName") { type = NavType.StringType },
                            navArgument("avatar") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
                        val friendName = backStackEntry.arguments?.getString("friendName") ?: ""
                        val avatar = backStackEntry.arguments?.getString("avatar")

                        // 关键：进入聊天界面时，更新全局状态，避免弹出该好友的消息通知
                        DisposableEffect(Unit) {
                            AppState.currentChatFriendId = friendId
                            onDispose {
                                AppState.currentChatFriendId = null
                            }
                        }

                        ChatScreen(navController, friendId, friendName, avatar)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 标记 App 在前台
        AppState.isAppInForeground = true
    }

    override fun onPause() {
        super.onPause()
        // 标记 App 在后台
        AppState.isAppInForeground = false
    }
}
