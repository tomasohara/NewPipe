/*
* SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package net.newpipe.app.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import net.newpipe.app.preview.ThemePreviewProvider
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.nothing_here_but_crickets
import org.jetbrains.compose.resources.stringResource

/**
 * Empty-state content for a destination with nothing to show yet. Shared by
 * DesktopNavigationShell's mocked pages and PlaceholderScreen so both draw
 * from one implementation.
 *
 * Duplicates (for expediency, since there is no shared cross-platform
 * layout to reuse instead) the layout and copy of the real Android
 * empty-list view at app/src/main/res/layout/list_empty_view.xml.
 *
 * Bug fix: the original prototype's placeholder was a single line,
 * "Nothing here but crickets." (with a trailing period, no kaomoji),
 * unlike the Android original. This version matches Android's copy and
 * two-line layout exactly.
 * @param modifier Modifier applied to the enclosing box
 */
@Composable
fun EmptyPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Kaomoji is hardcoded (not translated), same as the Android view
            Text(text = "¯\\_(ツ)_/¯", style = MaterialTheme.typography.headlineSmall)
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = stringResource(Res.string.nothing_here_but_crickets),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun EmptyPlaceholderPreview() {
    EmptyPlaceholder()
}
