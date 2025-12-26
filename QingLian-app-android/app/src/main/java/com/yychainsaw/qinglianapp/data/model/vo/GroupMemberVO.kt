package com.yychainsaw.qinglianapp.data.model.vo

data class GroupMemberVO(
    val userId: String,
    val username: String,
    val nickname: String?,
    val avatarUrl: String?,
    val role: String, // 例如: "OWNER", "ADMIN", "MEMBER"
    val joinedAt: String?
)