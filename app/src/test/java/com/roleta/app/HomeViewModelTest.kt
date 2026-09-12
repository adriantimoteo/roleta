package com.roleta.app

import com.roleta.app.data.datastore.SortOrder
import com.roleta.app.data.db.dao.ListWithCount
import com.roleta.app.data.repository.RoletaRepository
import com.roleta.app.ui.screen.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: RoletaRepository
    private lateinit var viewModel: HomeViewModel

    private val sampleList = ListWithCount("id1", "Movies", 1000L, 3)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        every { repository.getLists() } returns flowOf(listOf(sampleList))
        every { repository.listSortOrder } returns flowOf(SortOrder.ALPHA)
        every { repository.hasLaunched } returns flowOf(true)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun initialState_listsAreLoaded() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(sampleList), viewModel.uiState.value.lists)
    }

    @Test
    fun createList_success_dismissesDialog() = runTest {
        coEvery { repository.createList(any()) } returns null
        viewModel.openCreateDialog()
        viewModel.createList("Restaurants")
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showCreateDialog)
        assertNull(viewModel.uiState.value.createDialogError)
    }

    @Test
    fun createList_duplicate_showsError() = runTest {
        coEvery { repository.createList(any()) } returns "A list named \"Movies\" already exists."
        viewModel.openCreateDialog()
        viewModel.createList("Movies")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showCreateDialog)
        assertNotNull(viewModel.uiState.value.createDialogError)
    }

    @Test
    fun deleteList_callsRepository() = runTest {
        viewModel.openDeleteDialog(sampleList)
        viewModel.confirmDeleteList()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.deleteList("id1") }
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun toggleSortOrder_flipsPreference() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(SortOrder.ALPHA, viewModel.uiState.value.sortOrder)
        viewModel.toggleSortOrder()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.setListSortOrder(SortOrder.CREATION) }
    }

    @Test
    fun noSeedingWhenAlreadyLaunched() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { repository.seedSampleList() }
    }

    @Test
    fun seedsOnFirstLaunch() = runTest {
        Dispatchers.resetMain()
        Dispatchers.setMain(testDispatcher)
        every { repository.hasLaunched } returns flowOf(false)
        coEvery { repository.seedSampleList() } returns Unit
        val vm = HomeViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.seedSampleList() }
    }
}
