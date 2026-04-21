package com.galize.app.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.galize.app.ui.theme.GalizeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for HomeScreen using Compose Testing + Espresso
 * 
 * Note: These are instrumented tests that run on Android device/emulator.
 * Run with: ./gradlew connectedAndroidTest
 */
class HomeScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_shouldDisplayTitleAndSubtitle() {
        // Given
        composeTestRule.setContent {
            GalizeTheme {
                HomeScreen(
                    onNavigateToSettings = {},
                    onNavigateToHistory = {}
                )
            }
        }

        // Then - Verify title and subtitle are displayed
        composeTestRule.onNodeWithText("万物皆可 Galgame").assertIsDisplayed()
        composeTestRule.onNodeWithText("Everything is a Visual Novel").assertIsDisplayed()
    }

    @Test
    fun homeScreen_shouldDisplayStatusCard() {
        // Given
        composeTestRule.setContent {
            GalizeTheme {
                HomeScreen(
                    onNavigateToSettings = {},
                    onNavigateToHistory = {}
                )
            }
        }

        // Then - Verify status card is displayed
        composeTestRule.onNodeWithText("Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service is stopped").assertIsDisplayed()
    }

    @Test
    fun homeScreen_shouldDisplayStartButton() {
        // Given
        composeTestRule.setContent {
            GalizeTheme {
                HomeScreen(
                    onNavigateToSettings = {},
                    onNavigateToHistory = {}
                )
            }
        }

        // Then - Verify Start button is displayed (initially)
        composeTestRule.onNodeWithText("Start Galize").assertIsDisplayed()
    }

    @Test
    fun homeScreen_shouldHaveNavigationButtons() {
        // Given
        composeTestRule.setContent {
            GalizeTheme {
                HomeScreen(
                    onNavigateToSettings = {},
                    onNavigateToHistory = {}
                )
            }
        }

        // Then - Verify navigation icons exist
        composeTestRule.onNodeWithContentDescription("History").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun homeScreen_shouldNavigateToSettingsWhenSettingsIconClicked() {
        // Given
        var navigatedToSettings = false
        composeTestRule.setContent {
            GalizeTheme {
                HomeScreen(
                    onNavigateToSettings = { navigatedToSettings = true },
                    onNavigateToHistory = {}
                )
            }
        }

        // When - Click settings icon
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Then - Verify navigation callback was triggered
        assert(navigatedToSettings)
    }

    @Test
    fun homeScreen_shouldNavigateToHistoryWhenHistoryIconClicked() {
        // Given
        var navigatedToHistory = false
        composeTestRule.setContent {
            GalizeTheme {
                HomeScreen(
                    onNavigateToSettings = {},
                    onNavigateToHistory = { navigatedToHistory = true }
                )
            }
        }

        // When - Click history icon
        composeTestRule.onNodeWithContentDescription("History").performClick()

        // Then - Verify navigation callback was triggered
        assert(navigatedToHistory)
    }
}
