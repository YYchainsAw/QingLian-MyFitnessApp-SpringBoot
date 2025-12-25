package com.yychainsaw.qinglianapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yychainsaw.qinglianapp.network.RetrofitClient
import com.yychainsaw.qinglianapp.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _totalUnreadCount = MutableStateFlow(0L)
    val totalUnreadCount: StateFlow<Long> = _totalUnreadCount.asStateFlow()

    init {
        // 1. 初始化时：调用一次 HTTP 接口获取基准数据
        fetchInitialUnreadCount()

        // 2. 监听 WebSocket：有新消息来，本地计数 +1
        observeWebSocket()
    }

    private fun fetchInitialUnreadCount() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getUnreadCount()
                if (response.isSuccess()) {
                    _totalUnreadCount.value = response.data ?: 0L
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            WebSocketManager.messageFlow.collect { message ->
                // 收到新消息，未读数 +1
                // (这里假设 WebSocket 推送过来的都是未读消息)
                _totalUnreadCount.value += 1
            }
        }
    }

    // 当用户查看完消息返回时，调用此方法校准未读数
    fun refreshUnreadCount() {
        fetchInitialUnreadCount()
    }
}
