package com.lythe.media.im.net

import android.content.Context
import com.lythe.media.chats.data.remote.ApiService
import com.lythe.media.im.net.interceptor.AuthInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


object RetrofitClient {
    private const val BASE_URL = "https://192.168.156.180:8443"

    private lateinit var application: Context
    fun init(application: Context) {
        this.application = application.applicationContext
    }

    private val okHttpClient by lazy {
        val app = application ?: throw IllegalStateException("请先调用init()初始化上下文")
//        SSLUtils.getSSLOkHttpClient(app)
            SSLUtils.getUnsafeOkHttpClient()
            .newBuilder()
            .connectTimeout(10, TimeUnit.SECONDS) // 连接超时
            .readTimeout(15, TimeUnit.SECONDS) // 读取超时
            .writeTimeout(15, TimeUnit.SECONDS) // 写入超时
            .retryOnConnectionFailure(true) // 是否自动重试
            .addInterceptor(AuthInterceptor())
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(ApiService::class.java)
    }
}