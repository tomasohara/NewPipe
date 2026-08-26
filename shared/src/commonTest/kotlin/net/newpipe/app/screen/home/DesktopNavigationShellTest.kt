/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.menu_navigation
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class DesktopNavigationShellTest {

    @Test
    fun dummyServiceHomeShowsFourTabs() = runComposeUiTest {
        setContent {
            MaterialTheme {
                DesktopNavigationShellContent()
            }
        }

        DummyServiceTab.entries.forEach { tab ->
            onNodeWithTag("$TEST_TAG_DUMMY_SERVICE_TAB_PREFIX${tab.name}").assertIsDisplayed()
        }
    }

    @Test
    fun drawerShowsAndroidStyleGroupsAndBranding() = runComposeUiTest {
        setContent {
            MaterialTheme {
                DesktopNavigationShellContent()
            }
        }

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()

        onNodeWithTag(TEST_TAG_DRAWER_HEADER).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_TOP_GROUP).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_BOTTOM_GROUP).assertIsDisplayed()
    }
}
