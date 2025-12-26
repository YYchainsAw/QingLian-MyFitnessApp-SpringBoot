package com.yychainsaw.qinglianapp.data.api

import retrofit2.http.*


data class LoginReq(val username: String, val password: String)
data class TokenResp(val token: String, val userId: Long)
data class PostVO(val id: Long, val content: String, val username: String, val likeCount: Int)
// ... 其他 VO 可以根据需要补充

interface ApiService {
    // 登录
    @POST("auth/login")
    suspend fun login(@Body req: LoginReq): TokenResp

    // 获取社区 Feed
    @GET("community/feed")
    suspend fun getFeed(@Query("page") page: Int = 1): Any // 暂时用 Any，你需要定义 Page<PostVO> 的解析类
}