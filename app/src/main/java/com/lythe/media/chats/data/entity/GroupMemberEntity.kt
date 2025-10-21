package com.lythe.media.chats.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_members")
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val userId: String,
    val joinedAt: Long
)
