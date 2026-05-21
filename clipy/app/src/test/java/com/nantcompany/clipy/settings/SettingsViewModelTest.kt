package com.nantcompany.clipy.settings

import com.nantcompany.clipy.export.output.LocalOutputRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val repository = mockk<LocalOutputRepository>(relaxed = true)
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        viewModel = SettingsViewModel(repository)
    }

    @Test
    fun `toggleHardwareAcceleration should update uiState`() {
        val initial = viewModel.uiState.value.enableHardwareAcceleration
        viewModel.toggleHardwareAcceleration()
        assertEquals(!initial, viewModel.uiState.value.enableHardwareAcceleration)
    }

    @Test
    fun `setExportResolution should update uiState`() {
        viewModel.setExportResolution("4K")
        assertEquals("4K", viewModel.uiState.value.selectedResolution)
    }

    @Test
    fun `clearHistory should call repository clear and update state`() {
        viewModel.askClearHistory()
        assertTrue(viewModel.uiState.value.confirmClearHistory)

        viewModel.clearHistory()

        verify { repository.clear() }
        assertFalse(viewModel.uiState.value.confirmClearHistory)
        assertEquals("Export history cleared.", viewModel.uiState.value.message)
    }
}
