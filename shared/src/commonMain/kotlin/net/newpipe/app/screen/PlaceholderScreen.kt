/*
* SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package net.newpipe.app.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.newpipe.app.LocalSystemBack
import net.newpipe.app.composable.EmptyPlaceholder
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.navigation.Navigator
import org.koin.compose.koinInject

/**
 * Generic, single, reusable placeholder for any destination whose real
 * screen isn't implemented yet (e.g. settings categories, registered under
 * [net.newpipe.app.navigation.Destination.Placeholder]). Being on the real
 * navigation backstack, unlike the desktop shell's local-state mocks, it
 * gets a proper up button and (on desktop) the system-back control for free.
 */
@Composable
fun PlaceholderScreen(
    onSystemBack: (() -> Unit)? = LocalSystemBack.current,
    navigator: Navigator = koinInject()
) {
    PlaceholderScreenContent(
        onNavigateUp = { navigator.navigateUp() },
        onSystemBack = onSystemBack
    )
}

// Stateless content split out for testing, matching AboutScreen/
// AboutScreenContent and SettingsHomeScreen/SettingsHomeScreenContent
@Composable
fun PlaceholderScreenContent(
    onNavigateUp: () -> Unit = {},
    onSystemBack: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(onNavigateUp = onNavigateUp, onSystemBack = onSystemBack)
        }
    ) { paddingValues ->
        EmptyPlaceholder(modifier = Modifier.padding(paddingValues))
    }
}
