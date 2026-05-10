package com.nantcompany.clipy.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color = Color(0xFF6EA8FF), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("C", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF0B0F14))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Clipy",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Fast video tools",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFB7C4D6)
        )
        Spacer(modifier = Modifier.height(18.dp))
        CircularProgressIndicator(color = Color(0xFF6EA8FF))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Loading...", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB7C4D6))
    }
}
