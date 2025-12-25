package com.yychainsaw.qinglianapp.ui.profile

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.service.WebSocketService
import com.yychainsaw.qinglianapp.utils.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            // 退出登录按钮
            Button(
                onClick = {
                    // 1. 清除 Token (根据你的 TokenManager 实现，这里假设存空字符串即为清除)
                    TokenManager.saveToken(context, "")
                    RetrofitClient.authToken = null

                    // 2. 停止 WebSocket 服务
                    val intent = Intent(context, WebSocketService::class.java)
                    context.stopService(intent)

                    // 3. 跳转回登录页并清空返回栈
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4F)),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("退出登录", color = Color.White)
            }
        }
    }
}
