package com.nantcompany.clipy.processing

import com.nantcompany.clipy.export.job.ProcessEvent
import com.nantcompany.clipy.export.job.ProcessingJobManager
import com.nantcompany.clipy.export.job.ProcessingRequest
import com.nantcompany.clipy.export.output.OutputMedia
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class ProcessingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val jobManager = mockk<ProcessingJobManager>()
    private lateinit var viewModel: ProcessingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProcessingViewModel(jobManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start should update state to success when job completes`() = runTest {
        val mockRequest = mockk<ProcessingRequest>()
        val mockOutput = mockk<OutputMedia>()
        
        every { mockRequest.outputPath } returns "/tmp/output.mp4"
        every { jobManager.process(any(), any()) } returns ProcessEvent.Completed(mockOutput)

        viewModel.start(mockRequest)
        
        // Should be in Preparing/Processing state initially
        assertTrue(viewModel.uiState.value.isRunning)
        
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRunning)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertEquals(ProcessingPhase.Success, viewModel.uiState.value.phase)
        assertEquals(mockOutput, viewModel.uiState.value.output)
    }

    @Test
    fun `cancel should call jobManager and update state`() {
        every { jobManager.cancelProcessing() } returns Unit
        
        viewModel.cancel()

        verify { jobManager.cancelProcessing() }
        assertEquals(ProcessingPhase.Cancelled, viewModel.uiState.value.phase)
        assertFalse(viewModel.uiState.value.isRunning)
    }
}
