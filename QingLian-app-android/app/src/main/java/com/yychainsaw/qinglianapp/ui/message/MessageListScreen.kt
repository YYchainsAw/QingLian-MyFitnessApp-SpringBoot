package com.yychainsaw.qinglianapp.ui.message

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.dto.GroupCreateDTO
import com.yychainsaw.qinglianapp.data.model.dto.GroupMemberAddDTO
import com.yychainsaw.qinglianapp.data.model.vo.ChatGroupVO
import com.yychainsaw.qinglianapp.data.model.vo.FriendVO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.network.WebSocketManager
import com.yychainsaw.qinglianapp.ui.MainViewModel
import com.yychainsaw.qinglianapp.ui.community.resolveImageUrl
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import com.yychainsaw.qinglianapp.utils.TokenManager
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

interface IChatItem {
    val id: String
    val sortTime: String?
}

data class FriendItemWrapper(val friend: FriendVO) : IChatItem {
    override val id = friend.userId
    override val sortTime = friend.lastMessageTime
}

data class GroupItemWrapper(val group: ChatGroupVO) : IChatItem {
    override val id = group.groupId.toString()
    override val sortTime = group.lastMessageTime
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    navController: NavController,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 显示列表数据
    var displayList by remember { mutableStateOf<List<IChatItem>>(emptyList()) }
    // 原始好友数据（用于创建群聊时选择）
    var allFriends by remember { mutableStateOf<List<FriendVO>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    // 1. 刷新未读数
    LaunchedEffect(Unit) { mainViewModel.refreshUnreadCount() }

    // 2. 连接 WebSocket
    LaunchedEffect(Unit) {
        val token = TokenManager.getToken(context)
        if (!token.isNullOrEmpty()) WebSocketManager.connect(token)
    }


    fun loadAllData() {
        isLoading = true
        scope.launch {
            try {

                val friendsRes = RetrofitClient.apiService.getFriends()
                val requestsRes = RetrofitClient.apiService.getPendingFriendRequests()
                val groupsRes = RetrofitClient.apiService.getMyGroups()

                val validFriends =
                    if (friendsRes.isSuccess()) friendsRes.data ?: emptyList() else emptyList()
                val pendingRequests =
                    if (requestsRes.isSuccess()) requestsRes.data ?: emptyList() else emptyList()
                val myGroups =
                    if (groupsRes.isSuccess()) groupsRes.data ?: emptyList() else emptyList()

                allFriends = validFriends

                val friendItems = (pendingRequests + validFriends).map { FriendItemWrapper(it) }
                val groupItems = myGroups.map { GroupItemWrapper(it) }

                val combined = (friendItems + groupItems).sortedByDescending { it.sortTime }

                displayList = combined
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadAllData() }


    LaunchedEffect(Unit) {
        WebSocketManager.messageFlow.collect { loadAllData() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("消息", fontWeight = FontWeight.Bold) },
                actions = {

                    IconButton(onClick = { showCreateGroupDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Group",
                            tint = Color.Black
                        )
                    }

                    IconButton(onClick = { navController.navigate("add_friend") }) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = "Add Friend",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QingLianYellow)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (displayList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("暂无消息", color = Color.Gray) }
                    }
                } else {
                    items(displayList) { item ->
                        when (item) {
                            is FriendItemWrapper -> {
                                FriendItemView(friend = item.friend) {
                                    val encodedAvatar =
                                        if (item.friend.avatarUrl != null) URLEncoder.encode(
                                            item.friend.avatarUrl,
                                            StandardCharsets.UTF_8.toString()
                                        ) else ""
                                    val displayName =
                                        item.friend.nickname?.takeIf { it.isNotBlank() }
                                            ?: item.friend.username
                                    navController.navigate("chat/${item.friend.userId}/$displayName?avatar=$encodedAvatar")
                                }
                            }

                            is GroupItemWrapper -> {
                                GroupItemView(group = item.group) {

                                    val encodedName = URLEncoder.encode(item.group.name, StandardCharsets.UTF_8.toString())
                                    val encodedAvatar = if (item.group.avatarUrl != null)
                                        URLEncoder.encode(item.group.avatarUrl, StandardCharsets.UTF_8.toString())
                                    else ""

                                    navController.navigate("chat/GROUP_${item.group.groupId}/$encodedName?avatar=$encodedAvatar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            friends = allFriends,
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { groupName, selectedIds ->
                scope.launch {
                    try {

                        val createDto = GroupCreateDTO(
                            name = groupName,
                            notice = "欢迎加入群聊",
                            avatarUrl = ""
                        )


                        val createRes = RetrofitClient.apiService.createGroup(createDto)

                        if (createRes.isSuccess() && createRes.data != null) {
                            val groupId = createRes.data


                            if (selectedIds.isNotEmpty()) {
                                try {
                                    val memberDto = GroupMemberAddDTO(userIds = selectedIds)
                                    val addRes = RetrofitClient.apiService.addGroupMembers(
                                        groupId,
                                        memberDto
                                    )
                                    if (!addRes.isSuccess()) {
                                        Toast.makeText(
                                            context,
                                            "群已创建，但邀请成员失败: ${addRes.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(
                                        context,
                                        "群已创建，但邀请成员时发生错误",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            Toast.makeText(context, "创建成功", Toast.LENGTH_SHORT).show()
                            showCreateGroupDialog = false
                            loadAllData()
                        } else {
                            Toast.makeText(
                                context,
                                "创建失败: ${createRes.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

// --- UI 组件 ---

@Composable
fun GroupItemView(group: ChatGroupVO, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(QingLianYellow.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (group.avatarUrl.isNullOrBlank()) {
                Icon(Icons.Default.Group, contentDescription = null, tint = QingLianYellow)
            } else {
                AsyncImage(model = resolveImageUrl(group.avatarUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = group.lastMessage ?: "暂无消息", fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatFriendlyTime(group.lastMessageTime), fontSize = 11.sp, color = Color.LightGray)
            if (group.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(containerColor = Color.Red) { Text(text = "${group.unreadCount}", color = Color.White, fontSize = 10.sp) }
            }
        }
    }
    Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(start = 82.dp))
}

@Composable
fun FriendItemView(friend: FriendVO, onClick: () -> Unit) {
    // 复用之前的 FriendItem 逻辑，改个名
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
                AsyncImage(model = resolveImageUrl(friend.avatarUrl), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = friend.nickname?.takeIf { it.isNotBlank() } ?: friend.username, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = friend.lastMessage ?: "点击开始聊天", fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = formatFriendlyTime(friend.lastMessageTime), fontSize = 11.sp, color = Color.LightGray)
            if (friend.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(containerColor = Color.Red) { Text(text = if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString(), color = Color.White, fontSize = 10.sp) }
            }
        }
    }
    Divider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(start = 82.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    friends: List<FriendVO>,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("创建群聊", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("群名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("选择成员", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedIds.contains(friend.userId)) selectedIds.remove(friend.userId)
                                    else selectedIds.add(friend.userId)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(friend.userId),
                                onCheckedChange = {
                                    if (it) selectedIds.add(friend.userId) else selectedIds.remove(friend.userId)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = QingLianYellow)
                            )
                            Text(
                                text = friend.nickname ?: friend.username,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("取消", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (groupName.isNotBlank()) onConfirm(groupName, selectedIds.toList())
                            else {} // 提示必填
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QingLianYellow),
                        enabled = groupName.isNotBlank()
                    ) {
                        Text("创建", color = Color.Black)
                    }
                }
            }
        }
    }
}
fun formatFriendlyTime(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return ""
    try {
        // 假设后端返回的时间格式是 ISO 8601 (例如: 2023-10-27T10:00:00)
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timeStr) ?: return ""

        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply { time = date }

        return when {
            // 如果是今天，显示 HH:mm
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            }
            // 如果是今年，显示 MM-dd
            now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) -> {
                SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
            }
            // 否则显示 yyyy-MM-dd
            else -> {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            }
        }
    } catch (e: Exception) {
        // 解析失败则不显示或显示原字符串
        return ""
    }
}
// formatFriendlyTime 保持不变
