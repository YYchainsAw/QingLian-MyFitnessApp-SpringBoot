package com.yychainsaw.qinglianapp.ui.message

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.vo.FriendVO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.network.WebSocketManager
import com.yychainsaw.qinglianapp.ui.MainViewModel
import com.yychainsaw.qinglianapp.ui.community.resolveImageUrl
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import com.yychainsaw.qinglianapp.utils.TokenManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    var friends by remember { mutableStateOf<List<FriendVO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ... (保持原有的 LaunchedEffect 逻辑不变) ...
    // 1. 刷新未读数
    LaunchedEffect(Unit) { mainViewModel.refreshUnreadCount() }
    // 2. 连接 WebSocket
    LaunchedEffect(Unit) {
        val token = TokenManager.getToken(context)
        if (!token.isNullOrEmpty()) WebSocketManager.connect(token)
    }
    // 3. 加载好友
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getFriends()
            if (response.isSuccess()) friends = response.data ?: emptyList()
        } catch (e: Exception) { e.printStackTrace() } finally { isLoading = false }
    }
    // 4. 监听消息
    LaunchedEffect(Unit) {
        WebSocketManager.messageFlow.collect { newMessage ->
            // ... (保持原有的消息更新逻辑) ...
            val friendIndex = friends.indexOfFirst { it.userId == newMessage.senderId || it.userId == newMessage.receiverId }
            if (friendIndex != -1) {
                val oldFriend = friends[friendIndex]
                val isIncoming = (newMessage.senderId == oldFriend.userId)
                val newUnreadCount = if (isIncoming) oldFriend.unreadCount + 1 else oldFriend.unreadCount
                val updatedFriend = oldFriend.copy(lastMessage = newMessage.content, lastMessageTime = newMessage.sentAt, unreadCount = newUnreadCount)
                val newList = friends.toMutableList()
                newList.removeAt(friendIndex)
                newList.add(0, updatedFriend)
                friends = newList
            } else {
                // 如果是新好友（例如刚通过请求），重新拉取列表
                val response = RetrofitClient.apiService.getFriends()
                if (response.isSuccess()) friends = response.data ?: emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("消息", fontWeight = FontWeight.Bold) },
                actions = {
                    // --- 修改：点击跳转到添加好友界面 ---
                    IconButton(onClick = { navController.navigate("add_friend") }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Add Friend", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        // ... (保持原有的 UI 布局逻辑不变) ...
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QingLianYellow)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                if (friends.isEmpty()) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) { Text("暂无消息", color = Color.Gray) } }
                } else {
                    items(friends) { friend ->
                        FriendItem(
                            friend = friend,
                            onClick = {
                                val encodedAvatar = if (friend.avatarUrl != null) URLEncoder.encode(friend.avatarUrl, StandardCharsets.UTF_8.toString()) else ""
                                val displayName = friend.nickname?.takeIf { it.isNotBlank() } ?: friend.username
                                navController.navigate("chat/${friend.userId}/$displayName?avatar=$encodedAvatar")
                            }
                        )
                    }
                }
            }
        }
    }
}

// ... FriendItem 和 formatFriendlyTime 保持不变 ...
@Composable
fun FriendItem(friend: FriendVO, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(QingLianBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (friend.avatarUrl.isNullOrBlank()) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
            } else {
                AsyncImage(
                    model = resolveImageUrl(friend.avatarUrl),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.nickname?.takeIf { it.isNotBlank() } ?: friend.username,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = friend.lastMessage ?: "点击开始聊天",
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatFriendlyTime(friend.lastMessageTime), fontSize = 11.sp, color = Color.LightGray)
            if (friend.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(containerColor = Color.Red, contentColor = Color.White) {
                    Text(text = if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(start = 82.dp))
}

fun formatFriendlyTime(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return ""
    try {
        if (timeStr.contains("T")) {
            return timeStr.split("T")[1].take(5)
        }
        return timeStr
    } catch (e: Exception) {
        return ""
    }
}
