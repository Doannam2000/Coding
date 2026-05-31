package com.nantcompany.clipy.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nantcompany.clipy.theme.ClipyDesignTokens

@Composable
fun ClipyThemeBackground(
    content: @Composable () -> Unit
) {
    val bg = Brush.verticalGradient(
        listOf(
            Color(0xFF0A0A12),
            Color(0xFF090D1D),
            Color(0xFF020617)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Neon Purple Glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(colors = listOf(Color(0x55D8B4FE), Color.Transparent)),
                    CircleShape
                )
        )
        // Neon Cyan Glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(colors = listOf(Color(0x4467E8F9), Color.Transparent)),
                    CircleShape
                )
        )
        
        content()
    }
}

@Composable
fun ClipySectionTitle(text: String) {
    Text(
        text = text, 
        style = MaterialTheme.typography.titleMedium, 
        color = Color.White,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun ClipyPrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick() 
        }, 
        enabled = enabled, 
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ClipyDesignTokens.primaryAccent,
            contentColor = Color.Black,
            disabledContainerColor = ClipyDesignTokens.primaryAccent.copy(alpha = 0.3f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        )
    ) { 
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) 
    }
}

@Composable
fun ClipySecondaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    OutlinedButton(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick() 
        }, 
        enabled = enabled, 
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.primaryAccent.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ClipyDesignTokens.primaryAccent,
            disabledContentColor = ClipyDesignTokens.primaryAccent.copy(alpha = 0.3f)
        )
    ) { 
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp) 
    }
}

@Composable
fun ClipyToolCard(
    title: String,
    description: String,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier.fillMaxWidth().clickable { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick() 
        },
        shape = RoundedCornerShape(ClipyDesignTokens.toolCardCorner),
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = ClipyDesignTokens.primaryAccent.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = ClipyDesignTokens.primaryAccent, modifier = Modifier.size(24.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = ClipyDesignTokens.secondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ClipyEmptyState(
    title: String, 
    message: String, 
    modifier: Modifier = Modifier,
    actionLabel: String? = null, 
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.Info
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ClipyDesignTokens.cardSurface), 
        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
        border = androidx.compose.foundation.BorderStroke(1.dp, ClipyDesignTokens.cardBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = CircleShape,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ClipyDesignTokens.secondaryText, modifier = Modifier.size(40.dp))
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.secondaryText, textAlign = TextAlign.Center, lineHeight = 22.sp)
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ClipyPrimaryButton(actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ClipyErrorState(message: String, modifier: Modifier = Modifier, retryLabel: String = "Retry", onRetry: (() -> Unit)? = null) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x33FF4B4B)), 
        shape = RoundedCornerShape(ClipyDesignTokens.cardCorner),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FF4B4B))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFFF4B4B))
                Text("Error", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFF4B4B), fontWeight = FontWeight.Bold)
            }
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFCA5A5))
            if (onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4B4B), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(retryLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ClipyLoadingState(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(), 
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ClipyDesignTokens.primaryAccent)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.secondaryText)
    }
}

@Composable
fun ClipyBottomActionBar(primaryLabel: String, onPrimary: () -> Unit, secondaryLabel: String? = null, onSecondary: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (secondaryLabel != null && onSecondary != null) {
            ClipySecondaryButton(label = secondaryLabel, onClick = onSecondary)
        }
        ClipyPrimaryButton(label = primaryLabel, onClick = onPrimary)
    }
}

@Composable
fun ClipyConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        titleContentColor = Color.White,
        textContentColor = ClipyDesignTokens.secondaryText,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { 
            TextButton(onClick = onConfirm) { 
                Text(confirmLabel, color = ClipyDesignTokens.primaryAccent, fontWeight = FontWeight.Bold) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text(dismissLabel, color = Color.White) 
            } 
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipyTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = { actions() },
        windowInsets = WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

@Composable
fun ClipyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = ClipyDesignTokens.secondaryText) },
        leadingIcon = leadingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1E293B),
            unfocusedContainerColor = Color(0xFF1E293B),
            focusedIndicatorColor = ClipyDesignTokens.primaryAccent,
            unfocusedIndicatorColor = Color(0xFF334155),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}
