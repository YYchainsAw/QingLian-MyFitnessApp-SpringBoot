package com.yychainsaw.qinglianapp.data.model.dto

data class MessageSendDTO(
    val receiverId: String,
    val groupId: Long? = null,
    val content: String
)