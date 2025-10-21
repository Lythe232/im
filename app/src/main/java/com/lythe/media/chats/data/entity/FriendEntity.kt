package com.lythe.media.chats.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lythe.media.R
import com.lythe.media.chats.data.model.FriendModel
import java.io.Serializable

@Entity(tableName = "friends")
data class FriendEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val friendId: String,
    val username: String? = "",
    val status: Int = 0,
    val signature: String? = "ta什么都没说",
    val avatar: String? = "",
    val relationStatus: Int = 0,
    val remark: String? = "",
    val createTime: Long?,
    val updateTime: Long?
): Serializable

data class FriendListResponse(
    val code: Int,
    val message: String,
    val data: List<FriendEntity>,
    val timestamp: String
)
data class FriendResultResponse(
    val code: Int,
    val message: String,
    val data: FriendEntity,
    val timestamp: String
)
object FriendConverter {
    val TAG: String = "FriendConverter"

    fun fromEntity(entity: FriendEntity): FriendModel {
        return FriendModel(
            id = entity.friendId,  // 使用 friendId 作为 Model 的 id
            username = entity.username,
            status = entity.status,
            signature = entity.signature,
            avatar = entity.avatar,
            remark = entity.remark,
            relationStatus = entity.relationStatus,
            createTime = entity.createTime,
            updateTime = entity.updateTime
        )

    }

    fun toEntity(model: FriendModel): FriendEntity {
        return FriendEntity(
            id = 0,  // 设置为 0，Room 会自动生成
            friendId = model.id,  // 使用 model 的 id 作为 friendId
            username = model.username ?: "",
            status = model.status,
            signature = model.signature,
            avatar = model.avatar ?: "",
            relationStatus = model.relationStatus ?: 0,
            remark = model.remark ?: "",
            createTime = model.createTime ?: System.currentTimeMillis(),
            updateTime = model.updateTime
        )
    }

    // 批量转换方法
    fun fromEntityList(entities: List<FriendEntity>): List<FriendModel> {
        return entities.map { fromEntity(it) }
    }

    fun toEntityList(models: List<FriendModel>): List<FriendEntity> {
        return models.map { toEntity(it) }
    }

    // 如果需要更新现有实体（保留原有的自动生成ID）
    fun updateEntity(existingEntity: FriendEntity, model: FriendModel): FriendEntity {
        return existingEntity.copy(
            friendId = model.id,
            username = model.username?: existingEntity.username,
            status = model.status,
            signature = model.signature,
            avatar = model.avatar ?: existingEntity.avatar,
            relationStatus = model.relationStatus ?: existingEntity.relationStatus,
            remark = model.remark ?: existingEntity.remark,
            createTime = model.createTime ?: existingEntity.createTime,
            updateTime = model.updateTime ?: existingEntity.updateTime
        )
    }

    fun status2Text(status: Int): String {
        return when(status) {
            0 -> "在线"
            1 -> "离线"
            else -> "离线"
        }
    }
    fun status2Image(status: Int): Int {
        return when(status) {
            0 -> R.drawable.shape_friend_online_status
            1 -> R.drawable.shape_friend_unonline_status
            else -> R.drawable.shape_friend_unonline_status
        }
    }
}