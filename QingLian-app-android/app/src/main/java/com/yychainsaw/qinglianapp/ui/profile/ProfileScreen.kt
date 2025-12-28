package com.yychainsaw.qinglianapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.vo.UserVO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import com.yychainsaw.qinglianapp.utils.TokenManager
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var userVO by remember { mutableStateOf<UserVO?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // 提取加载逻辑
    fun loadUserInfo() {
        scope.launch {
            try {
                if (RetrofitClient.authToken.isNullOrBlank()) {
                    RetrofitClient.authToken = TokenManager.getToken(context)
                }
                if (RetrofitClient.authToken.isNullOrBlank()) {
                    isLoading = false
                    return@launch
                }
                val response = RetrofitClient.apiService.getUserInfo()

                if (response.isSuccess() && response.data != null) {
                    userVO = response.data
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // 监听生命周期：每次页面显示（包括从编辑页返回）时刷新数据
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadUserInfo()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // 1. 顶部个人信息卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(QingLianYellow)
                .padding(24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else {
                // 用户信息行
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(bottom = 40.dp) // 给底部卡片留出空间
                        .fillMaxWidth()
                        .clickable { navController.navigate("edit_profile") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 头像
                    AsyncImage(
                        model = userVO?.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // 昵称和ID
                    Column {
                        Text(
                            text = userVO?.nickname ?: "未设置昵称",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "ID: ${userVO?.username ?: "--"}",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                }
            }

            // 设置按钮 - 移到最后以确保在最上层，并调整位置避免遮挡
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                // 移除额外的 padding(top = 24.dp)，让它更靠上，避免与用户信息行重叠
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Black)
            }
        }

        // 2. 身体数据卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-40).dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "身高", value = "${userVO?.height ?: "--"} cm")
                StatItem(label = "体重", value = "${userVO?.weight ?: "--"} kg")

                // 修改性别显示逻辑
                val genderDisplay = when (userVO?.gender) {
                    "MALE" -> "男"
                    "FEMALE" -> "女"
                    else -> "未知"
                }
                StatItem(label = "性别", value = genderDisplay)
            }
        }

        // 3. 菜单列表
        Column(modifier = Modifier.padding(top = 0.dp)) {
            MenuItem(text = "我的好友") { navController.navigate("friends") }
            MenuItem(text = "健身记录") { navController.navigate("records") }
            MenuItem(text = "数据看板") { /* TODO: 跳转数据看板 */ }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = QingLianBlue)
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, fontSize = 16.sp, color = Color(0xFF333333))
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
    }
    Divider(color = Color(0xFFF0F0F0), thickness = 0.5.dp)
}
