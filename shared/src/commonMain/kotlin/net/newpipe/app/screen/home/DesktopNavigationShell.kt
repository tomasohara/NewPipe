/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Desktop-only navigation shell: mocks the Android main navigation (home tabs
// plus hamburger drawer) for a dummy streaming service. Real destinations are
// routed through the shared Navigator, so a feature that becomes real in
// shared code only needs its DrawerItem flipped from placeholder to
// destination.
//
// Duplicates (as a hardcoded mock, for expediency) the drawer group
// structure and header layout of the real Android navigation drawer at
// app/src/main/res/menu/drawer_items.xml and
// app/src/main/res/layout/drawer_header.xml — there is no shared
// cross-platform navigation-drawer model yet to draw from instead.
// Streamlining facilitated by Claude Code using model Fable 5.

package net.newpipe.app.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.newpipe.app.BuildConfig
import net.newpipe.app.composable.EmptyPlaceholder
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.theme.currentServiceScheme
import net.newpipe.app.theme.currentServiceTopAppBarColors
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.bookmarked_playlists
import newpipe.shared.generated.resources.donate
import newpipe.shared.generated.resources.downloads
import newpipe.shared.generated.resources.dummy_service
import newpipe.shared.generated.resources.featured
import newpipe.shared.generated.resources.history
import newpipe.shared.generated.resources.ic_bookmark
import newpipe.shared.generated.resources.ic_file_download
import newpipe.shared.generated.resources.ic_foreground
import newpipe.shared.generated.resources.ic_history
import newpipe.shared.generated.resources.ic_info_outline
import newpipe.shared.generated.resources.ic_menu
import newpipe.shared.generated.resources.ic_settings
import newpipe.shared.generated.resources.ic_stars
import newpipe.shared.generated.resources.ic_subscriptions
import newpipe.shared.generated.resources.ic_trending_up
import newpipe.shared.generated.resources.ic_tv
import newpipe.shared.generated.resources.ic_volunteer_activism
import newpipe.shared.generated.resources.menu_navigation
import newpipe.shared.generated.resources.settings
import newpipe.shared.generated.resources.tab_about
import newpipe.shared.generated.resources.tab_bookmarks
import newpipe.shared.generated.resources.tab_subscriptions
import newpipe.shared.generated.resources.trending
import newpipe.shared.generated.resources.whats_new
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal const val TEST_TAG_DUMMY_SERVICE_TAB_PREFIX = "dummy_service_tab_"
internal const val TEST_TAG_TOP_BAR_TITLE = "top_bar_title"
internal const val TEST_TAG_DRAWER_TOP_GROUP = "drawer_top_group"
internal const val TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP = "drawer_dummy_service_group"
internal const val TEST_TAG_DRAWER_BOTTOM_GROUP = "drawer_bottom_group"
internal const val TEST_TAG_DRAWER_HEADER = "drawer_header"

/**
 * Home tabs of the mocked dummy service, mirroring the Android home tab bar
 */
internal enum class DummyServiceTab(
    val title: StringResource,
    val icon: DrawableResource
) {
    FEATURED(Res.string.featured, Res.drawable.ic_stars),
    WHATS_NEW(Res.string.whats_new, Res.drawable.ic_subscriptions),
    SUBSCRIPTIONS(Res.string.tab_subscriptions, Res.drawable.ic_tv),
    BOOKMARKS(Res.string.tab_bookmarks, Res.drawable.ic_bookmark)
}

/**
 * A drawer entry: navigates to [destination] when set, selects [tab] on the
 * dummy home when set, and otherwise shows the crickets placeholder
 */
private data class DrawerItem(
    val label: StringResource,
    val icon: DrawableResource,
    val destination: Destination? = null,
    val tab: DummyServiceTab? = null
)

// Three drawer groups mirror menu_tabs_group, menu_kiosks_group, and
// menu_options_about_group in drawer_items.xml; menu_services_group (the
// real service switcher) has no desktop equivalent since there is only one
// dummy service.
private val topDrawerItems = listOf(
    DrawerItem(Res.string.tab_subscriptions, Res.drawable.ic_tv),
    DrawerItem(Res.string.whats_new, Res.drawable.ic_subscriptions),
    DrawerItem(Res.string.bookmarked_playlists, Res.drawable.ic_bookmark),
    DrawerItem(Res.string.downloads, Res.drawable.ic_file_download),
    DrawerItem(Res.string.history, Res.drawable.ic_history)
)

// Kiosk names are generic ("Featured"/"Trending") rather than tied to any
// one real service, since this drawer belongs to the mocked dummy service
private val dummyServiceDrawerItems = listOf(
    DrawerItem(Res.string.featured, Res.drawable.ic_stars, tab = DummyServiceTab.FEATURED),
    DrawerItem(Res.string.trending, Res.drawable.ic_trending_up)
)

// Settings and About & FAQ are wired to real destinations; the individual
// settings categories (not the Settings menu itself) fall back to the
// shared placeholder screen, see SettingsHomeScreen.
//
// Routing About through the real Navigator/Destination.About also fixes a
// bug from the original prototype: with a local mock page instead of a
// real destination, About lost the dual back controls (navigate-up vs.
// system-back-to-exit) that AboutScreen normally gets via LocalSystemBack.
private val bottomDrawerItems = listOf(
    DrawerItem(Res.string.settings, Res.drawable.ic_settings, destination = Destination.Settings),
    DrawerItem(Res.string.donate, Res.drawable.ic_volunteer_activism),
    DrawerItem(Res.string.tab_about, Res.drawable.ic_info_outline, destination = Destination.About)
)

/**
 * Navigation shell registered as [Destination.DummyHome]; real destinations
 * are pushed onto the shared navigation backstack
 */
@Composable
fun DesktopNavigationShell(navigator: Navigator = koinInject()) {
    DesktopNavigationShellContent(onNavigate = { navigator.navigateTo(it) })
}

/**
 * Stateful shell content: dummy-service tab row, drawer, and placeholder page
 * @param onNavigate Callback to navigate to a real (non-mocked) destination
 */
@Composable
internal fun DesktopNavigationShellContent(onNavigate: (Destination) -> Unit = {}) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // This composable now stays on the backstack while About/Settings are
    // open (they're pushed via onNavigate rather than replacing this
    // content), so selectedTab survives the round trip. In the original
    // prototype, About was a local mock page that unmounted this whole
    // composable, so returning from it always reset the tab back to
    // Featured — that bug can no longer happen.
    val selectedTab = remember { mutableStateOf(DummyServiceTab.FEATURED) }
    // Non-null while a placeholder-only drawer destination is showing;
    // null means the dummy-service home (tab row) is showing.
    // Also fixes a bug where the top bar kept showing the last-selected
    // tab's title even after switching to an unrelated placeholder page
    // (e.g. title stayed "Featured" while viewing the Downloads placeholder).
    val placeholderLabel = remember { mutableStateOf<StringResource?>(null) }
    val serviceScheme = currentServiceScheme()

    // Three-way branch mirrors DrawerItem's three states: a real
    // destination, a dummy-service tab selection, or (fallback) the shared
    // crickets placeholder
    val onDrawerItemClick: (DrawerItem) -> Unit = { item ->
        when {
            item.destination != null -> onNavigate(item.destination)
            item.tab != null -> {
                selectedTab.value = item.tab
                placeholderLabel.value = null
            }
            else -> placeholderLabel.value = item.label
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // Fixes a bug where the drawer content (header + up to 10
                // items) overflowed the default 800x600 window with no way
                // to scroll, silently clipping "About & FAQ" off the
                // bottom and making it unreachable
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DrawerHeader(serviceScheme)
                    DrawerGroup(TEST_TAG_DRAWER_TOP_GROUP, topDrawerItems, onDrawerItemClick)
                    HorizontalDivider()
                    DrawerGroup(
                        TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP,
                        dummyServiceDrawerItems,
                        onDrawerItemClick
                    )
                    HorizontalDivider()
                    DrawerGroup(TEST_TAG_DRAWER_BOTTOM_GROUP, bottomDrawerItems, onDrawerItemClick)
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Branded with the real currently-selected service's colors
                // (currentServiceTopAppBarColors/currentServiceScheme, also
                // used by About/Settings) so the mock shell looks consistent
                // with the rest of the app rather than using plain defaults
                TopAppBar(
                    colors = currentServiceTopAppBarColors(serviceScheme),
                    title = {
                        Text(
                            modifier = Modifier.testTag(TEST_TAG_TOP_BAR_TITLE),
                            text = stringResource(
                                placeholderLabel.value ?: selectedTab.value.title
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_menu),
                                contentDescription = stringResource(Res.string.menu_navigation)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (placeholderLabel.value == null) {
                    PrimaryTabRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTabIndex = selectedTab.value.ordinal,
                        containerColor = serviceScheme.primaryContainer,
                        contentColor = serviceScheme.onPrimaryContainer
                    ) {
                        DummyServiceTab.entries.forEach { tab ->
                            Tab(
                                modifier = Modifier.testTag(
                                    "$TEST_TAG_DUMMY_SERVICE_TAB_PREFIX${tab.name}"
                                ),
                                selected = selectedTab.value == tab,
                                icon = {
                                    Icon(
                                        painter = painterResource(tab.icon),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    selectedTab.value = tab
                                }
                            )
                        }
                    }
                }
                EmptyPlaceholder()
            }
        }
    }
}

@Composable
private fun DrawerGroup(
    testTag: String,
    items: List<DrawerItem>,
    onItemClick: (DrawerItem) -> Unit
) {
    Column(modifier = Modifier.testTag(testTag)) {
        items.forEach { item ->
            NavigationDrawerItem(
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null
                    )
                },
                label = { Text(stringResource(item.label)) },
                selected = false,
                onClick = { onItemClick(item) }
            )
        }
    }
}

// Duplicates the two-row layout (app icon + name, then service icon +
// name) of app/src/main/res/layout/drawer_header.xml, done for expediency
// since Compose Multiplatform can't reuse an Android XML layout directly;
// the service row is static "Dummy Service" text rather than a real
// service switcher
@Composable
private fun DrawerHeader(serviceScheme: ColorScheme) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_DRAWER_HEADER),
        color = serviceScheme.primaryContainer,
        contentColor = serviceScheme.onPrimaryContainer
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = painterResource(Res.drawable.ic_foreground),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = BuildConfig.APP_NAME,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Row(
                modifier = Modifier.padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.ic_foreground),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(Res.string.dummy_service),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
