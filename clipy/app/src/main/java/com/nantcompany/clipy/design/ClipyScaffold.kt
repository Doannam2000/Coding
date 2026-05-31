package com.nantcompany.clipy.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ClipyScaffold(
    title: String = "",
    onBackClick: (() -> Unit)? = null,
    showTopBar: Boolean = true,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (showTopBar) {
                ClipyTopBar(
                    title = title,
                    onBackClick = onBackClick ?: {},
                    actions = actions
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    }
}
