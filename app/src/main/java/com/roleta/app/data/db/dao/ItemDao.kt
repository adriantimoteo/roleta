package com.roleta.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.roleta.app.data.db.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE list_id = :listId AND status = 'ACTIVE' ORDER BY LOWER(text) ASC")
    fun getActiveItemsAlpha(listId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE list_id = :listId AND status = 'ACTIVE' ORDER BY created_at ASC")
    fun getActiveItemsCreation(listId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE list_id = :listId AND status = 'ACTIVE'")
    suspend fun getActiveItemsOnce(listId: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): ItemEntity?

    @Query("""
        SELECT COUNT(*) FROM items
        WHERE list_id = :listId AND LOWER(text) = LOWER(:text) AND status = 'ACTIVE' AND id != :excludeId
    """)
    suspend fun countActiveByText(listId: String, text: String, excludeId: String): Int

    @Query("UPDATE items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE items SET skip_count = skip_count + 1 WHERE id = :id")
    suspend fun incrementSkipCount(id: String)

    @Query("UPDATE items SET skip_count = 0 WHERE id = :id")
    suspend fun resetSkipCount(id: String)

    @Insert
    suspend fun insert(item: ItemEntity)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)
}
