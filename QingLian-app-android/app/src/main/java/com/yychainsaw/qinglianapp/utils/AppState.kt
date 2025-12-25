package com.yychainsaw.qinglianapp.utils

object AppState {
    // 应用是否在前台
    var isAppInForeground: Boolean = false
    
    // 当前正在聊天的对象ID (如果在聊天界面)
    var currentChatFriendId: String? = null
    
    // 是否在消息列表页 (可选，需在 UI 层配合更新)
    var isAtMessageList: Boolean = false
}
