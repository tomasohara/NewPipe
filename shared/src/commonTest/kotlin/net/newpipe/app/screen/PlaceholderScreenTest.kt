/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

// Test facilitated by Claude Code using model Fable 5.

package net.newpipe.app.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertTrue
import net.newpipe.app.extensions.withKoin
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.navigate_back
import newpipe.shared.generated.resources.nothing_here_but_crickets
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

@OptIn(ExperimentalTestApi::class)
class PlaceholderScreenTest {

    // TopAppBar's default colors read currentServiceScheme(), which
    // injects Settings via Koin, so a real singleton is required here too
    private val emptySettings = module {
        single<Settings> { MapSettings() }
    }

    @Test
    fun showsEmptyStateAndNavigatesUp() = runComposeUiTest {
        var navigatedUp = false
        withKoin(
            modules = listOf(emptySettings),
            content = {
                MaterialTheme {
                    PlaceholderScreenContent(onNavigateUp = { navigatedUp = true })
                }
            },
            onContent = {
                onNodeWithText(getString(Res.string.nothing_here_but_crickets)).assertIsDisplayed()

                onNodeWithContentDescription(getString(Res.string.navigate_back)).performClick()
                assertTrue(navigatedUp)
            }
        )
    }
}
