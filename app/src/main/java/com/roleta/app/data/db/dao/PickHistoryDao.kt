package com.roleta.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.roleta.app.data.db.entity.PickHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PickHistoryDao {

    // Shows history entries where:
    // - item was deleted (item_id IS NULL), OR
    // - item still exists but has been accepted/picked (status = 'PICKED')
    // Restoring an item sets it back to ACTIVE, which hides it from this query.
    @Query("""
        SELECT ph.id, ph.item_id, ph.list_id, ph.item_text_snapshot, ph.last_picked_at, ph.pick_count
        FROM pick_history ph
        LEFT JOIN items i ON ph.item_id = i.id
        WHERE ph.list_id = :listId
          AND (ph.item_id IS NULL OR i.status = 'PICKED')
        ORDER BY ph.last_picked_at DESC
    """)
    fun getHistoryForList(listId: String): Flow<List<PickHistoryEntity>>

    @Query("SELECT COUNT(*) FROM pick_history WHERE list_id = :listId AND (item_id IS NULL OR item_id IN (SELECT id FROM items WHERE status = 'PICKED'))")
    fun getHistoryCountForList(listId: String): Flow<Int>

    @Query("SELECT * FROM pick_history WHERE item_id = :itemId")
    suspend fun getByItemId(itemId: String): PickHistoryEntity?

    @Query("SELECT * FROM pick_history WHERE id = :id")
    suspend fun getById(id: String): PickHistoryEntity?

    @Insert
    suspend fun insert(entry: PickHistoryEntity)

    @Update
    suspend fun update(entry: PickHistoryEntity)

    @Query("DELETE FROM pick_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pick_history SET item_id = :newItemId WHERE id = :historyId")
    suspend fun updateItemId(historyId: String, newItemId: String)
}
