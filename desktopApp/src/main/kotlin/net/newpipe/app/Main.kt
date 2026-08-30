/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import net.newpipe.app.navigation.Destination

// Base font size adjustment: Compose Desktop renders the default Material
// text sizes noticeably larger than typical desktop UI text (body text
// comes out around 18pt); scaling sp by 12/18 brings it down to ~12pt.
// Applied via LocalDensity's fontScale so only text shrinks (like changing
// a browser's base font size) while dp-based layout is untouched.
private const val FONT_SCALE = 12f / 18f

/**
 * Entry point for compose-related UI components on Desktop
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "NewPipe") {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * FONT_SCALE)
        ) {
            App(
                startDestination = Destination.DummyHome,
                onCloseRequest = ::exitApplication,
                onSystemBack = ::exitApplication
            )
        }
    }
}
