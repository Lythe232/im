package com.lythe.media.im.net.interceptor

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.lythe.media.im.net.AuthManager
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody

class AuthInterceptor: Interceptor {

    private val authManager: AuthManager = AuthManager.getInstance()
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if(!authManager.isTokenValid) {
            val b = authManager.refreshTokenSync()
            if(!b) {
                return createUnauthorizedResponse(originalRequest)
            }
        }
        val authenticatedRequest = addAuthHeaders(originalRequest)
        val response = chain.proceed(authenticatedRequest)
        if(response.code == 401) {
            response.close()
            val refreshSuccess = authManager.refreshTokenSync()
            if(refreshSuccess) {
                val retryRequest = addAuthHeaders(originalRequest)
                return chain.proceed(retryRequest)
            } else {
                return createUnauthorizedResponse(originalRequest)
            }
        }
        return response
    }
    private fun addAuthHeaders(originRequest: Request): Request {
        val authInfo = authManager.authInfo
        val builder = originRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        if(authInfo.token != null) {
            builder.header("Authorization", authInfo.token);
        }
        if(authInfo.uid != null) {
            builder.header("Uid", authInfo.uid);
        }
        return builder.build()
    }
    private fun createUnauthorizedResponse(request: Request): Response {
        val mediaType = "text/plain".toMediaType() // 使用 MediaType.get() 获取类型
        val body = "Authentication required".toResponseBody(mediaType) // 创建响应体
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body(body)
            .build()
    }
}