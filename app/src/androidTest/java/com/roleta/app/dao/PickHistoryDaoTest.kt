package com.roleta.app.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roleta.app.data.db.RoletaDatabase
import com.roleta.app.data.db.dao.ItemDao
import com.roleta.app.data.db.dao.ListDao
import com.roleta.app.data.db.dao.PickHistoryDao
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.data.db.entity.ItemStatus
import com.roleta.app.data.db.entity.ListEntity
import com.roleta.app.data.db.entity.PickHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PickHistoryDaoTest {

    private lateinit var db: RoletaDatabase
    private lateinit var listDao: ListDao
    private lateinit var itemDao: ItemDao
    private lateinit var historyDao: PickHistoryDao
    private val listId = "list1"

    @Before
    fun setup() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, RoletaDatabase::class.java).build()
        listDao = db.listDao()
        itemDao = db.itemDao()
        historyDao = db.pickHistoryDao()
        listDao.insert(ListEntity(listId, "Test List", 1L))
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndGetByItemId() = runTest {
        val itemId = "item1"
        itemDao.insert(ItemEntity(itemId, listId, "Pizza", 1L, ItemStatus.PICKED))
        historyDao.insert(historyEntry("h1", itemId))
        val result = historyDao.getByItemId(itemId)
        assertNotNull(result)
        assertEquals("Pizza", result!!.itemTextSnapshot)
    }

    @Test
    fun getHistoryForList_showsPickedItems() = runTest {
        val itemId = "item1"
        itemDao.insert(ItemEntity(itemId, listId, "Ramen", 1L, ItemStatus.PICKED))
        historyDao.insert(historyEntry("h1", itemId))
        val entries = historyDao.getHistoryForList(listId).first()
        assertEquals(1, entries.size)
    }

    @Test
    fun getHistoryForList_hidesRestoredItems() = runTest {
        val itemId = "item1"
        itemDao.insert(ItemEntity(itemId, listId, "Sushi", 1L, ItemStatus.PICKED))
        historyDao.insert(historyEntry("h1", itemId))

        // Restore: flip item to ACTIVE
        itemDao.updateStatus(itemId, ItemStatus.ACTIVE)

        val entries = historyDao.getHistoryForList(listId).first()
        // History row exists but the item is now ACTIVE, so it should not appear
        assertEquals(0, entries.size)
    }

    @Test
    fun getHistoryForList_showsDeletedItemEntries() = runTest {
        // item_id NULL = item was deleted, still shows in history
        historyDao.insert(historyEntry("h1", null))
        val entries = historyDao.getHistoryForList(listId).first()
        assertEquals(1, entries.size)
    }

    @Test
    fun deleteItem_setsItemIdNullInHistory() = runTest {
        val itemId = "item1"
        itemDao.insert(ItemEntity(itemId, listId, "Burger", 1L, ItemStatus.PICKED))
        historyDao.insert(historyEntry("h1", itemId))

        // Deleting the item should SET NULL on the FK
        itemDao.deleteById(itemId)

        val entry = historyDao.getById("h1")
        assertNotNull(entry)
        assertNull(entry!!.itemId)
        assertEquals("Burger", entry.itemTextSnapshot)
    }

    @Test
    fun deleteList_cascadesToHistory() = runTest {
        historyDao.insert(historyEntry("h1", null))
        listDao.deleteById(listId)
        val entries = historyDao.getHistoryForList(listId).first()
        assertEquals(0, entries.size)
    }

    @Test
    fun updateItemId() = runTest {
        historyDao.insert(historyEntry("h1", null))
        historyDao.updateItemId("h1", "newItem")
        val entry = historyDao.getById("h1")
        assertEquals("newItem", entry!!.itemId)
    }

    private fun historyEntry(id: String, itemId: String?) = PickHistoryEntity(
        id = id,
        itemId = itemId,
        listId = listId,
        itemTextSnapshot = "Pizza",
        lastPickedAt = System.currentTimeMillis(),
        pickCount = 1
    )
}
