package com.yychainsaw.qinglianapp.data.model.vo

import com.google.gson.annotations.SerializedName

data class FriendVO(
    val userId: String,
    val username: String,
    @SerializedName("nickname", alternate = ["nick_name", "nickName"])
    val nickname: String? = null,
    val avatarUrl: String?,
    val email: String?,

    // --- 关键修改：添加 @SerializedName 注解 ---
    // 兼容后端可能返回的下划线命名 (snake_case)
    @SerializedName("lastMessage", alternate = ["last_message", "content", "message"])
    val lastMessage: String? = null,

    @SerializedName("lastMessageTime", alternate = ["last_message_time", "sentAt", "sent_at", "time", "createTime"])
    val lastMessageTime: String? = null,

    @SerializedName("unreadCount", alternate = ["unread_count", "unread", "count"])
    val unreadCount: Int = 0
)
