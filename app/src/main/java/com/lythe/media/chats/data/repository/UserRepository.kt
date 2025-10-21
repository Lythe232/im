package com.lythe.media.chats.data.repository

import android.content.Context
import com.lythe.media.chats.data.local.database.AppDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UserRepository private constructor(context: Context){
    companion object {
        private const val TAG = "UserRepository"

        @Volatile
        private var instance: UserRepository? = null
        fun getInstance(context: Context): UserRepository {
            return instance ?: synchronized(this) {
                instance ?: UserRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    private val db = AppDatabase.getInstance(context)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

}