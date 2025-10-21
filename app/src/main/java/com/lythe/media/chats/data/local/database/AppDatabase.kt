package com.lythe.media.chats.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lythe.media.chats.data.entity.ConversationEntity
import com.lythe.media.chats.data.entity.FriendEntity
import com.lythe.media.chats.data.entity.GroupEntity
import com.lythe.media.chats.data.entity.GroupMemberEntity
import com.lythe.media.chats.data.entity.MessageEntity
import com.lythe.media.chats.data.entity.UserEntity
import com.lythe.media.chats.data.local.dao.ConversationDao
import com.lythe.media.chats.data.local.dao.FriendDao
import com.lythe.media.chats.data.local.dao.GroupDao
import com.lythe.media.chats.data.local.dao.GroupMemberDao
import com.lythe.media.chats.data.local.dao.MessageDao
import java.util.concurrent.Executors

@Database(entities = [
    FriendEntity::class,
    GroupEntity::class,
    GroupMemberEntity::class,
    MessageEntity::class,
    UserEntity::class,
    ConversationEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "chat_database"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d(TAG, "数据库创建成功")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Log.d(TAG, "数据库打开成功")
                }
            })
                .setQueryExecutor(Executors.newSingleThreadExecutor())
                .fallbackToDestructiveMigration(true)   //删除旧表重新建，数据全清
                .build()
        }
        fun closeDatabase() {
            instance?.close()
            instance = null
            Log.d(TAG, "数据库已关闭")
        }
    }
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun conversationDao(): ConversationDao
    abstract fun friendDao(): FriendDao
}