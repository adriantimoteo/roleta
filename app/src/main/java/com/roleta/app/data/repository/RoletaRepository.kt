package com.roleta.app.data.repository

import com.roleta.app.data.datastore.AppPreferences
import com.roleta.app.data.datastore.SortOrder
import com.roleta.app.data.db.dao.ItemDao
import com.roleta.app.data.db.dao.ListDao
import com.roleta.app.data.db.dao.ListWithCount
import com.roleta.app.data.db.dao.PickHistoryDao
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.data.db.entity.ItemStatus
import com.roleta.app.data.db.entity.ListEntity
import com.roleta.app.data.db.entity.PickHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class CreateListResult {
    data class Success(val id: String) : CreateListResult()
    data class Error(val message: String) : CreateListResult()
}

sealed class RestoreResult {
    data object Success : RestoreResult()
    data object AlreadyActive : RestoreResult()
}

@Singleton
class RoletaRepository @Inject constructor(
    private val listDao: ListDao,
    private val itemDao: ItemDao,
    private val pickHistoryDao: PickHistoryDao,
    private val appPreferences: AppPreferences
) {

    // ── Settings ──────────────────────────────────────────────────────────────

    val hasLaunched: Flow<Boolean> = appPreferences.hasLaunched
    val listSortOrder: Flow<SortOrder> = appPreferences.listSortOrder
    val itemSortOrder: Flow<SortOrder> = appPreferences.itemSortOrder

    suspend fun setHasLaunched() = appPreferences.setHasLaunched()
    suspend fun setListSortOrder(sort: SortOrder) = appPreferences.setListSortOrder(sort)
    suspend fun setItemSortOrder(sort: SortOrder) = appPreferences.setItemSortOrder(sort)

    // ── Lists ─────────────────────────────────────────────────────────────────

    fun getLists(): Flow<List<ListWithCount>> =
        appPreferences.listSortOrder.flatMapLatest { sort ->
            if (sort == SortOrder.ALPHA) listDao.getListsAlpha() else listDao.getListsCreation()
        }

    suspend fun getListById(id: String): ListEntity? = listDao.getById(id)

    suspend fun createList(name: String): CreateListResult {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return CreateListResult.Error("List name cannot be empty.")
        if (listDao.countByName(trimmed, "") > 0) return CreateListResult.Error("A list named \"$trimmed\" already exists.")
        val id = UUID.randomUUID().toString()
        listDao.insert(ListEntity(id = id, name = trimmed, createdAt = now()))
        return CreateListResult.Success(id)
    }

    suspend fun renameList(id: String, newName: String): String? {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return "List name cannot be empty."
        if (listDao.countByName(trimmed, id) > 0) return "A list named \"$trimmed\" already exists."
        val existing = listDao.getById(id) ?: return null
        listDao.update(existing.copy(name = trimmed))
        return null
    }

    suspend fun deleteList(id: String) = listDao.deleteById(id)

    // ── Items ─────────────────────────────────────────────────────────────────

    fun getActiveItems(listId: String): Flow<List<ItemEntity>> =
        appPreferences.itemSortOrder.flatMapLatest { sort ->
            if (sort == SortOrder.ALPHA)
                itemDao.getActiveItemsAlpha(listId)
            else
                itemDao.getActiveItemsCreation(listId)
        }

    suspend fun addItem(listId: String, text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return "Item text cannot be empty."
        if (itemDao.countActiveByText(listId, trimmed, "") > 0)
            return "\"$trimmed\" is already in this list."
        itemDao.insert(ItemEntity(id = UUID.randomUUID().toString(), listId = listId, text = trimmed, createdAt = now()))
        return null
    }

    suspend fun editItem(id: String, listId: String, newText: String): String? {
        val trimmed = newText.trim()
        if (trimmed.isBlank()) return "Item text cannot be empty."
        if (itemDao.countActiveByText(listId, trimmed, id) > 0)
            return "\"$trimmed\" is already in this list."
        val existing = itemDao.getById(id) ?: return null
        itemDao.update(existing.copy(text = trimmed))
        return null
    }

    suspend fun deleteItem(id: String) = itemDao.deleteById(id)

    // ── Spin ──────────────────────────────────────────────────────────────────

    suspend fun getActiveItemsOnce(listId: String): List<ItemEntity> =
        itemDao.getActiveItemsOnce(listId)

    /** Returns existing pick history for an item (to show "Picked X times"). */
    suspend fun getPickHistoryForItem(itemId: String): PickHistoryEntity? =
        pickHistoryDao.getByItemId(itemId)

    suspend fun skipItem(itemId: String) {
        itemDao.incrementSkipCount(itemId)
    }

    suspend fun acceptPick(itemId: String, listId: String) {
        itemDao.updateStatus(itemId, ItemStatus.PICKED)
        itemDao.resetSkipCount(itemId)
        val existing = pickHistoryDao.getByItemId(itemId)
        val item = itemDao.getById(itemId)
        val snapshot = item?.text ?: existing?.itemTextSnapshot ?: ""
        val ts = now()
        if (existing != null) {
            pickHistoryDao.update(existing.copy(pickCount = existing.pickCount + 1, lastPickedAt = ts))
        } else {
            pickHistoryDao.insert(
                PickHistoryEntity(
                    id = UUID.randomUUID().toString(),
                    itemId = itemId,
                    listId = listId,
                    itemTextSnapshot = snapshot,
                    lastPickedAt = ts,
                    pickCount = 1
                )
            )
        }
    }

    // ── History ───────────────────────────────────────────────────────────────

    fun getHistory(listId: String): Flow<List<PickHistoryEntity>> =
        pickHistoryDao.getHistoryForList(listId)

    fun getHistoryCount(listId: String): Flow<Int> =
        pickHistoryDao.getHistoryCountForList(listId)

    suspend fun restoreItem(historyId: String): RestoreResult {
        val entry = pickHistoryDao.getById(historyId) ?: return RestoreResult.Success

        // Check for collision: an active item with the same text already exists
        val activeCollision = itemDao.countActiveByText(entry.listId, entry.itemTextSnapshot, "") > 0
        if (activeCollision) {
            pickHistoryDao.deleteById(historyId)
            return RestoreResult.AlreadyActive
        }

        if (entry.itemId != null) {
            // Item still exists (status=PICKED); flip it back to ACTIVE
            itemDao.updateStatus(entry.itemId, ItemStatus.ACTIVE)
        } else {
            // Item was deleted; re-create it and link it back to this history entry
            val newId = UUID.randomUUID().toString()
            itemDao.insert(
                ItemEntity(
                    id = newId,
                    listId = entry.listId,
                    text = entry.itemTextSnapshot,
                    createdAt = now(),
                    status = ItemStatus.ACTIVE
                )
            )
            pickHistoryDao.updateItemId(historyId, newId)
        }
        return RestoreResult.Success
    }

    // ── Import / Export ───────────────────────────────────────────────────────

    suspend fun exportItems(listId: String): List<String> =
        itemDao.getActiveItemsOnce(listId).map { it.text }

    /**
     * Replaces active items for a list with the provided lines.
     * Blank/duplicate lines are filtered. History is untouched.
     * Returns an error string if there are no valid items in the input.
     */
    suspend fun importItems(listId: String, lines: List<String>): String? {
        val valid = lines
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        if (valid.isEmpty()) return "No valid items found in the file."

        // Delete existing active items
        val existing = itemDao.getActiveItemsOnce(listId)
        existing.forEach { itemDao.deleteById(it.id) }

        // Insert new items
        val ts = now()
        valid.forEachIndexed { idx, text ->
            itemDao.insert(
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    text = text,
                    createdAt = ts + idx
                )
            )
        }
        return null
    }

    // ── First-launch seeding ──────────────────────────────────────────────────

    suspend fun seedSampleList() {
        val listId = UUID.randomUUID().toString()
        listDao.insert(ListEntity(id = listId, name = "Sample List", createdAt = now()))
        val items = listOf("Pizza", "Tacos", "Ramen", "Sushi", "Burger", "Pasta")
        items.forEachIndexed { idx, text ->
            itemDao.insert(
                ItemEntity(
                    id = UUID.randomUUID().toString(),
                    listId = listId,
                    text = text,
                    createdAt = now() + idx
                )
            )
        }
        setHasLaunched()
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun now() = System.currentTimeMillis()
}
