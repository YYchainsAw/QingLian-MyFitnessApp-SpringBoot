package com.yychainsaw.qinglianapp.data.model.vo

data class ChatGroupVO(
    val groupId: Long,
    val name: String,
    val avatarUrl: String?,
    val notice: String?,
    val ownerId: String?,
    // 用于列表展示的辅助字段 (后端可能需要额外返回，或者前端暂存)
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val unreadCount: Int = 0
)