package com.nantcompany.clipy.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.nantcompany.clipy.design.ClipyPrimaryButton
import com.nantcompany.clipy.design.ClipySecondaryButton
import com.nantcompany.clipy.design.ClipyScaffold
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

    ClipyScaffold(showTopBar = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Welcome to Clipy", style = MaterialTheme.typography.headlineMedium, color = Color.White)

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(ClipyDesignTokens.primaryAccent.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = page.iconText,
                    fontSize = 48.sp,
                    color = ClipyDesignTokens.primaryAccent
                )
            }
            
            Text(page.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(page.description, style = MaterialTheme.typography.bodyLarge, color = ClipyDesignTokens.secondaryText)

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                pages.indices.forEach { index ->
                    Spacer(
                        modifier = Modifier
                            .height(6.dp)
                            .weight(1f)
                            .background(
                                color = if (index == pageIndex) ClipyDesignTokens.primaryAccent else Color(0xFF2A3342),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            ClipyPrimaryButton(
                label = if (isLast) "Get Started" else "Next",
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = {
                    if (isLast) onFinish() else pageIndex += 1
                }
            )

            ClipySecondaryButton(
                label = "Skip",
                modifier = Modifier.fillMaxWidth().height(56.dp),
                onClick = onFinish
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
