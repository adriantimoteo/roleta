package com.roleta.app.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pick_history",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("list_id"), Index("item_id")]
)
data class PickHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String?,
    @ColumnInfo(name = "list_id") val listId: String,
    @ColumnInfo(name = "item_text_snapshot") val itemTextSnapshot: String,
    @ColumnInfo(name = "last_picked_at") val lastPickedAt: Long,
    @ColumnInfo(name = "pick_count") val pickCount: Int = 1
)
