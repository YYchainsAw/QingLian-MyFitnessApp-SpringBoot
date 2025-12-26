package com.yychainsaw.qinglianapp.network

import android.annotation.SuppressLint
import android.util.Log
import com.google.gson.Gson
import com.yychainsaw.qinglianapp.data.model.vo.MessageVO
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.util.concurrent.ConcurrentHashMap

object WebSocketManager {
    // 保持你的 URL 和 topic 路径不变 (根据日志，你的路径是用 . 分隔的)
    private const val WS_URL = "wss://7ed0e058.r7.cpolar.top/ws/websocket"
    private const val SUBSCRIBE_DESTINATION = "/user/queue/messages"

    private var stompClient: StompClient? = null
    private val gson = Gson()
    private val compositeDisposable = CompositeDisposable()
    private val groupSubscriptions = ConcurrentHashMap<Long, Disposable>()

    private val _messageFlow = MutableSharedFlow<MessageVO>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messageFlow = _messageFlow.asSharedFlow()

    @SuppressLint("CheckResult")
    fun connect(token: String) {
        if (stompClient?.isConnected == true) return
        disconnect()

        val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL)
        stompClient = client
        client.withClientHeartbeat(10000).withServerHeartbeat(10000)

        val headers = listOf(StompHeader("Authorization", "Bearer $token"))

        val lifecycleDisp = client.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d("WebSocket", "连接成功")
                        subscribeToPrivateTopic(client)
                    }
                    LifecycleEvent.Type.ERROR -> Log.e("WebSocket", "连接错误", lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> Log.d("WebSocket", "连接关闭")
                    else -> {}
                }
            }
        compositeDisposable.add(lifecycleDisp)
        client.connect(headers)
    }

    @SuppressLint("CheckResult")
    private fun subscribeToPrivateTopic(client: StompClient) {
        val disp = client.topic(SUBSCRIBE_DESTINATION)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                try {
                    val msg = gson.fromJson(topicMessage.payload, MessageVO::class.java)
                    _messageFlow.tryEmit(msg)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, { err -> Log.e("WebSocket", "私聊订阅失败", err) })
        compositeDisposable.add(disp)
    }

    @SuppressLint("CheckResult")
    fun joinGroup(groupId: Long) {
        if (stompClient == null || !stompClient!!.isConnected) return
        if (groupSubscriptions.containsKey(groupId)) return

        // 根据你的日志，路径是 /topic/group.15，所以这里用 . 分隔
        val topicPath = "/topic/group.$groupId"

        val disp = stompClient!!.topic(topicPath)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ topicMessage ->
                Log.d("WebSocket", "收到群($groupId)原始数据: ${topicMessage.payload}")
                try {
                    val msg = gson.fromJson(topicMessage.payload, MessageVO::class.java)

                    // --- 关键修复 ---
                    // 后端 JSON 里没有 groupId，我们在这里手动注入！
                    // 这样 ChatScreen 才能识别出这是哪个群的消息
                    val msgWithGroup = msg.copy(groupId = groupId)

                    _messageFlow.tryEmit(msgWithGroup)
                } catch (e: Exception) {
                    Log.e("WebSocket", "解析消息失败", e)
                }
            }, { err ->
                Log.e("WebSocket", "群($groupId)订阅失败", err)
            })

        groupSubscriptions[groupId] = disp
        Log.d("WebSocket", "已订阅群聊: $topicPath")
    }

    fun leaveGroup(groupId: Long) {
        groupSubscriptions[groupId]?.dispose()
        groupSubscriptions.remove(groupId)
    }

    fun disconnect() {
        compositeDisposable.clear()
        groupSubscriptions.values.forEach { it.dispose() }
        groupSubscriptions.clear()
        stompClient?.disconnect()
        stompClient = null
    }
}
