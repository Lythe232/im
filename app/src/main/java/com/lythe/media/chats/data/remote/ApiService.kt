package com.lythe.media.chats.data.remote

import com.lythe.media.chats.data.entity.FriendEntity
import com.lythe.media.chats.data.entity.FriendListResponse
import com.lythe.media.chats.data.entity.FriendResultResponse
import com.lythe.media.chats.data.entity.GroupEntity
import com.lythe.media.chats.data.model.FriendModel
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("/api/friend/friends")
    fun getFriends(): Call<FriendListResponse>
    @GET("api/friend/profile/{uid}")
    fun getFriendProfile(@Path("uid") uid: String): Call<FriendResultResponse>

    @POST("/api/friends/request")
    fun sendFriendRequest(@Body request: FriendRequest)

    @GET("/api/groups")
    fun getGroups(): List<GroupEntity>

    @POST("/api/groups/join")
    fun joinGroup(@Body joinRequest: GroupJoinRequest)

    @POST("/api/groups/leave")
    fun leaveGroup(@Body leaveRequest: GroupLeaveRequest)

}

data class FriendRequest(val friendId: String)
data class GroupJoinRequest(val groupId: String)
data class GroupLeaveRequest(val groupId: String)