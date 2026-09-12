package com.roleta.app.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.roleta.app.data.db.entity.ListEntity
import kotlinx.coroutines.flow.Flow

data class ListWithCount(
    val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "active_count") val activeCount: Int
)

@Dao
interface ListDao {

    @Query("""
        SELECT l.id, l.name, l.created_at,
               (SELECT COUNT(*) FROM items WHERE list_id = l.id AND status = 'ACTIVE') AS active_count
        FROM lists l
        ORDER BY LOWER(l.name) ASC
    """)
    fun getListsAlpha(): Flow<List<ListWithCount>>

    @Query("""
        SELECT l.id, l.name, l.created_at,
               (SELECT COUNT(*) FROM items WHERE list_id = l.id AND status = 'ACTIVE') AS active_count
        FROM lists l
        ORDER BY l.created_at ASC
    """)
    fun getListsCreation(): Flow<List<ListWithCount>>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getById(id: String): ListEntity?

    @Query("SELECT COUNT(*) FROM lists WHERE LOWER(name) = LOWER(:name) AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: String): Int

    @Insert
    suspend fun insert(list: ListEntity)

    @Update
    suspend fun update(list: ListEntity)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteById(id: String)
}
