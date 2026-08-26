/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.newpipe.app.screen.about.AboutScreenContent
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.app_name
import newpipe.shared.generated.resources.bookmarked_playlists
import newpipe.shared.generated.resources.donate
import newpipe.shared.generated.resources.dummy_service
import newpipe.shared.generated.resources.downloads
import newpipe.shared.generated.resources.featured
import newpipe.shared.generated.resources.history
import newpipe.shared.generated.resources.ic_bookmark
import newpipe.shared.generated.resources.ic_file_download
import newpipe.shared.generated.resources.ic_foreground
import newpipe.shared.generated.resources.ic_history
import newpipe.shared.generated.resources.ic_info_outline
import newpipe.shared.generated.resources.ic_menu
import newpipe.shared.generated.resources.menu_navigation
import newpipe.shared.generated.resources.ic_settings
import newpipe.shared.generated.resources.ic_stars
import newpipe.shared.generated.resources.ic_subscriptions
import newpipe.shared.generated.resources.ic_trending_up
import newpipe.shared.generated.resources.ic_tv
import newpipe.shared.generated.resources.ic_volunteer_activism
import newpipe.shared.generated.resources.nothing_here_but_crickets
import newpipe.shared.generated.resources.settings
import newpipe.shared.generated.resources.tab_about
import newpipe.shared.generated.resources.tab_bookmarks
import newpipe.shared.generated.resources.tab_subscriptions
import newpipe.shared.generated.resources.trending
import newpipe.shared.generated.resources.whats_new
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val TEST_TAG_DUMMY_SERVICE_TAB_PREFIX = "dummy_service_tab_"
internal const val TEST_TAG_DRAWER_TOP_GROUP = "drawer_top_group"
internal const val TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP = "drawer_dummy_service_group"
internal const val TEST_TAG_DRAWER_BOTTOM_GROUP = "drawer_bottom_group"
internal const val TEST_TAG_DRAWER_HEADER = "drawer_header"

private enum class DesktopPage {
    HOME,
    PLACEHOLDER,
    ABOUT
}

internal enum class DummyServiceTab(
    val title: @Composable () -> String,
    val icon: DrawableResource
) {
    FEATURED({ stringResource(Res.string.featured) }, Res.drawable.ic_stars),
    WHATS_NEW({ stringResource(Res.string.whats_new) }, Res.drawable.ic_subscriptions),
    SUBSCRIPTIONS({ stringResource(Res.string.tab_subscriptions) }, Res.drawable.ic_tv),
    BOOKMARKS({ stringResource(Res.string.tab_bookmarks) }, Res.drawable.ic_bookmark)
}

@Composable
fun DesktopNavigationShell() {
    val page = remember { mutableStateOf(DesktopPage.HOME) }

    when (page.value) {
        DesktopPage.ABOUT -> AboutScreenContent(onNavigateUp = { page.value = DesktopPage.HOME })
        DesktopPage.HOME,
        DesktopPage.PLACEHOLDER -> DesktopNavigationShellContent(
            showHome = page.value == DesktopPage.HOME,
            onShowHome = { page.value = DesktopPage.HOME },
            onShowPlaceholder = { page.value = DesktopPage.PLACEHOLDER },
            onShowAbout = { page.value = DesktopPage.ABOUT }
        )
    }
}

@Composable
fun DesktopNavigationShellContent(
    showHome: Boolean = true,
    onShowHome: () -> Unit = {},
    onShowPlaceholder: () -> Unit = {},
    onShowAbout: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val selectedTab = remember { mutableStateOf(DummyServiceTab.FEATURED) }
    val closeDrawer = { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader()
                DrawerGroup(
                    testTag = TEST_TAG_DRAWER_TOP_GROUP,
                    items = topDrawerItems(),
                    onItemClick = {
                        onShowPlaceholder()
                        closeDrawer()
                    }
                )
                HorizontalDivider()
                DrawerGroup(
                    testTag = TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP,
                    items = dummyServiceDrawerItems(),
                    onItemClick = { item ->
                        if (item.tab == null) {
                            onShowPlaceholder()
                        } else {
                            selectedTab.value = item.tab
                            onShowHome()
                        }
                        closeDrawer()
                    }
                )
                HorizontalDivider()
                DrawerGroup(
                    testTag = TEST_TAG_DRAWER_BOTTOM_GROUP,
                    items = bottomDrawerItems(),
                    onItemClick = { item ->
                        if (item.isAbout) {
                            onShowAbout()
                        } else {
                            onShowPlaceholder()
                        }
                        closeDrawer()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedTab.value.title()) },
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
                if (showHome) {
                    PrimaryTabRow(
                        modifier = Modifier.fillMaxWidth(),
                        selectedTabIndex = selectedTab.value.ordinal
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
                label = { Text(item.label()) },
                selected = false,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun DrawerHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_DRAWER_HEADER),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = painterResource(Res.drawable.ic_foreground),
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            androidx.compose.foundation.layout.Row(
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

private data class DrawerItem(
    val label: @Composable () -> String,
    val icon: DrawableResource,
    val tab: DummyServiceTab? = null,
    val isAbout: Boolean = false
)

@Composable
private fun topDrawerItems() = listOf(
    DrawerItem({ stringResource(Res.string.tab_subscriptions) }, Res.drawable.ic_tv),
    DrawerItem({ stringResource(Res.string.whats_new) }, Res.drawable.ic_subscriptions),
    DrawerItem({ stringResource(Res.string.bookmarked_playlists) }, Res.drawable.ic_bookmark),
    DrawerItem({ stringResource(Res.string.downloads) }, Res.drawable.ic_file_download),
    DrawerItem({ stringResource(Res.string.history) }, Res.drawable.ic_history)
)

@Composable
private fun dummyServiceDrawerItems() = listOf(
    DrawerItem({ stringResource(Res.string.featured) }, Res.drawable.ic_stars, DummyServiceTab.FEATURED),
    DrawerItem({ stringResource(Res.string.trending) }, Res.drawable.ic_trending_up)
)

@Composable
private fun bottomDrawerItems() = listOf(
    DrawerItem({ stringResource(Res.string.settings) }, Res.drawable.ic_settings),
    DrawerItem({ stringResource(Res.string.donate) }, Res.drawable.ic_volunteer_activism),
    DrawerItem({ stringResource(Res.string.tab_about) }, Res.drawable.ic_info_outline, isAbout = true)
)

@Composable
private fun EmptyPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.padding(24.dp),
            text = stringResource(Res.string.nothing_here_but_crickets),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}
