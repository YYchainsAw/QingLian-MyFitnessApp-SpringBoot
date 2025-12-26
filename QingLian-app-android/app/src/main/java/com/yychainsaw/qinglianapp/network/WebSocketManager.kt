package com.yychainsaw.qinglianapp.network

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.Gson
import com.yychainsaw.qinglianapp.data.model.vo.MessageVO
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage

object WebSocketManager {
    // 1. WebSocket 地址
    private const val WS_URL = "wss://7ed0e058.r7.cpolar.top/ws/websocket"

    // 2. 订阅路径：只负责接收
    private const val SUBSCRIBE_DESTINATION = "/user/queue/messages"

    private var stompClient: StompClient? = null
    private val gson = Gson()

    private val _messageFlow = MutableSharedFlow<MessageVO>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    @SuppressLint("CheckResult")
    fun connect(token: String) {
        if (stompClient?.isConnected == true) return

        val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL)
        stompClient = client

        // 设置心跳
        client.withClientHeartbeat(10000).withServerHeartbeat(10000)

        val headers = listOf(StompHeader("Authorization", "Bearer $token"))

        client.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d("WebSocket", "连接成功 (OPENED)，开始订阅消息...")
                        subscribeToTopic(client)
                    }
                    LifecycleEvent.Type.ERROR -> Log.e("WebSocket", "连接错误", lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> Log.d("WebSocket", "连接关闭")
                    else -> {}
                }
            }

        client.connect(headers)
    }

    @SuppressLint("CheckResult")
    private fun subscribeToTopic(client: StompClient) {
        client.topic(SUBSCRIBE_DESTINATION)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage: StompMessage ->
                try {
                    val payload = topicMessage.payload
                    Log.d("WebSocket", "收到消息: $payload")
                    val messageVO = gson.fromJson(payload, MessageVO::class.java)
                    _messageFlow.tryEmit(messageVO)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, { e ->
                Log.e("WebSocket", "订阅出错", e)
            })
    }


    fun emitMessage(messageVO: MessageVO) {
        _messageFlow.tryEmit(messageVO)
    }

    fun disconnect() {
        stompClient?.disconnect()
        stompClient = null
    }
}
