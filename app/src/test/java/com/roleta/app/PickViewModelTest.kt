package com.roleta.app

import com.roleta.app.data.db.entity.ItemEntity
import com.roleta.app.data.db.entity.PickHistoryEntity
import com.roleta.app.data.repository.RoletaRepository
import com.roleta.app.ui.screen.pick.PickUiState
import com.roleta.app.ui.screen.pick.PickViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PickViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: RoletaRepository
    private lateinit var viewModel: PickViewModel

    private val items = listOf(
        ItemEntity("i1", "list1", "Pizza", 1L),
        ItemEntity("i2", "list1", "Tacos", 2L, skipCount = 2),
        ItemEntity("i3", "list1", "Ramen", 3L)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.getActiveItemsOnce("list1") } returns items
        coEvery { repository.getPickHistoryForItem(any()) } returns null
        viewModel = PickViewModel(repository)
    }

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun init_startsSpinOnAnActiveItem() = runTest(testDispatcher) {
        viewModel.init("list1", "Food")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PickUiState.Spinning
        assertEquals(items.map { it.text }, state.items)
        assertTrue(state.targetIndex in items.indices)
        assertEquals("Food", state.listName)
    }

    @Test
    fun init_withFewerThanTwoItems_navigatesBack() = runTest(testDispatcher) {
        coEvery { repository.getActiveItemsOnce("list1") } returns items.take(1)
        val navigations = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.navigateBack.toList(navigations) }

        viewModel.init("list1", "Food")
        advanceUntilIdle()

        assertEquals(1, navigations.size)
        assertTrue(viewModel.uiState.value is PickUiState.Loading)
    }

    @Test
    fun animationSettled_showsTheSpunItem() = runTest(testDispatcher) {
        coEvery { repository.getPickHistoryForItem(any()) } returns
            PickHistoryEntity("h1", "i2", "list1", "Tacos", 5L, pickCount = 3)
        viewModel.init("list1", "Food")
        advanceUntilIdle()
        val spinning = viewModel.uiState.value as PickUiState.Spinning

        viewModel.onAnimationSettled()
        advanceUntilIdle()

        val result = viewModel.uiState.value as PickUiState.Result
        val picked = items[spinning.targetIndex]
        assertEquals(picked.text, result.selectedItemText)
        assertEquals(picked.skipCount, result.skipCount)
        assertEquals(3, result.priorPickCount)
    }

    @Test
    fun tryAgain_skipsCurrentPickAndSpinsAgain() = runTest(testDispatcher) {
        viewModel.init("list1", "Food")
        advanceUntilIdle()
        val first = viewModel.uiState.value as PickUiState.Spinning
        val skippedId = items[first.targetIndex].id

        viewModel.onTryAgain()
        advanceUntilIdle()

        coVerify { repository.skipItem(skippedId) }
        val second = viewModel.uiState.value as PickUiState.Spinning
        assertTrue(second.spinId > first.spinId)
    }

    @Test
    fun accept_recordsPickAndNavigatesBack() = runTest(testDispatcher) {
        val navigations = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.navigateBack.toList(navigations) }
        viewModel.init("list1", "Food")
        advanceUntilIdle()
        val spinning = viewModel.uiState.value as PickUiState.Spinning

        viewModel.onAccept()
        advanceUntilIdle()

        coVerify { repository.acceptPick(items[spinning.targetIndex].id, "list1") }
        assertEquals(1, navigations.size)
    }
}
