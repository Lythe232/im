package com.lythe.media.chats.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.lythe.media.chats.data.entity.GroupMemberEntity

@Dao
interface GroupMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertGroupMember(member: GroupMemberEntity)
}