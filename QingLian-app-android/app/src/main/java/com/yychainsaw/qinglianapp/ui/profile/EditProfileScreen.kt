package com.yychainsaw.qinglianapp.ui.profile

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.dto.UserUpdateDTO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import com.yychainsaw.qinglianapp.utils.UriUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 表单状态
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("MALE") } // 默认为 MALE，加载后会更新
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }

    // 1. 加载初始数据
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getUserInfo()
            if (response.isSuccess() && response.data != null) {
                val user = response.data
                nickname = user.nickname ?: ""
                // 如果后端返回 null，默认给 MALE，否则使用后端值
                gender = user.gender ?: "MALE"
                height = user.height?.toString() ?: ""
                weight = user.weight?.toString() ?: ""
                avatarUrl = user.avatarUrl
            }
        } catch (e: Exception) {
            Toast.makeText(context, "加载用户信息失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 图片选择器
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                uploadAndSetAvatar(context, uri) { newUrl ->
                    avatarUrl = newUrl
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = QingLianYellow)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QingLianYellow)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 头像编辑区域
                Box(modifier = Modifier.clickable {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // 昵称输入
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 性别选择 (UI显示中文，State存储英文枚举)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "性别",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // 选项：男
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { gender = "MALE" }
                                .padding(end = 24.dp)
                        ) {
                            RadioButton(
                                selected = gender == "MALE",
                                onClick = { gender = "MALE" },
                                colors = RadioButtonDefaults.colors(selectedColor = QingLianYellow)
                            )
                            Text("男")
                        }

                        // 选项：女
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { gender = "FEMALE" }
                        ) {
                            RadioButton(
                                selected = gender == "FEMALE",
                                onClick = { gender = "FEMALE" },
                                colors = RadioButtonDefaults.colors(selectedColor = QingLianYellow)
                            )
                            Text("女")
                        }
                    }
                }

                // 身高输入
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("身高 (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 体重输入
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("体重 (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 保存按钮
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val updateDto = UserUpdateDTO(
                                    nickname = nickname,
                                    gender = gender, // 传输 "MALE" 或 "FEMALE"
                                    height = height.toDoubleOrNull(),
                                    weight = weight.toDoubleOrNull(),
                                    avatarUrl = avatarUrl
                                )
                                val res = RetrofitClient.apiService.updateUserInfo(updateDto)
                                if (res.isSuccess()) {
                                    Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "修改失败: ${res.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "网络错误", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QingLianYellow),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("保存修改", color = Color.Black, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

// 辅助函数：上传头像
private suspend fun uploadAndSetAvatar(
    context: Context,
    uri: Uri,
    onSuccess: (String) -> Unit
) {
    try {
        // 使用 UriUtils 准备文件 Part
        val part = UriUtils.prepareFilePart(context, uri, "file")

        if (part != null) {
            val uploadResponse = RetrofitClient.apiService.upload(part)
            if (uploadResponse.isSuccess() && uploadResponse.data != null) {
                val newUrl = uploadResponse.data
                onSuccess(newUrl)
                // 可选：立即调用更新头像接口，或者等待用户点击保存时统一更新
                RetrofitClient.apiService.updateAvatar(newUrl)
            } else {
                Toast.makeText(context, "图片上传失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "文件读取失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "上传出错: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
