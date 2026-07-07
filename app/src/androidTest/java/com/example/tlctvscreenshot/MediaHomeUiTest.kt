package com.example.tlctvscreenshot

import android.content.Context
import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaHomeUiTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun launchInUiTestMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearPersistentUiTestState(context)

        val intent = Intent(context, MainActivity::class.java)
            .putExtra("com.example.tlctvscreenshot.UI_TEST_MODE", true)
        scenario = ActivityScenario.launch(intent)
        composeRule.waitForIdle()
    }

    @After
    fun closeActivity() {
        scenario?.close()
        scenario = null
        clearPersistentUiTestState(ApplicationProvider.getApplicationContext())
    }

    private fun clearPersistentUiTestState(context: Context) {
        context.getSharedPreferences("selected_tcl_device", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("tcl_6553_identity", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        File(context.filesDir, "Screenshots").deleteRecursively()
    }

    private fun assertAnyTextDisplayed(text: String, substring: Boolean = false) {
        composeRule.onAllNodesWithText(text, substring = substring)[0].assertIsDisplayed()
    }

    private fun assertAnyTextExists(text: String, substring: Boolean = false) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
        val nodes = composeRule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes()
        assertTrue("No node found for text: $text", nodes.isNotEmpty())
    }

    private fun enableDebugActivityPane() {
        composeRule.onNodeWithTag("settings_menu_button").performClick()
        composeRule.onNodeWithTag("settings_drawer").assertIsDisplayed()
        composeRule.onNodeWithTag("debug_mode_row").performClick()
        composeRule.onNodeWithTag("settings_done_button").performClick()
        composeRule.onNodeWithTag("settings_drawer").assertIsNotDisplayed()
        composeRule.onNodeWithTag("status_panel").assertIsDisplayed()
    }

    private fun captureTestScreenshotAndWaitForCount(count: Int) {
        composeRule.onNodeWithTag("action_capture_photo").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("gallery_item").fetchSemanticsNodes().size >= count
        }
    }

    @Test
    fun homeScreenRendersDarkMediaDashboardWidgets() {
        composeRule.onNodeWithTag("home_root").assertIsDisplayed()
        composeRule.onAllNodesWithText("Media Cast").assertCountEquals(0)
        composeRule.onNodeWithTag("top_tv_button").assertDoesNotExist()
        composeRule.onNodeWithTag("action_capture_photo").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("action_cast_menu").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("cast_options_menu").assertDoesNotExist()
        composeRule.onNodeWithTag("status_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_menu_button").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("top_status_area").assertIsDisplayed().assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("bottom_status_bar").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("bottom_connect_button").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("bottom_remote_button").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onNodeWithTag("gallery_section").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_tab_All").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_tab_Photos").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_tab_Favorites").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_refresh_button").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun leftEdgeTouchOnCaptureTileDoesNotOpenSettingsDrawer() {
        composeRule.onNodeWithTag("settings_drawer").assertIsNotDisplayed()

        composeRule.onNodeWithTag("action_capture_photo").performTouchInput {
            down(Offset(1f, 1f))
            moveBy(Offset(80f, 0f))
            up()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_drawer").assertIsNotDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("action_capture_photo").assertIsEnabled()
            }.isSuccess
        }

        composeRule.onNodeWithTag("action_capture_photo").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.onNodeWithTag("settings_drawer").assertIsNotDisplayed()
    }

    @Test
    fun settingsCanEnableDebugActivityPane() {
        composeRule.onNodeWithTag("status_panel").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_menu_button").performClick()
        composeRule.onNodeWithTag("settings_drawer").assertIsDisplayed().assertWidthIsEqualTo(320.dp)
        val drawerBounds = composeRule.onNodeWithTag("settings_drawer").fetchSemanticsNode().boundsInRoot
        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertEquals(rootBounds.right, drawerBounds.right, 1f)
        assertAnyTextDisplayed("Settings")
        assertAnyTextDisplayed("Debug mode")
        composeRule.onNodeWithTag("debug_mode_row").assertHasClickAction().performClick()
        composeRule.onNodeWithTag("status_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_done_button").performClick()
        composeRule.onNodeWithTag("settings_drawer").assertIsNotDisplayed()
        composeRule.onNodeWithTag("status_panel").assertIsDisplayed()
    }

    @Test
    fun connectDialogDiscoversImmediatelyAndConnectsFromDeviceCard() {
        composeRule.onNodeWithTag("bottom_connect_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertIsDisplayed()
        assertAnyTextDisplayed("Connect TV")
        assertAnyTextDisplayed("Choose a device on this network")
        assertAnyTextDisplayed("Current network: Test Wi-Fi")
        assertAnyTextDisplayed("No TV selected.")
        composeRule.onNodeWithTag("wifi_settings_button").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("discover_button").assertWidthIsEqualTo(128.dp)
        composeRule.onNodeWithTag("connect_tv_icon", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("connect_tv_halo", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("manual_tv_ip").assertDoesNotExist()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("discovered_device_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("discovered_device_card").assertIsDisplayed().assertHasClickAction()
        composeRule.onNodeWithTag("device_type_icon", useUnmergedTree = true).assertExists()
        assertAnyTextDisplayed("Test Living Room TV — 192.0.2.10")
        assertAnyTextExists("Tap to connect", substring = true)
        composeRule.onNodeWithTag("discovered_device_card").performClick()
        assertAnyTextExists("Selected: Test Living Room TV — 192.0.2.10")
        assertAnyTextExists("Connected to Test Living Room TV.")
        assertAnyTextExists("Connected")
        composeRule.onNodeWithTag("connect_done_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertDoesNotExist()
        assertAnyTextExists("TV connected — fallback capture only")
        composeRule.onNodeWithTag("bottom_fast_retry_button").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun fallbackStatusCanRetryFastConnection() {
        enableDebugActivityPane()
        composeRule.onNodeWithTag("bottom_connect_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("discovered_device_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("discovered_device_card").performClick()
        composeRule.onNodeWithTag("connect_done_button").performClick()
        assertAnyTextExists("TV connected — fallback capture only")

        composeRule.onNodeWithTag("bottom_fast_retry_button").assertIsDisplayed().assertHasClickAction().performClick()
        assertAnyTextExists("Retrying fast TV connection...")
    }

    @Test
    fun captureTileAddsHorizontalGalleryItemAndGalleryActionsWork() {
        enableDebugActivityPane()
        captureTestScreenshotAndWaitForCount(1)
        assertAnyTextDisplayed("Captured test TV screenshot.")
        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onNodeWithTag("gallery_strip").assertIsDisplayed()
        composeRule.onAllNodesWithTag("gallery_item").assertCountEquals(1)
        composeRule.onNodeWithTag("gallery_item").assertIsDisplayed()
        assertAnyTextExists("TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_capture_preview").assertIsDisplayed()

        composeRule.onNodeWithTag("gallery_item_preview").performSemanticsAction(SemanticsActions.OnClick)
        assertAnyTextExists("Opened TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_share_button").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertAnyTextExists("Test shared TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_delete_button").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("delete_capture_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_delete_button").performClick()
        composeRule.onNodeWithTag("delete_capture_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("selected_delete_button").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("confirm_delete_button").performClick()
        assertAnyTextExists("Deleted TestCapture-", substring = true)
        composeRule.onAllNodesWithTag("gallery_item").assertCountEquals(0)
    }

    @Test
    fun galleryPaneUsesVerticalListAndOpensCaptureForMainShare() {
        enableDebugActivityPane()
        repeat(3) { index ->
            captureTestScreenshotAndWaitForCount(index + 1)
        }
        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onNodeWithTag("gallery_strip").assertIsDisplayed()
        composeRule.onAllNodesWithTag("gallery_item").assertCountEquals(3)

        composeRule.onNodeWithTag("gallery_open_pane_button").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.onNodeWithTag("gallery_pane_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_pane_list").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("gallery_pane_item").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithTag("gallery_pane_list").performScrollToIndex(2)
        assertTrue(composeRule.onAllNodesWithTag("gallery_pane_item").fetchSemanticsNodes().isNotEmpty())

        composeRule.onAllNodesWithTag("gallery_pane_item_open_button")[0].performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("gallery_pane_dialog").assertDoesNotExist()
        assertAnyTextExists("Opened TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_capture_preview").assertIsDisplayed()
        composeRule.onNodeWithTag("selected_share_button").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertAnyTextExists("Test shared TestCapture-", substring = true)
    }

    @Test
    fun mediaTilesAndGalleryTabsRespond() {
        enableDebugActivityPane()
        composeRule.onNodeWithTag("action_cast_menu").performClick()
        composeRule.onNodeWithTag("cast_option_photo").assertIsDisplayed().performClick()
        assertAnyTextDisplayed("Photo casting is not required", substring = true)
        composeRule.onNodeWithTag("action_cast_menu").performClick()
        composeRule.onNodeWithTag("cast_option_music").assertIsDisplayed().performClick()
        assertAnyTextDisplayed("Music casting is not configured", substring = true)

        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onNodeWithTag("gallery_tab_All").performClick()
        composeRule.onNodeWithTag("gallery_tab_Photos").performClick()
        composeRule.onNodeWithTag("gallery_tab_Favorites").performClick()
        composeRule.onNodeWithTag("gallery_refresh_button").performClick()
        composeRule.onNodeWithTag("gallery_section").assertIsDisplayed()
    }

    @Test
    fun bottomConnectAndRemoteControlsUpdateStatus() {
        composeRule.onNodeWithTag("bottom_connect_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("connect_done_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertDoesNotExist()

        composeRule.onNodeWithTag("bottom_status_bar").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("connect_done_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertDoesNotExist()

        composeRule.onNodeWithTag("bottom_remote_button").performClick()
        composeRule.onNodeWithTag("remote_dialog").assertIsDisplayed()
        val remoteButtons = listOf(
            Triple("Power", "⏻", "remote_button_Power"),
            Triple("Home", "🏠", "remote_button_Home"),
            Triple("Back", "↩", "remote_button_Back"),
            Triple("Up", "⬆", "remote_button_Up"),
            Triple("Left", "⬅", "remote_button_Left"),
            Triple("OK", "OK", "remote_button_OK"),
            Triple("Right", "➡", "remote_button_Right"),
            Triple("Down", "⬇", "remote_button_Down"),
            Triple("Vol -", "🔉", "remote_button_Vol_minus"),
            Triple("Mute", "🔇", "remote_button_Mute"),
            Triple("Vol +", "🔊", "remote_button_Vol_plus"),
            Triple("Menu", "☰", "remote_button_Menu"),
            Triple("Ch -", "CH−", "remote_button_Ch_minus"),
            Triple("Ch +", "CH+", "remote_button_Ch_plus")
        )
        remoteButtons.forEach { (_, displayLabel, tag) ->
            assertAnyTextDisplayed(displayLabel)
            composeRule.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
        }
        remoteButtons.forEach { (label, _, tag) ->
            composeRule.onNodeWithTag(tag).performClick()
            assertAnyTextDisplayed("Test remote sent $label.")
        }
        composeRule.onNodeWithTag("remote_close_button").performClick()
        composeRule.onNodeWithTag("remote_dialog").assertDoesNotExist()
    }
}
