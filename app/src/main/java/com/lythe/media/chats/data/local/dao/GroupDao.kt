package com.lythe.media.chats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lythe.media.chats.data.entity.GroupEntity

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroup(groupEntity: GroupEntity)
    @Query("SELECT groupName FROM `groups` WHERE groupId = :groupId")
    fun getGroupNameById(groupId: String): String?
}