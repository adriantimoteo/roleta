package com.roleta.app.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roleta.app.data.db.RoletaDatabase
import com.roleta.app.data.db.dao.ItemDao
import com.roleta.app.data.db.dao.ListDao
import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.data.db.entity.ItemStatus
import com.roleta.app.data.db.entity.ListEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var db: RoletaDatabase
    private lateinit var listDao: ListDao
    private lateinit var itemDao: ItemDao
    private val listId = "list1"

    @Before
    fun setup() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, RoletaDatabase::class.java).build()
        listDao = db.listDao()
        itemDao = db.itemDao()
        listDao.insert(ListEntity(listId, "Test List", 1L))
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndGetActive() = runTest {
        itemDao.insert(item("i1", "Pizza"))
        itemDao.insert(item("i2", "Ramen"))
        val active = itemDao.getActiveItemsAlpha(listId).first()
        assertEquals(2, active.size)
    }

    @Test
    fun getActiveItemsAlpha_sortedCaseInsensitive() = runTest {
        itemDao.insert(item("i1", "Zebra"))
        itemDao.insert(item("i2", "apple"))
        itemDao.insert(item("i3", "Mango"))
        val names = itemDao.getActiveItemsAlpha(listId).first().map { it.text }
        assertEquals(listOf("apple", "Mango", "Zebra"), names)
    }

    @Test
    fun pickedItemsNotInActiveQuery() = runTest {
        itemDao.insert(item("i1", "Pizza"))
        itemDao.insert(item("i2", "Ramen"))
        itemDao.updateStatus("i1", ItemStatus.PICKED)
        val active = itemDao.getActiveItemsAlpha(listId).first()
        assertEquals(1, active.size)
        assertEquals("Ramen", active.first().text)
    }

    @Test
    fun countActiveByText_caseInsensitive() = runTest {
        itemDao.insert(item("i1", "Jollibee"))
        assertEquals(1, itemDao.countActiveByText(listId, "jollibee", ""))
        assertEquals(1, itemDao.countActiveByText(listId, "JOLLIBEE", ""))
        assertEquals(0, itemDao.countActiveByText(listId, "jollibee", "i1")) // excludes self
    }

    @Test
    fun pickedItemsNotCountedAsDuplicates() = runTest {
        itemDao.insert(item("i1", "Pizza"))
        itemDao.updateStatus("i1", ItemStatus.PICKED)
        // Can now re-add "Pizza" since it's picked, not active
        assertEquals(0, itemDao.countActiveByText(listId, "Pizza", ""))
    }

    @Test
    fun deleteItem_removesFromActive() = runTest {
        itemDao.insert(item("i1", "Tacos"))
        itemDao.deleteById("i1")
        assertNull(itemDao.getById("i1"))
        assertTrue(itemDao.getActiveItemsOnce(listId).isEmpty())
    }

    @Test
    fun editItem() = runTest {
        val it = item("i1", "Old")
        itemDao.insert(it)
        itemDao.update(it.copy(text = "New"))
        assertEquals("New", itemDao.getById("i1")!!.text)
    }

    private fun item(id: String, text: String) =
        ItemEntity(id = id, listId = listId, text = text, createdAt = System.currentTimeMillis())
}
