package com.yychainsaw.qinglianapp.data.model.vo

data class MessageVO(
    val id: Long,
    val senderId: String,
    val senderName: String? = null,
    val receiverId: String? = null,
    val groupId: Long? = null,
    val content: String,
    val sentAt: String,
    val isRead: Boolean
)