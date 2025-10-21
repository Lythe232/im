package com.lythe.media.chats.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val groupId: String,
    val groupName: String,
    val createdAt: Long,
    val updatedAt: Long
)
