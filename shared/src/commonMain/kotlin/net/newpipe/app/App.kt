/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import net.newpipe.app.di.KoinApp
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.NavDisplay
import net.newpipe.app.navigation.navModule
import net.newpipe.app.theme.AppTheme
import net.newpipe.app.theme.Service
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

val LocalSystemBack = staticCompositionLocalOf<(() -> Unit)?> { null }

// Non-null only on platforms that also host the classic (non-Compose)
// interface: invoked when the user picks a real service in the dummy
// shell's service menu, after the selection has been persisted, so the
// platform can hand the screen back to that interface (Android finishes
// ComposeActivity). Null hides the real services from the shell's menu.
// Handoff support facilitated by Claude Code using model Claude Fable 5.
val LocalRealServiceHandoff = staticCompositionLocalOf<((Service) -> Unit)?> { null }

/**
 * Entry point for the multiplatform compose application
 * @param startDestination Starting destination for the app; defaults to about
 * @param onCloseRequest Callback to close the app
 * @param onSystemBack Callback to dispatch a platform-specific back action
 * @param onRealServiceSelected Callback to hand a real service over to the platform's
 * classic interface, see [LocalRealServiceHandoff]
 * @param withKoin Additional logic to execute after initialising Koin and setting content
 */
@Composable
fun App(
    startDestination: Destination = Destination.About,
    onCloseRequest: () -> Unit,
    onSystemBack: (() -> Unit)? = null,
    onRealServiceSelected: ((Service) -> Unit)? = null,
    withKoin: @Composable () -> Unit = {}
) {
    KoinApplication(
        configuration = koinConfiguration<KoinApp>(
            appDeclaration = {
                modules(navModule())
            }
        )
    ) {
        CompositionLocalProvider(
            LocalSystemBack provides onSystemBack,
            LocalRealServiceHandoff provides onRealServiceSelected
        ) {
            AppTheme {
                NavDisplay(
                    startDestination = startDestination,
                    onCloseRequest = onCloseRequest
                )
                withKoin()
            }
        }
    }
}
