package com.yychainsaw.qinglianapp.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import com.yychainsaw.qinglianapp.data.api.ApiService


object NetworkModule {
    // 模拟器访问本地电脑必须用这个 IP
    private const val BASE_URL = "https://6874fe46.r7.cpolar.top/"
    

    var userToken: String? = null

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            // 如果有 Token，自动加到 Header
            userToken?.let {
                requestBuilder.header("Authorization", it)
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}