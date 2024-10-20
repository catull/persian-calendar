package com.byagowi.persiancalendar.ui.common

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.SHARED_CONTENT_KEY_THREE_DOTS_MENU

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ThreeDotsDropdownMenu(
    animatedContentScope: AnimatedContentScope,
    content: @Composable ColumnScope.(onDismissRequest: () -> Unit) -> Unit,
) {
    Box(
        Modifier.sharedElement(
            rememberSharedContentState(key = SHARED_CONTENT_KEY_THREE_DOTS_MENU),
            animatedVisibilityScope = animatedContentScope,
        )
    ) {
        var showMenu by rememberSaveable { mutableStateOf(false) }
        AppIconButton(
            icon = Icons.Default.MoreVert,
            title = stringResource(R.string.more_options),
        ) { showMenu = !showMenu }
        AppDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            content = content,
        )
    }
}
