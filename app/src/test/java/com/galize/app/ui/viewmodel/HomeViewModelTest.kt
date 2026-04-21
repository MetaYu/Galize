package com.galize.app.ui.viewmodel

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for HomeViewModel using JUnit5 + MockK + Turbine
 * 
 * Note: This is a simplified test without Android Context dependencies.
 * For full Context testing, consider using Robolectric or instrumented tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("Initial State Tests")
    inner class InitialStateTests {

        @Test
        fun `should have correct initial state`() = runTest {
            // When & Then
            viewModel.isServiceRunning.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.hasOverlayPermission.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.hasNotificationPermission.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.hasMediaProjectionPermission.test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.errorMessage.test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("Clear Error Tests")
    inner class ClearErrorTests {

        @Test
        fun `should clear error message`() = runTest {
            // Given - use reflection to set error state for testing
            val errorField = viewModel.javaClass.getDeclaredField("_errorMessage")
            errorField.isAccessible = true
            val errorFlow = errorField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<String?>
            errorFlow.value = "Test error"

            // When
            viewModel.clearError()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            viewModel.errorMessage.test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("State Flow Emission Tests")
    inner class StateFlowEmissionTests {

        @Test
        fun `should emit multiple states correctly`() = runTest {
            // This test verifies the StateFlow behavior pattern
            // In real scenarios, you would test actual permission checks
            
            val isServiceRunningField = viewModel.javaClass.getDeclaredField("_isServiceRunning")
            isServiceRunningField.isAccessible = true
            val serviceFlow = isServiceRunningField.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<Boolean>

            // When - emit multiple states
            viewModel.isServiceRunning.test {
                // Initial state
                assertFalse(awaitItem())

                // Simulate state change
                serviceFlow.value = true
                assertTrue(awaitItem())

                // Simulate another state change
                serviceFlow.value = false
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
