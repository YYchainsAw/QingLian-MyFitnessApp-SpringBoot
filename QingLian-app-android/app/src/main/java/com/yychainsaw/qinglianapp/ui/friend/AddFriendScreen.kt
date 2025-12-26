package com.yychainsaw.qinglianapp.ui.friend

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.dto.MessageSendDTO // 新增导入
import com.yychainsaw.qinglianapp.data.model.vo.UserVO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.ui.community.resolveImageUrl
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import kotlinx.coroutines.launch
import kotlin.code

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserVO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // 执行搜索
    fun doSearch() {
        if (keyword.isBlank()) return
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.apiService.searchUsers(keyword)
                if (response.isSuccess()) {
                    searchResults = response.data ?: emptyList()
                    if (searchResults.isEmpty()) {
                        Toast.makeText(context, "未找到相关用户", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "搜索失败: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // 发送好友请求
    // 发送好友请求 (调试版)
    fun sendRequest(user: UserVO) {
        // 1. 检查 ID 是否存在
        android.util.Log.d("AddFriendDebug", "准备添加好友: username=${user.username}, userId=${user.userId}")

        if (user.userId.isNullOrBlank()) {
            Toast.makeText(context, "错误：用户ID为空，无法添加", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                android.util.Log.d("AddFriendDebug", "正在发起 HTTP 请求: sendFriendRequest...")

                // 2. 发送好友请求
                val response = RetrofitClient.apiService.sendFriendRequest(user.userId)

                android.util.Log.d("AddFriendDebug", "请求返回: code=${response.code}, msg=${response.message}, success=${response.isSuccess()}")

                if (response.isSuccess()) {
                    Toast.makeText(context, "已发送好友请求", Toast.LENGTH_SHORT).show()

                    // 3. 尝试发送打招呼消息 (触发 WebSocket)
                    try {
                        android.util.Log.d("AddFriendDebug", "尝试发送打招呼消息...")
                        val helloContent = "你好，我想添加你为好友"
                        val msgDto = MessageSendDTO(receiverId = user.userId, content = helloContent)
                        val msgRes = RetrofitClient.apiService.sendMessage(msgDto)
                        android.util.Log.d("AddFriendDebug", "打招呼消息结果: ${msgRes.code}")
                    } catch (e: Exception) {
                        android.util.Log.e("AddFriendDebug", "打招呼消息发送异常", e)
                    }

                } else {
                    // 业务失败 (如：已经是好友，或后端报错)
                    Toast.makeText(context, "请求失败: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 网络或解析异常
                android.util.Log.e("AddFriendDebug", "发生严重异常", e)
                e.printStackTrace()
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加好友", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 搜索框区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索用户名/昵称") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QingLianYellow,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { doSearch() },
                    colors = ButtonDefaults.buttonColors(containerColor = QingLianYellow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("搜索", color = Color.Black)
                }
            }

            // 结果列表
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QingLianYellow)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults) { user ->
                        UserResultItem(user = user, onAddClick = { sendRequest(user) })
                    }
                }
            }
        }
    }
}

@Composable
fun UserResultItem(user: UserVO, onAddClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl.isNullOrBlank()) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    AsyncImage(
                        model = resolveImageUrl(user.avatarUrl),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 用户信息 (左边)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.nickname?.takeIf { it.isNotBlank() } ?: user.username,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "用户名: ${user.username}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "性别: ${user.gender ?: "保密"}",
                    fontSize = 12.sp,
                    color = QingLianBlue
                )
            }

            // 添加按钮 (右边)
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = QingLianBlue),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}
