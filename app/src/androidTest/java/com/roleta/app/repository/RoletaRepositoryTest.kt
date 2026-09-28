package com.roleta.app.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roleta.app.data.datastore.AppPreferences
import com.roleta.app.data.db.RoletaDatabase
import com.roleta.app.data.db.entity.ItemStatus
import com.roleta.app.data.repository.BulkImportParser
import com.roleta.app.data.repository.CreateListResult
import com.roleta.app.data.repository.RestoreResult
import com.roleta.app.data.repository.RoletaRepository
import com.roleta.app.data.repository.SkipReason
import com.roleta.app.data.repository.SkippedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoletaRepositoryTest {

    private lateinit var db: RoletaDatabase
    private lateinit var repository: RoletaRepository
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStoreFile: File

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, RoletaDatabase::class.java).build()
        dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStoreFile = File(ctx.cacheDir, "repo-test-${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(scope = dataStoreScope) { dataStoreFile }
        repository = RoletaRepository(db, db.listDao(), db.itemDao(), db.pickHistoryDao(), AppPreferences(dataStore))
    }

    @After
    fun teardown() {
        db.close()
        dataStoreScope.cancel()
        dataStoreFile.delete()
    }

    // ── Bulk import ───────────────────────────────────────────────────────────

    @Test
    fun bulkImport_newTitle_createsListWithItems() = runTest {
        val result = repository.bulkImport(BulkImportParser.parse("Trips\nBaguio\nLa Union\nbaguio")!!)

        assertTrue(result.createdList)
        assertEquals("Trips", result.listName)
        assertEquals(2, result.addedCount)
        assertEquals(listOf(SkippedItem("baguio", SkipReason.DUPLICATE_IN_TEXT)), result.skipped)
        val texts = db.itemDao().getActiveItemsOnce(result.listId).map { it.text }.toSet()
        assertEquals(setOf("Baguio", "La Union"), texts)
    }

    @Test
    fun bulkImport_existingTitle_addsToThatListAndSkipsActiveDuplicates() = runTest {
        val listId = (repository.createList("Movies") as CreateListResult.Success).id
        repository.addItem(listId, "Dune")

        val result = repository.bulkImport(BulkImportParser.parse("movies\nDUNE\nArrival")!!)

        assertFalse(result.createdList)
        assertEquals(listId, result.listId)
        assertEquals("Movies", result.listName)
        assertEquals(1, result.addedCount)
        assertEquals(listOf(SkippedItem("DUNE", SkipReason.ALREADY_IN_LIST)), result.skipped)
        assertEquals(1, repository.getLists().first().size)
        assertEquals(setOf("Dune", "Arrival"), db.itemDao().getActiveItemsOnce(listId).map { it.text }.toSet())
    }

    // ── Accept / restore ──────────────────────────────────────────────────────

    @Test
    fun acceptPick_marksPickedAndRecordsHistory() = runTest {
        val listId = (repository.createList("Food") as CreateListResult.Success).id
        repository.addItem(listId, "Pizza")
        val item = db.itemDao().getActiveItemsOnce(listId).single()
        repository.skipItem(item.id)

        repository.acceptPick(item.id, listId)

        val updated = db.itemDao().getById(item.id)!!
        assertEquals(ItemStatus.PICKED, updated.status)
        assertEquals(0, updated.skipCount)
        val history = repository.getPickHistoryForItem(item.id)!!
        assertEquals(1, history.pickCount)
        assertEquals("Pizza", history.itemTextSnapshot)
    }

    @Test
    fun acceptPick_again_incrementsPickCount() = runTest {
        val listId = (repository.createList("Food") as CreateListResult.Success).id
        repository.addItem(listId, "Pizza")
        val item = db.itemDao().getActiveItemsOnce(listId).single()

        repository.acceptPick(item.id, listId)
        val historyId = repository.getPickHistoryForItem(item.id)!!.id
        repository.restoreItem(historyId)
        repository.acceptPick(item.id, listId)

        assertEquals(2, repository.getPickHistoryForItem(item.id)!!.pickCount)
    }

    @Test
    fun restoreItem_afterItemDeleted_recreatesAndRelinks() = runTest {
        val listId = (repository.createList("Food") as CreateListResult.Success).id
        repository.addItem(listId, "Pizza")
        val item = db.itemDao().getActiveItemsOnce(listId).single()
        repository.acceptPick(item.id, listId)
        val historyId = repository.getPickHistoryForItem(item.id)!!.id
        repository.deleteItem(item.id)

        assertEquals(RestoreResult.Success, repository.restoreItem(historyId))

        val restored = db.itemDao().getActiveItemsOnce(listId).single()
        assertEquals("Pizza", restored.text)
        assertNotNull(repository.getPickHistoryForItem(restored.id))
    }

    @Test
    fun restoreItem_whenSameTextAlreadyActive_dropsHistoryEntry() = runTest {
        val listId = (repository.createList("Food") as CreateListResult.Success).id
        repository.addItem(listId, "Pizza")
        val item = db.itemDao().getActiveItemsOnce(listId).single()
        repository.acceptPick(item.id, listId)
        val historyId = repository.getPickHistoryForItem(item.id)!!.id
        repository.addItem(listId, "Pizza")

        assertEquals(RestoreResult.AlreadyActive, repository.restoreItem(historyId))
        assertTrue(repository.getHistory(listId).first().isEmpty())
    }
}
