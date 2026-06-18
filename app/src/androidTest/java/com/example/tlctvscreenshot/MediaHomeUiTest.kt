package com.example.tlctvscreenshot

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
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
        context.getSharedPreferences("selected_tcl_device", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("tcl_6553_identity", Context.MODE_PRIVATE).edit().clear().commit()
        File(context.filesDir, "TCast/Images").deleteRecursively()

        val intent = Intent(context, MainActivity::class.java)
            .putExtra("com.example.tlctvscreenshot.UI_TEST_MODE", true)
        scenario = ActivityScenario.launch(intent)
        composeRule.waitForIdle()
    }

    @After
    fun closeActivity() {
        scenario?.close()
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

    @Test
    fun homeScreenRendersDarkMediaDashboardWidgets() {
        composeRule.onNodeWithTag("home_root").assertIsDisplayed()
        assertAnyTextDisplayed("Media Cast")
        composeRule.onNodeWithTag("top_tv_button").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("action_capture_tv").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("action_cast_photo").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("action_cast_video").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("action_cast_music").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("status_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_status_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom_connect_button").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("bottom_remote_button").assertIsDisplayed().assertIsEnabled()
        assertAnyTextDisplayed("Please connect your TV......")

        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onNodeWithTag("gallery_section").assertIsDisplayed()
        composeRule.onNodeWithTag("gallery_tab_All").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_tab_Photos").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_tab_Videos").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_tab_Favorites").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("gallery_refresh_button").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun connectDialogSupportsDiscoveryManualEntryIdentityAndDismiss() {
        composeRule.onNodeWithTag("top_tv_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertIsDisplayed()
        assertAnyTextDisplayed("Connect TV")
        assertAnyTextDisplayed("No TV selected.")
        composeRule.onNodeWithTag("manual_tv_ip").performTextReplacement("192.0.2.55")
        composeRule.onNodeWithTag("manual_tv_ip").assertTextContains("192.0.2.55")
        composeRule.onNodeWithTag("phone_name_field").performTextReplacement("Compose Test Phone")
        composeRule.onNodeWithTag("phone_name_field").assertTextContains("Compose Test Phone")
        composeRule.onNodeWithTag("use_android_id_button").performClick()

        composeRule.onNodeWithTag("discover_button").performClick()
        composeRule.onNodeWithTag("discovered_device_card").assertIsDisplayed()
        assertAnyTextDisplayed("Test Living Room TV — 192.0.2.10")
        composeRule.onNodeWithTag("use_discovered_device_button").performClick()
        assertAnyTextExists("Selected: Test Living Room TV — 192.0.2.10")
        composeRule.onNodeWithTag("connect_done_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertDoesNotExist()
        assertAnyTextExists("TV connected")
    }

    @Test
    fun captureTileAddsGalleryItemAndGalleryActionsWork() {
        composeRule.onNodeWithTag("action_capture_tv").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("gallery_item").fetchSemanticsNodes().isNotEmpty()
        }
        assertAnyTextDisplayed("Captured test TV screenshot.")
        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onAllNodesWithTag("gallery_item").assertCountEquals(1)
        composeRule.onNodeWithTag("gallery_item").assertIsDisplayed()
        assertAnyTextExists("TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_capture_preview").assertIsDisplayed()

        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_item_delete_button"))
        composeRule.onNodeWithTag("gallery_item_open_button").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertAnyTextExists("Opened TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_share_button").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertAnyTextExists("Test shared TestCapture-", substring = true)
        composeRule.onNodeWithTag("selected_export_button").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertAnyTextExists("Test exported TestCapture-", substring = true)

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
    fun mediaTilesAndGalleryTabsRespond() {
        composeRule.onNodeWithTag("action_cast_photo").performClick()
        assertAnyTextDisplayed("Photo casting is not required", substring = true)
        composeRule.onNodeWithTag("action_cast_video").performClick()
        assertAnyTextDisplayed("Video casting is not configured", substring = true)
        composeRule.onNodeWithTag("action_cast_music").performClick()
        assertAnyTextDisplayed("Music casting is not configured", substring = true)

        composeRule.onNodeWithTag("home_root").performScrollToNode(hasTestTag("gallery_section"))
        composeRule.onNodeWithTag("gallery_tab_All").performClick()
        composeRule.onNodeWithTag("gallery_tab_Photos").performClick()
        composeRule.onNodeWithTag("gallery_tab_Videos").performClick()
        composeRule.onNodeWithTag("gallery_tab_Favorites").performClick()
        composeRule.onNodeWithTag("gallery_refresh_button").performClick()
        assertAnyTextDisplayed("No saved captures yet.")
    }

    @Test
    fun bottomConnectAndRemoteControlsUpdateStatus() {
        composeRule.onNodeWithTag("bottom_connect_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("connect_done_button").performClick()
        composeRule.onNodeWithTag("connect_dialog").assertDoesNotExist()

        composeRule.onNodeWithTag("bottom_remote_button").performClick()
        composeRule.onNodeWithTag("remote_dialog").assertIsDisplayed()
        listOf("Power", "Home", "Back", "Up", "Left", "OK", "Right", "Down", "Vol -", "Mute", "Vol +", "Menu", "Ch -", "Ch +").forEach { label ->
            assertAnyTextDisplayed(label)
        }
        composeRule.onNodeWithTag("remote_button_Up").performClick()
        assertAnyTextDisplayed("Test remote sent Up.")
        composeRule.onNodeWithTag("remote_button_OK").performClick()
        assertAnyTextDisplayed("Test remote sent OK.")
        composeRule.onNodeWithTag("remote_button_Back").performClick()
        assertAnyTextDisplayed("Test remote sent Back.")
        composeRule.onNodeWithTag("remote_close_button").performClick()
        composeRule.onNodeWithTag("remote_dialog").assertDoesNotExist()
    }
}
