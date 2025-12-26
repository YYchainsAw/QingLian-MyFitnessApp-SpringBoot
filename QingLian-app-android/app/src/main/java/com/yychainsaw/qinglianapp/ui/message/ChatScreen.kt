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
import com.yychainsaw.qinglianapp.data.model.entity.MessageEntity
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.network.WebSocketManager
import com.yychainsaw.qinglianapp.ui.community.resolveImageUrl
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    friendId: String, // 可能是用户ID，也可能是 "GROUP_{groupId}"
    friendName: String,
    friendAvatar: String?
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // --- 1. 识别是否为群聊 ---
    val isGroupChat = friendId.startsWith("GROUP_")
    val realChatId = if (isGroupChat) friendId.removePrefix("GROUP_") else friendId

    var messages by remember { mutableStateOf<List<MessageEntity>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf("") }
    var currentUserAvatar by remember { mutableStateOf<String?>(null) }

    // 好友状态逻辑（仅私聊有效，群聊默认允许发言）
    var isFriend by remember { mutableStateOf(isGroupChat) }

    // 初始化加载
    LaunchedEffect(Unit) {
        try {
            // 获取当前用户信息
            val userRes = RetrofitClient.apiService.getUserInfo()
            if (userRes.isSuccess()) {
                currentUserId = userRes.data?.userId ?: userRes.data?.username ?: ""
                currentUserAvatar = userRes.data?.avatarUrl
            }

            // 加载历史记录
            loadHistory(realChatId, isGroupChat) { msgs -> messages = msgs }

            // 标记已读 & 检查好友关系
            if (!isGroupChat) {
                RetrofitClient.apiService.markAsRead(realChatId)
                val friendsRes = RetrofitClient.apiService.getFriends()
                if (friendsRes.isSuccess()) {
                    val friendList = friendsRes.data ?: emptyList()
                    isFriend = friendList.any { it.userId == realChatId }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- 2. WebSocket 监听逻辑 ---
    LaunchedEffect(Unit) {
        WebSocketManager.messageFlow.collect { msgVO ->
            // 判断消息是否属于当前会话
            val isCurrentSessionMsg = if (isGroupChat) {
                // 群聊：receiverId 通常是群ID
                msgVO.receiverId == realChatId
            } else {
                // 私聊：发送者是对方
                msgVO.senderId == realChatId
            }

            if (isCurrentSessionMsg) {
                val newEntity = MessageEntity(
                    msgId = msgVO.id,
                    senderId = msgVO.senderId,
                    receiverId = msgVO.receiverId,
                    content = msgVO.content,
                    sentAt = msgVO.sentAt,
                    isRead = true
                    // 移除 avatarUrl，因为 MessageEntity 定义中没有该字段
                )
                messages = messages + newEntity

                // 收到消息即标记已读 (仅私聊)
                if (!isGroupChat) {
                    try { RetrofitClient.apiService.markAsRead(realChatId) } catch (_: Exception) {}
                }
            }
        }
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // 接受好友请求逻辑 (仅私聊)
    fun acceptAndGreet() {
        scope.launch {
            try {
                val acceptRes = RetrofitClient.apiService.acceptFriendRequest(realChatId)
                if (acceptRes.isSuccess()) {
                    isFriend = true
                    Toast.makeText(context, "已添加好友", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(friendName, fontWeight = FontWeight.Bold)
                            if (isGroupChat) {
                                Text("群聊 (${messages.size}条消息)", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                // 非好友且非群聊时显示接受栏
                if (!isFriend && !isGroupChat) {
                    Surface(
                        color = QingLianYellow.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("对方请求添加你为好友", fontSize = 14.sp)
                            Button(
                                onClick = { acceptAndGreet() },
                                colors = ButtonDefaults.buttonColors(containerColor = QingLianYellow),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("接受", color = Color.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (isFriend || isGroupChat) {
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
                                inputText = "" // 立即清空输入框，提升体验
                                scope.launch {
                                    try {
                                        // --- 修复开始：区分群聊和私聊传参 ---
                                        val sendDto = if (isGroupChat) {
                                            // 群聊：传 groupId，receiverId 为 null
                                            MessageSendDTO(
                                                groupId = realChatId.toLong(),
                                                content = contentToSend
                                            )
                                        } else {
                                            // 私聊：传 receiverId，groupId 为 null
                                            MessageSendDTO(
                                                receiverId = realChatId,
                                                content = contentToSend
                                            )
                                        }
                                        // --- 修复结束 ---

                                        val res = RetrofitClient.apiService.sendMessage(sendDto)
                                        if (res.isSuccess() && res.data != null) {
                                            val msgVO = res.data
                                            val newEntity = MessageEntity(
                                                msgId = msgVO.id,
                                                senderId = currentUserId,
                                                receiverId = realChatId, // 本地存储时，receiverId 存群ID或对方ID均可，只要加载逻辑一致
                                                content = contentToSend,
                                                sentAt = msgVO.sentAt,
                                                isRead = true
                                            )
                                            messages = messages + newEntity
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // 修复：由于 MessageEntity 没有 avatarUrl，我们使用传入的 friendAvatar (群聊时为群头像)
                val displayAvatar = if (isMe) currentUserAvatar else friendAvatar

                MessageItemRow(msg = msg, isMe = isMe, avatarUrl = displayAvatar)
            }
        }
    }
}

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

// --- 3. 历史记录加载逻辑 ---
suspend fun loadHistory(chatId: String, isGroup: Boolean, onResult: (List<MessageEntity>) -> Unit) {
    try {
        val response = if (isGroup) {
            RetrofitClient.apiService.getGroupMessageHistory(chatId.toLong(), 1, 50)
        } else {
            RetrofitClient.apiService.getMessageHistory(chatId, 1, 50)
        }

        if (response.isSuccess()) {
            onResult(response.data?.items ?: emptyList())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
