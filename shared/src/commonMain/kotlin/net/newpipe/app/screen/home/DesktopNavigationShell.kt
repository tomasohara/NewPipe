/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Desktop-only navigation shell: mocks the Android main navigation (home tabs
// plus hamburger drawer) for two mocked streaming services (DummyTube and
// Dummmycamp), switchable from the drawer header the same way the real app
// switches services. Real destinations are routed through the shared
// Navigator, so a feature that becomes real in shared code only needs its
// DrawerItem flipped from placeholder to destination.
//
// Duplicates (as a hardcoded mock, for expediency) the drawer group
// structure and header layout of the real Android navigation drawer at
// app/src/main/res/menu/drawer_items.xml and
// app/src/main/res/layout/drawer_header.xml — there is no shared
// cross-platform navigation-drawer model yet to draw from instead.
// Streamlining facilitated by Claude Code using model Fable 5.

package net.newpipe.app.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import net.newpipe.app.BuildConfig
import net.newpipe.app.Constants.KEY_STREAMING_SERVICE
import net.newpipe.app.composable.EmptyPlaceholder
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.theme.Service
import net.newpipe.app.theme.currentServiceTopAppBarColors
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.bookmarked_playlists
import newpipe.shared.generated.resources.donate
import newpipe.shared.generated.resources.downloads
import newpipe.shared.generated.resources.duration_live
import newpipe.shared.generated.resources.featured
import newpipe.shared.generated.resources.history
import newpipe.shared.generated.resources.ic_arrow_drop_down
import newpipe.shared.generated.resources.ic_bookmark
import newpipe.shared.generated.resources.ic_file_download
import newpipe.shared.generated.resources.ic_foreground
import newpipe.shared.generated.resources.ic_history
import newpipe.shared.generated.resources.ic_info_outline
import newpipe.shared.generated.resources.ic_live_tv
import newpipe.shared.generated.resources.ic_menu
import newpipe.shared.generated.resources.ic_movie
import newpipe.shared.generated.resources.ic_music_note
import newpipe.shared.generated.resources.ic_placeholder_bandcamp
import newpipe.shared.generated.resources.ic_podcasts
import newpipe.shared.generated.resources.ic_radio
import newpipe.shared.generated.resources.ic_settings
import newpipe.shared.generated.resources.ic_smart_display
import newpipe.shared.generated.resources.ic_stars
import newpipe.shared.generated.resources.ic_subscriptions
import newpipe.shared.generated.resources.ic_tv
import newpipe.shared.generated.resources.ic_videogame_asset
import newpipe.shared.generated.resources.ic_volunteer_activism
import newpipe.shared.generated.resources.ic_whatshot
import newpipe.shared.generated.resources.menu_navigation
import newpipe.shared.generated.resources.radio
import newpipe.shared.generated.resources.settings
import newpipe.shared.generated.resources.tab_about
import newpipe.shared.generated.resources.tab_bookmarks
import newpipe.shared.generated.resources.tab_subscriptions
import newpipe.shared.generated.resources.trending
import newpipe.shared.generated.resources.trending_gaming
import newpipe.shared.generated.resources.trending_movies
import newpipe.shared.generated.resources.trending_music
import newpipe.shared.generated.resources.trending_podcasts
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
internal const val TEST_TAG_SERVICE_SWITCHER = "drawer_service_switcher"
internal const val TEST_TAG_DRAWER_SERVICE_MENU = "drawer_service_menu"
internal const val TEST_TAG_SERVICE_OPTION_PREFIX = "drawer_service_option_"

/**
 * Mocked streaming services selectable from the drawer header. DummyTube
 * stands in for YouTube and Dummycamp for Bandcamp: each borrows its real
 * counterpart's color schemes and service icon (the same icons
 * ServiceHelper.kt assigns), so the only visible difference from the real
 * drawer is the service name. Selecting one stores [realService] in the
 * shared Settings, so screens that brand via currentService() (About,
 * Settings, ...) pick up the matching colors.
 */
internal enum class DummyService(
    val serviceName: String,
    val icon: DrawableResource,
    val realService: Service
) {
    DUMMY_TUBE("DummyTube", Res.drawable.ic_smart_display, Service.YOUTUBE),
    DUMMYCAMP("Dummycamp", Res.drawable.ic_placeholder_bandcamp, Service.BANDCAMP);

    val lightScheme: ColorScheme get() = realService.lightScheme
    val darkScheme: ColorScheme get() = realService.darkScheme
}

/**
 * Home tabs shared by both mocked services, mirroring the Android home tab bar
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
// real service switcher) is instead mirrored by the DummyService switch
// control in the drawer header, matching where the real app puts it.
private val topDrawerItems = listOf(
    DrawerItem(Res.string.tab_subscriptions, Res.drawable.ic_tv),
    DrawerItem(Res.string.whats_new, Res.drawable.ic_subscriptions),
    DrawerItem(Res.string.bookmarked_playlists, Res.drawable.ic_bookmark),
    DrawerItem(Res.string.downloads, Res.drawable.ic_file_download),
    DrawerItem(Res.string.history, Res.drawable.ic_history)
)

// Per-service kiosk groups mirror what the real drawer shows for each
// service (names and icons from app/src/main/java/org/schabi/newpipe/util/
// KioskTranslator.kt): DummyTube gets YouTube's six kiosks, Dummycamp gets
// Bandcamp's Featured + Radio.
private val dummyTubeKioskItems = listOf(
    DrawerItem(Res.string.trending_gaming, Res.drawable.ic_videogame_asset),
    DrawerItem(Res.string.trending_music, Res.drawable.ic_music_note),
    DrawerItem(Res.string.trending_movies, Res.drawable.ic_movie),
    DrawerItem(Res.string.trending_podcasts, Res.drawable.ic_podcasts),
    DrawerItem(Res.string.trending, Res.drawable.ic_whatshot),
    DrawerItem(Res.string.duration_live, Res.drawable.ic_live_tv)
)

private val dummycampKioskItems = listOf(
    DrawerItem(Res.string.featured, Res.drawable.ic_stars, tab = DummyServiceTab.FEATURED),
    DrawerItem(Res.string.radio, Res.drawable.ic_radio)
)

private fun kioskItemsFor(service: DummyService) = when (service) {
    DummyService.DUMMY_TUBE -> dummyTubeKioskItems
    DummyService.DUMMYCAMP -> dummycampKioskItems
}

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
fun DesktopNavigationShell(
    navigator: Navigator = koinInject(),
    settings: Settings = koinInject()
) {
    // Bug fix: the selected service used to live only in a remember inside
    // the shell content, but NavDisplay composes just the top backstack
    // entry, so opening About/Settings dropped that state and the shell
    // came back reset to DummyTube — while About itself was branded from
    // the Settings-backed currentService() (default YouTube red) no matter
    // what was selected here. Storing the selection as the real
    // counterpart's name under the same Settings key the real app uses
    // fixes both: the choice survives navigation (and restarts), and
    // About/Settings brand consistently with the shell.
    DesktopNavigationShellContent(
        initialService = DummyService.entries.find { dummy ->
            dummy.realService.serviceName == settings.getString(
                KEY_STREAMING_SERVICE,
                Service.YOUTUBE.serviceName
            )
        } ?: DummyService.DUMMY_TUBE,
        onServiceSelected = { service ->
            settings.putString(KEY_STREAMING_SERVICE, service.realService.serviceName)
        },
        onNavigate = { navigator.navigateTo(it) }
    )
}

/**
 * Stateful shell content: dummy-service tab row, drawer, and placeholder page
 * @param initialService Mocked service selected when the shell (re)appears
 * @param onServiceSelected Callback when the user picks a different service
 * @param onNavigate Callback to navigate to a real (non-mocked) destination
 */
@Composable
internal fun DesktopNavigationShellContent(
    initialService: DummyService = DummyService.DUMMY_TUBE,
    onServiceSelected: (DummyService) -> Unit = {},
    onNavigate: (Destination) -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Bug fix: this was a plain remember, but NavDisplay only composes the
    // top backstack entry, so opening About/Settings unmounted the shell
    // and reset the tab to Featured on return. rememberSaveable survives
    // the round trip via NavDisplay's rememberSaveableStateHolderNavEntryDecorator.
    val selectedTab = rememberSaveable { mutableStateOf(DummyServiceTab.FEATURED) }
    // Non-null while a placeholder-only drawer destination is showing;
    // null means the dummy-service home (tab row) is showing.
    // Also fixes a bug where the top bar kept showing the last-selected
    // tab's title even after switching to an unrelated placeholder page
    // (e.g. title stayed "Featured" while viewing the Downloads placeholder).
    val placeholderLabel = remember { mutableStateOf<StringResource?>(null) }
    // The mocked service selected via the drawer-header switch control.
    // Branding follows this selection, so switching services visibly
    // recolors the top bar, tab row, and drawer header — same effect as
    // switching services in the real app. Plain remember is enough here
    // because initialService (from Settings) re-seeds it whenever the
    // shell re-enters the composition.
    val selectedService = remember { mutableStateOf(initialService) }
    // Whether the drawer body currently shows the service-selection menu
    // instead of the main menu; the header's switch control toggles between
    // the two, same as the real drawer
    val serviceMenuOpen = remember { mutableStateOf(false) }
    val darkTheme = isSystemInDarkTheme()
    val serviceScheme = if (darkTheme) {
        selectedService.value.darkScheme
    } else {
        selectedService.value.lightScheme
    }

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
            // Neutral gray sheet matching the real drawer menus' background
            // rather than the app theme's tinted surface color
            ModalDrawerSheet(
                drawerContainerColor = if (darkTheme) Color(0xFF2B2B2B) else Color(0xFFF5F5F5)
            ) {
                // Fixes a bug where the drawer content (header + up to 10
                // items) overflowed the default 800x600 window with no way
                // to scroll, silently clipping "About & FAQ" off the
                // bottom and making it unreachable
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DrawerHeader(
                        serviceScheme = serviceScheme,
                        selectedService = selectedService.value,
                        serviceMenuOpen = serviceMenuOpen.value,
                        onSwitcherClick = { serviceMenuOpen.value = !serviceMenuOpen.value }
                    )
                    if (serviceMenuOpen.value) {
                        // Service-selection menu: replaces the main menu
                        // (as in the real drawer), listing every mocked
                        // service with the current one highlighted
                        Column(modifier = Modifier.testTag(TEST_TAG_DRAWER_SERVICE_MENU)) {
                            DummyService.entries.forEach { service ->
                                val selected = service == selectedService.value
                                // As in the real service list, the current
                                // service is marked by its own brand color
                                // and bold text, not by a filled Material
                                // selection pill
                                val brandColor = (
                                    if (darkTheme) service.darkScheme else service.lightScheme
                                    ).primaryContainer
                                NavigationDrawerItem(
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .testTag(
                                            "$TEST_TAG_SERVICE_OPTION_PREFIX${service.name}"
                                        ),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = Color.Transparent,
                                        selectedIconColor = brandColor,
                                        selectedTextColor = brandColor
                                    ),
                                    icon = {
                                        Icon(
                                            painter = painterResource(service.icon),
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = service.serviceName,
                                            fontWeight = if (selected) FontWeight.Bold else null
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        selectedService.value = service
                                        onServiceSelected(service)
                                        serviceMenuOpen.value = false
                                    }
                                )
                            }
                        }
                    } else {
                        DrawerGroup(TEST_TAG_DRAWER_TOP_GROUP, topDrawerItems, onDrawerItemClick)
                        HorizontalDivider()
                        DrawerGroup(
                            TEST_TAG_DRAWER_DUMMY_SERVICE_GROUP,
                            kioskItemsFor(selectedService.value),
                            onDrawerItemClick
                        )
                        HorizontalDivider()
                        DrawerGroup(
                            TEST_TAG_DRAWER_BOTTOM_GROUP,
                            bottomDrawerItems,
                            onDrawerItemClick
                        )
                    }
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
                        IconButton(onClick = {
                            // Reopen on the main menu even if the drawer was
                            // dismissed while showing the service menu
                            serviceMenuOpen.value = false
                            scope.launch { drawerState.open() }
                        }) {
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

// Duplicates the two-row layout (app icon + name, then the service switch
// control) of app/src/main/res/layout/drawer_header.xml, done for
// expediency since Compose Multiplatform can't reuse an Android XML layout
// directly. The switch control has the same three components as Android's:
// service icon, service name, and drop-down selection arrow.
@Composable
private fun DrawerHeader(
    serviceScheme: ColorScheme,
    selectedService: DummyService,
    serviceMenuOpen: Boolean,
    onSwitcherClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_DRAWER_HEADER),
        color = serviceScheme.primaryContainer,
        contentColor = serviceScheme.onPrimaryContainer
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            ServiceSwitcher(
                modifier = Modifier.padding(top = 20.dp),
                selectedService = selectedService,
                serviceMenuOpen = serviceMenuOpen,
                onClick = onSwitcherClick
            )
        }
    }
}

// Mirrors the bottom row of drawer_header.xml: service icon + name centered
// (weighted) with the ic_arrow_drop_down selection arrow at the end, over
// the bottom scrim gradient of drawer_header_bottom_background.xml. As in
// the real drawer, clicking it toggles the drawer body between the main
// menu and the service-selection menu, flipping the arrow while the
// service menu is showing.
@Composable
private fun ServiceSwitcher(
    modifier: Modifier = Modifier,
    selectedService: DummyService,
    serviceMenuOpen: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TEST_TAG_SERVICE_SWITCHER)
            .clickable(onClick = onClick)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                )
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(selectedService.icon),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = selectedService.serviceName,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(
            modifier = Modifier
                .size(24.dp)
                .rotate(if (serviceMenuOpen) 180f else 0f),
            painter = painterResource(Res.drawable.ic_arrow_drop_down),
            contentDescription = null
        )
    }
}
