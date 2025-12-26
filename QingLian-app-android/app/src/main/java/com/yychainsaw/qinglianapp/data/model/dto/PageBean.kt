package com.yychainsaw.qinglianapp.data.model.dto

data class PageBean<T>(
    val total: Long,
    val items: List<T>
)