package com.yychainsaw.qinglianapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.vo.UserVO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import com.yychainsaw.qinglianapp.utils.TokenManager

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    var userVO by remember { mutableStateOf<UserVO?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            if (RetrofitClient.authToken.isNullOrBlank()) {
                RetrofitClient.authToken = TokenManager.getToken(context)
            }
            if (RetrofitClient.authToken.isNullOrBlank()) {
                isLoading = false
                return@LaunchedEffect
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
            // 设置按钮
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-12).dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Black)
            }

            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp),
                        color = Color.White
                    ) {
                        if (userVO?.avatarUrl.isNullOrEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            AsyncImage(
                                model = userVO?.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userVO?.nickname ?: "未登录",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            // 跳转编辑资料页
                            IconButton(
                                onClick = { navController.navigate("edit_profile") },
                                modifier = Modifier.size(24.dp).padding(start = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black.copy(alpha = 0.6f))
                            }
                        }
                        Text(
                            text = "ID: ${userVO?.username ?: "--"}",
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                }
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
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "身高", value = "${userVO?.height ?: "--"} cm")
                StatItem(label = "体重", value = "${userVO?.weight ?: "--"} kg")
                StatItem(label = "性别", value = userVO?.gender ?: "--")
            }
        }

        // 3. 菜单列表
        Column(modifier = Modifier.padding(top = 0.dp)) {
            MenuItem(text = "我的好友") { navController.navigate("friends") }
            MenuItem(text = "健身记录") { navController.navigate("records") }
            // 预留：数据看板入口 (getUserDashboard)
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
