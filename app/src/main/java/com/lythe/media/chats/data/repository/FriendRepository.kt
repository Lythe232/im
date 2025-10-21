package com.lythe.media.chats.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.lythe.media.chats.data.entity.FriendConverter
import com.lythe.media.chats.data.entity.FriendListResponse
import com.lythe.media.chats.data.local.dao.FriendDao
import com.lythe.media.chats.data.local.database.AppDatabase
import com.lythe.media.chats.data.model.FriendModel
import com.lythe.media.chats.data.remote.ApiService
import com.lythe.media.chats.data.repository.base.BaseRemoteRepository
import com.lythe.media.im.net.RetrofitClient
import okhttp3.internal.http2.Http2Reader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FriendRepository private constructor(context: Context) : BaseRemoteRepository() {
    companion object {
        private const val TAG = "FriendRepository"
        @Volatile
        private var instance: FriendRepository? = null
        fun getInstance(context: Context): FriendRepository {
            return instance ?: synchronized(this) {
                instance ?: FriendRepository(context.applicationContext).also { instance = it }
            }
        }
    }
    enum class LoadStatus {
        LOCAL,    // 本地数据
        REMOTE    // 远程更新后的数据
    }
    private val db = AppDatabase.getInstance(context)
    private val friendDao: FriendDao = db.friendDao()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    // 同步获取本地好友列表
    fun getLocalFriendsSync(): List<FriendModel> {
        return try {
            val allFriends = friendDao.getAllFriends()
            FriendConverter.fromEntityList(allFriends)
        } catch (e: Exception) {
            emptyList()
        }
    }
    // 从网络获取好友列表
    fun getRemoteFriendList(callback: Callback<FriendListResponse>) {
        val call = RetrofitClient.apiService.getFriends()
        execute(call, callback)
    }

    // 从本地数据库异步获取好友列表
    fun getLocalFriendList(callback: Callback<List<FriendModel>>) {
        executor.execute {
            try {
                val allFriends = friendDao.getAllFriends()
                val fromEntityList = FriendConverter.fromEntityList(allFriends)
                callback.onSuccess(fromEntityList)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    // 同步网络数据
    fun refreshFriends(callback: Callback<List<FriendModel>>) {
//        val localData = getLocalFriendsSync()
//        if(notifyLocal) {
//            mainHandler.post {
//                callback.onDataLoaded(localData, LoadStatus.LOCAL)
//            }
//        }

        executor.execute {
            try {
                val response = RetrofitClient.apiService.getFriends().execute()
                if(response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "refresh list")
                    val entityList = response.body()!!.data
                    friendDao.clear()
                    friendDao.insertFriends(entityList)

                    val fromEntityList = FriendConverter.fromEntityList(entityList)
                    mainHandler.post {
                        callback.onSuccess(fromEntityList)
                    }
                } else {
                    mainHandler.post {
                        callback.onError(Exception("网络请求失败"))
                    }
                }
            } catch (e: Exception) {
                val allFriends = friendDao.getAllFriends()
                val localFriends = FriendConverter.fromEntityList(allFriends)

                mainHandler.post {
                    if(localFriends.isNotEmpty()) {
                        callback.onSuccess(localFriends)
                    } else {
                        callback.onError(e);
                    }
                }
            }
        }
    }
    fun loadFriendProfile(uid: String, callback: Callback<FriendModel>) {
        executor.execute {
            try {
                val response = RetrofitClient.apiService.getFriendProfile(uid).execute()
                if(response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "loadFriendProfile")
                    val entity = response.body()!!.data
                    val fromEntity = FriendConverter.fromEntity(entity)
                    mainHandler.post {
                        callback.onSuccess(fromEntity)
                    }
                } else {
                    mainHandler.post {
                        callback.onError(Exception("网络请求失败"))
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    Log.e("Profile", "异常：${e.message}")
                    callback.onError(e)
                }
            }
        }
    }
}