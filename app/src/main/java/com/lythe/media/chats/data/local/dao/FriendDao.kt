package com.lythe.media.chats.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lythe.media.chats.data.entity.FriendEntity

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFriends(friends: List<FriendEntity>)
    @Query("SELECT username FROM friends WHERE friendId = :uid")
    fun getUserNameById(uid: String): String?;
    @Query("SELECT * FROM friends ORDER BY username ASC")
    fun getAllFriends() : List<FriendEntity>
    @Query("DELETE FROM friends")
    fun clear();
}