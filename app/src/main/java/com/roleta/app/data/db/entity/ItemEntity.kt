package com.roleta.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("list_id")]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id") val listId: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val status: String = ItemStatus.ACTIVE,
    @ColumnInfo(name = "skip_count") val skipCount: Int = 0
)

object ItemStatus {
    const val ACTIVE = "ACTIVE"
    const val PICKED = "PICKED"
}
