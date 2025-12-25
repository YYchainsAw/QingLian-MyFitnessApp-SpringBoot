package com.yychainsaw.qinglianapp.data.model.dto

data class GroupCreateDTO(
    val name: String,
    val avatarUrl: String? = null,
    val notice: String? = null
)