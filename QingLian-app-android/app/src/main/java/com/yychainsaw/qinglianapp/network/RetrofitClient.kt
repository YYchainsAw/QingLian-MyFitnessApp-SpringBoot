package com.yychainsaw.qinglianapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://7ed0e058.r7.cpolar.top/"

    // 全局保存 Token，在 MainActivity 启动或登录成功时赋值
    var authToken: String? = null

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            // 添加认证拦截器
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()

                // 如果有 Token，则添加到 Header
                authToken?.let { token ->
                    // 根据你的接口文档，Header 是 Authorization: <token>
                    requestBuilder.header("Authorization", token)
                }

                chain.proceed(requestBuilder.build())
            }
            // 修改超时时间为 5 分钟，解决批量上传图片超时问题
            .connectTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES) // 上传文件主要依赖写入超时
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
