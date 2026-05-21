package com.nantcompany.clipy.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nantcompany.clipy.design.ClipyThemeBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinish: (Boolean) -> Unit) {
    val context = LocalContext.current
    val onboardingStore = remember { OnboardingStateStore(context) }

    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds delay
        onFinish(onboardingStore.isCompleted())
    }

    ClipyThemeBackground {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color = Color(0xFF87B5FF), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("C", style = MaterialTheme.typography.headlineLarge, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Clipy",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The ultimate video companion",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFBAC6D7)
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(color = Color(0xFF87B5FF), modifier = Modifier.size(32.dp))
        }
    }
}
