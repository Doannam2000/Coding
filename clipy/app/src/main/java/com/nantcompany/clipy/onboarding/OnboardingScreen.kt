package com.nantcompany.clipy.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.theme.ClipyDesignTokens

data class OnboardingPage(
    val title: String,
    val description: String,
    val iconText: String
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage("Cut and compress quickly", "Trim clips and reduce file size in seconds.", "✂"),
            OnboardingPage("Merge and extract audio", "Join videos or save audio tracks from videos.", "♫"),
            OnboardingPage("Create photo slideshows", "Turn selected images into shareable videos.", "▦")
        )
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLast = pageIndex == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to Clipy", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = page.iconText,
            style = MaterialTheme.typography.headlineLarge,
            color = ClipyDesignTokens.primaryAccent
        )
        Text(page.title, style = MaterialTheme.typography.titleLarge)
        Text(page.description, style = MaterialTheme.typography.bodyLarge, color = ClipyDesignTokens.secondaryText)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            pages.indices.forEach { index ->
                Spacer(
                    modifier = Modifier
                        .height(8.dp)
                        .weight(1f)
                        .background(
                            color = if (index == pageIndex) ClipyDesignTokens.primaryAccent else Color(0xFF2A3342),
                            shape = CircleShape
                        )
                )
            }
        }

        Text("${pageIndex + 1}/${pages.size}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.weight(1f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (isLast) onFinish() else pageIndex += 1
            }
        ) {
            Text(if (isLast) "Get Started" else "Next")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onFinish
        ) {
            Text("Skip")
        }
    }
}
