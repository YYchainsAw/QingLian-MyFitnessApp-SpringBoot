package com.yychainsaw.qinglianapp.ui.message

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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
import com.yychainsaw.qinglianapp.data.model.dto.MessageSendDTO
import com.yychainsaw.qinglianapp.data.model.entity.MessageEntity
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.network.WebSocketManager
import com.yychainsaw.qinglianapp.ui.community.resolveImageUrl
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    friendId: String,
    friendName: String,
    friendAvatar: String?
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf("") }
    var currentUserAvatar by remember { mutableStateOf<String?>(null) }

    // 新增：判断是否需要显示“接受好友请求”按钮
    // 简单逻辑：如果进入聊天界面但对方不在好友列表中（或者通过特定消息进入），则显示
    // 这里我们通过尝试获取好友列表来判断，如果不在列表中，假设是请求状态
    var isFriend by remember { mutableStateOf(true) }

    // 初始化加载
    LaunchedEffect(Unit) {
        try {
            val userRes = RetrofitClient.apiService.getUserInfo()
            if (userRes.isSuccess()) {
                currentUserId = userRes.data?.userId ?: userRes.data?.username ?: ""
                currentUserAvatar = userRes.data?.avatarUrl
            }
            loadHistory(friendId) { msgs -> messages = msgs }
            RetrofitClient.apiService.markAsRead(friendId)

            // 检查是否已经是好友
            val friendsRes = RetrofitClient.apiService.getFriends()
            if (friendsRes.isSuccess()) {
                val friendList = friendsRes.data ?: emptyList()
                isFriend = friendList.any { it.userId == friendId }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ... (WebSocket 监听逻辑保持不变) ...
    LaunchedEffect(Unit) {
        WebSocketManager.messageFlow.collect { msgVO ->
            if (msgVO.senderId == friendId) {
                val newEntity = MessageEntity(
                    msgId = msgVO.id,
                    senderId = msgVO.senderId,
                    receiverId = msgVO.receiverId,
                    content = msgVO.content,
                    sentAt = msgVO.sentAt,
                    isRead = true
                )
                messages = messages + newEntity
                try { RetrofitClient.apiService.markAsRead(friendId) } catch (_: Exception) {}
            }
        }
    }

    // ... (自动滚动逻辑保持不变) ...
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // 接受好友请求并发送问候语
    fun acceptAndGreet() {
        scope.launch {
            try {
                // 1. 接受请求
                val acceptRes = RetrofitClient.apiService.acceptFriendRequest(friendId)
                if (acceptRes.isSuccess()) {
                    isFriend = true
                    Toast.makeText(context, "已添加好友", Toast.LENGTH_SHORT).show()

                    // 2. 自动发送问候语
                    val greeting = "你好，很高兴认识你！"
                    val sendDto = MessageSendDTO(receiverId = friendId, content = greeting)
                    val sendRes = RetrofitClient.apiService.sendMessage(sendDto)

                    if (sendRes.isSuccess() && sendRes.data != null) {
                        // 更新本地界面
                        val timeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                        val tempMsg = MessageEntity(
                            msgId = System.currentTimeMillis(),
                            senderId = currentUserId,
                            receiverId = friendId,
                            content = greeting,
                            sentAt = timeStr,
                            isRead = false
                        )
                        messages = messages + tempMsg
                        WebSocketManager.emitMessage(sendRes.data)
                    }
                } else {
                    Toast.makeText(context, "操作失败: ${acceptRes.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(friendName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // --- 新增：如果不是好友，显示接受请求栏 ---
                if (!isFriend) {
                    Surface(
                        color = QingLianYellow.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("对方请求添加你为好友", fontSize = 14.sp, color = Color.DarkGray)
                            Button(
                                onClick = { acceptAndGreet() },
                                colors = ButtonDefaults.buttonColors(containerColor = QingLianYellow),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("接受", fontSize = 12.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // 只有是好友时才允许输入，或者根据需求开放
            if (isFriend) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        placeholder = { Text("发送消息...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = QingLianYellow, unfocusedBorderColor = Color.LightGray)
                    )
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && currentUserId.isNotBlank()) {
                                val contentToSend = inputText
                                val timeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

                                val tempMsg = MessageEntity(
                                    msgId = System.currentTimeMillis(),
                                    senderId = currentUserId,
                                    receiverId = friendId,
                                    content = contentToSend,
                                    sentAt = timeStr,
                                    isRead = false
                                )
                                messages = messages + tempMsg
                                inputText = ""

                                scope.launch {
                                    try {
                                        val sendDto = MessageSendDTO(receiverId = friendId, content = contentToSend)
                                        val response = RetrofitClient.apiService.sendMessage(sendDto)
                                        if (response.isSuccess() && response.data != null) {
                                            WebSocketManager.emitMessage(response.data)
                                        } else {
                                            Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp).background(QingLianYellow, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages.reversed()) { msg ->
                val isMe = msg.senderId == currentUserId
                MessageItemRow(msg = msg, isMe = isMe, avatarUrl = if (isMe) currentUserAvatar else friendAvatar)
            }
        }
    }
}

// ... MessageItemRow, ChatAvatar, loadHistory 保持不变 ...
@Composable
fun MessageItemRow(msg: MessageEntity, isMe: Boolean, avatarUrl: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) { ChatAvatar(avatarUrl); Spacer(modifier = Modifier.width(8.dp)) }
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start, modifier = Modifier.weight(1f, fill = false)) {
            Surface(
                color = if (isMe) QingLianYellow else Color.White,
                shape = if (isMe) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp) else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                shadowElevation = 1.dp
            ) {
                Text(text = msg.content, modifier = Modifier.padding(12.dp), fontSize = 16.sp, color = Color.Black)
            }
            val displayTime = try { msg.sentAt.take(16).replace("T", " ") } catch (e: Exception) { "" }
            Text(text = displayTime, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp))
        }
        if (isMe) { Spacer(modifier = Modifier.width(8.dp)); ChatAvatar(avatarUrl) }
    }
}

@Composable
fun ChatAvatar(url: String?) {
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
        if (url.isNullOrBlank()) Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
        else AsyncImage(model = resolveImageUrl(url), contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

suspend fun loadHistory(friendId: String, onResult: (List<MessageEntity>) -> Unit) {
    try {
        val response = RetrofitClient.apiService.getMessageHistory(friendId, 1, 50)
        if (response.isSuccess()) {
            onResult(response.data?.items ?: emptyList())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
