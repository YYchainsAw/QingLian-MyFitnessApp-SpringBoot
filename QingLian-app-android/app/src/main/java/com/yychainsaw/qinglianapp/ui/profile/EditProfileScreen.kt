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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.yychainsaw.qinglianapp.data.model.dto.UserUpdateDTO
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 表单状态
    var nickname by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("男") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // 初始化加载用户信息
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.apiService.getUserInfo()
            if (response.isSuccess() && response.data != null) {
                val user = response.data
                nickname = user.nickname ?: ""
                avatarUrl = user.avatarUrl ?: ""
                gender = user.gender ?: "男"
                height = user.height?.toString() ?: ""
                weight = user.weight?.toString() ?: ""
            }
        } catch (e: Exception) {
            Toast.makeText(context, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // 图片选择器
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                // 选择图片后立即上传获取URL
                uploadImage(context, it) { url ->
                    avatarUrl = url
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("编辑资料", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F7FA)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QingLianBlue)
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
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AsyncImage(
                        model = if (avatarUrl.isNotEmpty()) avatarUrl else "https://via.placeholder.com/150",
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(QingLianYellow)
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    }
                }

                // 输入表单
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) height = it },
                        label = { Text("身高 (cm)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) weight = it },
                        label = { Text("体重 (kg)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                // 性别选择
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("性别:", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = gender == "男", onClick = { gender = "男" })
                    Text("男")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = gender == "女", onClick = { gender = "女" })
                    Text("女")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 保存按钮
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                val updateDto = UserUpdateDTO(
                                    nickname = nickname,
                                    avatarUrl = avatarUrl,
                                    gender = gender,
                                    height = height.toDoubleOrNull(),
                                    weight = weight.toDoubleOrNull()
                                )
                                val res = RetrofitClient.apiService.updateUserInfo(updateDto)
                                if (res.isSuccess()) {
                                    Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "保存失败: ${res.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = QingLianBlue),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("保存修改", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 辅助函数：上传图片
private suspend fun uploadImage(context: Context, uri: Uri, onResult: (String) -> Unit) {
    try {
        val file = uriToFile(context, uri) ?: return
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        
        val response = RetrofitClient.apiService.upload(body)
        if (response.isSuccess() && response.data != null) {
            onResult(response.data)
        } else {
            Toast.makeText(context, "图片上传失败", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "上传出错: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// 辅助函数：Uri 转 File
private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        val myFile = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val outputStream = FileOutputStream(myFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        myFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
