/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Test additions facilitated by Claude Code using model Fable 5.

package net.newpipe.app.screen.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.newpipe.app.extensions.withKoin
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.downloads
import newpipe.shared.generated.resources.menu_navigation
import newpipe.shared.generated.resources.nothing_here_but_crickets
import newpipe.shared.generated.resources.settings
import newpipe.shared.generated.resources.tab_about
import newpipe.shared.generated.resources.tab_bookmarks
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class DesktopNavigationShellTest {

    // A real Settings singleton is required because the shell reads
    // currentServiceScheme() (branding), which injects Settings via Koin
    private val emptySettings = module {
        single<Settings> { MapSettings() }
    }

    private fun ComposeUiTest.setShellContent(
        onNavigate: (Destination) -> Unit = {}
    ) = withKoin(
        modules = listOf(emptySettings),
        content = {
            MaterialTheme {
                DesktopNavigationShellContent(onNavigate = onNavigate)
            }
        }
    )

    @Test
    fun dummyServiceHomeShowsFourTabs() = runComposeUiTest {
        setShellContent()

        DummyServiceTab.entries.forEach { tab ->
            onNodeWithTag("$TEST_TAG_DUMMY_SERVICE_TAB_PREFIX${tab.name}").assertIsDisplayed()
        }
    }

    @Test
    fun tabClickUpdatesSelectionAndTitle() = runComposeUiTest {
        setShellContent()

        val bookmarksTag = "$TEST_TAG_DUMMY_SERVICE_TAB_PREFIX${DummyServiceTab.BOOKMARKS.name}"
        onNodeWithTag(bookmarksTag).performClick()

        onNodeWithTag(bookmarksTag).assertIsSelected()
        onNodeWithTag(TEST_TAG_TOP_BAR_TITLE)
            .assertTextEquals(getString(Res.string.tab_bookmarks))
    }

    @Test
    fun drawerShowsAndroidStyleGroupsAndBranding() = runComposeUiTest {
        setShellContent()

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()

        onNodeWithTag(TEST_TAG_DRAWER_HEADER).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_TOP_GROUP).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_BOTTOM_GROUP).assertIsDisplayed()
    }

    @Test
    fun drawerPlaceholderItemHidesTabsAndSetsTitle() = runComposeUiTest {
        setShellContent()

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()
        onNodeWithText(getString(Res.string.downloads)).performClick()

        onNodeWithTag(TEST_TAG_TOP_BAR_TITLE)
            .assertTextEquals(getString(Res.string.downloads))
        onNodeWithTag("$TEST_TAG_DUMMY_SERVICE_TAB_PREFIX${DummyServiceTab.FEATURED.name}")
            .assertDoesNotExist()
        onNodeWithText(getString(Res.string.nothing_here_but_crickets)).assertIsDisplayed()
    }

    @Test
    fun drawerSettingsRoutesToSettingsDestination() = runComposeUiTest {
        var navigated: Destination? = null
        setShellContent(onNavigate = { navigated = it })

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()
        onNodeWithText(getString(Res.string.settings)).performClick()

        assertEquals(Destination.Settings, navigated)
    }

    // Uses a real Navigator (rather than a recording lambda, as the other
    // tests do) to pin the actual backstack/exit contract for About, not
    // just that onNavigate fired
    @Test
    fun drawerNavigationRoutesThroughSharedNavigator() = runComposeUiTest {
        var closeRequested = false
        val navigator = Navigator(
            startDestination = Destination.DummyHome,
            onCloseRequest = { closeRequested = true }
        )

        setShellContent(onNavigate = { navigator.navigateTo(it) })

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()
        onNodeWithText(getString(Res.string.tab_about)).performClick()
        assertEquals(
            listOf<Destination>(Destination.DummyHome, Destination.About),
            navigator.backstack.toList()
        )

        // Back from About returns to the shell; back from the shell exits
        navigator.navigateUp()
        assertEquals(listOf<Destination>(Destination.DummyHome), navigator.backstack.toList())
        navigator.navigateUp()
        assertTrue(closeRequested)
    }
}
