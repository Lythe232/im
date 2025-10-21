package com.lythe.media.chats.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity (
    @PrimaryKey val uid: String,
    val username: String = "",
    val email: String = "",
    val avatar: String,
    val gender: String,
    val signature: String,
    val createTime: Long,
    val lastLogin: Long,
    val status: String = "offline",
)