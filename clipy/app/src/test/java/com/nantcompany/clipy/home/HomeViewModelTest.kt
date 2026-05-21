package com.nantcompany.clipy.home

import com.nantcompany.clipy.export.output.LocalOutputRepository
import com.nantcompany.clipy.export.output.OutputMedia
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository = mockk<LocalOutputRepository>()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRecentExports should update uiState with data from repository`() = runTest {
        val mockData = listOf(
            OutputMedia("1", "file1.mp4", "/path/1", 1000L, operation = "cut"),
            OutputMedia("2", "file2.mp4", "/path/2", 2000L, operation = "compress")
        )
        every { repository.getAll() } returns mockData

        viewModel.loadRecentExports(limit = 2)

        assertEquals(mockData, viewModel.uiState.value.recentExports)
    }

    @Test
    fun `refreshRecentExports should update isRefreshing and then load data`() = runTest {
        val mockData = listOf(
            OutputMedia("1", "file1.mp4", "/path/1", 1000L, operation = "cut")
        )
        every { repository.getAll() } returns mockData

        viewModel.refreshRecentExports(limit = 1)

        // Initially refreshing should be true (if we could capture it mid-execution)
        // But with runTest and StandardTestDispatcher, we can use advanceUntilIdle
        
        assertTrue(viewModel.uiState.value.isRefreshing)
        
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(mockData, viewModel.uiState.value.recentExports)
    }
}
