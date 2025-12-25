package com.yychainsaw.qinglianapp.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yychainsaw.qinglianapp.ui.MainViewModel
import com.yychainsaw.qinglianapp.ui.community.CommunityScreen
import com.yychainsaw.qinglianapp.ui.fitness.FitnessScreen
import com.yychainsaw.qinglianapp.ui.message.MessageListScreen
import com.yychainsaw.qinglianapp.ui.profile.ProfileScreen
import com.yychainsaw.qinglianapp.ui.theme.QingLianBlue
import com.yychainsaw.qinglianapp.ui.theme.QingLianGreen
import com.yychainsaw.qinglianapp.ui.theme.QingLianYellow

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val hasBadge: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    // 注入 ViewModel
    mainViewModel: MainViewModel = viewModel()
) {
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    // 使用 ViewModel 中的状态，不再轮询
    val unreadCount by mainViewModel.totalUnreadCount.collectAsState()

    BackHandler(enabled = selectedItemIndex != 0) {
        selectedItemIndex = 0
    }

    val items = listOf(
        BottomNavItem("社区", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("健身", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
        BottomNavItem("消息", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, hasBadge = true),
        BottomNavItem("我的", Icons.Filled.Person, Icons.Outlined.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = QingLianYellow.copy(alpha = 0.1f)
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = { selectedItemIndex = index },
                        label = { Text(item.label) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.hasBadge && unreadCount > 0) {
                                        Badge {
                                            Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (index == selectedItemIndex) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = QingLianBlue,
                            selectedTextColor = QingLianBlue,
                            indicatorColor = QingLianGreen.copy(alpha = 0.3f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedItemIndex) {
                0 -> CommunityScreen(
                    onPostCreate = { navController.navigate("post_create") }
                )
                1 -> FitnessScreen()
                // 将 ViewModel 传递给 MessageListScreen，以便它能触发刷新
                2 -> MessageListScreen(navController, mainViewModel)
                3 -> ProfileScreen(navController)
            }
        }
    }
}
