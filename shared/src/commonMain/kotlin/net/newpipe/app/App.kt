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
import org.koin.compose.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

val LocalSystemBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Entry point for the multiplatform compose application
 * @param startDestination Starting destination for the app; defaults to about
 * @param onCloseRequest Callback to close the app
 * @param onSystemBack Callback to dispatch a platform-specific back action
 * @param withKoin Additional logic to execute after initialising Koin and setting content
 */
@Composable
fun App(
    startDestination: Destination = Destination.About,
    onCloseRequest: () -> Unit,
    onSystemBack: (() -> Unit)? = null,
    withKoin: @Composable () -> Unit = {}
) {
    KoinApplication(
        configuration = koinConfiguration<KoinApp>(
            appDeclaration = {
                modules(navModule())
            }
        )
    ) {
        CompositionLocalProvider(LocalSystemBack provides onSystemBack) {
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
