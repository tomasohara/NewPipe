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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.newpipe.app.Constants.KEY_STREAMING_SERVICE
import net.newpipe.app.extensions.withKoin
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.theme.Service
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.downloads
import newpipe.shared.generated.resources.menu_navigation
import newpipe.shared.generated.resources.nothing_here_but_crickets
import newpipe.shared.generated.resources.radio
import newpipe.shared.generated.resources.settings
import newpipe.shared.generated.resources.tab_about
import newpipe.shared.generated.resources.tab_bookmarks
import newpipe.shared.generated.resources.trending_gaming
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class DesktopNavigationShellTest {

    // No Koin modules needed anymore: shell branding now follows the
    // locally selected DummyService instead of injecting Settings via
    // currentServiceScheme(). withKoin is kept for consistency with the
    // other screen tests (and its stopKoin cleanup).
    private fun ComposeUiTest.setShellContent(
        onNavigate: (Destination) -> Unit = {}
    ) = withKoin(
        modules = emptyList(),
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

    // The switch control mirrors Android's three-component service switcher
    // (icon + name + arrow): clicking it toggles the drawer body between the
    // main menu and the service-selection menu, and selecting the other
    // service updates the header plus the per-service kiosk group
    @Test
    fun serviceSwitcherTogglesMenuAndSelectsOtherService() = runComposeUiTest {
        setShellContent()

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()
        onNodeWithTag(TEST_TAG_SERVICE_SWITCHER).assertIsDisplayed()
        onNodeWithText(DummyService.DUMMY_TUBE.serviceName).assertIsDisplayed()
        // DummyTube mirrors YouTube's six-kiosk group; no Radio kiosk
        onNodeWithText(getString(Res.string.trending_gaming)).assertExists()
        onNodeWithText(getString(Res.string.radio)).assertDoesNotExist()

        // Toggle to the service-selection menu: it replaces the main menu
        onNodeWithTag(TEST_TAG_SERVICE_SWITCHER).performClick()
        onNodeWithTag(TEST_TAG_DRAWER_SERVICE_MENU).assertIsDisplayed()
        onNodeWithTag(TEST_TAG_DRAWER_TOP_GROUP).assertDoesNotExist()
        DummyService.entries.forEach { service ->
            onNodeWithTag("$TEST_TAG_SERVICE_OPTION_PREFIX${service.name}").assertIsDisplayed()
        }

        // Selecting the other service returns to the main menu, rebranded
        onNodeWithTag("$TEST_TAG_SERVICE_OPTION_PREFIX${DummyService.DUMMYCAMP.name}")
            .performClick()
        onNodeWithTag(TEST_TAG_DRAWER_SERVICE_MENU).assertDoesNotExist()
        onNodeWithText(DummyService.DUMMYCAMP.serviceName).assertIsDisplayed()
        onNodeWithText(DummyService.DUMMY_TUBE.serviceName).assertDoesNotExist()
        // Dummycamp mirrors Bandcamp's Featured + Radio kiosk group
        onNodeWithText(getString(Res.string.radio)).assertExists()
        onNodeWithText(getString(Res.string.trending_gaming)).assertDoesNotExist()
    }

    // Exercises the Koin-injected wrapper: selecting a dummy service must
    // store its real counterpart's name under the same Settings key the
    // real app uses. This is what keeps the selection alive across the
    // About/Settings round trip (the shell is unmounted meanwhile) and
    // brands those screens' currentService()-based headers consistently.
    @Test
    fun serviceSelectionStoresRealCounterpartInSettings() = runComposeUiTest {
        val settings = MapSettings()
        withKoin(
            modules = listOf(
                module {
                    single<Settings> { settings }
                    single {
                        Navigator(startDestination = Destination.DummyHome, onCloseRequest = {})
                    }
                }
            ),
            content = { MaterialTheme { DesktopNavigationShell() } }
        )

        onNodeWithContentDescription(getString(Res.string.menu_navigation)).performClick()
        onNodeWithTag(TEST_TAG_SERVICE_SWITCHER).performClick()
        onNodeWithTag("$TEST_TAG_SERVICE_OPTION_PREFIX${DummyService.DUMMYCAMP.name}")
            .performClick()

        assertEquals(
            Service.BANDCAMP.serviceName,
            settings.getString(KEY_STREAMING_SERVICE, "")
        )
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
        // The six DummyTube kiosks push the bottom group below the fold of
        // the scrollable drawer, so scroll the item into view first
        onNodeWithText(getString(Res.string.settings)).performScrollTo().performClick()

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
        // Scroll needed for the same reason as in the Settings test above
        onNodeWithText(getString(Res.string.tab_about)).performScrollTo().performClick()
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
