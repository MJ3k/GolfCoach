package com.example.golfcoach.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Retrofit 单例
// 以后在任何地方，只要 RetrofitClient.api.xxx(...) 就可以调用
object RetrofitClient {

    // 注意：
    // - 如果你用 Android 模拟器，并且 FastAPI 跑在你本机的 8000 端口，
    //   那么这里 BASE_URL 写 "http://10.0.2.2:8000"
    // - 如果你用真机，并且和电脑在同一个 WiFi 下，
    //   那么要写成电脑在局域网中的 IP，比如 "http://192.168.0.15:8000"
    private const val BASE_URL = "http://10.0.2.2:8000"

    // 对外暴露一个 ApiService 实例
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // 让 Retrofit 用 Gson 来把 JSON 转成 Kotlin data class
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
