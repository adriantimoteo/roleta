package com.roleta.app.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roleta.app.data.db.RoletaDatabase
import com.roleta.app.data.db.dao.ListDao
import com.roleta.app.data.db.entity.ListEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListDaoTest {

    private lateinit var db: RoletaDatabase
    private lateinit var dao: ListDao

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, RoletaDatabase::class.java).build()
        dao = db.listDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertAndRetrieve() = runTest {
        val list = ListEntity("id1", "Movies", 1000L)
        dao.insert(list)
        val result = dao.getById("id1")
        assertNotNull(result)
        assertEquals("Movies", result!!.name)
    }

    @Test
    fun getListsAlpha_orderedCaseInsensitive() = runTest {
        dao.insert(ListEntity("a", "Zebra", 1L))
        dao.insert(ListEntity("b", "apple", 2L))
        dao.insert(ListEntity("c", "Mango", 3L))
        val lists = dao.getListsAlpha().first()
        assertEquals(listOf("apple", "Mango", "Zebra"), lists.map { it.name })
    }

    @Test
    fun getListsCreation_orderedByTimestamp() = runTest {
        dao.insert(ListEntity("a", "Last", 300L))
        dao.insert(ListEntity("b", "First", 100L))
        dao.insert(ListEntity("c", "Middle", 200L))
        val lists = dao.getListsCreation().first()
        assertEquals(listOf("First", "Middle", "Last"), lists.map { it.name })
    }

    @Test
    fun countByName_caseInsensitive() = runTest {
        dao.insert(ListEntity("id1", "Restaurants", 1L))
        assertEquals(1, dao.countByName("restaurants", ""))
        assertEquals(1, dao.countByName("RESTAURANTS", ""))
        assertEquals(0, dao.countByName("restaurants", "id1")) // excludes self
    }

    @Test
    fun deleteById_cascadesToItems() = runTest {
        dao.insert(ListEntity("id1", "ToDelete", 1L))
        dao.deleteById("id1")
        assertNull(dao.getById("id1"))
    }

    @Test
    fun renameList() = runTest {
        val list = ListEntity("id1", "Old Name", 1L)
        dao.insert(list)
        dao.update(list.copy(name = "New Name"))
        assertEquals("New Name", dao.getById("id1")!!.name)
    }

    @Test
    fun activeItemCount_updatesWithItems() = runTest {
        dao.insert(ListEntity("id1", "Test", 1L))
        val initial = dao.getListsAlpha().first().first()
        assertEquals(0, initial.activeCount)
    }
}
